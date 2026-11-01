package dev.alucady.combatedirecional.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import dev.alucady.combatedirecional.config.CombatConfig;

import javax.annotation.Nonnull;

/**
 * /combate            -> mostra que o mod carregou + alguns valores do config
 * /combate recarregar -> relê o CombatConfig.json (só operadores)
 */
public final class CombateCommand extends CommandBase {

    // Chave do texto que vem do NOSSO asset pack (Server/Languages/.../combatedirecional.lang).
    static final String ASSET_PACK_KEY = "combatedirecional.status.assetpack";

    private final CombateDirecionalPlugin plugin;

    public CombateCommand(@Nonnull CombateDirecionalPlugin plugin) {
        // A descrição é uma chave de tradução (texto no arquivo .lang).
        super("combate", "combatedirecional.commands.combate.desc");
        this.plugin = plugin;
        // Qualquer jogador pode usar /combate (só mostra informações).
        this.requireNoPermission();
        this.addSubCommand(new Recarregar(plugin));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        CombatConfig c = this.plugin.combat();

        context.sendMessage(Message.raw("=== Combate Direcional v" + this.plugin.getManifest().getVersion() + " ===")
                .color("#f0c040").bold(true));
        context.sendMessage(Message.raw("Plugin Java: OK").color("#60e060"));

        // Prova de que o asset pack foi carregado: o texto só existe se o .lang foi lido.
        boolean assetPackOk = I18nModule.get().getMessage("en-US", ASSET_PACK_KEY) != null;
        if (assetPackOk) {
            context.sendMessage(Message.translation(ASSET_PACK_KEY).color("#60e060"));
        } else {
            context.sendMessage(Message.raw("Asset pack NAO encontrado (veja IncludesAssetPack no manifest)").color("#ff6060"));
        }

        context.sendMessage(Message.raw(String.format(
                "Leve: %d ms, dano %.0f, stamina %d | Pesado: %d ms, dano %.0f, stamina %d",
                c.lightAttack.windupMs, c.lightAttack.damage, c.lightAttack.staminaCost,
                c.heavyAttack.windupMs, c.heavyAttack.damage, c.heavyAttack.staminaCost)));
        context.sendMessage(Message.raw(String.format(
                "Aparo: janela %d ms | Quebra: %d ms | Esquiva: %d ms invencivel | Furia: %d mana",
                c.parry.windowMs, c.guardBreak.windupMs, c.dodge.invulnerableMs, c.mana.furyCost)));
    }

    /** Subcomando: /combate recarregar */
    static final class Recarregar extends CommandBase {
        private final CombateDirecionalPlugin plugin;

        Recarregar(@Nonnull CombateDirecionalPlugin plugin) {
            super("recarregar", "combatedirecional.commands.combate.recarregar.desc");
            this.plugin = plugin;
            // Sem setPermissionGroup: só operadores (permissão padrão gerada pelo servidor).
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
}
