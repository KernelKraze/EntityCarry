plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "io.papermc.entitycarry"
version = "1.0.1"
description = "A plugin that allows players to carry entities and other players"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    
    // Optional: Add Guava for better collections (already included in Paper)
    compileOnly("com.google.guava:guava:33.0.0-jre")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    
    processResources {
        filteringCharset = "UTF-8"
    }
    
    runServer {
        minecraftVersion("1.21.4")
        jvmArgs("-Xmx2G", "-Xms1G")
    }
    
    test {
        useJUnitPlatform()
    }
    
    jar {
        archiveClassifier.set("")
    }
}

// Plugin YAML generation
bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "io.papermc.entitycarry.EntityCarryPlugin"
    apiVersion = "1.21"
    name = "EntityCarry"
    version = project.version.toString()
    description = project.description
    author = "KernelKraze"
    
    commands {
        register("carryaccept") {
            description = "Accept a carry request from another player"
            usage = "§c用法: /<command>"
            aliases = listOf("caccept", "acceptcarry")
            permission = "entitycarry.be_carried"
            permissionMessage = "§c你没有权限执行此命令"
        }
        register("carryreload") {
            description = "Reload the plugin configuration"
            usage = "§c用法: /<command>"
            aliases = listOf("creload")
            permission = "entitycarry.admin"
            permissionMessage = "§c你没有权限执行此命令"
        }
        register("carrytoggle") {
            description = "Toggle whether you can be carried by other players"
            usage = "§c用法: /<command>"
            aliases = listOf("ctoggle")
            permission = "entitycarry.be_carried"
            permissionMessage = "§c你没有权限执行此命令"
        }
    }
    
    permissions {
        register("entitycarry.*") {
            description = "Gives access to all EntityCarry commands and features"
            children = listOf(
                "entitycarry.admin",
                "entitycarry.use",
                "entitycarry.carry.player",
                "entitycarry.bypass.cooldown",
                "entitycarry.entity.*",
                "entitycarry.type.passive",
                "entitycarry.type.hostile",
                "entitycarry.be_carried"
            )
        }
        register("entitycarry.admin") {
            description = "Allows reloading the plugin configuration"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("entitycarry.use") {
            description = "Allows basic usage of the carry feature"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.FALSE
        }
        register("entitycarry.carry.player") {
            description = "Allows carrying other players"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("entitycarry.bypass.cooldown") {
            description = "Bypasses carry cooldown restrictions"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("entitycarry.entity.*") {
            description = "Allows carrying all entity types"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("entitycarry.type.passive") {
            description = "Allows carrying passive mobs"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.FALSE
        }
        register("entitycarry.type.hostile") {
            description = "Allows carrying hostile mobs"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.OP
        }
        register("entitycarry.be_carried") {
            description = "Allows being carried by other players"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.FALSE
        }
    }
}