# Animações do Combate Direcional (Fase 7)

Guia para criar as animações no **Blockbench** com o plugin oficial do Hytale.
Tudo original: as poses abaixo descrevem o *movimento* que queremos (peso,
antecipação, continuação), sem copiar nenhum arquivo de outro jogo.

## Princípios (leia antes de animar)

1. **Antecipação clara.** Antes de todo golpe o corpo "carrega" para o lado
   oposto: quadril gira para trás, peso vai para a perna de trás, a arma recua.
   Quanto mais pesado o golpe, maior e mais lenta a antecipação.
2. **Corpo inteiro.** A ordem do movimento é **pés → quadril → tronco → ombro →
   braço → arma** (cada parte começa 1–2 frames depois da anterior). Nunca só o braço.
3. **Impacto = frame mais "esticado".** No frame do impacto o corpo está
   totalmente comprometido (passo à frente, tronco torcido, braço estendido).
4. **Continuação (follow-through).** Depois do impacto a arma passa do alvo,
   o tronco continua girando um pouco e só então volta. Isso dá peso.
5. **Recuperação até a guarda.** O último frame tem que ser **idêntico** à pose
   de guarda daquela direção, para a transição ficar suave.
6. **Ease.** Antecipação: ease-in (devagar → rápido). Golpe: rápido, quase linear.
   Recuperação: ease-out (rápido → devagar).
7. **Estilo blocado do Hytale:** poses fortes e legíveis à distância, silhuetas
   claras, sem rotações minúsculas que "somem" no modelo cúbico.

## Tempos (batem com o CombatConfig.json)

Os números vêm do `CombatConfig.json`. Se você mudar os tempos lá, mude a
duração da animação também. 60 fps: 1 frame ≈ 16,7 ms.

> **Exausto:** os golpes ficam 25% mais lentos, mas a animação toca na velocidade
> normal (a API atual não permite mudar a velocidade por golpe). Se quiser,
> faça a antecipação "segurar" a pose no final (um *hold* curto) para disfarçar.

| # | ID (no config `Animations`) | Duração total | Momento-chave |
|---|---|---|---|
| 1 | `BlockLeft` / guarda esquerda | 250 ms (+ pose final em loop) | 0–120 ms troca de guarda |
| 2 | `BlockTop` / guarda cima | 250 ms | idem |
| 3 | `BlockRight` / guarda direita | 250 ms | idem |
| 4 | `LightLeft` | **780 ms** | impacto em **480 ms** (frame 29) |
| 5 | `LightTop` | 780 ms | impacto em 480 ms |
| 6 | `LightRight` | 780 ms | impacto em 480 ms |
| 7 | `HeavyLeft` | **1350 ms** | impacto em **850 ms** (frame 51); finta possível até 710 ms |
| 8 | `HeavyTop` | 1350 ms | impacto em 850 ms |
| 9 | `HeavyRight` | 1350 ms | impacto em 850 ms |
| 10 | `GuardBreak` | 770 ms | agarrão em **320 ms** (frame 19) |
| 11 | `Parry` | 400 ms | desvio em 0–100 ms |
| 12 | `Stunned` | 1000 ms (loop) | — (aparo: 850 ms, quebra: 1000/1400 ms) |
| 13 | `DodgeForward` | **450 ms** | invencível 0–380 ms |
| 14 | `DodgeBack` | 450 ms | idem |
| 15 | `DodgeLeft` | 450 ms | idem |
| 16 | `DodgeRight` | 450 ms | idem |
| 17 | `Roll` | **950 ms** (700 rolando + 250 levantando) | invencível 0–420 ms; vulnerável 420–950 |
| 18 | `Arcane` | 1350 ms | impacto em 850 ms |
| 19 | `Fury` | 400 ms | grito/brilho em 150 ms |

## Poses-chave por animação

> Convenção: "esquerda/direita" = do ponto de vista do personagem.
> Base da guarda: pés afastados na largura dos ombros, pé esquerdo à frente,
> joelhos levemente dobrados, espada com as duas mãos.

### 1–3. Guardas (esquerda, cima, direita)
- **Esquerda:** espada na diagonal do lado esquerdo do corpo, ponta para cima/frente;
  tronco gira ~15° para a esquerda; peso 55% no pé de trás.
- **Cima:** espada horizontal acima da cabeça (lâmina para frente), cotovelos
  altos; tronco reto, queixo baixo.
- **Direita:** espelho da esquerda, tronco gira ~15° para a direita.
- Transição (0–120 ms): o quadril vira primeiro, a arma chega por último
  (overshoot de 3–5° e volta). Pose final em loop com respiração leve
  (tronco sobe/desce 1 pixel a cada 1 s).

### 4–6. Ataques leves (780 ms, impacto em 480)
- **0 ms:** pose de guarda da direção.
- **0–300 ms (antecipação curta):** arma recua ~30° para trás do lado do golpe,
  quadril gira contra o golpe, ombro sobe.
- **300–480 ms (golpe):** passo curto à frente com o pé da frente; quadril gira
  primeiro, depois tronco, depois braços; arma corta a diagonal
  (esquerda: de cima-esquerda para baixo-direita; direita: espelho;
  cima: vertical de cima para baixo).
