package dev.alucady.combatedirecional.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.alucady.combatedirecional.combat.CombatRules;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Interação customizada do mod. A espada do mod liga cada botão a uma destas:
 *
 *   { "Type": "CombateDirecional_Acao", "Action": "Light" }
 *
 * Action pode ser: Light, Heavy, GuardBreak, Dodge.
 * Quando o jogador aperta o botão, o servidor chama firstRun() e a gente
 * repassa para as regras de combate.
 */
public final class CombatActionInteraction extends SimpleInstantInteraction {

    public static final String TYPE_ID = "CombateDirecional_Acao";

    public static final BuilderCodec<CombatActionInteraction> CODEC = BuilderCodec.builder(
                    CombatActionInteraction.class, CombatActionInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Acao do Combate Direcional (Light, Heavy, GuardBreak, Dodge).")
            .<String>appendInherited(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (interaction, value) -> interaction.action = value,
                    interaction -> interaction.action,
                    (interaction, parent) -> interaction.action = parent.action)
            .add()
            .build();

    private String action = "Light";

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> cb = context.getCommandBuffer();
        if (ref == null || cb == null) return;
        CombatRules.Input input = switch (this.action == null ? "" : this.action.toLowerCase(Locale.ROOT)) {
            case "heavy" -> CombatRules.Input.HEAVY;
            case "guardbreak" -> CombatRules.Input.GUARD_BREAK;
            case "dodge" -> CombatRules.Input.DODGE;
            default -> CombatRules.Input.LIGHT;
        };
        CombatRules.handleInput(ref, input, cb);
    }

    @Nonnull
    @Override
    public String toString() {
        return "CombatActionInteraction{action='" + this.action + "'}";
    }
}
