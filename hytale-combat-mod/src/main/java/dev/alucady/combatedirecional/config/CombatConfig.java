package dev.alucady.combatedirecional.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Todos os números do combate ficam aqui.
 *
 * O servidor grava este arquivo em: mods/Alucady_CombateDirecional/CombatConfig.json
 * Você pode editar o JSON e usar "/combate recarregar" sem reiniciar o servidor.
 *
 * Regra da API do Hytale: as chaves do JSON precisam começar com letra MAIÚSCULA.
 * Tempos em milissegundos (ms). 1000 ms = 1 segundo.
 */
public final class CombatConfig {

    // ------------------------------------------------------------------ Ataque leve
    public static final class LightAttack {
        public int windupMs = 480;   // preparação antes do impacto
        public double damage = 9;
        public int staminaCost = 10;
        public int manaGain = 10;    // mana ganha ao acertar
        public int recoveryMs = 300; // PROPOSTA: continuação/recuperação depois do impacto

        public static final BuilderCodec<LightAttack> CODEC = BuilderCodec.builder(LightAttack.class, LightAttack::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("Damage", Codec.DOUBLE), (o, v) -> o.damage = v, o -> o.damage).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .append(new KeyedCodec<>("RecoveryMs", Codec.INTEGER), (o, v) -> o.recoveryMs = v, o -> o.recoveryMs).add()
                .build();
    }

    // ------------------------------------------------------------------ Ataque pesado
    public static final class HeavyAttack {
        public int windupMs = 850;
        public double damage = 20;
        public int staminaCost = 18;
        public int manaGain = 15;
        public int feintCutoffMs = 140; // só dá pra fintar até X ms antes do impacto
        public int feintRecoveryMs = 120; // PROPOSTA: tempo até agir de novo após fintar
        public int recoveryMs = 500;    // PROPOSTA

        public static final BuilderCodec<HeavyAttack> CODEC = BuilderCodec.builder(HeavyAttack.class, HeavyAttack::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("Damage", Codec.DOUBLE), (o, v) -> o.damage = v, o -> o.damage).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .append(new KeyedCodec<>("FeintCutoffMs", Codec.INTEGER), (o, v) -> o.feintCutoffMs = v, o -> o.feintCutoffMs).add()
                .append(new KeyedCodec<>("FeintRecoveryMs", Codec.INTEGER), (o, v) -> o.feintRecoveryMs = v, o -> o.feintRecoveryMs).add()
                .append(new KeyedCodec<>("RecoveryMs", Codec.INTEGER), (o, v) -> o.recoveryMs = v, o -> o.recoveryMs).add()
                .build();
    }

    // ------------------------------------------------------------------ Bloqueio
    public static final class Block {
        public int manaGain = 6;
        public int attackerRecoilMs = 350;   // PROPOSTA: atacante "quica" na guarda
        public double heavyChipDamage = 0;   // PROPOSTA: dano que passa pela guarda no pesado

        public static final BuilderCodec<Block> CODEC = BuilderCodec.builder(Block.class, Block::new)
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .append(new KeyedCodec<>("AttackerRecoilMs", Codec.INTEGER), (o, v) -> o.attackerRecoilMs = v, o -> o.attackerRecoilMs).add()
                .append(new KeyedCodec<>("HeavyChipDamage", Codec.DOUBLE), (o, v) -> o.heavyChipDamage = v, o -> o.heavyChipDamage).add()
                .build();
    }

    // ------------------------------------------------------------------ Aparo
    public static final class Parry {
        public int windowMs = 300;        // últimos X ms do golpe inimigo
        public int attackerStunMs = 850;
        public int manaGain = 20;

        public static final BuilderCodec<Parry> CODEC = BuilderCodec.builder(Parry.class, Parry::new)
                .append(new KeyedCodec<>("WindowMs", Codec.INTEGER), (o, v) -> o.windowMs = v, o -> o.windowMs).add()
                .append(new KeyedCodec<>("AttackerStunMs", Codec.INTEGER), (o, v) -> o.attackerStunMs = v, o -> o.attackerStunMs).add()
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .build();
    }

