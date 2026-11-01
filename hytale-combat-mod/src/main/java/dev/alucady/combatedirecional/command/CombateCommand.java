package dev.alucady.combatedirecional.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import dev.alucady.combatedirecional.combat.CombatRules;
import dev.alucady.combatedirecional.combat.Combatant;
import dev.alucady.combatedirecional.combat.Direction;
import dev.alucady.combatedirecional.config.CombatConfig;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * /combate                    -> status do mod + controles
 * /combate recarregar         -> relê o CombatConfig.json (operadores)
 * /combate hud                -> liga/desliga o HUD de combate
 * /combate guarda <dir>       -> muda a guarda (esquerda|cima|direita) — controle reserva
 * /combate acao <acao>        -> leve|pesado|quebra|esquiva|rolar — controle reserva para testes
 */
public final class CombateCommand extends CommandBase {

    static final String ASSET_PACK_KEY = "combatedirecional.status.assetpack";

    private final CombateDirecionalPlugin plugin;

    public CombateCommand(@Nonnull CombateDirecionalPlugin plugin) {
        super("combate", "combatedirecional.commands.combate.desc");
        this.plugin = plugin;
        this.requireNoPermission();
        this.addSubCommand(new Recarregar(plugin));
        this.addSubCommand(new Hud());
        this.addSubCommand(new Guarda());
        this.addSubCommand(new Acao());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        CombatConfig c = this.plugin.combat();

        context.sendMessage(Message.raw("=== Combate Direcional v" + this.plugin.getManifest().getVersion() + " ===")
                .color("#f0c040").bold(true));
        context.sendMessage(Message.raw("Plugin Java: OK").color("#60e060"));
        boolean assetPackOk = I18nModule.get().getMessage("en-US", ASSET_PACK_KEY) != null;
        if (assetPackOk) {
            context.sendMessage(Message.translation(ASSET_PACK_KEY).color("#60e060"));
        } else {
            context.sendMessage(Message.raw("Asset pack NAO encontrado (veja IncludesAssetPack no manifest)").color("#ff6060"));
        }
        context.sendMessage(Message.raw(String.format(
                "Leve: %d ms, dano %.0f | Pesado: %d ms, dano %.0f | Aparo: %d ms | Quebra: %d ms",
                c.lightAttack.windupMs, c.lightAttack.damage, c.heavyAttack.windupMs, c.heavyAttack.damage,
                c.parry.windowMs, c.guardBreak.windupMs)));
        context.sendMessage(Message.raw("Pegue a espada: /give CombateDirecional_Espada").color("#a0d0ff"));
        context.sendMessage(Message.raw("CONTROLES (segurando a espada):").bold(true));
        context.sendMessage(Message.raw(" Guarda: gire a camera rapido p/ esquerda/direita, ou olhe rapido p/ cima"));
        context.sendMessage(Message.raw(" Botao esquerdo: LEVE | Botao direito: PESADO (no fim do golpe inimigo = APARO)"));
        context.sendMessage(Message.raw(" Habilidade 1: QUEBRA (durante seu pesado = FINTA; durante quebra inimiga = CONTRA-QUEBRA)"));
        context.sendMessage(Message.raw(" Usar (F) / Esquiva: ESQUIVA (correndo = ROLAMENTO)"));
        context.sendMessage(Message.raw(" Agachado + pesado: GOLPE ARCANO (50 mana) | Agachado + quebra: FURIA (100 mana)"));
        context.sendMessage(Message.raw(" Reserva: /combate guarda <esquerda|cima|direita>, /combate acao <leve|pesado|quebra|esquiva|rolar>"));
    }

    // ------------------------------------------------------------------ /combate recarregar
    static final class Recarregar extends CommandBase {
        private final CombateDirecionalPlugin plugin;

        Recarregar(@Nonnull CombateDirecionalPlugin plugin) {
            super("recarregar", "combatedirecional.commands.combate.recarregar.desc");
            this.plugin = plugin;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            try {
                this.plugin.reloadConfig();
                context.sendMessage(Message.raw("CombatConfig.json recarregado!").color("#60e060"));
            } catch (Exception e) {
                context.sendMessage(Message.raw("Erro ao ler o CombatConfig.json: " + e.getMessage()).color("#ff6060"));
            }
        }
    }

    // ------------------------------------------------------------------ /combate hud
    static final class Hud extends AbstractPlayerCommand {
        Hud() {
            super("hud", "combatedirecional.commands.combate.hud.desc");
            this.requireNoPermission();
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Combatant c = store.getComponent(ref, CombateDirecionalPlugin.get().combatantType());
            if (c == null) {
                context.sendMessage(Message.raw("Aguarde um instante e tente de novo."));
                return;
            }
            c.hudWanted = !c.hudWanted;
            context.sendMessage(Message.raw("HUD de combate: " + (c.hudWanted ? "LIGADO" : "DESLIGADO")));
        }
    }

    // ------------------------------------------------------------------ /combate guarda <dir>
    static final class Guarda extends AbstractPlayerCommand {
        private final RequiredArg<String> dirArg =
                this.withRequiredArg("direcao", "combatedirecional.commands.combate.guarda.arg", ArgTypes.STRING);

        Guarda() {
            super("guarda", "combatedirecional.commands.combate.guarda.desc");
            this.requireNoPermission();
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Direction dir = Direction.parse(this.dirArg.get(context));
            if (dir == null) {
                context.sendMessage(Message.raw("Use: esquerda, cima ou direita").color("#ff6060"));
                return;
            }
            Combatant c = store.getComponent(ref, CombateDirecionalPlugin.get().combatantType());
            if (c == null) return;
            CombatRules.setGuard(ref, c, dir, store, CombatRules.now());
            context.sendMessage(Message.raw("Guarda: " + dir.arrow + " " + dir.label));
        }
    }

    // ------------------------------------------------------------------ /combate acao <acao>
    static final class Acao extends AbstractPlayerCommand {
        private final RequiredArg<String> actionArg =
                this.withRequiredArg("acao", "combatedirecional.commands.combate.acao.arg", ArgTypes.STRING);

        Acao() {
            super("acao", "combatedirecional.commands.combate.acao.desc");
            this.requireNoPermission();
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String a = this.actionArg.get(context).toLowerCase(Locale.ROOT);
            switch (a) {
                case "leve", "light" -> CombatRules.handleInput(ref, CombatRules.Input.LIGHT, store);
                case "pesado", "heavy", "aparo" -> CombatRules.handleInput(ref, CombatRules.Input.HEAVY, store);
                case "quebra", "finta" -> CombatRules.handleInput(ref, CombatRules.Input.GUARD_BREAK, store);
                case "esquiva", "dodge" -> CombatRules.handleInput(ref, CombatRules.Input.DODGE, store);
                case "rolar", "roll", "rolamento" -> {
                    Combatant c = store.getComponent(ref, CombateDirecionalPlugin.get().combatantType());
                    if (c != null) CombatRules.startDodge(ref, c, store, CombatRules.now(), true);
                }
                default -> context.sendMessage(Message.raw("Use: leve, pesado, quebra, esquiva ou rolar").color("#ff6060"));
            }
        }
    }
}
