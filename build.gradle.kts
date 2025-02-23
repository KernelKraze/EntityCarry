plugins {
    id("xyz.jpenilla.run-paper") version "2.1.0"
    id("java")
}

repositories {
    // 这里添加 Maven 仓库
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public")
}

tasks {
    runServer {
        minecraftVersion("1.21.4")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
