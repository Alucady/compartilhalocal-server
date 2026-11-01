package dev.alucady.combatedirecional.combat;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.alucady.combatedirecional.hud.CombatHud;

import javax.annotation.Nullable;

/**
 * Estado de combate de UMA entidade (jogador ou NPC duelista).
 *
 * É um "componente" do sistema ECS do Hytale: fica pendurado na entidade e o
 * {@link CombatTickSystem} atualiza ele a cada tick do servidor.
 * Não é salvo no disco (some quando o jogador sai), de propósito.
 *
 * Todos os tempos são em milissegundos do relógio do servidor ({@link CombatRules#now()}).
 */
public final class Combatant implements Component<EntityStore> {

    public enum State {
        IDLE,       // em guarda, pode agir e bloquear
        WINDUP,     // preparando um golpe (antecipação)
        RECOVERY,   // recuperação depois de uma ação (pode bloquear, não pode atacar)
        STUNNED,    // atordoado: não age, não bloqueia
        DODGE,      // esquiva curta
        ROLL        // rolamento
    }

    // --------------------------------------------------------------- guarda / estado
    public Direction guard = Direction.TOP;
    public State state = State.IDLE;
    public long stateStart;
    public long stateEnd;

    // --------------------------------------------------------------- golpe atual
    @Nullable public AttackKind attackKind;
    @Nullable public Direction attackDir;
    public long impactAt;
    @Nullable public Ref<EntityStore> target;
    /** Número que muda a cada golpe novo (a IA usa para reagir uma vez por golpe). */
    public long attackSerial;

    // --------------------------------------------------------------- esquiva / rolamento
    public long invulnerableFrom;
    public long invulnerableUntil;

    // --------------------------------------------------------------- troca de guarda com atraso
    @Nullable public Direction pendingGuard;
    public long guardSwitchAt;

    // --------------------------------------------------------------- agarrado por uma quebra de guarda
    @Nullable public Ref<EntityStore> grabbedBy;
    public long grabbedUntil;
    public long aiGrabHandled;

    // --------------------------------------------------------------- recursos
    public double stamina = -1;   // -1 = ainda não inicializado (usa o máximo do config)
    public double mana = 0;
    public boolean exhausted;
    public long lastSpendAt;
    public long furyUntil;

    // --------------------------------------------------------------- movimento (para regen e esquiva)
    public boolean hasLastPos;
    public double lastX, lastY, lastZ;
    public double velX, velZ;     // velocidade horizontal suavizada (blocos/s)
    public boolean moving;

    // --------------------------------------------------------------- "flick" do mouse
    public final long[] sampleTime = new long[32];
    public final float[] sampleYaw = new float[32];
    public final float[] samplePitch = new float[32];
    public int sampleCount;
    public int sampleHead;

    // --------------------------------------------------------------- jogador
    @Nullable public CombatHud hud;
    public boolean hudWanted = true;
    public long nextHudUpdate;

    // --------------------------------------------------------------- IA (NPC duelista)
    public boolean ai;
    public String displayName = "";
    public long aiNextThinkAt;
    public long aiReactedToSerial = -1;
    public long aiReactionAt;
    public int aiReaction;          // 0 nada, 1 guarda, 2 aparo, 3 esquiva, 4 contra-quebra
    @Nullable public Direction aiReactionGuard;
    public long aiFeintAt;          // >0 = vai fintar o pesado neste momento
    @Nullable public String lastNameplate;

    // --------------------------------------------------------------- barras do jogo
    public float lastMirroredStamina = -1;
    public float lastMirroredMana = -1;

    // ================================================================ helpers

    public boolean isInvulnerable(long now) {
        return now >= this.invulnerableFrom && now < this.invulnerableUntil;
    }

    public boolean isFury(long now) {
        return now < this.furyUntil;
    }

    /** Pode iniciar uma ação nova agora? */
    public boolean isGrabbed(long now) {
        return this.grabbedBy != null && now < this.grabbedUntil;
    }

    public boolean canAct(long now) {
        if (isGrabbed(now)) return false;
        return switch (this.state) {
            case IDLE -> true;
            case RECOVERY, DODGE, ROLL, STUNNED -> now >= this.stateEnd;
            case WINDUP -> false;
        };
    }

    /** Está com a guarda levantada (pode bloquear)? */
    public boolean canBlock(long now) {
        return switch (this.state) {
            case IDLE, RECOVERY -> true;
            case STUNNED, DODGE, ROLL -> now >= this.stateEnd;
            case WINDUP -> false;
        };
    }

    public void setState(State state, long now, long durationMs) {
        this.state = state;
        this.stateStart = now;
        this.stateEnd = now + Math.max(0, durationMs);
        if (state != State.WINDUP) {
            this.attackKind = null;
            this.attackDir = null;
            this.target = null;
        }
    }

    @Override
    public Component<EntityStore> clone() {
        Combatant c = new Combatant();
        c.guard = this.guard;
        c.stamina = this.stamina;
        c.mana = this.mana;
        c.exhausted = this.exhausted;
        c.ai = this.ai;
        c.displayName = this.displayName;
        c.hudWanted = this.hudWanted;
        return c;
    }
}
