plugins {
    application
}

dependencies {
    implementation(rootProject.libs.toml4j)
    api(rootProject.libs.bundles.configurate)
    implementation(rootProject.libs.bundles.night.config)
    implementation(rootProject.libs.plexus.utils)
    api(rootProject.libs.clikt)
}

application {
    mainClass.set("app.simplecloud.serverhost.cli.LauncherKt")
}


