package dev.alucady.combatedirecional.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import dev.alucady.combatedirecional.combat.CombatRules;
import dev.alucady.combatedirecional.combat.Combatant;
import dev.alucady.combatedirecional.npc.DuelistBrain;
import it.unimi.dsi.fastutil.Pair;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

/**
 * /duelista [papel]         -> cria um NPC duelista na sua frente
 * /duelista remover         -> remove os duelistas
 * /duelista papeis [filtro] -> lista os "papéis" (roles) de NPC disponíveis no seu jogo
 *
 * Como funciona: usamos um NPC do jogo base só como "corpo" (modelo + vida),
 * congelamos a IA original dele (componente Frozen, o mesmo do /npc freeze)
 * e a nossa IA (DuelistBrain) passa a controlar: olhar, andar, guarda e golpes.
 */
public final class DuelistaCommand extends AbstractPlayerCommand {

    /** Palpites de papéis humanoides; só usamos os que existirem no seu jogo. */
    private static final String[] CANDIDATE_ROLES = {
            "Skeleton_Fighter", "Skeleton_Soldier", "Trork_Warrior", "Trork_Brawler",
            "Outlander_Brute", "Goblin_Scrapper", "Feran_Windwalker", "Kweebec_Seedling"
    };

    private final OptionalArg<String> roleArg =
            this.withOptionalArg("papel", "combatedirecional.commands.duelista.papel", ArgTypes.STRING);

    public DuelistaCommand() {
        super("duelista", "combatedirecional.commands.duelista.desc");
        this.addSubCommand(new Remover());
        this.addSubCommand(new Papeis());
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef, @Nonnull World world) {
        NPCPlugin npcs = NPCPlugin.get();
        if (npcs == null) {
            context.sendMessage(Message.raw("O sistema de NPCs do servidor nao esta ativo.").color("#ff6060"));
            return;
        }

        String role = this.roleArg.provided(context) ? this.roleArg.get(context) : CombateDirecionalPlugin.get().combat().duelist.role;
        if (role == null || role.isBlank() || !npcs.hasRoleName(role)) {
            String asked = role;
            role = null;
            for (String candidate : CANDIDATE_ROLES) {
                if (npcs.hasRoleName(candidate)) {
                    role = candidate;
                    break;
                }
            }
            if (role == null) {
                context.sendMessage(Message.raw("Papel de NPC " + (asked == null || asked.isBlank() ? "" : "'" + asked + "' ")
                        + "nao encontrado. Veja os disponiveis com /duelista papeis e use /duelista <papel>.").color("#ff6060"));
                return;
            }
        }

        TransformComponent t = store.getComponent(ref, TransformComponent.getComponentType());
        if (t == null) return;
        double[] f = CombatRules.forward(ref, store);
        Vector3d pos = new Vector3d(t.getPosition());
        pos.x += f[0] * 3.0;
        pos.z += f[1] * 3.0;
        float yaw = (float) Math.atan2(f[0], f[1]); // virado para o jogador

        Pair<Ref<EntityStore>, INonPlayerCharacter> spawned = npcs.spawnNPC(store, role, null, pos, new Rotation3f(0f, yaw, 0f));
        if (spawned == null || spawned.first() == null) {
            context.sendMessage(Message.raw("Nao consegui criar o NPC com o papel '" + role + "'.").color("#ff6060"));
            return;
        }
        Ref<EntityStore> npcRef = spawned.first();

        // desliga a IA original e liga a nossa
        store.ensureComponent(npcRef, Frozen.getComponentType());
        Combatant c = new Combatant();
        c.ai = true;
        c.displayName = "Duelista";
        c.aiNextThinkAt = CombatRules.now() + 1500;
        store.putComponent(npcRef, CombateDirecionalPlugin.get().combatantType(), c);
        store.putComponent(npcRef, Nameplate.getComponentType(), new Nameplate("Duelista"));
        DuelistBrain.ACTIVE.add(npcRef);

        context.sendMessage(Message.raw("Duelista criado (corpo: " + role + "). Boa sorte!").color("#60e060"));
    }

    // ------------------------------------------------------------------ /duelista remover
    static final class Remover extends AbstractPlayerCommand {
        Remover() {
            super("remover", "combatedirecional.commands.duelista.remover.desc");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef, @Nonnull World world) {
            int removed = 0;
            for (Ref<EntityStore> npcRef : DuelistBrain.ACTIVE) {
                if (npcRef != null && npcRef.isValid() && npcRef.getStore() == store) {
                    NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
                    if (npc != null) {
                        npc.setToDespawn();
                        removed++;
                    }
                }
                DuelistBrain.ACTIVE.remove(npcRef);
            }
            context.sendMessage(Message.raw(removed + " duelista(s) removido(s)."));
        }
    }

    // ------------------------------------------------------------------ /duelista papeis [filtro]
    static final class Papeis extends AbstractPlayerCommand {
        private final OptionalArg<String> filterArg =
                this.withOptionalArg("filtro", "combatedirecional.commands.duelista.filtro", ArgTypes.STRING);

        Papeis() {
            super("papeis", "combatedirecional.commands.duelista.papeis.desc");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                               @Nonnull PlayerRef playerRef, @Nonnull World world) {
            NPCPlugin npcs = NPCPlugin.get();
            if (npcs == null) return;
            String filter = this.filterArg.provided(context) ? this.filterArg.get(context).toLowerCase(Locale.ROOT) : "";
            List<String> names = npcs.getRoleTemplateNames(true);
            names.sort(String::compareTo);
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (String n : names) {
                if (!filter.isEmpty() && !n.toLowerCase(Locale.ROOT).contains(filter)) continue;
                if (shown > 0) sb.append(", ");
                sb.append(n);
                if (++shown >= 60) {
                    sb.append(" ...");
                    break;
                }
            }
            context.sendMessage(Message.raw(shown == 0 ? "Nenhum papel encontrado." : sb.toString()));
            context.sendMessage(Message.raw("Dica: use um humanoide, ex.: /duelista papeis skeleton").color("#a0d0ff"));
        }
    }
}
