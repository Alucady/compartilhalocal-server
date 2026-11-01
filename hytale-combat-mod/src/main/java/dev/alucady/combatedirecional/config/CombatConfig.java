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

        public static final BuilderCodec<LightAttack> CODEC = BuilderCodec.builder(LightAttack.class, LightAttack::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("Damage", Codec.DOUBLE), (o, v) -> o.damage = v, o -> o.damage).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .build();
    }

    // ------------------------------------------------------------------ Ataque pesado
    public static final class HeavyAttack {
        public int windupMs = 850;
        public double damage = 20;
        public int staminaCost = 18;
        public int manaGain = 15;
        public int feintCutoffMs = 140; // só dá pra fintar até X ms antes do impacto

        public static final BuilderCodec<HeavyAttack> CODEC = BuilderCodec.builder(HeavyAttack.class, HeavyAttack::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("Damage", Codec.DOUBLE), (o, v) -> o.damage = v, o -> o.damage).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
                .append(new KeyedCodec<>("FeintCutoffMs", Codec.INTEGER), (o, v) -> o.feintCutoffMs = v, o -> o.feintCutoffMs).add()
                .build();
    }

    // ------------------------------------------------------------------ Bloqueio
    public static final class Block {
        public int manaGain = 6;

        public static final BuilderCodec<Block> CODEC = BuilderCodec.builder(Block.class, Block::new)
                .append(new KeyedCodec<>("ManaGain", Codec.INTEGER), (o, v) -> o.manaGain = v, o -> o.manaGain).add()
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

        public static final BuilderCodec<GuardBreak> CODEC = BuilderCodec.builder(GuardBreak.class, GuardBreak::new)
                .append(new KeyedCodec<>("WindupMs", Codec.INTEGER), (o, v) -> o.windupMs = v, o -> o.windupMs).add()
                .append(new KeyedCodec<>("StunMs", Codec.INTEGER), (o, v) -> o.stunMs = v, o -> o.stunMs).add()
                .append(new KeyedCodec<>("StunExhaustedMs", Codec.INTEGER), (o, v) -> o.stunExhaustedMs = v, o -> o.stunExhaustedMs).add()
                .build();
    }

    // ------------------------------------------------------------------ Esquiva curta
    public static final class Dodge {
        public int invulnerableMs = 380;
        public int staminaCost = 12;

        public static final BuilderCodec<Dodge> CODEC = BuilderCodec.builder(Dodge.class, Dodge::new)
                .append(new KeyedCodec<>("InvulnerableMs", Codec.INTEGER), (o, v) -> o.invulnerableMs = v, o -> o.invulnerableMs).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .build();
    }

    // ------------------------------------------------------------------ Rolamento
    public static final class Roll {
        public int durationMs = 700;                 // PROPOSTA: duração total (não estava na especificação)
        public double invulnerableFraction = 0.60;   // primeiros 60% da animação
        public int staminaCost = 20;
        public int recoveryMs = 250;                 // PROPOSTA: brecha vulnerável no final

        public static final BuilderCodec<Roll> CODEC = BuilderCodec.builder(Roll.class, Roll::new)
                .append(new KeyedCodec<>("DurationMs", Codec.INTEGER), (o, v) -> o.durationMs = v, o -> o.durationMs).add()
                .append(new KeyedCodec<>("InvulnerableFraction", Codec.DOUBLE), (o, v) -> o.invulnerableFraction = v, o -> o.invulnerableFraction).add()
                .append(new KeyedCodec<>("StaminaCost", Codec.INTEGER), (o, v) -> o.staminaCost = v, o -> o.staminaCost).add()
                .append(new KeyedCodec<>("RecoveryMs", Codec.INTEGER), (o, v) -> o.recoveryMs = v, o -> o.recoveryMs).add()
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

        public static final BuilderCodec<Stamina> CODEC = BuilderCodec.builder(Stamina.class, Stamina::new)
                .append(new KeyedCodec<>("Max", Codec.DOUBLE), (o, v) -> o.max = v, o -> o.max).add()
                .append(new KeyedCodec<>("RegenPerSecond", Codec.DOUBLE), (o, v) -> o.regenPerSecond = v, o -> o.regenPerSecond).add()
                .append(new KeyedCodec<>("RegenDelayMs", Codec.INTEGER), (o, v) -> o.regenDelayMs = v, o -> o.regenDelayMs).add()
                .append(new KeyedCodec<>("ExhaustRecoverAt", Codec.DOUBLE), (o, v) -> o.exhaustRecoverAt = v, o -> o.exhaustRecoverAt).add()
                .append(new KeyedCodec<>("ExhaustedWindupMultiplier", Codec.DOUBLE), (o, v) -> o.exhaustedWindupMultiplier = v, o -> o.exhaustedWindupMultiplier).add()
                .append(new KeyedCodec<>("ExhaustedDamageTakenMultiplier", Codec.DOUBLE), (o, v) -> o.exhaustedDamageTakenMultiplier = v, o -> o.exhaustedDamageTakenMultiplier).add()
                .build();
    }

    // ------------------------------------------------------------------ Mana e habilidades
    public static final class Mana {
        public double max = 100;
        public int arcaneStrikeCost = 50;     // Golpe Arcano: pesado imbloqueável
        public int furyCost = 100;            // Fúria
        public double furyDamageMultiplier = 1.5;
        public int furyDurationMs = 8000;

        public static final BuilderCodec<Mana> CODEC = BuilderCodec.builder(Mana.class, Mana::new)
                .append(new KeyedCodec<>("Max", Codec.DOUBLE), (o, v) -> o.max = v, o -> o.max).add()
                .append(new KeyedCodec<>("ArcaneStrikeCost", Codec.INTEGER), (o, v) -> o.arcaneStrikeCost = v, o -> o.arcaneStrikeCost).add()
                .append(new KeyedCodec<>("FuryCost", Codec.INTEGER), (o, v) -> o.furyCost = v, o -> o.furyCost).add()
                .append(new KeyedCodec<>("FuryDamageMultiplier", Codec.DOUBLE), (o, v) -> o.furyDamageMultiplier = v, o -> o.furyDamageMultiplier).add()
                .append(new KeyedCodec<>("FuryDurationMs", Codec.INTEGER), (o, v) -> o.furyDurationMs = v, o -> o.furyDurationMs).add()
                .build();
    }

    // ------------------------------------------------------------------ "Sensação" (fase 6)
    public static final class Feel {
        public int hitStopMs = 60;   // congelada no impacto (40–90 ms)

        public static final BuilderCodec<Feel> CODEC = BuilderCodec.builder(Feel.class, Feel::new)
                .append(new KeyedCodec<>("HitStopMs", Codec.INTEGER), (o, v) -> o.hitStopMs = v, o -> o.hitStopMs).add()
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
            .build();
}