    // ------------------------------------------------------------------ Quebra de guarda
    public static final class GuardBreak {
        public int windupMs = 320;
        public int stunMs = 1000;
        public int stunExhaustedMs = 1400;
        public int staminaCost = 10;         // PROPOSTA
        public int whiffRecoveryMs = 450;    // PROPOSTA: errou a quebra
        public int counteredRecoveryMs = 500; // PROPOSTA: quebra contra-quebrada

        public static final BuilderCodec<GuardBreak> CODEC = BuilderCodec.builder(GuardBreak.class, GuardBreak::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("StunMs", Codec.INTEGER), (o, v) -> o.stunMs = v, o -> o.stunMs).add()
                .append(new KeyedCodec<>("StunExhaustedMs", Codec.INTEGER), (o, v) -> o.stunExhaustedMs = v, o -> o.stunExhaustedMs).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("WhiffRecoveryMs", Codec.INTEGER), (o, v) -> o.whiffRecoveryMs = v, o -> o.whiffRecoveryMs).add()
                .append(new KeyedCodec<>("CounteredRecoveryMs", Codec.INTEGER), (o, v) -> o.counteredRecoveryMs = v, o -> o.counteredRecoveryMs).add()
                .build();
    }

    // ------------------------------------------------------------------ Esquiva curta
    public static final class Dodge {
        public int invulnerableMs = 380;
        public int staminaCost = 12;
        public int durationMs = 450;     // PROPOSTA: duração total da esquiva
        public double impulse = 9.0;     // PROPOSTA: força do impulso (blocos/s)

        public static final BuilderCodec<Dodge> CODEC = BuilderCodec.builder(Dodge.class, Dodge::new)
                .append(new KeyedCodec<>("InvulnerableMs", Codec.INTEGER), (o, v) -> o.invulnerableMs = v, o -> o.invulnerableMs).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("DurationMs", Codec.INTEGER), (o, v) -> o.durationMs = v, o -> o.durationMs).add()
                .append(new KeyedCodec<>("Impulse", Codec.DOUBLE), (o, v) -> o.impulse = v, o -> o.impulse).add()
                .build();
    }

    // ------------------------------------------------------------------ Rolamento
    public static final class Roll {
        public int durationMs = 700;                 // PROPOSTA: duração total (não estava na especificação)
        public double invulnerableFraction = 0.60;   // primeiros 60% da animação
        public int staminaCost = 20;
        public int recoveryMs = 250;                 // PROPOSTA: brecha vulnerável no final
        public double impulse = 13.0;                // PROPOSTA

        public static final BuilderCodec<Roll> CODEC = BuilderCodec.builder(Roll.class, Roll::new)
                .append(new KeyedCodec<>("DurationMs", Codec.INTEGER), (o, v) -> o.durationMs = v, o -> o.durationMs).add()
                .append(new KeyedCodec<>("InvulnerableFraction", Codec.DOUBLE), (o, v) -> o.invulnerableFraction = v, o -> o.invulnerableFraction).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("RecoveryMs", Codec.INTEGER), (o, v) -> o.recoveryMs = v, o -> o.recoveryMs).add()
                .append(new KeyedCodec<>("Impulse", Codec.DOUBLE), (o, v) -> o.impulse = v, o -> o.impulse).add()
                .build();
    }

    // ------------------------------------------------------------------ Stamina e exaustão
    public static final class Stamina {
        public double max = 100;                    // PROPOSTA
        public double regenPerSecond = 25;          // PROPOSTA: só regenera parado
        public int regenDelayMs = 600;              // PROPOSTA: espera após gastar
        public double exhaustRecoverAt = 55;        // sai da exaustão ao chegar nisto
        public double exhaustedWindupMultiplier = 1.25;     // golpes 25% mais lentos
        public double exhaustedDamageTakenMultiplier = 1.25; // +25% de dano recebido
        public boolean regenOnlyWhenStill = true;   // "regenera parado"
        public boolean mirrorToVanillaBar = true;   // mostra nossa stamina na barra do jogo

        public static final BuilderCodec<Stamina> CODEC = BuilderCodec.builder(Stamina.class, Stamina::new)
                .append(new KeyedCodec<>("Max", Codec.DOUBLE), (o, v) -> o.max = v, o -> o.max).add()
                .append(new KeyedCodec<>("RegenPerSecond", Codec.DOUBLE), (o, v) -> o.regenPerSecond = v, o -> o.regenPerSecond).add()
                .append(new KeyedCodec<>("RegenDelayMs", Codec.INTEGER), (o, v) -> o.regenDelayMs = v, o -> o.regenDelayMs).add()
                .append(new KeyedCodec<>("ExhaustRecoverAt", Codec.DOUBLE), (o, v) -> o.exhaustRecoverAt = v, o -> o.exhaustRecoverAt).add()
                .append(new KeyedCodec<>("ExhaustedWindupMultiplier", Codec.DOUBLE), (o, v) -> o.exhaustedWindupMultiplier = v, o -> o.exhaustedWindupMultiplier).add()
                .append(new KeyedCodec<>("ExhaustedDamageTakenMultiplier", Codec.DOUBLE), (o, v) -> o.exhaustedDamageTakenMultiplier = v, o -> o.exhaustedDamageTakenMultiplier).add()
                .append(new KeyedCodec<>("RegenOnlyWhenStill", Codec.BOOLEAN), (o, v) -> o.regenOnlyWhenStill = v, o -> o.regenOnlyWhenStill).add()
                .append(new KeyedCodec<>("MirrorToVanillaBar", Codec.BOOLEAN), (o, v) -> o.mirrorToVanillaBar = v, o -> o.mirrorToVanillaBar).add()
                .build();
    }

    // ------------------------------------------------------------------ Mana e habilidades
    public static final class Mana {
        public double max = 100;
        public int arcaneStrikeCost = 50;     // Golpe Arcano: pesado imbloqueável
        public int furyCost = 100;            // Fúria
        public double furyDamageMultiplier = 1.5;
        public int furyDurationMs = 8000;
        public boolean mirrorToVanillaBar = true;

        public static final BuilderCodec<Mana> CODEC = BuilderCodec.builder(Mana.class, Mana::new)
                .append(new KeyedCodec<>("Max", Codec.DOUBLE), (o, v) -> o.max = v, o -> o.max).add()
                .append(new KeyedCodec<>("ArcaneStrikeCost", Codec.INTEGER), (o, v) -> o.arcaneStrikeCost = v, o -> o.arcaneStrikeCost).add()
                .append(new KeyedCodec<>("FuryCost", Codec.INTEGER), (o, v) -> o.furyCost = v, o -> o.furyCost).add()
                .append(new KeyedCodec<>("FuryDamageMultiplier", Codec.DOUBLE), (o, v) -> o.furyDamageMultiplier = v, o -> o.furyDamageMultiplier).add()
                .append(new KeyedCodec<>("FuryDurationMs", Codec.INTEGER), (o, v) -> o.furyDurationMs = v, o -> o.furyDurationMs).add()
                .append(new KeyedCodec<>("MirrorToVanillaBar", Codec.BOOLEAN), (o, v) -> o.mirrorToVanillaBar = v, o -> o.mirrorToVanillaBar).add()
                .build();
    }

    // ------------------------------------------------------------------ "Sensação" e efeitos (fase 6)
    // IDs de sons/partículas/câmera vêm do jogo base. Se algum não existir na sua
    // versão, o mod avisa no log e simplesmente não toca aquele efeito.
    // Deixe "" (vazio) para desligar um efeito.
    public static final class Feel {
        public int hitStopMs = 60;   // pausa no impacto (40–90 ms)
        public String hitSound = "SFX_Sword_T2_Impact";
        public String blockSound = "SFX_Sword_T2_Impact";
        public String parrySound = "SFX_Sword_T2_Impact";
        public String swingSound = "SFX_Light_Melee_T2_Swing";
        public String hitParticle = "Impact_Sword_Basic";
        public String blockParticle = "Impact_Sword_Basic";
        public String guardBreakParticle = "";
        public String furyParticle = "";
        public String arcaneParticle = "";
        public String hitCameraEffect = "Impact";
        public String heavyHitCameraEffect = "Impact";
        public String lightHitCameraEffect = "Impact_Light";
        public boolean showTitles = true;      // títulos grandes: APARO!, QUEBRA!, EXAUSTO
        public boolean showNameplates = true;  // texto acima da cabeça do duelista

        public static final BuilderCodec<Feel> CODEC = BuilderCodec.builder(Feel.class, Feel::new)
                .append(new KeyedCodec<>("HitStopMs", Codec.INTEGER), (o, v) -> o.hitStopMs = v, o -> o.hitStopMs).add()
                .append(new KeyedCodec<>("HitSound", Codec.STRING), (o, v) -> o.hitSound = v, o -> o.hitSound).add()
                .append(new KeyedCodec<>("BlockSound", Codec.STRING), (o, v) -> o.blockSound = v, o -> o.blockSound).add()
                .append(new KeyedCodec<>("ParrySound", Codec.STRING), (o, v) -> o.parrySound = v, o -> o.parrySound).add()
                .append(new KeyedCodec<>("SwingSound", Codec.STRING), (o, v) -> o.swingSound = v, o -> o.swingSound).add()
                .append(new KeyedCodec<>("HitParticle", Codec.STRING), (o, v) -> o.hitParticle = v, o -> o.hitParticle).add()
                .append(new KeyedCodec<>("BlockParticle", Codec.STRING), (o, v) -> o.blockParticle = v, o -> o.blockParticle).add()
                .append(new KeyedCodec<>("GuardBreakParticle", Codec.STRING), (o, v) -> o.guardBreakParticle = v, o -> o.guardBreakParticle).add()
                .append(new KeyedCodec<>("FuryParticle", Codec.STRING), (o, v) -> o.furyParticle = v, o -> o.furyParticle).add()
                .append(new KeyedCodec<>("ArcaneParticle", Codec.STRING), (o, v) -> o.arcaneParticle = v, o -> o.arcaneParticle).add()
                .append(new KeyedCodec<>("HitCameraEffect", Codec.STRING), (o, v) -> o.hitCameraEffect = v, o -> o.hitCameraEffect).add()
                .append(new KeyedCodec<>("HeavyHitCameraEffect", Codec.STRING), (o, v) -> o.heavyHitCameraEffect = v, o -> o.heavyHitCameraEffect).add()
                .append(new KeyedCodec<>("LightHitCameraEffect", Codec.STRING), (o, v) -> o.lightHitCameraEffect = v, o -> o.lightHitCameraEffect).add()
                .append(new KeyedCodec<>("ShowTitles", Codec.BOOLEAN), (o, v) -> o.showTitles = v, o -> o.showTitles).add()
                .append(new KeyedCodec<>("ShowNameplates", Codec.BOOLEAN), (o, v) -> o.showNameplates = v, o -> o.showNameplates).add()
                .build();
    }

    // ------------------------------------------------------------------ Controles (guarda pelo mouse)
    public static final class Controls {
        public double flickYawDegrees = 12;     // girar a câmera X graus rápido = trocar guarda esq/dir
        public double flickPitchDegrees = 10;   // olhar pra cima X graus rápido = guarda em cima
        public int flickWindowMs = 150;         // "rápido" = dentro deste tempo
        public boolean invertYaw = false;       // troque se esquerda/direita ficarem trocadas
        public boolean mirrorSides = true;      // golpe vindo da esquerda do atacante chega na SUA direita
        public boolean guardOnlyWhileCrouching = false; // true = só troca guarda segurando agachar

        public static final BuilderCodec<Controls> CODEC = BuilderCodec.builder(Controls.class, Controls::new)
                .append(new KeyedCodec<>("FlickYawDegrees", Codec.DOUBLE), (o, v) -> o.flickYawDegrees = v, o -> o.flickYawDegrees).add()
                .append(new KeyedCodec<>("FlickPitchDegrees", Codec.DOUBLE), (o, v) -> o.flickPitchDegrees = v, o -> o.flickPitchDegrees).add()
                .append(new KeyedCodec<>("FlickWindowMs", Codec.INTEGER), (o, v) -> o.flickWindowMs = v, o -> o.flickWindowMs).add()
                .append(new KeyedCodec<>("InvertYaw", Codec.BOOLEAN), (o, v) -> o.invertYaw = v, o -> o.invertYaw).add()
                .append(new KeyedCodec<>("MirrorSides", Codec.BOOLEAN), (o, v) -> o.mirrorSides = v, o -> o.mirrorSides).add()
                .append(new KeyedCodec<>("GuardOnlyWhileCrouching", Codec.BOOLEAN), (o, v) -> o.guardOnlyWhileCrouching = v, o -> o.guardOnlyWhileCrouching).add()
                .build();
    }

    // ------------------------------------------------------------------ Alcance dos golpes
    public static final class Reach {
        public double range = 3.2;          // blocos
        public double coneDegrees = 75;     // abertura do "cone" à frente
        public double lockOnRange = 8;      // trava no alvo a esta distância

        public static final BuilderCodec<Reach> CODEC = BuilderCodec.builder(Reach.class, Reach::new)
                .append(new KeyedCodec<>("Range", Codec.DOUBLE), (o, v) -> o.range = v, o -> o.range).add()
                .append(new KeyedCodec<>("ConeDegrees", Codec.DOUBLE), (o, v) -> o.coneDegrees = v, o -> o.coneDegrees).add()
                .append(new KeyedCodec<>("LockOnRange", Codec.DOUBLE), (o, v) -> o.lockOnRange = v, o -> o.lockOnRange).add()
                .build();
    }

    // ------------------------------------------------------------------ HUD
    public static final class Hud {
        public boolean enabled = true;

        public static final BuilderCodec<Hud> CODEC = BuilderCodec.builder(Hud.class, Hud::new)
                .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (o, v) -> o.enabled = v, o -> o.enabled).add()
                .build();
    }

    // ------------------------------------------------------------------ Animações (fase 7)
    // Por padrão usa o conjunto de animações da espada do jogo base como "provisório".
    // Quando você criar as suas no Blockbench, troque AnimationSet para "CombateDirecional_Espada".
    public static final class Animations {
        public boolean enabled = true;
        public String animationSet = "Sword";
        public String lightLeft = "SwingLeft";
        public String lightTop = "SwingDown";
        public String lightRight = "SwingRight";
        public String heavyLeft = "SwingLeft";
        public String heavyTop = "SwingDown";
        public String heavyRight = "SwingRight";
        public String guardBreak = "";
        public String parry = "";
        public String stunned = "";
        public String blockLeft = "";
        public String blockTop = "";
        public String blockRight = "";
        public String dodgeForward = "";
        public String dodgeBack = "";
        public String dodgeLeft = "";
        public String dodgeRight = "";
        public String roll = "";
        public String arcane = "";
        public String fury = "";

        public static final BuilderCodec<Animations> CODEC = BuilderCodec.builder(Animations.class, Animations::new)
                .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (o, v) -> o.enabled = v, o -> o.enabled).add()
                .append(new KeyedCodec<>("AnimationSet", Codec.STRING), (o, v) -> o.animationSet = v, o -> o.animationSet).add()
                .append(new KeyedCodec<>("LightLeft", Codec.STRING), (o, v) -> o.lightLeft = v, o -> o.lightLeft).add()
                .append(new KeyedCodec<>("LightTop", Codec.STRING), (o, v) -> o.lightTop = v, o -> o.lightTop).add()
                .append(new KeyedCodec<>("LightRight", Codec.STRING), (o, v) -> o.lightRight = v, o -> o.lightRight).add()
                .append(new KeyedCodec<>("HeavyLeft", Codec.STRING), (o, v) -> o.heavyLeft = v, o -> o.heavyLeft).add()
                .append(new KeyedCodec<>("HeavyTop", Codec.STRING), (o, v) -> o.heavyTop = v, o -> o.heavyTop).add()
                .append(new KeyedCodec<>("HeavyRight", Codec.STRING), (o, v) -> o.heavyRight = v, o -> o.heavyRight).add()
                .append(new KeyedCodec<>("GuardBreak", Codec.STRING), (o, v) -> o.guardBreak = v, o -> o.guardBreak).add()
                .append(new KeyedCodec<>("Parry", Codec.STRING), (o, v) -> o.parry = v, o -> o.parry).add()
                .append(new KeyedCodec<>("Stunned", Codec.STRING), (o, v) -> o.stunned = v, o -> o.stunned).add()
                .append(new KeyedCodec<>("BlockLeft", Codec.STRING), (o, v) -> o.blockLeft = v, o -> o.blockLeft).add()
                .append(new KeyedCodec<>("BlockTop", Codec.STRING), (o, v) -> o.blockTop = v, o -> o.blockTop).add()
                .append(new KeyedCodec<>("BlockRight", Codec.STRING), (o, v) -> o.blockRight = v, o -> o.blockRight).add()
                .append(new KeyedCodec<>("DodgeForward", Codec.STRING), (o, v) -> o.dodgeForward = v, o -> o.dodgeForward).add()
                .append(new KeyedCodec<>("DodgeBack", Codec.STRING), (o, v) -> o.dodgeBack = v, o -> o.dodgeBack).add()
                .append(new KeyedCodec<>("DodgeLeft", Codec.STRING), (o, v) -> o.dodgeLeft = v, o -> o.dodgeLeft).add()
                .append(new KeyedCodec<>("DodgeRight", Codec.STRING), (o, v) -> o.dodgeRight = v, o -> o.dodgeRight).add()
                .append(new KeyedCodec<>("Roll", Codec.STRING), (o, v) -> o.roll = v, o -> o.roll).add()
                .append(new KeyedCodec<>("Arcane", Codec.STRING), (o, v) -> o.arcane = v, o -> o.arcane).add()
                .append(new KeyedCodec<>("Fury", Codec.STRING), (o, v) -> o.fury = v, o -> o.fury).add()
                .build();
    }

    // ------------------------------------------------------------------ NPC duelista (fase 8)
    public static final class Duelist {
        public String role = "";                 // papel (role) de NPC do jogo usado como "corpo"
        public double reactionChance = 0.55;     // chance de acertar a guarda contra seu golpe
        public int reactionMinMs = 220;          // tempo de reação humano simulado
        public int reactionMaxMs = 420;
        public double parryChance = 0.15;
        public double dodgeChance = 0.10;
        public double guardBreakChance = 0.15;
        public double heavyChance = 0.35;
        public double feintChance = 0.20;
        public int thinkMinMs = 700;             // intervalo entre decisões de ataque
        public int thinkMaxMs = 1600;
        public double walkSpeed = 3.0;           // blocos por segundo
        public double preferredDistance = 2.4;

        public static final BuilderCodec<Duelist> CODEC = BuilderCodec.builder(Duelist.class, Duelist::new)
                .append(new KeyedCodec<>("Role", Codec.STRING), (o, v) -> o.role = v, o -> o.role).add()
                .append(new KeyedCodec<>("ReactionChance", Codec.DOUBLE), (o, v) -> o.reactionChance = v, o -> o.reactionChance).add()
                .append(new KeyedCodec<>("ReactionMinMs", Codec.INTEGER), (o, v) -> o.reactionMinMs = v, o -> o.reactionMinMs).add()
                .append(new KeyedCodec<>("ReactionMaxMs", Codec.INTEGER), (o, v) -> o.reactionMaxMs = v, o -> o.reactionMaxMs).add()
                .append(new KeyedCodec<>("ParryChance", Codec.DOUBLE), (o, v) -> o.parryChance = v, o -> o.parryChance).add()
                .append(new KeyedCodec<>("DodgeChance", Codec.DOUBLE), (o, v) -> o.dodgeChance = v, o -> o.dodgeChance).add()
                .append(new KeyedCodec<>("GuardBreakChance", Codec.DOUBLE), (o, v) -> o.guardBreakChance = v, o -> o.guardBreakChance).add()
                .append(new KeyedCodec<>("HeavyChance", Codec.DOUBLE), (o, v) -> o.heavyChance = v, o -> o.heavyChance).add()
                .append(new KeyedCodec<>("FeintChance", Codec.DOUBLE), (o, v) -> o.feintChance = v, o -> o.feintChance).add()
                .append(new KeyedCodec<>("ThinkMinMs", Codec.INTEGER), (o, v) -> o.thinkMinMs = v, o -> o.thinkMinMs).add()
                .append(new KeyedCodec<>("ThinkMaxMs", Codec.INTEGER), (o, v) -> o.thinkMaxMs = v, o -> o.thinkMaxMs).add()
                .append(new KeyedCodec<>("WalkSpeed", Codec.DOUBLE), (o, v) -> o.walkSpeed = v, o -> o.walkSpeed).add()
                .append(new KeyedCodec<>("PreferredDistance", Codec.DOUBLE), (o, v) -> o.preferredDistance = v, o -> o.preferredDistance).add()
                .build();
    }

    // ------------------------------------------------------------------ Arquivo completo
    public LightAttack lightAttack = new LightAttack();
    public HeavyAttack heavyAttack = new HeavyAttack();
    public Block block = new Block();
    public Parry parry = new Parry();
    public GuardBreak guardBreak = new GuardBreak();
    public Dodge dodge = new Dodge();
    public Roll roll = new Roll();
    public Stamina stamina = new Stamina();
    public Mana mana = new Mana();
    public Feel feel = new Feel();
    public Controls controls = new Controls();
    public Reach reach = new Reach();
    public Hud hud = new Hud();
    public Animations animations = new Animations();
    public Duelist duelist = new Duelist();

    public static final BuilderCodec<CombatConfig> CODEC = BuilderCodec.builder(CombatConfig.class, CombatConfig::new)
            .documentation("Valores do Combate Direcional. Tempos em milissegundos.")
            .append(new KeyedCodec<>("LightAttack", LightAttack.CODEC), (o, v) -> o.lightAttack = v, o -> o.lightAttack).add()
            .append(new KeyedCodec<>("HeavyAttack", HeavyAttack.CODEC), (o, v) -> o.heavyAttack = v, o -> o.heavyAttack).add()
            .append(new KeyedCodec<>("Block", Block.CODEC), (o, v) -> o.block = v, o -> o.block).add()
            .append(new KeyedCodec<>("Parry", Parry.CODEC), (o, v) -> o.parry = v, o -> o.parry).add()
            .append(new KeyedCodec<>("GuardBreak", GuardBreak.CODEC), (o, v) -> o.guardBreak = v, o -> o.guardBreak).add()
            .append(new KeyedCodec<>("Dodge", Dodge.CODEC), (o, v) -> o.dodge = v, o -> o.dodge).add()
            .append(new KeyedCodec<>("Roll", Roll.CODEC), (o, v) -> o.roll = v, o -> o.roll).add()
            .append(new KeyedCodec<>("Stamina", Stamina.CODEC), (o, v) -> o.stamina = v, o -> o.stamina).add()
            .append(new KeyedCodec<>("Mana", Mana.CODEC), (o, v) -> o.mana = v, o -> o.mana).add()
            .append(new KeyedCodec<>("Feel", Feel.CODEC), (o, v) -> o.feel = v, o -> o.feel).add()
            .append(new KeyedCodec<>("Controls", Controls.CODEC), (o, v) -> o.controls = v, o -> o.controls).add()
            .append(new KeyedCodec<>("Reach", Reach.CODEC), (o, v) -> o.reach = v, o -> o.reach).add()
            .append(new KeyedCodec<>("Hud", Hud.CODEC), (o, v) -> o.hud = v, o -> o.hud).add()
            .append(new KeyedCodec<>("Animations", Animations.CODEC), (o, v) -> o.animations = v, o -> o.animations).add()
            .append(new KeyedCodec<>("Duelist", Duelist.CODEC), (o, v) -> o.duelist = v, o -> o.duelist).add()
            .build();
}
