package dev.alucady.combatedirecional.combat;

/** As 3 direções de guarda/golpe, sempre do ponto de vista de quem está agindo. */
public enum Direction {
    LEFT("ESQUERDA", "◀"),
    TOP("CIMA", "▲"),
    RIGHT("DIREITA", "▶");

    public final String label;
    public final String arrow;

    Direction(String label, String arrow) {
        this.label = label;
        this.arrow = arrow;
    }

    /** Lado oposto (esquerda vira direita). Cima continua cima. */
    public Direction mirrored() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case TOP -> TOP;
        };
    }

    public static Direction parse(String s) {
        if (s == null) return null;
        return switch (s.trim().toLowerCase()) {
            case "esquerda", "esq", "left", "l", "e" -> LEFT;
            case "cima", "topo", "top", "up", "c", "t" -> TOP;
            case "direita", "dir", "right", "r", "d" -> RIGHT;
            default -> null;
        };
    }
}
