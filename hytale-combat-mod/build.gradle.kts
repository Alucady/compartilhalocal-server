plugins {
    java
}

group = property("group") as String
version = property("version") as String

val modName = property("mod_name") as String

java {
    // O servidor do Hytale roda em Java 25.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    // Repositório oficial do Hytale (guarda as 5 últimas versões do servidor).
    maven {
        name = "hytale"
        url = uri("https://maven.hytale.com/release")
    }
}

dependencies {
    // "compileOnly": usamos o servidor só para compilar; ele já existe quando o mod roda.
    compileOnly("com.hypixel.hytale:Server:${property("hytale_server_version")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Preenche o manifest.json com os valores do gradle.properties.
tasks.processResources {
    val props = mapOf(
        "group" to project.property("mod_group"),
        "name" to modName,
        "version" to project.version,
        "description" to project.property("mod_description"),
        "author" to project.property("mod_author"),
        "serverRange" to project.property("hytale_server_range"),
    )
    inputs.properties(props)
    filesMatching("manifest.json") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName = modName
}

// Copia o .jar pronto para a pasta "mods" do seu servidor.
// Uso: ./gradlew instalarMod -PmodsDir="C:/caminho/do/servidor/mods"
tasks.register<Copy>("instalarMod") {
    group = "hytale"
    description = "Compila e copia o mod para a pasta mods do servidor (-PmodsDir=...)."
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(providers.gradleProperty("modsDir").orElse(
        providers.provider { throw GradleException("Informe a pasta: -PmodsDir=\"/caminho/do/servidor/mods\"") }
    ))
}
