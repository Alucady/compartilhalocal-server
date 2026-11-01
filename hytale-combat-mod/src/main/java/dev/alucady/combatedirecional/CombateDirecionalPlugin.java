package dev.alucady.combatedirecional;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.alucady.combatedirecional.combat.Combatant;
import dev.alucady.combatedirecional.combat.CombatDamageFilter;
import dev.alucady.combatedirecional.combat.CombatTickSystem;
import dev.alucady.combatedirecional.command.CombateCommand;
import dev.alucady.combatedirecional.command.DuelistaCommand;
import dev.alucady.combatedirecional.interaction.CombatActionInteraction;
import dev.alucady.combatedirecional.config.CombatConfig;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Ponto de entrada do mod. O servidor cria esta classe porque ela está
 * no campo "Main" do manifest.json.
 *
 * Ciclo de vida (do servidor do Hytale):
 *   construtor -> preLoad (lê os configs) -> setup() -> start() -> ... -> shutdown()
 */
public final class CombateDirecionalPlugin extends JavaPlugin {

    private static CombateDirecionalPlugin instance;

    // withConfig() só pode ser chamado no construtor (antes do setup).
    private final Config<CombatConfig> config;
    private ComponentType<EntityStore, Combatant> combatantType;

    public CombateDirecionalPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("CombatConfig", CombatConfig.CODEC);
    }

    public static CombateDirecionalPlugin get() {
        return instance;
    }

    /** Valores atuais do combate. */
    public CombatConfig combat() {
        return this.config.get();
    }

    /** Tipo do componente de estado de combate (registrado no setup). */
    public ComponentType<EntityStore, Combatant> combatantType() {
        return this.combatantType;
    }

    /** Relê o CombatConfig.json do disco. */
    public void reloadConfig() {
        this.config.load().join();
    }

    @Override
    protected void setup() {
        // 1) Interação customizada: a espada do asset pack usa "Type": "CombateDirecional_Acao".
        this.getCodecRegistry(Interaction.CODEC)
                .register(CombatActionInteraction.TYPE_ID, CombatActionInteraction.class, CombatActionInteraction.CODEC);

        // 2) Componente ECS com o estado de combate (não é salvo no disco).
        this.combatantType = this.getEntityStoreRegistry().registerComponent(Combatant.class, Combatant::new);

        // 3) Sistemas: tick do combate e filtro de dano (invencibilidade / exaustão).
        this.getEntityStoreRegistry().registerSystem(new CombatTickSystem(this.combatantType));
        this.getEntityStoreRegistry().registerSystem(new CombatDamageFilter(this.combatantType));

        // 4) Comandos.
        this.getCommandRegistry().registerCommand(new CombateCommand(this));
        this.getCommandRegistry().registerCommand(new DuelistaCommand());
        this.getLogger().at(Level.INFO).log("Combate Direcional: setup concluido.");
    }

    @Override
    protected void start() {
        // Grava o arquivo de config (com os valores padrão) se ainda não existir,
        // para você poder editar.
        this.config.save();
        this.getLogger().at(Level.INFO).log(
                "Combate Direcional %s carregado! Config em: %s",
                this.getManifest().getVersion(),
                this.getDataDirectory().resolve("CombatConfig.json").toAbsolutePath());
    }

    @Override
    protected void shutdown() {
        this.getLogger().at(Level.INFO).log("Combate Direcional: desligando.");
    }
}
