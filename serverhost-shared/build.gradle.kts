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
    implementation("io.ktor:ktor-client-java:3.0.1")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.0.1")
    testImplementation(rootProject.libs.kotlin.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}