package dev.alucady.combatedirecional.combat;

/** Tipos de ação ofensiva com preparação (windup). */
public enum AttackKind {
    LIGHT("LEVE"),
    HEAVY("PESADO"),
    ARCANE("ARCANO"),      // pesado imbloqueável
    GUARD_BREAK("QUEBRA");

    public final String label;

    AttackKind(String label) {
        this.label = label;
    }

    public boolean isStrike() {
        return this != GUARD_BREAK;
    }

    public boolean isHeavyLike() {
        return this == HEAVY || this == ARCANE;
    }
}
