import java.text.SimpleDateFormat
import java.util.Date
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
}

group = "de.jeff_media"
version = "15.0.0"
description = "Allows automatic chest sorting!"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.jetbrains:annotations:26.0.2")
}

val configVersion = SimpleDateFormat("yyyyMMddHHmm").format(Date())

tasks.processResources {
    val props = mapOf(
        "name" to project.name,
        "version" to project.version,
        "description" to project.description,
        "url" to "https://www.chestsort.de",
        "main" to "de.jeff_media.chestsort.ChestSortPlugin",
        "prefix" to project.name,
        "configVersion" to configVersion
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "config.yml", "config-version.txt")) {
        filter<ReplaceTokens>("tokens" to props)
    }
}

tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}