- **480 ms (impacto):** braços quase estendidos, tronco torcido ~35°.
- **480–600 ms (continuação):** a arma passa ~40° do ponto de impacto; tronco continua girando.
- **600–780 ms (recuperação):** volta à guarda da mesma direção (ease-out).

### 7–9. Ataques pesados (1350 ms, impacto em 850)
- **0–600 ms (antecipação grande):** arma vai bem para trás e alta, tronco gira
  ~45° para longe do alvo, peso todo no pé de trás, joelhos dobram mais.
  É aqui que o adversário "lê" o golpe: a pose precisa ser **inconfundível** por direção.
- **600–710 ms:** pequeno *hold* (a arma treme 1–2°): fim da janela de finta.
- **710–850 ms (golpe):** passo LONGO à frente, quadril → tronco → braços, muito rápido.
- **850 ms (impacto):** corpo totalmente comprometido, joelho da frente dobrado, arma baixa.
- **850–1050 ms (continuação):** arma passa do alvo e quase toca o chão; ombros acompanham.
- **1050–1350 ms (recuperação):** levanta pesado (ease-out), volta à guarda.

### 10. Quebra de guarda (770 ms, agarrão em 320)
- **0–200 ms:** abaixa o centro de gravidade, mão livre vai para trás.
- **200–320 ms:** avanço com o ombro e a mão livre empurrando/agarrando à frente.
- **320 ms:** braço estendido, corpo inclinado para frente.
- **320–770 ms:** se acertou, puxa para trás; se errou, perde o equilíbrio e volta à guarda.

### 11. Aparo (400 ms)
- **0–100 ms:** a arma sai da guarda e bate de lado na direção do golpe inimigo
  (movimento curto e seco, o tronco quase não se move).
- **100–250 ms:** a arma "rebate" para fora, abrindo o adversário.
- **250–400 ms:** volta à guarda.

### 12. Atordoado (1000 ms, loop)
- Tronco inclinado para trás, cabeça tombada para o lado, arma baixa numa mão,
  pés desalinhados; a cada ~500 ms um pequeno cambaleio para o lado.

### 13–16. Esquivas curtas (450 ms)
- **0–80 ms:** agacha um pouco (antecipação) e inclina para o lado da esquiva.
- **80–300 ms:** passo largo/deslize na direção; tronco abaixado; arma colada ao corpo.
- **300–450 ms:** o pé de trás alcança e volta à guarda.
- Frente: avança e abaixa os ombros. Trás: recua e levanta a arma. Lados: passo cruzado lateral.

### 17. Rolamento (950 ms)
- **0–120 ms:** mergulho para frente (cabeça abaixa, braços cruzam com a arma).
- **120–600 ms:** rolamento sobre o ombro (rotação de 360° do corpo todo no eixo lateral).
- **600–700 ms:** chega agachado.
- **700–950 ms:** levanta devagar (é a "brecha" vulnerável) e volta à guarda.

### 18. Golpe Arcano (1350 ms, impacto em 850)
- Igual ao pesado de **cima**, mas a antecipação é mais alta e com o corpo
  arqueado para trás; a mão livre passa pela lâmina (gesto de "carregar" a magia).
  Use partículas (`ArcaneParticle` no config) para o brilho.

### 19. Fúria (400 ms)
- **0–150 ms:** abaixa o tronco e fecha os punhos.
- **150 ms:** explode para cima: peito aberto, braços abertos, cabeça para trás.
- **150–400 ms:** volta à guarda. Partículas em `FuryParticle`.

## Como ligar as animações ao mod

1. No Blockbench, anime sobre o **modelo de personagem do Hytale** e exporte cada
   animação como `.blockyanim`.
2. Coloque os arquivos em:
   `src/main/resources/Common/Characters/Animations/CombateDirecional/`
   (o servidor só aceita animações de personagem dentro de `Characters/` ou `NPC/`).
3. Copie `docs/modelos/CombateDirecional_Espada.animations.json` para
   `src/main/resources/Server/Item/Animations/CombateDirecional_Espada.json`
   e confira se os nomes dos arquivos batem.
4. No `CombatConfig.json`, seção `Animations`:
   - `"AnimationSet": "CombateDirecional_Espada"`
   - preencha cada ID (`LightLeft`, `HeavyTop`, …) com o nome da entrada do JSON.
5. (Opcional) Para a pose de guarda/andar com a espada também ser a sua, adicione
   `"PlayerAnimationsId": "CombateDirecional_Espada"` na espada
   (`Server/Item/Items/CombateDirecional/CombateDirecional_Espada.json`) e
   inclua no conjunto as animações de parado/andar que a espada original tinha.
6. Recompile (`gradlew build`), reinstale e use `/combate recarregar`.

Enquanto você não cria as suas, o mod usa como "provisório" as animações da
espada do jogo base (`AnimationSet: "Sword"`, `SwingLeft`, `SwingRight`, `SwingDown`).
Se alguma não existir, ela só não toca; o combate funciona igual.
