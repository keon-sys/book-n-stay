dependencies {
    api(project(":domain"))
}

tasks.bootJar {
    enabled = false
}
