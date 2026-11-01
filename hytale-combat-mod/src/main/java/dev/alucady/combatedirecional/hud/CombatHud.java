package dev.alucady.combatedirecional.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * HUD de combate: 4 linhas de texto no canto inferior esquerdo.
 *
 *   GUARDA ▲ CIMA
 *   STAMINA 74/100 | MANA 30/100 | FÚRIA 5s
 *   ⚠ PESADO! Bloqueie ▶ DIREITA          (vermelho, só quando vem golpe)
 *   Preparando LEVE ◀                      (seu estado atual)
 *
 * Usa a linguagem de UI do Hytale "inline" (sem arquivo .ui separado),
 * do mesmo jeito que o HUD de espectador do próprio servidor.
 */
public final class CombatHud extends CustomUIHud {

    public static final String KEY = "CombateDirecional";

    private static final String DOCUMENT = """
            Group {
              Anchor: (Left: 24, Bottom: 230, Width: 640, Height: 120);

              Label #CdGuard {
                Anchor: (Left: 0, Top: 0, Height: 28);
                Style: (FontSize: 22, TextColor: #f0c040, RenderBold: true);
              }
              Label #CdStats {
                Anchor: (Left: 0, Top: 30, Height: 22);
                Style: (FontSize: 16, TextColor: #ffffff);
              }
              Label #CdIncoming {
                Anchor: (Left: 0, Top: 56, Height: 28);
                Style: (FontSize: 22, TextColor: #ff5050, RenderBold: true);
              }
              Label #CdState {
                Anchor: (Left: 0, Top: 88, Height: 22);
                Style: (FontSize: 15, TextColor: #a0d0ff);
              }
            }
            """;

    private String guard = "";
    private String stats = "";
    private String incoming = "";
    private String state = "";

    public CombatHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.appendInline(null, DOCUMENT);
        builder.set("#CdGuard.Text", this.guard);
        builder.set("#CdStats.Text", this.stats);
        builder.set("#CdIncoming.Text", this.incoming);
        builder.set("#CdState.Text", this.state);
    }

    /** Atualiza só o que mudou (economiza rede). */
    public void setLines(String guard, String stats, String incoming, String state) {
        UICommandBuilder builder = new UICommandBuilder();
        boolean changed = false;
        if (!guard.equals(this.guard)) {
            this.guard = guard;
            builder.set("#CdGuard.Text", guard);
            changed = true;
        }
        if (!stats.equals(this.stats)) {
            this.stats = stats;
            builder.set("#CdStats.Text", stats);
            changed = true;
        }
        if (!incoming.equals(this.incoming)) {
            this.incoming = incoming;
            builder.set("#CdIncoming.Text", incoming);
            changed = true;
        }
        if (!state.equals(this.state)) {
            this.state = state;
            builder.set("#CdState.Text", state);
            changed = true;
        }
        if (changed) {
            this.update(false, builder);
        }
    }
}
