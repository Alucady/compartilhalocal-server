package dev.alucady.combatedirecional.npc;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import dev.alucady.combatedirecional.combat.AttackKind;
import dev.alucady.combatedirecional.combat.CombatRules;
import dev.alucady.combatedirecional.combat.Combatant;
import dev.alucady.combatedirecional.combat.Direction;
import dev.alucady.combatedirecional.config.CombatConfig;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IA simples do NPC duelista de treino.
 *
 * O NPC usa as MESMAS regras do jogador (CombatRules): mesma stamina, mesmos
 * tempos, pode ser aparado, quebrado, fintado... A IA só decide "o que apertar".
 *
 * Defesa: quando você começa um golpe contra ele, ele "percebe" depois de um
 *         tempo de reação humano e pode: virar a guarda, aparar, esquivar ou
 *         contra-quebrar (chances no CombatConfig -> Duelist).
 * Ataque: de tempos em tempos escolhe leve/pesado/quebra, às vezes finta, e
 *         prefere atacar pelo lado que você NÃO está defendendo.
 */
public final class DuelistBrain {

    /** Duelistas vivos (para o comando /duelista remover). */
    public static final Set<Ref<EntityStore>> ACTIVE = ConcurrentHashMap.newKeySet();

    private static final int R_NONE = 0, R_GUARD = 1, R_PARRY = 2, R_DODGE = 3, R_COUNTER = 4;

    private DuelistBrain() {
    }

    private static CombatConfig cfg() {
        return CombateDirecionalPlugin.get().combat();
    }

