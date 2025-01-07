val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.0.2"
    id("org.gretty") version "4.0.3"
    id("war")
}

group = "com.example.cloud_driver"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.tomcat.jakarta.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files(file("libs").listFiles()))

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.squareup.okhttp:okhttp:2.7.5")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("com.auth0:java-jwt:3.19.2")
    implementation("org.bytedeco:javacv:1.5.10")
    implementation("org.bytedeco:ffmpeg-platform:6.1.1-1.5.10")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.mybatis:mybatis:3.5.15")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-tomcat-jakarta-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml-jvm")
    implementation("io.ktor:ktor-server-servlet-jakarta:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-partial-content:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-sse:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

gretty {
    servletContainer = "tomcat10"
    contextPath = "/CloudDriver-ktor"
    logbackConfigFile = "src/main/resources/logback.xml"
}

afterEvaluate {
    tasks.getByName("run") {
        dependsOn("appRun")
    }
}