# Pesquisa da API do Hytale (servidor 0.7.5)

Como a API está em Early Access e muda bastante, eu **não confio só em
tutoriais**. Fonte principal: o próprio `Server.jar` oficial
(`https://maven.hytale.com/release`, versão 0.7.5, publicado em 29/10/2026).
Inspecionei as assinaturas reais das classes (com `javap`) antes de usar
qualquer uma. Tutoriais da comunidade (hytalemodding.dev, templates do
GitHub) serviram só como guia — e alguns já estavam desatualizados.

## Verificado e usado na Fase 1

| O quê | Classe / detalhe real |
|---|---|
| Classe base do plugin | `com.hypixel.hytale.server.core.plugin.JavaPlugin`, construtor `(JavaPluginInit)` |
| Ciclo de vida | `setup()`, `start()`, `shutdown()` (protected) |
| Registrar comando | `getCommandRegistry().registerCommand(...)` |
| Comando simples | `CommandBase(nome, chaveDescricao)` + `executeSync(CommandContext)` |
| Subcomando | `addSubCommand(...)` |
| Permissão aberta | `requireNoPermission()` (o antigo `setPermissionGroup(GameMode)` está marcado para remoção) |
| Mensagens | `Message.raw(...)`, `Message.translation(...)`, `.color("#hex")`, `.bold(...)` |
| Config em arquivo | `withConfig(nome, BuilderCodec)` — **só no construtor**; arquivo em `mods/<Group>_<Name>/<nome>.json` |
| Chaves do JSON | **Precisam começar com letra maiúscula** (senão o servidor lança erro) |
| Manifest | `Group`, `Name`, `Version`, `Description`, `Authors`, `Main`, `ServerVersion` (faixa semver, ex. `~0.7.5`), `IncludesAssetPack` |
| Asset pack dentro do jar | Raiz do jar = raiz do pack; pastas `Server/` e `Common/` |
| Textos (.lang) | `Server/Languages/<idioma>/<arquivo>.lang`, formato `chave = valor`; a chave final ganha o prefixo `<arquivo>.` |
| Pasta de mods | `mods/` (padrão do servidor) |

## Prévia das próximas fases (existe na API, vou validar o uso em cada fase)

| Necessidade | O que encontrei | Situação |
|---|---|---|
| Entradas do jogador | `InteractionType`: `Primary` (botão esquerdo), `Secondary` (direito), `Ability1..3`, `Use`, **`Dodge`** | Promissor |
| Direção do mouse | `PlayerMouseMotionEvent` (tem `getMouseMotion()`) | Preciso confirmar se o cliente envia isso sempre ou só em casos específicos |
| HUD customizado | `CustomUIHud`, `HudManager` | Existe; vou verificar se aceita UI de mod sem mexer no cliente |
| Tocar animação | `AnimationUtils.playAnimation(ref, AnimationSlot, nome, ...)` | Existe |
| Tremida de câmera | `CameraShake`, `CameraShakeEffect`, `CameraShakeInteraction` | Existe |
| Invencibilidade | componente `Invulnerable` | Existe |
| Partículas / sons | `ParticleUtil`, `SoundUtil` | Existe |
| Stamina / mana | `EntityStatMap`, `DefaultEntityStatTypes` | Existe |
| Dano | `DamageEventSystem` | Existe |
| **Hit-stop** (congelar ~60 ms) | Não há nada pronto | Provavelmente vou **simular**: pausar a animação/velocidade do atacante e do alvo por alguns ticks. Confirmo na Fase 6. |

## Como verificar você mesmo

```bash
# baixa o jar oficial e lista uma classe
curl -O https://maven.hytale.com/release/com/hypixel/hytale/Server/0.7.5/Server-0.7.5.jar
javap -cp Server-0.7.5.jar com.hypixel.hytale.server.core.plugin.JavaPlugin
```