    private static int rand(int min, int max) {
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static double roll() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static void tick(Ref<EntityStore> me, Combatant c, ComponentAccessor<EntityStore> cb, long now, float dt) {
        ACTIVE.add(me);
        CombatConfig cfg = cfg();
        CombatConfig.Duelist d = cfg.duelist;

        TransformComponent myT = cb.getComponent(me, TransformComponent.getComponentType());
        if (myT == null) return;
        Vector3d myPos = new Vector3d(myT.getPosition());

        Ref<EntityStore> foe = nearestPlayer(me, myPos, cb, 16);
        if (foe == null) {
            c.moving = false;
            return;
        }
        TransformComponent foeT = cb.getComponent(foe, TransformComponent.getComponentType());
        if (foeT == null) return;
        Vector3d foePos = foeT.getPosition();
        double dx = foePos.x - myPos.x;
        double dz = foePos.z - myPos.z;
        double dist = Math.hypot(dx, dz);

        boolean stunned = c.state == Combatant.State.STUNNED && now < c.stateEnd;

        // ---------------------------------------------------------- olhar para o jogador
        if (!stunned && dist > 0.01) {
            float yaw = (float) Math.atan2(-dx, -dz); // inverso de forward = (-sin(yaw), -cos(yaw))
            Rotation3f rot = new Rotation3f(0f, yaw, 0f);
            myT.setRotation(rot);
            HeadRotation head = cb.getComponent(me, HeadRotation.getComponentType());
            if (head != null) head.setRotation(rot);
        }

        // ---------------------------------------------------------- andar (aproximar / manter distância)
        c.moving = false;
        boolean free = c.state == Combatant.State.IDLE
                || (c.state == Combatant.State.RECOVERY && now >= c.stateEnd);
        if (free && dist > 0.01) {
            double step = 0;
            if (dist > d.preferredDistance + 0.4) {
                step = Math.min(d.walkSpeed * dt, dist - d.preferredDistance);
            } else if (dist < d.preferredDistance - 0.8) {
                step = -d.walkSpeed * 0.5 * dt;
            }
            if (step != 0) {
                myPos.x += dx / dist * step;
                myPos.z += dz / dist * step;
                myT.setPosition(myPos);
                c.moving = true;
            }
        }

        Combatant o = cb.getComponent(foe, CombateDirecionalPlugin.get().combatantType());

        // ---------------------------------------------------------- perceber golpe do jogador
        if (o != null && o.state == Combatant.State.WINDUP && me.equals(o.target)
                && o.attackSerial != c.aiReactedToSerial && o.attackKind != null && o.attackDir != null) {
            c.aiReactedToSerial = o.attackSerial;
            planReaction(c, o, now, cfg);
        }

        // ---------------------------------------------------------- executar reação planejada
        if (c.aiReaction != R_NONE && now >= c.aiReactionAt) {
            int r = c.aiReaction;
            c.aiReaction = R_NONE;
            if (c.aiReactionGuard != null) {
                CombatRules.setGuard(me, c, c.aiReactionGuard, cb, now);
            }
            switch (r) {
                case R_PARRY -> CombatRules.handleInput(me, CombatRules.Input.HEAVY, cb);
                case R_DODGE -> CombatRules.startDodge(me, c, cb, now, false);
                case R_COUNTER -> CombatRules.handleInput(me, CombatRules.Input.GUARD_BREAK, cb);
                default -> { }
            }
        }

        // ---------------------------------------------------------- atacar
        if (stunned || c.aiReaction != R_NONE || !c.canAct(now) || now < c.aiNextThinkAt) return;
        if (dist > cfg.reach.range + 0.2) return;
        c.aiNextThinkAt = now + rand(d.thinkMinMs, d.thinkMaxMs);

        if (c.mana >= cfg.mana.furyCost && roll() < 0.5) {
            CombatRules.activateFury(me, c, cb, now);
            return;
        }

        boolean foeStunned = o != null && o.state == Combatant.State.STUNNED && now < o.stateEnd;
        AttackKind kind;
        double r = roll();
        if (foeStunned) {
            kind = AttackKind.HEAVY; // punir!
        } else if (r < d.guardBreakChance) {
            kind = AttackKind.GUARD_BREAK;
        } else if (r < d.guardBreakChance + d.heavyChance) {
            kind = (c.mana >= cfg.mana.arcaneStrikeCost && roll() < 0.3) ? AttackKind.ARCANE : AttackKind.HEAVY;
        } else {
            kind = AttackKind.LIGHT;
        }

        // escolher o lado: de preferência um que o jogador NÃO está defendendo
        Direction dir = Direction.values()[rand(0, 2)];
        if (o != null && roll() < 0.6) {
            for (int i = 0; i < 3 && CombatRules.requiredGuard(dir) == o.guard; i++) {
                dir = Direction.values()[(dir.ordinal() + 1) % 3];
            }
        }
        if (kind != AttackKind.GUARD_BREAK) {
            CombatRules.setGuard(me, c, dir, cb, now);
        }
        CombatRules.startAttack(me, c, kind, cb, now);

        // finta ocasional no pesado
        if (kind == AttackKind.HEAVY && c.state == Combatant.State.WINDUP && roll() < d.feintChance) {
            long windup = c.impactAt - now;
            c.aiFeintAt = now + (long) (windup * (0.35 + 0.3 * roll()));
            if (c.impactAt - c.aiFeintAt <= cfg.heavyAttack.feintCutoffMs) c.aiFeintAt = 0;
        }
    }

    private static void planReaction(Combatant c, Combatant o, long now, CombatConfig cfg) {
        CombatConfig.Duelist d = cfg.duelist;
        long reactAt = now + rand(d.reactionMinMs, d.reactionMaxMs);
        Direction need = CombatRules.requiredGuard(o.attackDir);
        double r = roll();
        c.aiReactionGuard = null;

        if (o.attackKind == AttackKind.GUARD_BREAK) {
            if (r < d.reactionChance * 0.7) {
                c.aiReaction = R_COUNTER;
                c.aiReactionAt = Math.min(reactAt, o.impactAt - 40);
            } else if (r < d.reactionChance * 0.7 + d.dodgeChance) {
                c.aiReaction = R_DODGE;
                c.aiReactionAt = Math.min(reactAt, o.impactAt - 100);
            } else {
                c.aiReaction = R_NONE;
            }
            return;
        }

        if (r < d.dodgeChance || (o.attackKind == AttackKind.ARCANE && r < d.reactionChance)) {
            c.aiReaction = R_DODGE;
            c.aiReactionAt = Math.max(reactAt, o.impactAt - 200); // esquiva perto do impacto
        } else if (r < d.dodgeChance + d.parryChance) {
            c.aiReaction = R_PARRY;
            c.aiReactionGuard = need;
            c.aiReactionAt = Math.max(reactAt, o.impactAt - cfg.parry.windowMs / 2);
        } else if (o.attackKind != AttackKind.ARCANE && r < d.dodgeChance + d.parryChance + d.reactionChance) {
            c.aiReaction = R_GUARD;
            c.aiReactionGuard = need;
            c.aiReactionAt = reactAt;
        } else {
            c.aiReaction = R_NONE; // não percebeu: vai levar o golpe
        }
    }

    @Nullable
    private static Ref<EntityStore> nearestPlayer(Ref<EntityStore> me, Vector3d pos, ComponentAccessor<EntityStore> cb, double radius) {
        List<Ref<EntityStore>> near = new ArrayList<>(TargetUtil.getAllEntitiesInSphere(pos, radius, cb));
        Ref<EntityStore> best = null;
        double bestD = Double.MAX_VALUE;
        for (Ref<EntityStore> r : near) {
            if (r == null || !r.isValid() || r.equals(me)) continue;
            if (cb.getComponent(r, Player.getComponentType()) == null) continue;
            TransformComponent t = cb.getComponent(r, TransformComponent.getComponentType());
            if (t == null) continue;
            double dd = t.getPosition().distanceSquared(pos);
            if (dd < bestD) {
                bestD = dd;
                best = r;
            }
        }
        return best;
    }
}
