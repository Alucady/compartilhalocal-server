# Passagem de contexto para a sessão local (Windows)

Este arquivo foi escrito pela sessão de nuvem que criou o mod. Ele resume tudo
para a sessão que roda no PC do usuário continuar o trabalho.

## O que é o projeto
Mod para Hytale chamado **Combate Direcional** (plugin Java + asset pack num único .jar):
combate direcional inspirado na sensação do For Honor (guarda em 3 direções,
bloqueio, aparo, finta, quebra de guarda, contra-quebra), esquiva e rolamento
estilo Dark Souls, stamina com exaustão, mana com Golpe Arcano e Fúria, HUD,
efeitos e um NPC duelista com IA. Tudo original (nenhum arquivo de outros jogos).

- Repositório: `Alucady/compartilhalocal-server`, branch `claude/hytale-directional-combat-oy77zc`
- Pasta do mod: `hytale-combat-mod/`
- Versão atual: **0.3.0**, compilada contra o servidor Hytale **0.7.5**
  (`com.hypixel.hytale:Server` em `https://maven.hytale.com/release`)
- Perfis de regras: `/combate perfil original` (especificação do usuário) e
  `/combate perfil fiel` (valores deduzidos de medições da comunidade de For Honor / DS3)

## Tarefa imediata
1. Clonar a branch acima.
2. Compilar: na pasta `hytale-combat-mod`, rodar `gradlew.bat build`
   (precisa de JDK 25; o Gradle tenta baixar sozinho via toolchain/foojay).
3. Copiar `hytale-combat-mod\build\libs\CombateDirecional-0.3.0.jar` para
   `%AppData%\Hytale\UserData\Mods` (criar a pasta se não existir).
4. O usuário vai criar um mundo novo e ativar o mod nas configurações do mundo.
5. Ajudar no teste: `/combate`, `/give CombateDirecional_Espada`, `/duelista`.
   Ler os logs do Hytale (provavelmente em `%AppData%\Hytale\UserData\Logs`)
   procurando `CombateDirecional`, `WARNING`, `SEVERE`, e corrigir o que falhar.

## Riscos conhecidos (o que provavelmente vai precisar de ajuste)
- A espada herda de `Weapon_Sword_Iron` (ID vindo de docs da comunidade). Se não
  existir, a espada não carrega: trocar o `Parent` em
  `src/main/resources/Server/Item/Items/CombateDirecional/CombateDirecional_Espada.json`.
- IDs de sons/partículas/câmera (`SFX_Sword_T2_Impact`, `Impact_Sword_Basic`,
  `Impact`, `Impact_Light`) vêm de docs da comunidade; se faltarem, o mod só
  avisa no log e segue. Ajustáveis em `CombatConfig.json` > `Feel`.
- HUD usa UI inline (`CombatHud.java`); se der "Failed to apply Custom UI HUD
  commands", desligar com `/combate hud` ou `"Hud": {"Enabled": false}` e ajustar a sintaxe.
- Animações provisórias da espada do jogo (`Sword`/`SwingLeft`; `SwingRight` e
  `SwingDown` são palpites).
- A guarda muda por "flick" de câmera (eventos de mouse só chegam no modo de câmera
  especial do servidor). Sensibilidade em `Controls`.
- O duelista usa um NPC do jogo como corpo + componente `Frozen`; se o papel padrão
  não existir, usar `/duelista papeis` e `/duelista <papel>`.
- Se a versão do jogo do usuário não for 0.7.x, trocar `hytale_server_version`
  em `gradle.properties` e recompilar.

## Onde está cada coisa
- `README.md`: controles, roteiro de teste por fase, configuração
- `docs/PESQUISA_API.md`: API verificada no Server.jar e limitações/contornos
- `docs/MODELO_INFERIDO.md`: regras deduzidas (perfil fiel) com fontes
- `docs/ANIMACOES.md`: lista das 19 animações com durações e poses-chave
- Código: `src/main/java/dev/alucady/combatedirecional/` (regras em `combat/CombatRules.java`)

## Próximos passos combinados com o usuário (depois do teste)
1. Movimento em guarda (mais lento) e lock-on simulado, até onde a API permitir.
2. Combos/cadeias, empurrão e arremesso.
3. Perfis por arma (peso).
4. Animações próprias no Blockbench seguindo `docs/ANIMACOES.md`.

O usuário é iniciante: explicar de forma simples, em português.
