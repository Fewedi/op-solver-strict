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

val jacksonVersion = "2.18.1"
val kandyLetsPlotVersion = "0.7.0"
val jgraphtVersion = "1.5.2"
val slf4jVersion = "2.0.16"


dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.jgrapht:jgrapht-core:$jgraphtVersion")

    implementation("org.jetbrains.kotlinx:kandy-lets-plot:$kandyLetsPlotVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation(kotlin("test"))
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}