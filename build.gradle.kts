plugins {
    java
    checkstyle
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml.paper)
}

group = "com.cebonk03"
version = "1.0.0-SNAPSHOT"
description = "Packet-based GUI library for Paper/Folia"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// ── Repositories ──────────────────────────────────────────────────────────────

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io/")
}

// ── Dependencies ──────────────────────────────────────────────────────────────

dependencies {
    // Paper API — provided at runtime by the server
    compileOnly(libs.paper.api)

    // External plugin dependency — NOT shaded; must be installed on the server
    compileOnly(libs.packetevents.spigot)

    // Soft external plugin dependencies — compileOnly, runtime check, not shaded
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api)

    // Annotations
    compileOnly(libs.jspecify)

    // Shaded dependencies (relocated inside the final jar)
    implementation(libs.configurate.yaml)
    implementation(libs.caffeine)

    // ── Testing ────────────────────────────────────────────────────────────
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.packetevents.spigot)
}

// ── Test configuration ────────────────────────────────────────────────────────

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// ── Shadow / fat-jar configuration ────────────────────────────────────────────

tasks.shadowJar {
    archiveClassifier.set("")

    relocate(
        "org.spongepowered.configurate",
        "com.cebonk03.packetmenu.libs.configurate"
    )
    relocate(
        "com.github.benmanes.caffeine",
        "com.cebonk03.packetmenu.libs.caffeine"
    )

    minimize()
}

// ── paper-plugin.yml generation ───────────────────────────────────────────────

paper {
    name = "PacketMenu"
    version = project.version.toString()
    main = "com.cebonk03.packetmenu.bootstrap.PacketMenuPlugin"
    apiVersion = "1.21"
    foliaSupported = true

    serverDependencies {
        register("PacketEvents") {
            required = true
            load = net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("PlaceholderAPI") {
            required = false
        }
        register("Vault") {
            required = false
        }
    }
}

// ── Checkstyle ────────────────────────────────────────────────────────────────

checkstyle {
    toolVersion = libs.versions.checkstyle.tool.get()
    configFile = file("config/checkstyle/checkstyle.xml")
}

// ── SpotBugs ──────────────────────────────────────────────────────────────────

spotbugs {
    excludeFilter = file("config/spotbugs/exclude.xml")
}
