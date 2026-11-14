plugins { java }

group = "org.kcm.bloomery.fawaymc.friend"
version = "1.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("me.clip:placeholderapi:2.12.3")
    // Jedis / Redis
    implementation("redis.clients:jedis:5.1.5")
    implementation("org.apache.commons:commons-pool2:2.12.0")
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}