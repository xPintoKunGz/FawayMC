plugins {
    java
}

group = "org.kcm.bloomery.fawaymc.daily"
version = "1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("me.clip:placeholderapi:2.12.3")
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }