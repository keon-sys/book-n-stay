dependencies {
    implementation(project(":domain"))
}

tasks.bootJar {
    enabled = false
}
