package dev.alucady.combatedirecional.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.alucady.combatedirecional.config.CombatConfig;
import dev.alucady.combatedirecional.hud.CombatHud;
import dev.alucady.combatedirecional.npc.DuelistBrain;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Roda a cada tick do mundo para todo jogador e todo NPC duelista:
 * - avança os timers (preparação -> impacto, recuperação, atordoamento, esquiva)
 * - lê o "flick" do mouse para trocar a guarda
 * - regenera stamina / controla exaustão
 * - atualiza HUD, barras do jogo e placa de nome do duelista
 * - roda a IA do duelista
 */
public final class CombatTickSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, Combatant> combatantType;
    private final Query<EntityStore> query;

    public CombatTickSystem(ComponentType<EntityStore, Combatant> combatantType) {
        this.combatantType = combatantType;
        this.query = Query.or(Player.getComponentType(), combatantType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Combatant c = chunk.getComponent(index, this.combatantType);
        if (c == null) {
            // Jogador novo: ganha um estado de combate.
            cb.putComponent(ref, this.combatantType, new Combatant());
            return;
        }
        CombatRules.initResources(c);
        CombatConfig cfg = CombatRules.cfg();
        long now = CombatRules.now();
        boolean isPlayer = chunk.getComponent(index, Player.getComponentType()) != null;

        if (!c.ai) {
            updateMovement(ref, c, cb, dt);
        }
        if (isPlayer) {
            updateFlick(ref, c, cb, now, cfg);
        }

        CombatRules.applyPendingGuard(c, now);
        CombatRules.resolveGrab(ref, c, cb, now);

        // ---------------------------------------------------------- máquina de estados
        switch (c.state) {
            case WINDUP -> {
                if (c.ai && c.aiFeintAt > 0 && now >= c.aiFeintAt) {
                    c.aiFeintAt = 0;
                    CombatRules.handleInput(ref, CombatRules.Input.GUARD_BREAK, cb); // finta
                } else if (now >= c.impactAt) {
                    CombatRules.resolveImpact(ref, c, cb, now);
                }
            }
            case ROLL -> {
                if (now >= c.stateEnd) {
                    c.setState(Combatant.State.RECOVERY, now, cfg.roll.recoveryMs); // brecha no fim do rolamento
                }
            }
            case DODGE, RECOVERY, STUNNED -> {
                if (now >= c.stateEnd) {
                    c.setState(Combatant.State.IDLE, now, 0);
                }
            }
            case IDLE -> { }
        }

        // ---------------------------------------------------------- stamina
        boolean acting = c.state == Combatant.State.WINDUP || c.state == Combatant.State.DODGE || c.state == Combatant.State.ROLL;
        boolean canRegen = !acting
                && now - c.lastSpendAt >= cfg.stamina.regenDelayMs
                && (!cfg.stamina.regenOnlyWhenStill || !c.moving);
        if (canRegen && c.stamina < cfg.stamina.max) {
            c.stamina = Math.min(cfg.stamina.max, c.stamina + cfg.stamina.regenPerSecond * dt);
        }
        if (c.exhausted && c.stamina >= cfg.stamina.exhaustRecoverAt) {
            c.exhausted = false;
        }

        // ---------------------------------------------------------- IA
        if (c.ai) {
            DuelistBrain.tick(ref, c, cb, now, dt);
        }

        // ---------------------------------------------------------- visual
        mirrorBars(ref, c, cb, cfg);
        if (isPlayer) {
            updateHud(ref, c, cb, now, cfg);
        } else if (c.ai && cfg.feel.showNameplates) {
            updateNameplate(ref, c, cb, now);
        }
    }

    // ===================================================================== movimento

    private static void updateMovement(Ref<EntityStore> ref, Combatant c, CommandBuffer<EntityStore> cb, float dt) {
        TransformComponent t = cb.getComponent(ref, TransformComponent.getComponentType());
        if (t == null || dt <= 0) return;
        Vector3d p = t.getPosition();
        if (c.hasLastPos) {
            double vx = (p.x - c.lastX) / dt;
            double vz = (p.z - c.lastZ) / dt;
            if (Math.hypot(vx, vz) > 40) { // teleporte: ignora
                vx = 0;
                vz = 0;
            }
            c.velX = c.velX * 0.5 + vx * 0.5;
            c.velZ = c.velZ * 0.5 + vz * 0.5;
            c.moving = Math.hypot(c.velX, c.velZ) > 0.6;
        }
        c.lastX = p.x;
        c.lastY = p.y;
        c.lastZ = p.z;
        c.hasLastPos = true;
    }

    // ===================================================================== guarda pelo mouse ("flick")

    private static void updateFlick(Ref<EntityStore> ref, Combatant c, CommandBuffer<EntityStore> cb, long now, CombatConfig cfg) {
        HeadRotation head = cb.getComponent(ref, HeadRotation.getComponentType());
        if (head == null) return;
        float yaw = head.getRotation().yaw();
        float pitch = head.getRotation().pitch();

        int n = c.sampleTime.length;
        c.sampleHead = (c.sampleHead + 1) % n;
        c.sampleTime[c.sampleHead] = now;
        c.sampleYaw[c.sampleHead] = yaw;
        c.samplePitch[c.sampleHead] = pitch;
        c.sampleCount = Math.min(n, c.sampleCount + 1);

        if (cfg.controls.guardOnlyWhileCrouching) {
            MovementStates ms = CombatRules.movementStates(ref, cb);
            if (ms == null || !ms.crouching) return;
        }

        // amostra mais antiga dentro da janela de tempo
        int oldest = -1;
        for (int i = 1; i < c.sampleCount; i++) {
            int idx = Math.floorMod(c.sampleHead - i, n);
            if (now - c.sampleTime[idx] > cfg.controls.flickWindowMs) break;
            oldest = idx;
        }
        if (oldest < 0) return;

        double dyaw = Math.toDegrees(wrap(yaw - c.sampleYaw[oldest]));
        double dpitch = Math.toDegrees(pitch - c.samplePitch[oldest]);

        Direction dir = null;
        if (Math.abs(dyaw) >= cfg.controls.flickYawDegrees && Math.abs(dyaw) >= Math.abs(dpitch)) {
            // yaw aumentando = virando para a ESQUERDA (convenção do Hytale)
            dir = dyaw > 0 ? Direction.LEFT : Direction.RIGHT;
            if (cfg.controls.invertYaw) dir = dir.mirrored();
        } else if (dpitch >= cfg.controls.flickPitchDegrees) {
            dir = Direction.TOP;
        }
        if (dir != null) {
            CombatRules.setGuard(ref, c, dir, cb, now);
            c.sampleCount = 0; // evita disparar de novo com o mesmo movimento
        }
    }

    private static double wrap(double a) {
        while (a > Math.PI) a -= Math.PI * 2;
        while (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    // ===================================================================== barras do jogo

    private static void mirrorBars(Ref<EntityStore> ref, Combatant c, CommandBuffer<EntityStore> cb, CombatConfig cfg) {
        EntityStatMap stats = cb.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return;
        if (cfg.stamina.mirrorToVanillaBar) {
            float v = mirror(stats, DefaultEntityStatTypes.getStamina(), c.stamina / cfg.stamina.max, c.lastMirroredStamina);
            if (v >= 0) c.lastMirroredStamina = v;
        }
        if (cfg.mana.mirrorToVanillaBar) {
            float v = mirror(stats, DefaultEntityStatTypes.getMana(), c.mana / cfg.mana.max, c.lastMirroredMana);
            if (v >= 0) c.lastMirroredMana = v;
        }
    }

    private static float mirror(EntityStatMap stats, int index, double fraction, float last) {
        if (index < 0) return -1;
        EntityStatValue value = stats.get(index);
        if (value == null || value.getMax() <= 0) return -1;
        float target = (float) (Math.max(0, Math.min(1, fraction)) * value.getMax());
        if (Math.abs(target - value.get()) < 0.05f && Math.abs(target - last) < 0.05f) return last;
        stats.setStatValue(index, target);
        return target;
    }

    // ===================================================================== HUD do jogador

    private static void updateHud(Ref<EntityStore> ref, Combatant c, CommandBuffer<EntityStore> cb, long now, CombatConfig cfg) {
        Player player = cb.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = cb.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) return;

        boolean want = cfg.hud.enabled && c.hudWanted;
        if (!want) {
            if (c.hud != null) {
                player.getHudManager().removeCustomHud(playerRef, CombatHud.KEY);
                c.hud = null;
            }
            return;
        }
        if (c.hud == null) {
            c.hud = new CombatHud(playerRef);
            player.getHudManager().addCustomHud(playerRef, c.hud);
        }
        if (now < c.nextHudUpdate) return;
        c.nextHudUpdate = now + 100;

        // linha 1: minha guarda + guarda do inimigo à frente
        StringBuilder guard = new StringBuilder("GUARDA " + c.guard.arrow + " " + c.guard.label);
        if (c.pendingGuard != null) guard.append(" → ").append(c.pendingGuard.arrow);
        Ref<EntityStore> foe = CombatRules.acquireTarget(ref, cb, cfg.reach.lockOnRange, 60);
        if (foe != null) {
            Combatant f = cb.getComponent(foe, CombatRules.type());
            if (f != null) {
                Direction blocks = cfg.controls.mirrorSides ? f.guard.mirrored() : f.guard;
                guard.append("   |   Inimigo bloqueia ").append(blocks.arrow).append(' ').append(blocks.label);
                if (f.state == Combatant.State.STUNNED && now < f.stateEnd) guard.append("  (ATORDOADO!)");
            }
        }

        // linha 2: recursos
        StringBuilder stats = new StringBuilder();
        stats.append("STAMINA ").append(Math.round(c.stamina)).append('/').append(Math.round(cfg.stamina.max));
        stats.append("   |   MANA ").append(Math.round(c.mana)).append('/').append(Math.round(cfg.mana.max));
        if (c.exhausted) stats.append("   |   EXAUSTO");
        if (c.isFury(now)) stats.append("   |   FÚRIA ").append((c.furyUntil - now) / 1000 + 1).append('s');
        stats.append("   |   Perfil ").append(cfg.profile);
        if (c.mana >= cfg.mana.furyCost) stats.append("   |   Fúria pronta (agachar + quebra)");
        else if (c.mana >= cfg.mana.arcaneStrikeCost) stats.append("   |   Arcano pronto (agachar + pesado)");

        // linha 3: golpe chegando
        String incoming = c.isGrabbed(now) ? "⚠ AGARRADO! Aperte QUEBRA para escapar" : "";
        List<Ref<EntityStore>> attackers = CombatRules.incomingAttackers(ref, cb);
        long bestRemaining = Long.MAX_VALUE;
        for (Ref<EntityStore> aRef : attackers) {
            Combatant a = cb.getComponent(aRef, CombatRules.type());
            if (a == null || a.attackKind == null || a.attackDir == null) continue;
            long remaining = a.impactAt - now;
            if (remaining >= bestRemaining) continue;
            bestRemaining = remaining;
            if (a.attackKind == AttackKind.GUARD_BREAK) {
                incoming = "⚠ QUEBRA DE GUARDA! Aperte QUEBRA para contra-quebrar";
            } else {
                Direction req = CombatRules.requiredGuard(a.attackDir);
                boolean parryNow = remaining <= cfg.parry.windowMs && remaining >= cfg.parry.windowEndMs;
                if (a.attackKind == AttackKind.ARCANE) {
                    incoming = "⚠ ARCANO (imbloqueável) " + req.arrow + " — esquive ou apare" + (parryNow ? "  [APARE!]" : "");
                } else {
                    incoming = "⚠ " + a.attackKind.label + " — bloqueie " + req.arrow + " " + req.label
                            + (parryNow && a.attackKind.isStrike() ? "  [APARE AGORA!]" : "");
                }
            }
        }

        // linha 4: meu estado
        String state = switch (c.state) {
            case IDLE -> "Em guarda";
            case WINDUP -> "Preparando " + (c.attackKind == null ? "" : c.attackKind.label) + " "
                    + (c.attackDir == null ? "" : c.attackDir.arrow)
                    + (c.attackKind != null && c.attackKind.isHeavyLike() && c.impactAt - now > cfg.heavyAttack.feintCutoffMs
                    ? "  (QUEBRA = fintar)" : "");
            case RECOVERY -> "Recuperando";
            case STUNNED -> "ATORDOADO";
            case DODGE -> c.isInvulnerable(now) ? "Esquiva (invencível)" : "Esquiva";
            case ROLL -> c.isInvulnerable(now) ? "Rolamento (invencível)" : "Rolamento (vulnerável)";
        };

        c.hud.setLines(guard.toString(), stats.toString(), incoming, state);
    }

    // ===================================================================== placa de nome do duelista

    private static void updateNameplate(Ref<EntityStore> ref, Combatant c, CommandBuffer<EntityStore> cb, long now) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.displayName.isEmpty() ? "Duelista" : c.displayName);
        Direction blocks = CombatRules.cfg().controls.mirrorSides ? c.guard.mirrored() : c.guard;
        sb.append("  [guarda ").append(blocks.arrow).append(']');
        if (c.state == Combatant.State.WINDUP && c.attackKind != null && c.attackDir != null) {
            Direction req = CombatRules.requiredGuard(c.attackDir);
            sb.append("  ⚠").append(c.attackKind.label).append(' ').append(req.arrow);
        } else if (c.state == Combatant.State.STUNNED && now < c.stateEnd) {
            sb.append("  ✖ ATORDOADO");
        }
        if (c.exhausted) sb.append("  (exausto)");
        if (c.isFury(now)) sb.append("  FÚRIA");
        String text = sb.toString();
        if (text.equals(c.lastNameplate)) return;
        c.lastNameplate = text;
        Nameplate np = cb.getComponent(ref, Nameplate.getComponentType());
        if (np == null) {
            cb.putComponent(ref, Nameplate.getComponentType(), new Nameplate(text));
        } else {
            np.setText(text);
        }
    }
}
