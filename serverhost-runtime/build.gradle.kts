
plugins {
    application
}


dependencies {
    api(project(":serverhost-shared"))
    implementation(rootProject.libs.bundles.log4j)
    api("org.spongepowered:configurate-yaml:4.0.0")
    api("org.spongepowered:configurate-gson:4.0.0")
    api("org.spongepowered:configurate-extra-kotlin:4.1.2")
    api("commons-io:commons-io:2.15.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
    mainClass = "app.simplecloud.droplet.serverhost.runtime.launcher.LauncherKt"
}