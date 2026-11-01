package dev.alucady.combatedirecional.config;

/**
 * Perfis prontos de valores.
 *
 * ORIGINAL = os valores da especificação do projeto.
 * FIEL     = valores deduzidos por observação/medição da comunidade de cada jogo
 *            (engenharia reversa comportamental: só números e regras, nenhum arquivo).
 *            Cada número está marcado com a origem: [FH] For Honor, [DS3] Dark Souls 3,
 *            [est] estimativa minha (não achei medição confiável).
 *
 * Só mexe nas regras de combate. HUD, animações, efeitos, controles de flick e
 * o duelista ficam como você configurou.
 */
public final class Profiles {

    private Profiles() {
    }

    public static void applyOriginal(CombatConfig c) {
        CombatConfig d = new CombatConfig();
        c.profile = "Original";
        c.lightAttack = d.lightAttack;
        c.heavyAttack = d.heavyAttack;
        c.block = d.block;
        c.parry = d.parry;
        c.guardBreak = d.guardBreak;
        c.dodge = d.dodge;
        c.roll = d.roll;
        c.stamina = d.stamina;
        c.mana = d.mana;
        c.controls.guardSwitchMs = d.controls.guardSwitchMs;
    }

    public static void applyFaithful(CombatConfig c) {
        applyOriginal(c); // parte da base e só troca o que tem medição
        c.profile = "Fiel";

        // ---- ataques
        c.lightAttack.windupMs = 500;            // [est] leves "rápidos" da maioria dos heróis
        c.heavyAttack.windupMs = 800;            // [FH] faixa 800–1000 ms; 800 = pesados ainda puníveis pela quebra
        c.heavyAttack.feintCutoffMs = 300;       // [FH] a janela de finta termina quando abre a de aparo
        c.heavyAttack.hyperArmorFrom = 0.5;      // [DS3] "poise frames" na 2a metade da preparação

        // ---- aparo
        c.parry.windowMs = 300;                  // [FH] abre 300 ms antes do impacto
        c.parry.windowEndMs = 100;               // [FH] fecha 100 ms antes do impacto
        c.parry.lightParriedStunMs = 900;        // [FH] leve aparado: 900 ms
        c.parry.heavyParriedStunMs = 600;        // [FH] pesado aparado: 600 ms

        // ---- quebra de guarda
        c.guardBreak.windupMs = 400;             // [FH] quebra normal 400 ms
        c.guardBreak.counterAfterLandMs = 300;   // [FH] contra-quebra até 300 ms depois de pegar
        c.guardBreak.staminaCost = 15;           // [FH] custa 15 de stamina
        c.guardBreak.vulnerabilityRules = true;  // [FH] só pega no começo do golpe do outro
        c.guardBreak.vulnerableFastMs = 100;     // [FH] golpes até 700 ms: primeiros 100 ms
        c.guardBreak.vulnerableSlowMs = 400;     // [FH] golpes 800–1000 ms: primeiros 400 ms

        // ---- esquiva curta (estilo FH)
        c.dodge.durationMs = 633;                // [FH] esquiva completa ~633 ms
        c.dodge.invulnerableStartMs = 166;       // [FH] invencível de 166 ms...
        c.dodge.invulnerableMs = 300;            // [FH] ...até 300 ms
        c.dodge.forwardInvulnerable = false;     // [FH] esquiva para frente não tem invencibilidade

        // ---- rolamento (estilo DS3, rolamento leve)
        c.roll.durationMs = 700;                 // [est]
        c.roll.invulnerableFraction = 433.0 / 700.0; // [DS3] 13 frames a 30 fps = 433 ms
        c.roll.recoveryMs = 250;                 // [est]

        // ---- stamina
        c.stamina.regenPerSecond = 28;           // [DS3] 45/s numa barra de 160 => ~28/s na nossa barra de 100
        c.stamina.regenOnlyWhenStill = false;    // [DS3]/[FH] regenera andando, só não regenera agindo
        c.stamina.exhaustedCanFeint = false;     // [FH] sem stamina não finta
        c.stamina.exhaustedCanRoll = false;      // [FH] sem stamina não rola

        // ---- outros
        c.block.counterHitMultiplier = 1.3;      // [DS3] golpe que pega o outro atacando: +30%
        c.controls.guardSwitchMs = 100;          // [FH] trocar a guarda leva 100 ms
    }
}
