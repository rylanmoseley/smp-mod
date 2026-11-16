plugins {
    `java-library`
    `maven-publish`
    id("idea")
    id("net.neoforged.moddev") version "2.0.142"
    kotlin("jvm") version "2.2.20"
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

val mod_version: String by project
val mod_group_id: String by project
val mod_id: String by project
val neo_version: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project
val neo_version_range: String by project
val loader_version_range: String by project
val mod_name: String by project
val mod_license: String by project
val mod_authors: String by project
val mod_description: String by project
val parchment_mappings_version: String by project
val parchment_minecraft_version: String by project
val kff_version: String by project
val kff_version_range: String by project

version = mod_version
group = mod_group_id

base {
    archivesName.set(mod_id)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude("src/generated/**/.cache")
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
        content { includeGroup("thedarkcolour") }
    }
    maven {
        name = "Xaero's Maven"
        setUrl("https://chocolateminecraft.com/maven")
        content { includeGroup("xaero.pac") }
    }
    maven {
        name = "Fuzs Mod Resources"
        setUrl("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    }
}

neoForge {
    version = neo_version

    parchment {
        mappingsVersion.set(parchment_mappings_version)
        minecraftVersion.set(parchment_minecraft_version)
    }

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }

        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", mod_id)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", mod_id)
        }

        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", mod_id)
        }

        create("data") {
            data()
            programArguments.addAll(
                "--mod",
                mod_id,
                "--all",
                "--output",
                file("src/generated/resources").absolutePath,
                "--existing",
                file("src/main/resources").absolutePath
            )
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val localRuntime by configurations.creating

configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kff_version")

    // Open Parties and Claims (OPAC) dependency
    compileOnly("xaero.pac:open-parties-and-claims-neoforge-1.21.1:0.33.0")
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "minecraft_version"       to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neo_version"             to neo_version,
        "neo_version_range"       to neo_version_range,
        "loader_version_range"    to loader_version_range,
        "mod_id"                  to mod_id,
        "mod_name"                to mod_name,
        "mod_license"             to mod_license,
        "mod_version"             to mod_version,
        "mod_authors"             to mod_authors,
        "mod_description"         to mod_description,
        "kff_version_range"       to kff_version_range
    )

    inputs.properties(replaceProperties)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
