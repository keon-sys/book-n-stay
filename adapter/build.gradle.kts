dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}