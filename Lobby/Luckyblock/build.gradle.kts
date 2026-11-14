plugins { java }

group = "org.kcm.bloomery.fawaymc.luckyblock"
version = "1.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.dmulloy2.net/repository/public/" )
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }