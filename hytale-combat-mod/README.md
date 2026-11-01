# Combate Direcional — mod para Hytale

Combate direcional (guarda em 3 direções, bloqueio, aparo, finta, quebra de
guarda), esquiva e rolamento com invencibilidade, stamina com exaustão, mana com
Golpe Arcano e Fúria, HUD de combate e um NPC duelista para treinar.
Tudo original, recriado a partir da observação das mecânicas.

**Status: Fases 1 a 8 implementadas e compilando contra o servidor oficial 0.7.5.**
Ainda **não foram testadas dentro do jogo** (o ambiente onde foram feitas não
roda o cliente do Hytale). Use o roteiro de teste abaixo e me mande o log se algo falhar.

## Estrutura

```
hytale-combat-mod/
├── build.gradle.kts / gradle.properties / settings.gradle.kts / gradlew
├── docs/
│   ├── PESQUISA_API.md        ← o que a API permite, limitações e contornos
│   ├── ANIMACOES.md           ← lista completa das animações (Fase 7)
│   └── modelos/               ← modelo de JSON para ligar suas animações
└── src/main/
    ├── java/dev/alucady/combatedirecional/
    │   ├── CombateDirecionalPlugin.java     ← registra tudo
    │   ├── config/CombatConfig.java         ← TODOS os números (vira CombatConfig.json)
    │   ├── combat/
    │   │   ├── Combatant.java               ← estado de combate de cada jogador/NPC
    │   │   ├── CombatRules.java             ← regras: ataques, bloqueio, aparo, finta, quebra, esquiva, mana
    │   │   ├── CombatTickSystem.java        ← roda a cada tick: timers, guarda pelo mouse, stamina, HUD
    │   │   ├── CombatDamageFilter.java      ← invencibilidade e +25% de dano no exausto
    │   │   ├── Direction.java / AttackKind.java
    │   ├── interaction/CombatActionInteraction.java  ← liga os botões da espada às regras
    │   ├── feel/Feedback.java               ← sons, partículas, câmera, títulos, animações
    │   ├── hud/CombatHud.java               ← HUD de combate
    │   ├── npc/DuelistBrain.java            ← IA do duelista
    │   └── command/                         ← /combate e /duelista
    └── resources/                           ← asset pack
        ├── manifest.json
        └── Server/
            ├── Item/Items/CombateDirecional/CombateDirecional_Espada.json
            ├── Item/Interactions/CombateDirecional/*.json
            ├── Item/RootInteractions/CombateDirecional/*.json
            └── Languages/{en-US,pt-BR}/combatedirecional.lang
```

## Compilar e instalar

1. Instale o **Java 25** ([Temurin](https://adoptium.net/)).
2. Na pasta `hytale-combat-mod`: `gradlew.bat build` (Windows) ou `./gradlew build`.
3. Copie `build/libs/CombateDirecional-0.2.0.jar` para a pasta `mods/` do servidor/mundo.
4. Reinicie o servidor / reabra o mundo.

Se o seu jogo não estiver na versão 0.7.x, troque `hytale_server_version` em
`gradle.properties` e recompile.

## Controles (PC)

Segure a **Espada de Duelo** (`/give CombateDirecional_Espada`).

| Ação | Como |
|---|---|
| Trocar guarda | **Gire a câmera rápido** para a esquerda/direita, ou **olhe rápido para cima** |
| Ataque leve | Botão esquerdo (sai do lado da sua guarda) |
| Ataque pesado | Botão direito |
| Aparar | Botão direito **com a guarda certa** nos últimos 300 ms do golpe inimigo |
| Quebra de guarda | Tecla de **Habilidade 1** |
| Finta | Habilidade 1 **durante o seu pesado** (até 140 ms antes do impacto) |
| Contra-quebra | Habilidade 1 **durante a quebra do inimigo** |
| Esquiva curta | **Usar (F)** (ou a tecla de esquiva do jogo, se houver) — vai na direção em que você anda; parado = para trás |
| Rolamento | Esquiva **correndo** |
| Golpe Arcano (50 mana) | **Agachado** + pesado — imbloqueável |
| Fúria (100 mana) | **Agachado** + Habilidade 1 — +50% de dano por 8 s |

**Regra do lado:** um golpe que sai da **esquerda** de quem ataca chega na
**direita** de quem defende (como de frente para um espelho). O HUD sempre diz
para que lado você precisa virar a guarda. Se preferir sem espelho:
`"Controls": {"MirrorSides": false}`.

Controles reserva (para testar sem depender das teclas):
`/combate guarda <esquerda|cima|direita>` e `/combate acao <leve|pesado|quebra|esquiva|rolar>`.

## Roteiro de teste (fase por fase)

Antes de tudo: `/combate` deve mostrar "Plugin Java: OK" e "Asset pack carregado!".

### Fase 2 — guarda, leve, pesado, bloqueio
1. `/give CombateDirecional_Espada` e segure a espada.
2. O HUD (canto inferior esquerdo) mostra `GUARDA ▲ CIMA`.
3. Gire a câmera rápido para a esquerda → `GUARDA ◀ ESQUERDA`. Para a direita → `▶`. Olhe rápido para cima → `▲`.
   - Se esquerda/direita ficarem invertidas: `"Controls": {"InvertYaw": true}`.
   - Se trocar sem querer ao mirar: aumente `FlickYawDegrees` (ex.: 18).
4. `/duelista` (ou `/duelista <papel>`; veja os papéis com `/duelista papeis`).
5. Bata no duelista: clique esquerdo/direito. Ele perde vida; às vezes bloqueia (placa de nome).
6. Quando ele atacar, a placa mostra `⚠LEVE ▶` e o HUD diz "bloqueie ▶ DIREITA". Vire a guarda para o lado indicado: o golpe não causa dano.

### Fase 3 — stamina, exaustão, aparo, finta, quebra, contra-quebra
1. Ataque várias vezes seguidas: a stamina no HUD cai; ao zerar aparece **EXAUSTO**.
2. Fique parado: a stamina volta; o EXAUSTO some ao chegar em 55.
3. **Aparo:** quando o HUD mostrar `[APARE AGORA!]`, aperte o botão direito com a guarda certa → título **APARO!**, duelista atordoado, +20 mana.
4. **Finta:** comece um pesado e aperte Habilidade 1 logo em seguida → título **FINTA**.
5. **Quebra:** Habilidade 1 perto do duelista → **QUEBRA!** e ele fica `✖ ATORDOADO`; um leve logo depois sempre acerta.
6. **Contra-quebra:** quando o HUD mostrar "⚠ QUEBRA DE GUARDA!", aperte Habilidade 1 → **CONTRA-QUEBRA!**.

### Fase 4 — esquiva, rolamento, invencibilidade
1. Aperte F (Usar): você dá um passo rápido; o HUD mostra "Esquiva (invencível)".
2. Esquive no momento do golpe do duelista: título **ESQUIVOU!**, sem dano.
3. Correndo + F: rolamento; no fim aparece "Rolamento (vulnerável)" (a brecha).

### Fase 5 — mana, Arcano, Fúria
1. Acerte, bloqueie e apare para juntar mana (aparece no HUD; bloqueio +6, leve +10, pesado +15, aparo +20).
2. Com 50+: agache e aperte o botão direito → **Golpe Arcano**; o duelista recebe "IMBLOQUEÁVEL!".
3. Com 100: agache e aperte Habilidade 1 → **FÚRIA!**; o HUD mostra o tempo restante e o dano aumenta 50%.

### Fase 6 — indicadores e sensação
- Ao acertar: som de impacto, partícula e tremida de câmera.
- Títulos para APARO, QUEBRA, EXAUSTO, IMBLOQUEÁVEL.
- Se o log mostrar "Efeito ... nao existe nesta versao", troque o ID em `Feel` no config (ou deixe `""`).

### Fase 7 — animações
- Hoje toca as animações da espada do jogo como provisório.
- Siga `docs/ANIMACOES.md` para criar as suas no Blockbench e ligá-las.

### Fase 8 — duelista
- `/duelista` cria, `/duelista remover` remove. Ajuste a dificuldade em `Duelist` no config
  (`ReactionChance`, `ParryChance`, `ThinkMinMs`…). Use em terreno plano.

## Configuração (`mods/Alucady_CombateDirecional/CombatConfig.json`)

Criado automaticamente na primeira vez. Edite e use `/combate recarregar`.
Tempos em milissegundos. Valores marcados como **PROPOSTA** no
`CombatConfig.java` não estavam na sua especificação: sugeri um valor inicial.

| Seção | O que controla |
|---|---|
| `LightAttack`, `HeavyAttack` | preparação, dano, stamina, mana, recuperação, finta |
| `Block`, `Parry`, `GuardBreak` | mana do bloqueio, janela/atordoamento do aparo, quebra e contra-quebra |
| `Dodge`, `Roll` | invencibilidade, stamina, força do impulso, brecha final |
| `Stamina` | máximo, regeneração, exaustão (+25%), "só regenera parado", barra do jogo |
| `Mana` | máximo, custo do Arcano/Fúria, multiplicador e duração da Fúria |
| `Feel` | hit-stop, IDs de sons/partículas/câmera, títulos, placas de nome |
| `Controls` | sensibilidade do flick, inverter, espelhar lados, exigir agachar |
| `Reach` | alcance e ângulo dos golpes, distância do lock-on |
| `Hud` | liga/desliga o HUD |
| `Animations` | conjunto e IDs das animações |
| `Duelist` | papel do NPC e dificuldade da IA |

## Limitações conhecidas

Detalhes em `docs/PESQUISA_API.md`. Resumo: não dá para ler o mouse diretamente
(por isso o "flick"), não há hit-stop visual de verdade (é lógico + tremida),
correr não gasta stamina enquanto o espelhamento da barra está ligado, e o
duelista anda sem pathfinding.
