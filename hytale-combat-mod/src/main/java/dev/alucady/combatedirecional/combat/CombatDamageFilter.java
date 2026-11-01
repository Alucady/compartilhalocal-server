package dev.alucady.combatedirecional.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Filtro de dano (roda antes do dano ser aplicado, para QUALQUER fonte de dano):
 * - invencível (esquiva/rolamento) => cancela o dano
 * - EXAUSTO => +25% de dano recebido
 */
public final class CombatDamageFilter extends DamageEventSystem {

    private final ComponentType<EntityStore, Combatant> combatantType;

    public CombatDamageFilter(ComponentType<EntityStore, Combatant> combatantType) {
        this.combatantType = combatantType;
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.combatantType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> cb, @Nonnull Damage damage) {
        Combatant c = chunk.getComponent(index, this.combatantType);
        if (c == null) return;
        long now = CombatRules.now();
        if (c.isInvulnerable(now)) {
            damage.setCancelled(true);
            return;
        }
        if (c.exhausted) {
            damage.setAmount((float) (damage.getAmount() * CombatRules.cfg().stamina.exhaustedDamageTakenMultiplier));
        }
    }
}
