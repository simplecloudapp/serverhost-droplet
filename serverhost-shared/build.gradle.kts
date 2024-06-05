plugins {
    `maven-publish`
}

dependencies {
    api(libs.simpleCloudController)
    implementation(rootProject.libs.bundles.log4j)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}