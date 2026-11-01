package dev.alucady.combatedirecional.combat;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import dev.alucady.combatedirecional.config.CombatConfig;
import dev.alucady.combatedirecional.feel.Feedback;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras do combate. Tudo aqui roda na thread do mundo (é chamado pelo tick
 * do ECS ou por uma Interação), então pode mexer nas entidades com segurança.
 */
public final class CombatRules {

    /** Entradas do jogador (vêm das Interações da espada, da IA ou de comandos). */
    public enum Input {
        LIGHT,        // botão esquerdo
        HEAVY,        // botão direito (também usado para APARAR)
        GUARD_BREAK,  // habilidade 1 (também FINTA e CONTRA-QUEBRA)
        DODGE         // esquiva (correndo = rolamento)
    }

    private CombatRules() {
    }

    public static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    static CombatConfig cfg() {
        return CombateDirecionalPlugin.get().combat();
    }

    static ComponentType<EntityStore, Combatant> type() {
        return CombateDirecionalPlugin.get().combatantType();
    }

    /** Pega o estado de combate da entidade, criando se ainda não existir. */
    public static Combatant getOrCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> cb) {
        Combatant c = cb.getComponent(ref, type());
        if (c == null) {
            c = new Combatant();
            cb.putComponent(ref, type(), c);
        }
        initResources(c);
        return c;
    }

    static void initResources(Combatant c) {
        if (c.stamina < 0) {
            c.stamina = cfg().stamina.max;
        }
    }

    // =================================================================== ENTRADAS

    /** Ponto de entrada único para todas as ações. */
    public static void handleInput(Ref<EntityStore> ref, Input input, ComponentAccessor<EntityStore> cb) {
        if (ref == null || !ref.isValid()) return;
        Combatant c = getOrCreate(ref, cb);
        long now = now();
        MovementStates ms = movementStates(ref, cb);
        boolean crouching = ms != null && (ms.crouching || ms.forcedCrouching);
        boolean sprinting = ms != null && ms.sprinting;
        CombatConfig cfg = cfg();

        switch (input) {
            case LIGHT -> startAttack(ref, c, AttackKind.LIGHT, cb, now);
            case HEAVY -> {
                // 1) APARO: tem golpe inimigo chegando nos últimos ms e minha guarda está certa?
                if (tryParry(ref, c, cb, now)) return;
                // 2) GOLPE ARCANO: pesado agachado com mana suficiente
                if (crouching && c.mana >= cfg.mana.arcaneStrikeCost) {
                    startAttack(ref, c, AttackKind.ARCANE, cb, now);
                } else {
                    startAttack(ref, c, AttackKind.HEAVY, cb, now);
                }
            }
            case GUARD_BREAK -> {
                // 1) FÚRIA: agachado com mana cheia
                if (crouching && c.mana >= cfg.mana.furyCost && c.canAct(now)) {
                    activateFury(ref, c, cb, now);
                    return;
                }
                // 2) FINTA: estou preparando um pesado e ainda dá tempo
                if (tryFeint(ref, c, cb, now)) return;
                // 3) CONTRA-QUEBRA: alguém está tentando quebrar MINHA guarda agora
                if (tryCounterBreak(ref, c, cb, now)) return;
                // 4) QUEBRA DE GUARDA normal
                startAttack(ref, c, AttackKind.GUARD_BREAK, cb, now);
            }
            case DODGE -> startDodge(ref, c, cb, now, sprinting);
        }
    }

    /** Troca de guarda (pelo mouse, comando ou IA). */
    public static void setGuard(Ref<EntityStore> ref, Combatant c, Direction dir, ComponentAccessor<EntityStore> cb, long now) {
        if (c.guard == dir) return;
        if (c.state == Combatant.State.STUNNED && now < c.stateEnd) return;
        c.guard = dir;
        if (c.state == Combatant.State.IDLE || c.state == Combatant.State.RECOVERY) {
            CombatConfig.Animations a = cfg().animations;
            Feedback.animation(ref, cb, switch (dir) {
                case LEFT -> a.blockLeft;
                case TOP -> a.blockTop;
                case RIGHT -> a.blockRight;
            });
        }
    }

    // =================================================================== ATAQUES

    public static void startAttack(Ref<EntityStore> ref, Combatant c, AttackKind kind, ComponentAccessor<EntityStore> cb, long now) {
        if (!c.canAct(now)) return;
        CombatConfig cfg = cfg();

        int windup;
        int staminaCost;
        switch (kind) {
            case LIGHT -> { windup = cfg.lightAttack.windupMs; staminaCost = cfg.lightAttack.staminaCost; }
            case HEAVY, ARCANE -> { windup = cfg.heavyAttack.windupMs; staminaCost = cfg.heavyAttack.staminaCost; }
            default -> { windup = cfg.guardBreak.windupMs; staminaCost = cfg.guardBreak.staminaCost; }
        }
        if (kind == AttackKind.ARCANE) {
            c.mana -= cfg.mana.arcaneStrikeCost;
        }
        if (c.exhausted) {
            windup = (int) Math.round(windup * cfg.stamina.exhaustedWindupMultiplier);
        }
        spendStamina(ref, c, staminaCost, cb, now);

        c.setState(Combatant.State.WINDUP, now, windup);
        c.attackKind = kind;
        c.attackDir = c.guard; // o golpe sai do lado da sua guarda (como no estilo "For Honor")
        c.impactAt = now + windup;
        c.target = acquireTarget(ref, cb, cfg.reach.lockOnRange, 70);
        c.attackSerial++;

        // ---- feedback: animação de antecipação + avisos
        Feedback.animation(ref, cb, attackAnimation(kind, c.attackDir));
        if (kind == AttackKind.ARCANE) {
            Feedback.particle(cfg.feel.arcaneParticle, ref, cb);
            if (c.target != null) {
                Feedback.title(c.target, cb, "IMBLOQUEÁVEL!", "Esquive ou apare", "#c070ff");
            }
        }
    }

    static String attackAnimation(AttackKind kind, Direction dir) {
        CombatConfig.Animations a = cfg().animations;
        return switch (kind) {
            case LIGHT -> switch (dir) { case LEFT -> a.lightLeft; case TOP -> a.lightTop; case RIGHT -> a.lightRight; };
            case HEAVY -> switch (dir) { case LEFT -> a.heavyLeft; case TOP -> a.heavyTop; case RIGHT -> a.heavyRight; };
            case ARCANE -> a.arcane.isBlank()
                    ? switch (dir) { case LEFT -> a.heavyLeft; case TOP -> a.heavyTop; case RIGHT -> a.heavyRight; }
                    : a.arcane;
            case GUARD_BREAK -> a.guardBreak;
        };
    }

    /** Direção em que o DEFENSOR precisa estar para bloquear um golpe vindo de attackDir. */
    public static Direction requiredGuard(Direction attackDir) {
        return cfg().controls.mirrorSides ? attackDir.mirrored() : attackDir;
    }

    /** Chamado no momento do impacto (fim da preparação). */
    static void resolveImpact(Ref<EntityStore> attackerRef, Combatant a, ComponentAccessor<EntityStore> cb, long now) {
        CombatConfig cfg = cfg();
        AttackKind kind = a.attackKind == null ? AttackKind.LIGHT : a.attackKind;
        Direction dir = a.attackDir == null ? a.guard : a.attackDir;

        Ref<EntityStore> targetRef = a.target;
        if (targetRef == null || !targetRef.isValid() || !inReach(attackerRef, targetRef, cb, cfg.reach.range + 0.6, cfg.reach.coneDegrees)) {
            targetRef = acquireTarget(attackerRef, cb, cfg.reach.range, cfg.reach.coneDegrees);
        }

        Feedback.sound(cfg.feel.swingSound, attackerRef, cb);

        // ---------------- errou (ninguém no alcance)
        if (targetRef == null) {
            int rec = kind == AttackKind.GUARD_BREAK ? cfg.guardBreak.whiffRecoveryMs
                    : kind == AttackKind.LIGHT ? cfg.lightAttack.recoveryMs : cfg.heavyAttack.recoveryMs;
            a.setState(Combatant.State.RECOVERY, now, rec);
            return;
        }

        Combatant t = cb.getComponent(targetRef, type());
        if (t != null) initResources(t);

        // ---------------- QUEBRA DE GUARDA
        if (kind == AttackKind.GUARD_BREAK) {
            if (t != null && t.isInvulnerable(now)) {
                a.setState(Combatant.State.RECOVERY, now, cfg.guardBreak.whiffRecoveryMs);
                return;
            }
            if (t != null) {
                int stun = t.exhausted ? cfg.guardBreak.stunExhaustedMs : cfg.guardBreak.stunMs;
                stun(targetRef, t, cb, now, stun);
                Feedback.title(targetRef, cb, "GUARDA QUEBRADA!", t.exhausted ? "Exausto: atordoamento maior" : null, "#ff8040");
            }
            Feedback.title(attackerRef, cb, "QUEBRA!", "Ataque agora", "#ffd040");
            Feedback.particle(cfg.feel.guardBreakParticle, targetRef, cb);
            Feedback.sound(cfg.feel.blockSound, targetRef, cb);
            a.setState(Combatant.State.RECOVERY, now, 200);
            return;
        }

        // ---------------- ESQUIVOU (invencível)
        if (t != null && t.isInvulnerable(now)) {
            a.setState(Combatant.State.RECOVERY, now, kind == AttackKind.LIGHT ? cfg.lightAttack.recoveryMs : cfg.heavyAttack.recoveryMs);
            Feedback.title(targetRef, cb, "ESQUIVOU!", null, "#80ffb0");
            return;
        }

        boolean heavyLike = kind.isHeavyLike();

        // ---------------- BLOQUEIO
        boolean blocked = t != null
                && kind != AttackKind.ARCANE
                && t.canBlock(now)
                && t.guard == requiredGuard(dir);
        if (blocked) {
            gainMana(t, cfg.block.manaGain);
            a.setState(Combatant.State.RECOVERY, now, cfg.block.attackerRecoilMs);
            Feedback.sound(cfg.feel.blockSound, targetRef, cb);
            Feedback.particle(cfg.feel.blockParticle, targetRef, cb);
            if (heavyLike && cfg.block.heavyChipDamage > 0) {
                applyDamage(attackerRef, targetRef, (float) cfg.block.heavyChipDamage, cb);
            }
            return;
        }

        // ---------------- ACERTOU
        double damage = heavyLike ? cfg.heavyAttack.damage : cfg.lightAttack.damage;
        if (a.isFury(now)) {
            damage *= cfg.mana.furyDamageMultiplier;
        }
        // (o +25% de dano em alvo EXAUSTO é aplicado no filtro de dano, para valer contra qualquer fonte)
        applyDamage(attackerRef, targetRef, (float) damage, cb);

        if (kind == AttackKind.LIGHT) gainMana(a, cfg.lightAttack.manaGain);
        if (kind == AttackKind.HEAVY) gainMana(a, cfg.heavyAttack.manaGain);

        // Golpe recebido interrompe o golpe que o alvo estava preparando.
        if (t != null && t.state == Combatant.State.WINDUP) {
            t.setState(Combatant.State.RECOVERY, now, 250);
        }

        // HIT-STOP: congelamos a linha do tempo de combate dos dois por alguns ms.
        int hitStop = Math.max(0, cfg.feel.hitStopMs);
        int rec = (kind == AttackKind.LIGHT ? cfg.lightAttack.recoveryMs : cfg.heavyAttack.recoveryMs) + hitStop;
        a.setState(Combatant.State.RECOVERY, now, rec);
        if (t != null && t.state != Combatant.State.WINDUP) {
            t.stateEnd = Math.max(t.stateEnd, now) + hitStop;
        }

        Feedback.sound(cfg.feel.hitSound, targetRef, cb);
        Feedback.particle(cfg.feel.hitParticle, targetRef, cb);
        String shake = heavyLike ? cfg.feel.heavyHitCameraEffect : cfg.feel.lightHitCameraEffect;
        if (shake == null || shake.isBlank()) shake = cfg.feel.hitCameraEffect;
        Feedback.cameraShake(shake, targetRef, cb);
        Feedback.cameraShake(cfg.feel.lightHitCameraEffect, attackerRef, cb);
    }

    @SuppressWarnings("deprecation")
    static void applyDamage(Ref<EntityStore> attackerRef, Ref<EntityStore> targetRef, float amount, ComponentAccessor<EntityStore> cb) {
        // Causa de dano "Physical" do jogo base (procura pelo ID; o campo estático antigo é só reserva).
        int causeIndex = DamageCause.getAssetMap().getIndex("Physical");
        Damage damage;
        if (causeIndex != Integer.MIN_VALUE) {
            damage = new Damage(new Damage.EntitySource(attackerRef), causeIndex, amount);
        } else if (DamageCause.PHYSICAL != null) {
            damage = new Damage(new Damage.EntitySource(attackerRef), DamageCause.PHYSICAL, amount);
        } else {
            return;
        }
        DamageSystems.executeDamage(targetRef, cb, damage);
    }

    // =================================================================== APARO / FINTA / CONTRA-QUEBRA

    static boolean tryParry(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now) {
        if (!c.canBlock(now) && !c.canAct(now)) return false;
        CombatConfig cfg = cfg();
        for (Ref<EntityStore> attackerRef : incomingAttackers(ref, cb)) {
            Combatant a = cb.getComponent(attackerRef, type());
            if (a == null || a.attackKind == null || !a.attackKind.isStrike()) continue;
            long remaining = a.impactAt - now;
            if (remaining < 0 || remaining > cfg.parry.windowMs) continue;
            if (a.attackDir == null || c.guard != requiredGuard(a.attackDir)) continue;

            // APARO!
            stun(attackerRef, a, cb, now, cfg.parry.attackerStunMs);
            gainMana(c, cfg.parry.manaGain);
            c.setState(Combatant.State.RECOVERY, now, 150);
            Feedback.animation(ref, cb, cfg.animations.parry);
            Feedback.sound(cfg.feel.parrySound, ref, cb);
            Feedback.particle(cfg.feel.blockParticle, ref, cb);
            Feedback.cameraShake(cfg.feel.lightHitCameraEffect, ref, cb);
            Feedback.title(ref, cb, "APARO!", "+" + cfg.parry.manaGain + " mana", "#60e0ff");
            Feedback.title(attackerRef, cb, "APARADO!", "Atordoado", "#ff6060");
            return true;
        }
        return false;
    }

    static boolean tryFeint(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now) {
        if (c.state != Combatant.State.WINDUP || c.attackKind == null || !c.attackKind.isHeavyLike()) return false;
        CombatConfig cfg = cfg();
        if (c.impactAt - now <= cfg.heavyAttack.feintCutoffMs) return false; // tarde demais
        c.setState(Combatant.State.RECOVERY, now, cfg.heavyAttack.feintRecoveryMs);
        // Para a animação de ataque: tocamos a "pose de guarda" da direção atual.
        CombatConfig.Animations an = cfg.animations;
        Feedback.animation(ref, cb, switch (c.guard) { case LEFT -> an.blockLeft; case TOP -> an.blockTop; case RIGHT -> an.blockRight; });
        Feedback.title(ref, cb, "FINTA", null, "#d0d0d0");
        return true;
    }

    static boolean tryCounterBreak(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now) {
        if (c.state == Combatant.State.STUNNED && now < c.stateEnd) return false;
        for (Ref<EntityStore> attackerRef : incomingAttackers(ref, cb)) {
            Combatant a = cb.getComponent(attackerRef, type());
            if (a == null || a.attackKind != AttackKind.GUARD_BREAK) continue;
            a.setState(Combatant.State.RECOVERY, now, cfg().guardBreak.counteredRecoveryMs);
            c.setState(Combatant.State.RECOVERY, now, 120);
            Feedback.sound(cfg().feel.blockSound, ref, cb);
            Feedback.title(ref, cb, "CONTRA-QUEBRA!", null, "#80ffb0");
            Feedback.title(attackerRef, cb, "QUEBRA ANULADA", null, "#ff6060");
            return true;
        }
        return false;
    }

    /** Lista de quem está, AGORA, preparando um golpe contra esta entidade. */
    public static List<Ref<EntityStore>> incomingAttackers(Ref<EntityStore> me, ComponentAccessor<EntityStore> cb) {
        List<Ref<EntityStore>> result = new ArrayList<>();
        Vector3d pos = Feedback.positionOf(me, cb);
        if (pos == null) return result;
        List<Ref<EntityStore>> near = new ArrayList<>(TargetUtil.getAllEntitiesInSphere(pos, cfg().reach.lockOnRange + 2, cb));
        for (Ref<EntityStore> other : near) {
            if (other == null || !other.isValid() || other.equals(me)) continue;
            Combatant a = cb.getComponent(other, type());
            if (a != null && a.state == Combatant.State.WINDUP && me.equals(a.target)) {
                result.add(other);
            }
        }
        return result;
    }

    // =================================================================== ATORDOAMENTO / RECURSOS

    static void stun(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now, int ms) {
        c.setState(Combatant.State.STUNNED, now, ms);
        Feedback.animation(ref, cb, cfg().animations.stunned);
    }

    static void spendStamina(Ref<EntityStore> ref, Combatant c, double amount, ComponentAccessor<EntityStore> cb, long now) {
        c.stamina = Math.max(0, c.stamina - amount);
        c.lastSpendAt = now;
        if (c.stamina <= 0 && !c.exhausted) {
            c.exhausted = true;
            Feedback.title(ref, cb, "EXAUSTO", "Golpes lentos, dano recebido maior", "#ffb040");
        }
    }

    static void gainMana(@Nullable Combatant c, double amount) {
        if (c == null) return;
        c.mana = Math.min(cfg().mana.max, c.mana + amount);
    }

    public static void activateFury(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now) {
        CombatConfig cfg = cfg();
        c.mana -= cfg.mana.furyCost;
        c.furyUntil = now + cfg.mana.furyDurationMs;
        c.setState(Combatant.State.RECOVERY, now, 400);
        Feedback.animation(ref, cb, cfg.animations.fury);
        Feedback.particle(cfg.feel.furyParticle, ref, cb);
        Feedback.cameraShake(cfg.feel.heavyHitCameraEffect, ref, cb);
        Feedback.title(ref, cb, "FÚRIA!", "+" + Math.round((cfg.mana.furyDamageMultiplier - 1) * 100) + "% de dano por "
                + (cfg.mana.furyDurationMs / 1000) + "s", "#ff3030");
    }

    // =================================================================== ESQUIVA / ROLAMENTO

    public static void startDodge(Ref<EntityStore> ref, Combatant c, ComponentAccessor<EntityStore> cb, long now, boolean roll) {
        // Pode esquivar durante a recuperação de um golpe (cancela a recuperação), mas não atordoado
        // nem no meio da preparação de um golpe.
        if (c.state == Combatant.State.WINDUP) return;
        if (c.state == Combatant.State.STUNNED && now < c.stateEnd) return;
        if ((c.state == Combatant.State.DODGE || c.state == Combatant.State.ROLL) && now < c.stateEnd) return;
        CombatConfig cfg = cfg();

        // direção: para onde estou andando, em relação para onde estou olhando
        double[] fwd = forward(ref, cb);
        double mx = c.velX, mz = c.velZ;
        double speed = Math.hypot(mx, mz);
        double dx, dz;
        String anim;
        if (speed < 0.8) {           // parado: esquiva para trás
            dx = -fwd[0];
            dz = -fwd[1];
            anim = cfg.animations.dodgeBack;
        } else {
            dx = mx / speed;
            dz = mz / speed;
            double along = dx * fwd[0] + dz * fwd[1];
            double side = dx * (-fwd[1]) + dz * fwd[0]; // >0 = direita
            if (Math.abs(along) >= Math.abs(side)) {
                anim = along >= 0 ? cfg.animations.dodgeForward : cfg.animations.dodgeBack;
            } else {
                anim = side >= 0 ? cfg.animations.dodgeRight : cfg.animations.dodgeLeft;
            }
        }

        if (roll) {
            spendStamina(ref, c, cfg.roll.staminaCost, cb, now);
            c.setState(Combatant.State.ROLL, now, cfg.roll.durationMs);
            c.invulnerableUntil = now + Math.round(cfg.roll.durationMs * cfg.roll.invulnerableFraction);
            push(ref, cb, dx, dz, cfg.roll.impulse);
            Feedback.animation(ref, cb, cfg.animations.roll);
        } else {
            spendStamina(ref, c, cfg.dodge.staminaCost, cb, now);
            c.setState(Combatant.State.DODGE, now, cfg.dodge.durationMs);
            c.invulnerableUntil = now + cfg.dodge.invulnerableMs;
            push(ref, cb, dx, dz, cfg.dodge.impulse);
            Feedback.animation(ref, cb, anim);
        }
    }

    /** Impulso horizontal. Jogadores: instrução de velocidade enviada ao cliente. NPC: deslocamento direto. */
    static void push(Ref<EntityStore> ref, ComponentAccessor<EntityStore> cb, double dx, double dz, double strength) {
        Velocity v = cb.getComponent(ref, Velocity.getComponentType());
        if (v != null && cb.getComponent(ref, Player.getComponentType()) != null) {
            v.addInstruction(new Vector3d(dx * strength, 0.0, dz * strength), null, ChangeVelocityType.Add);
            return;
        }
        TransformComponent t = cb.getComponent(ref, TransformComponent.getComponentType());
        if (t != null) {
            Vector3d p = new Vector3d(t.getPosition());
            double dist = strength * 0.12; // aproximação: ~1 bloco
            p.x += dx * dist;
            p.z += dz * dist;
            t.setPosition(p);
        }
    }

    // =================================================================== MIRA / ALVO

    /** Vetor (x, z) horizontal para onde a entidade está olhando. */
    public static double[] forward(Ref<EntityStore> ref, ComponentAccessor<EntityStore> cb) {
        float yaw = yawOf(ref, cb);
        return new double[]{-Math.sin(yaw), -Math.cos(yaw)};
    }

    public static float yawOf(Ref<EntityStore> ref, ComponentAccessor<EntityStore> cb) {
        HeadRotation head = cb.getComponent(ref, HeadRotation.getComponentType());
        if (head != null) return head.getRotation().yaw();
        TransformComponent t = cb.getComponent(ref, TransformComponent.getComponentType());
        return t == null ? 0f : t.getRotation().yaw();
    }

    static boolean inReach(Ref<EntityStore> from, Ref<EntityStore> to, ComponentAccessor<EntityStore> cb, double range, double coneDeg) {
        Vector3d a = Feedback.positionOf(from, cb);
        Vector3d b = Feedback.positionOf(to, cb);
        if (a == null || b == null) return false;
        double dx = b.x - a.x, dz = b.z - a.z, dy = b.y - a.y;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > range || Math.abs(dy) > 2.5) return false;
        if (dist < 0.6) return true;
        double[] f = forward(from, cb);
        double cos = (dx * f[0] + dz * f[1]) / dist;
        return cos >= Math.cos(Math.toRadians(coneDeg / 2.0));
    }

    /** Procura o melhor alvo vivo à frente (prioriza o mais centralizado e próximo). */
    @Nullable
    public static Ref<EntityStore> acquireTarget(Ref<EntityStore> me, ComponentAccessor<EntityStore> cb, double range, double coneDeg) {
        Vector3d pos = Feedback.positionOf(me, cb);
        if (pos == null) return null;
        double[] f = forward(me, cb);
        List<Ref<EntityStore>> near = new ArrayList<>(TargetUtil.getAllEntitiesInSphere(pos, range, cb));
        Ref<EntityStore> best = null;
        double bestScore = Double.MAX_VALUE;
        double minCos = Math.cos(Math.toRadians(coneDeg / 2.0));
        for (Ref<EntityStore> other : near) {
            if (other == null || !other.isValid() || other.equals(me)) continue;
            if (cb.getComponent(other, EntityStatMap.getComponentType()) == null) continue; // não é "vivo"
            Vector3d op = Feedback.positionOf(other, cb);
            if (op == null) continue;
            double dx = op.x - pos.x, dz = op.z - pos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range || Math.abs(op.y - pos.y) > 2.5) continue;
            double cos = dist < 0.01 ? 1 : (dx * f[0] + dz * f[1]) / dist;
            if (cos < minCos) continue;
            double score = dist * (2.0 - cos);
            if (score < bestScore) {
                bestScore = score;
                best = other;
            }
        }
        return best;
    }

    @Nullable
    static MovementStates movementStates(Ref<EntityStore> ref, ComponentAccessor<EntityStore> cb) {
        MovementStatesComponent msc = cb.getComponent(ref, MovementStatesComponent.getComponentType());
        return msc == null ? null : msc.getMovementStates();
    }
}
