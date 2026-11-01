# Modelo inferido (engenharia reversa comportamental)

Este documento explica **como eu deduzi** o funcionamento interno do combate
de referência (For Honor) e das esquivas (Dark Souls 3) **só pelo comportamento**:
medições da comunidade (frame data), guias e observação. Nenhum arquivo, código
ou asset dos jogos foi aberto ou copiado. O código do mod é original, em Java.

Ative com `/combate perfil fiel` (volta com `/combate perfil original`).

Legenda das fontes: **[FH]** medição da comunidade de For Honor,
**[DS3]** medição da comunidade de Dark Souls 3, **[est]** estimativa minha.

## 1. Máquina de estados deduzida

Observando o jogo, todo lutador está sempre em **um** destes estados:

```
          ┌──────────── troca de guarda (100 ms até "valer") ─────────────┐
          ▼                                                                │
   ┌─────────────┐  ataque   ┌──────────────┐ impacto ┌─────────────┐      │
   │ EM GUARDA   │──────────▶│ PREPARAÇÃO   │────────▶│ RECUPERAÇÃO │──────┘
   │ (bloqueia)  │           │ (não bloqueia)│        │ (bloqueia)  │
   └─────┬───────┘           └──────┬───────┘         └─────────────┘
         │ esquiva                  │ finta (só pesado, antes da janela de aparo)
         ▼                          ▼
   ┌─────────────┐            volta para EM GUARDA
   │ ESQUIVA /   │
   │ ROLAMENTO   │── invencível só numa parte ──▶ RECUPERAÇÃO
   └─────────────┘
   ATORDOADO e AGARRADO: não age, não bloqueia (agarrado ainda pode contra-quebrar)
```

Pistas que levaram a esse modelo:
- Não dá para bloquear enquanto prepara um golpe (é por isso que existe "trade" de golpes).
- A guarda nova não protege no mesmo instante: golpes que chegam logo depois da troca
  ainda acertam → existe um **atraso de troca** (~100 ms [FH]).
- A finta só funciona até um certo ponto, e esse ponto coincide com a abertura
  da janela de aparo [FH] → a finta é cancelada quando o "compromisso" do golpe começa.

## 2. Linha do tempo de um golpe (exemplo: pesado de 800 ms, perfil fiel)

```
0 ms                 400                 500        700   800
|--------------------|-------------------|----------|-----|
[ quebra pega aqui   ]                                       ← vulnerável à quebra nos primeiros 400 ms [FH]
                     [  hiperarmadura (leves não interrompem) ]  ← 2a metade [DS3]
[        pode fintar            ]                             ← até 300 ms antes do impacto [FH]
                                         [ APARO  ]           ← 300 a 100 ms antes do impacto [FH]
                                                    [tarde]   ← últimos 100 ms: aparo não pega mais
                                                          ▲ impacto
```

## 3. Regras deduzidas e como implementei

| Regra observada | Dedução | Onde no código |
|---|---|---|
| Aparo só funciona perto do fim do golpe, mas não no último instante [FH] | janela = [impacto−300, impacto−100] ms | `CombatRules.tryParry` (`WindowMs`, `WindowEndMs`) |
| Leve aparado deixa você mais tempo exposto que pesado aparado (900 vs 600 ms) [FH] | atordoamento depende do tipo do golpe aparado | `LightParriedStunMs`, `HeavyParriedStunMs` |
| A contra-quebra funciona mesmo depois de a quebra "pegar" (~300 ms) [FH]; tempo total de reação 600–700 ms | quebra tem 2 fases: preparação (400 ms) + agarrão (300 ms) | estado "AGARRADO" em `Combatant`, `CounterAfterLandMs`, `CombatRules.resolveGrab` |
| Quebra só pega golpes no começo: até 100 ms em golpes ≤700 ms, até 400 ms em golpes de 800–1000 ms [FH] | regra de vulnerabilidade por velocidade do golpe | `VulnerabilityRules`, `VulnerableFastMs`, `VulnerableSlowMs` |
| Quebra custa 15 de stamina [FH] | custo fixo | `GuardBreak.StaminaCost` |
| Sem stamina: não finta e não rola [FH] | travas na exaustão | `ExhaustedCanFeint`, `ExhaustedCanRoll` |
| Esquiva lateral/para trás invencível de 166 a 300 ms; para frente não tem invencibilidade; duração ~633 ms [FH] | invencibilidade com início atrasado e dependente da direção | `InvulnerableStartMs`, `InvulnerableMs`, `ForwardInvulnerable`, `DurationMs` |
| Rolamento leve: 13 frames de invencibilidade a 30 fps = 433 ms [DS3] | invencibilidade desde o início, fração da duração | `Roll.InvulnerableFraction` |
| Pesados com "poise frames" na 2a metade da preparação [DS3] | leves não interrompem pesados nessa fase | `HeavyAttack.HyperArmorFrom` |
| Golpe que pega o outro no meio do ataque causa +30% [DS3] | multiplicador de contra-golpe | `Block.CounterHitMultiplier` |
| Stamina regenera 45/s numa barra de 160 [DS3] | ~28/s na nossa barra de 100; regenera andando | `Stamina.RegenPerSecond`, `RegenOnlyWhenStill: false` |

## 4. O que ainda é estimativa (dá para medir melhor)

- Velocidade exata dos leves (500 ms) e pesados (800 ms): variam por personagem no jogo de referência.
- Duração total do rolamento e brecha final.
- Atraso para a stamina voltar a regenerar.

### Como medir você mesmo (método de engenharia reversa por vídeo)
1. Grave ou baixe um vídeo de gameplay a **60 fps**.
2. Abra num player que avança quadro a quadro (ex.: VLC com a tecla `E`, ou o Shotcut).
3. Anote o quadro em que o golpe **começa** (primeiro movimento) e o quadro do **impacto**.
4. `tempo (ms) = (quadro_impacto − quadro_início) × 16,7`.
5. Repita 5 vezes e use a média. Coloque o valor no `CombatConfig.json` e `/combate recarregar`.

## Fontes
- For Honor: guias e wiki da comunidade (forhonor.fandom.com: Guard Break, Art of Battle; forhonorinfohub.com; discussões da comunidade na Steam).
- Dark Souls 3: "DS3 mechanics cheat sheet" (github.com/gastevens), wiki Fextralife (Carthus Bloodring), guia de pesos do TheGamer.
