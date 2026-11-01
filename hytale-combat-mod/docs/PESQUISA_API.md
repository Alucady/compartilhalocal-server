# Pesquisa da API do Hytale (servidor 0.7.5)

Fonte principal: o `Server.jar` oficial (`https://maven.hytale.com/release`,
versão 0.7.5). Antes de usar qualquer classe ou método eu conferi a assinatura
real com `javap` e li o código de referência dos plugins oficiais que vêm dentro
do jar (ex.: `CameraPlugin`, `SpectatingHud`, `NPCFreezeCommand`). Docs da
comunidade (hytalemodding.dev, doctale.dev) serviram só de guia, e algumas
estavam desatualizadas (ex.: `HudManager.setCustomHud` não existe mais; o certo é
`addCustomHud`).

## O que o mod usa (tudo verificado no jar)

| Parte | API real |
|---|---|
| Plugin | `JavaPlugin(JavaPluginInit)`, `setup()`, `start()`, `shutdown()` |
| Config | `withConfig(nome, BuilderCodec)` no construtor; chaves JSON com maiúscula |
| Interação customizada | `getCodecRegistry(Interaction.CODEC).register(id, Classe, CODEC)`; classe estende `SimpleInstantInteraction` e implementa `firstRun(...)` (mesmo padrão do `CameraShakeInteraction` oficial) |
| Assets de item | `Server/Item/Items`, `Server/Item/Interactions`, `Server/Item/RootInteractions`; o ID é o nome do arquivo; `"Parent"` herda de outro item |
| Estado por entidade | componente ECS: `getEntityStoreRegistry().registerComponent(Classe, Supplier)` |
| Tick | `EntityTickingSystem<EntityStore>` registrado com `registerSystem` |
| Filtro de dano | `DamageEventSystem` no grupo `DamageModule.get().getFilterDamageGroup()` (igual ao `FilterUnkillable` oficial) |
| Aplicar dano | `DamageSystems.executeDamage(ref, accessor, new Damage(new Damage.EntitySource(atacante), causa, valor))` |
| Rotação da cabeça | `HeadRotation.getRotation().yaw()/pitch()` (radianos; yaw maior = virando à esquerda) |
| Entidades próximas | `TargetUtil.getAllEntitiesInSphere(pos, raio, accessor)` |
| Impulso da esquiva | `Velocity.addInstruction(vetor, null, ChangeVelocityType.Add)` (enviado ao cliente pelo `PlayerVelocityInstructionSystem`) |
| Estados de movimento | `MovementStatesComponent.getMovementStates()` → `crouching`, `sprinting`… |
| Barras do jogo | `EntityStatMap.setStatValue(DefaultEntityStatTypes.getStamina()/getMana(), valor)` |
| HUD | `CustomUIHud` + `UICommandBuilder.appendInline(...)` + `set("#Id.Text", ...)`, adicionado com `Player.getHudManager().addCustomHud(...)` |
| Títulos na tela | `EventTitleUtil.showEventTitleToPlayer(...)` |
| Placa de nome | componente `Nameplate` |
| Sons | `SoundEvent.getAssetMap().getIndex(id)` + `SoundUtil.playSoundEvent3d(...)` |
| Partículas | `ParticleSystem.getAssetMap().getAsset(id)` + `ParticleUtil.spawnParticleEffect(...)` |
| Tremida de câmera | `CameraEffect.getAssetMap()` + `createCameraShakePacket()` enviado ao jogador |
| Animação | `AnimationUtils.playAnimation(ref, AnimationSlot.Action, conjunto, id, true, accessor)` |
| NPC | `NPCPlugin.get().spawnNPC(...)`, `getRoleTemplateNames(true)`, componente `Frozen` (o mesmo do `/npc freeze`) |

## Limitações encontradas e como contornei

| O que você pediu | Limitação real da API 0.7.5 | O que fiz |
|---|---|---|
| Guarda com "segurar botão + direção do mouse" | Os eventos de mouse (`PlayerMouseMotionEvent`) **só chegam no servidor no modo de câmera especial** (visão de cima, `sendMouseMotion`). Também não existe tecla nova customizada. | A guarda muda com um **"flick"**: girar a câmera rápido (≥12° em 150 ms) para esquerda/direita ou olhar rápido para cima. O servidor lê isso pela rotação da cabeça. Ajustável no config; opção de exigir agachar; comando reserva `/combate guarda`. |
| Botões de ação | Só dá para usar as entradas que o jogo já tem: Primário, Secundário, Habilidade 1–3, Usar, Esquiva. | Leve = esquerdo, Pesado/Aparo = direito, Quebra/Finta/Contra-quebra = Habilidade 1, Esquiva/Rolamento = Usar (F) e a entrada "Dodge". Arcano/Fúria = agachado + pesado/quebra. |
| Hit-stop (congelar 40–90 ms) | Não existe como pausar a animação de um jogador pelo servidor (a animação roda no cliente). | Hit-stop "lógico": a linha do tempo de combate dos dois lutadores pausa `HitStopMs`, somado à tremida de câmera no impacto. |
| HUD customizado | Existe, mas a linguagem de UI é pouco documentada e a comunidade diz que vai migrar para NoesisGUI. | HUD simples com 4 linhas de texto (formato copiado da estrutura do HUD oficial de espectador). Se der erro no seu cliente: `"Hud": {"Enabled": false}` ou `/combate hud`. Os títulos na tela e as placas de nome continuam funcionando. |
| Indicador da direção do golpe inimigo | Não há como desenhar setas no mundo. | HUD ("⚠ PESADO — bloqueie ▶ DIREITA", "[APARE AGORA!]"), placa de nome do duelista mostrando guarda e golpe, títulos (APARO!, IMBLOQUEÁVEL!, EXAUSTO). |
| Controlar animação do jogador | Possível tocar animações por cima (slot `Action`); não dá para mudar a velocidade por golpe. | Animações por ID no config; placeholders da espada do jogo até você criar as suas (Fase 7). |
| Stamina "regenera parado" | O jogo tem a própria stamina (correr gasta). | O mod tem a stamina dele e **espelha** na barra do jogo. Efeito colateral: correr deixa de gastar stamina enquanto o espelhamento está ligado (`MirrorToVanillaBar: false` desliga). |
| NPC duelista | Criar um NPC do zero exige o sistema de "roles" (JSON complexo e quase sem documentação). | Uso um NPC do jogo como "corpo", congelo a IA original (`Frozen`) e a minha IA controla. Ele anda por teleporte suave (sem pathfinding): use em terreno plano. |

## IDs vindos do jogo base (não estão no jar do servidor)

Estes IDs vêm da documentação da comunidade (doctale.dev). Sons, partículas e
efeitos de câmera são **checados quando o mod roda**: se não existirem, aparece
um aviso no log e o efeito é pulado. O `Parent` da espada não dá para checar
antes: se `Weapon_Sword_Iron` não existir, a espada não carrega (veja o log).

- Item base: `Weapon_Sword_Iron`
- Conjunto de animações: `Sword` (`SwingLeft`; `SwingRight`/`SwingDown` são palpites)
- Sons: `SFX_Sword_T2_Impact`, `SFX_Light_Melee_T2_Swing`
- Partícula: `Impact_Sword_Basic`
- Câmera: `Impact`, `Impact_Light`
- Causa de dano: `Physical`

## Como verificar você mesmo

```bash
curl -O https://maven.hytale.com/release/com/hypixel/hytale/Server/0.7.5/Server-0.7.5.jar
javap -cp Server-0.7.5.jar com.hypixel.hytale.server.core.plugin.JavaPlugin
```
