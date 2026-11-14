plugins { java }

group = "org.kcm.bloomery.fawaymc.bungee"
version = "26.3"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }