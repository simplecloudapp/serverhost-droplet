plugins {
    `maven-publish`
}

dependencies {
    api(libs.simpleCloudController)
    api(libs.simpleCloudPubSub)
    implementation(rootProject.libs.bundles.log4j)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}