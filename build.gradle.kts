plugins {
    kotlin("jvm") version "2.0.20"
    application
}

group = "masterthesis"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("masterthesis.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
    // maybe this is not needed
    implementation("org.jgrapht:jgrapht-core:1.5.2")
    //implementation("org.jetbrains.kotlinx:dataframe:0.14.1")
    implementation("org.jetbrains.kotlinx:kandy-lets-plot:0.7.0")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}



tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}