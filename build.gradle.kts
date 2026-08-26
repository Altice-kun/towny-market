plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = "jp.manus"
version = "0.8.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit", module = "bukkit") }
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

tasks {
    processResources {
        filesMatching("plugin.yml") { expand("version" to project.version) }
    }
    shadowJar { archiveClassifier.set("") }
    build { dependsOn(shadowJar) }
    test { useJUnitPlatform() }
}
