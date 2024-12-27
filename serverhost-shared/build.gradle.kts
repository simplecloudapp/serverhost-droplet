plugins {
    `maven-publish`
}

dependencies {
    api(libs.simplecloud.controller)
    api(libs.simplecloud.pubsub)
    api(rootProject.libs.commons.io)
    api(project(":serverhost-configurator"))
    implementation(rootProject.libs.gson)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.kotlin.reflect)
    implementation(rootProject.libs.ktor.cio)
    testImplementation(rootProject.libs.kotlin.test)
    implementation(rootProject.libs.commons.compress)
    implementation(rootProject.libs.xz)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}