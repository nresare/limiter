plugins {
    id("java")
}

group = "com.resare"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jetty:jetty-server:11.0.9")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.3.0-alpha14")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}