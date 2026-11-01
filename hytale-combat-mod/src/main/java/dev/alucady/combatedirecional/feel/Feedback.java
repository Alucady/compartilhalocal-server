package dev.alucady.combatedirecional.feel;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.camera.CameraEffect;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import dev.alucady.combatedirecional.CombateDirecionalPlugin;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tudo que é "sensação": sons, partículas, tremida de câmera, títulos na tela e animações.
 *
 * Cada ID vem do CombatConfig. Antes de usar, conferimos se ele existe nos assets
 * do jogo; se não existir, avisamos UMA vez no log e seguimos sem o efeito
 * (o combate continua funcionando).
 */
public final class Feedback {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private Feedback() {
    }

    private static void warnOnce(String kind, String id) {
        if (WARNED.add(kind + ":" + id)) {
            CombateDirecionalPlugin.get().getLogger().at(Level.WARNING)
                    .log("Efeito %s '%s' nao existe nesta versao do jogo. Ajuste no CombatConfig.json (Feel).", kind, id);
        }
    }

    private static boolean empty(@Nullable String s) {
        return s == null || s.isBlank();
    }

    @Nullable
    public static Vector3d positionOf(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        TransformComponent t = accessor.getComponent(ref, TransformComponent.getComponentType());
        return t == null ? null : new Vector3d(t.getPosition());
    }

    // ------------------------------------------------------------------ som 3D
    public static void sound(@Nullable String soundId, Ref<EntityStore> at, ComponentAccessor<EntityStore> accessor) {
        if (empty(soundId)) return;
        int index = SoundEvent.getAssetMap().getIndex(soundId);
        if (index == Integer.MIN_VALUE) {
            warnOnce("som", soundId);
            return;
        }
        Vector3d p = positionOf(at, accessor);
        if (p == null) return;
        SoundUtil.playSoundEvent3d(index, SoundCategory.SFX, p.x, p.y + 1.0, p.z, accessor);
    }

    // ------------------------------------------------------------------ partículas
    public static void particle(@Nullable String particleId, Ref<EntityStore> at, ComponentAccessor<EntityStore> accessor) {
        if (empty(particleId)) return;
        if (ParticleSystem.getAssetMap().getAsset(particleId) == null) {
            warnOnce("particula", particleId);
            return;
        }
        Vector3d p = positionOf(at, accessor);
        if (p == null) return;
        p.y += 1.1; // altura do peito
        ParticleUtil.spawnParticleEffect(particleId, p, accessor);
    }

    // ------------------------------------------------------------------ tremida de câmera
    public static void cameraShake(@Nullable String effectId, Ref<EntityStore> player, ComponentAccessor<EntityStore> accessor) {
        if (empty(effectId)) return;
        PlayerRef playerRef = accessor.getComponent(player, PlayerRef.getComponentType());
        if (playerRef == null) return; // só jogadores têm câmera
        int index = CameraEffect.getAssetMap().getIndex(effectId);
        if (index == Integer.MIN_VALUE) {
            warnOnce("camera", effectId);
            return;
        }
        CameraEffect effect = CameraEffect.getAssetMap().getAsset(index);
        if (effect != null) {
            playerRef.getPacketHandler().writeNoCache(effect.createCameraShakePacket());
        }
    }

    // ------------------------------------------------------------------ títulos grandes na tela
    public static void title(Ref<EntityStore> player, ComponentAccessor<EntityStore> accessor,
                             String primary, @Nullable String secondary, String color) {
        if (!CombateDirecionalPlugin.get().combat().feel.showTitles) return;
        PlayerRef playerRef = accessor.getComponent(player, PlayerRef.getComponentType());
        if (playerRef == null) return;
        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw(primary).color(color).bold(true),
                Message.raw(secondary == null ? "" : secondary),
                EventTitleStyle.Default,
                null,
                0.7f,   // tempo na tela (s)
                0.05f,  // aparecer
                0.35f); // sumir
    }

    // ------------------------------------------------------------------ animação
    public static void animation(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, @Nullable String animationId) {
        var cfg = CombateDirecionalPlugin.get().combat().animations;
        if (!cfg.enabled || empty(animationId)) return;
        String set = empty(cfg.animationSet) ? null : cfg.animationSet;
        // Slot "Action": camada de ação por cima do movimento (pernas continuam andando).
        AnimationUtils.playAnimation(ref, AnimationSlot.Action, set, animationId, true, accessor);
    }
}
