# Combate Direcional — mod para Hytale

Combate direcional (guarda em 3 direções, aparo, finta, quebra de guarda),
esquiva/rolamento, stamina, mana e habilidades. Tudo original, feito a partir
da observação das mecânicas — nenhum arquivo de outro jogo é usado.

**Fase atual: 1 — projeto configurado + comando de teste.**

## O que tem aqui

```
hytale-combat-mod/
├── build.gradle.kts            ← receita de compilação (Gradle)
├── gradle.properties           ← nome, versão do mod e versão do servidor Hytale
├── settings.gradle.kts
├── gradlew / gradlew.bat       ← rode o Gradle por aqui (não precisa instalar)
├── docs/PESQUISA_API.md        ← o que descobri sobre a API e o que é/não é possível
└── src/main/
    ├── java/dev/alucady/combatedirecional/
    │   ├── CombateDirecionalPlugin.java   ← ponto de entrada do plugin
    │   ├── command/CombateCommand.java    ← comando /combate
    │   └── config/CombatConfig.java       ← TODOS os números do combate
    └── resources/                         ← isto É o asset pack
        ├── manifest.json                  ← "IncludesAssetPack": true
        └── Server/Languages/{en-US,pt-BR}/combatedirecional.lang
```

O mesmo arquivo `.jar` é **plugin Java + asset pack** ao mesmo tempo: o servidor
lê o código Java e também as pastas `Server/` (e futuramente `Common/`) que
estão dentro dele. Assim você só instala um arquivo.

## 1. Pré-requisitos

1. **Java 25 (JDK).** Recomendo o [Eclipse Temurin 25](https://adoptium.net/).
   Se não tiver, o Gradle tenta baixar sozinho na primeira compilação.
2. **Hytale** instalado (para testar no jogo).
3. Internet na primeira compilação (baixa o Gradle e o `Server.jar` oficial
   de `https://maven.hytale.com/release`).

> A versão do servidor usada para compilar está em `gradle.properties`
> (`hytale_server_version=0.7.5`). Se o seu jogo estiver em outra versão,
> troque esse número (as versões disponíveis estão listadas
> [aqui](https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml))
> e o `hytale_server_range` logo abaixo.

## 2. Compilar

Abra um terminal **dentro da pasta `hytale-combat-mod`** e rode:

- Windows: `gradlew.bat build`
- Linux/Mac: `./gradlew build`

Deu certo se aparecer `BUILD SUCCESSFUL`. O mod fica em:

```
build/libs/CombateDirecional-0.1.0.jar
```

## 3. Instalar

Copie o `.jar` para a pasta **`mods`** do mundo/servidor. Dois jeitos:

- **Na mão:** copie `build/libs/CombateDirecional-0.1.0.jar` para a pasta `mods`.
  - Servidor dedicado: `mods/` fica ao lado do `HytaleServer.jar`.
  - Mundo single-player: o jogo roda um servidor local por baixo; coloque o
    `.jar` na pasta `mods/` desse mundo (dentro da pasta de saves do Hytale).
    Não consegui confirmar o caminho exato no seu sistema; se não achar, me
    diga onde ficam seus saves e eu te guio.
- **Automático:** `gradlew.bat instalarMod -PmodsDir="C:\caminho\para\mods"`

Depois (re)inicie o servidor / reabra o mundo. Se o mundo tiver uma lista de
mods, confira se “Alucady:CombateDirecional” está ativado.

## 4. Testar a Fase 1 ✅

1. **No log do servidor**, procure:
   ```
   Combate Direcional 0.1.0 carregado! Config em: .../mods/Alucady_CombateDirecional/CombatConfig.json
   ```
2. **No jogo**, abra o chat e digite `/combate`. Deve aparecer:
   - `=== Combate Direcional v0.1.0 ===`
   - `Plugin Java: OK` (verde)
   - `Asset pack carregado!` (verde) ← prova de que o asset pack foi lido
   - Duas linhas com os valores (Leve 480 ms, Pesado 850 ms, …)
3. **Teste o arquivo de configuração:**
   1. Abra `mods/Alucady_CombateDirecional/CombatConfig.json`.
   2. Mude `"WindupMs": 480` (dentro de `LightAttack`) para `400` e salve.
   3. No jogo: `/combate recarregar` (precisa ser operador), depois `/combate`.
   4. A linha deve mostrar `Leve: 400 ms`.

Se algo falhar, me mande o trecho do log que tiver `CombateDirecional` ou `ERROR`/`SEVERE`.

## Configuração (`CombatConfig.json`)

Todos os tempos estão em **milissegundos** (1000 ms = 1 s). Valores marcados
como **PROPOSTA** no código não estavam na especificação e eu sugeri um valor
inicial — ajuste à vontade:

| Seção | Campo | Padrão | |
|---|---|---|---|
| Roll | DurationMs | 700 | PROPOSTA |
| Roll | RecoveryMs | 250 | PROPOSTA |
| Stamina | Max | 100 | PROPOSTA |
| Stamina | RegenPerSecond | 25 | PROPOSTA |
| Stamina | RegenDelayMs | 600 | PROPOSTA |
| Mana | Max | 100 | PROPOSTA (Fúria custa 100) |

## Fases

1. ✅ Projeto do plugin + asset pack + comando de teste
2. Guarda direcional + ataques leve/pesado + bloqueio
3. Stamina, exaustão, aparo, finta, quebra e contra-quebra
4. Esquiva, rolamento e invencibilidade
5. Mana, Golpe Arcano e Fúria
6. Indicadores visuais e “sensação” (hit-stop, sons, partículas)
7. Lista de animações + integração
8. NPC duelista para treino
