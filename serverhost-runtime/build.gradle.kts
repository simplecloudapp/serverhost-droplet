import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
}

dependencies {
    api(project(":serverhost-shared"))
    api(rootProject.libs.kotlinCoroutines)
    api(rootProject.libs.bundles.configurate)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.clikt)
    implementation(rootProject.libs.commonsIo)
    implementation(rootProject.libs.toml4j)
}

tasks.named("shadowJar", ShadowJar::class) {
    dependencies {
        include(dependency(rootProject.libs.kotlinCoroutines.get()))
        include(dependency(rootProject.libs.log4jApi.get()))
        include(dependency(rootProject.libs.log4jCore.get()))
        include(dependency(rootProject.libs.log4jSlf4j.get()))
        include(dependency(rootProject.libs.clikt.get()))
        include(dependency(rootProject.libs.commonsIo.get()))
        include(dependency(rootProject.libs.toml4j.get()))
    }
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.launcher.LauncherKt"
}