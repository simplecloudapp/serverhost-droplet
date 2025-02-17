plugins {
    application
}

dependencies {
    api(rootProject.libs.bundles.configurate)

    implementation(rootProject.libs.toml4j)
    implementation(rootProject.libs.bundles.night.config)
    api(rootProject.libs.plexus.utils)
    api(rootProject.libs.clikt)
}

application {
    mainClass.set("app.simplecloud.serverhost.cli.LauncherKt")
}


