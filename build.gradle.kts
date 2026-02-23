plugins {
    kotlin("jvm") version "2.0.20"
    application
}

group = "masterthesis"
version = "1.0-SNAPSHOT"

application {
    applicationDefaultJvmArgs = listOf("-Xms2G", "-Xmx16G")
    mainClass.set("masterthesis.MainKt")
}

repositories {
    mavenCentral()
}

val jacksonVersion = "2.18.1"
val kandyLetsPlotVersion = "0.7.0"
val slf4jVersion = "2.0.16"
val gurobiVersion = "12.0.0"
val kotlinCsvVersion = "1.10.0"
val ortoolsVersion = "9.12.4544"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.jetbrains.kotlinx:kandy-lets-plot:$kandyLetsPlotVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")


    implementation("com.jsoizo:kotlin-csv:$kotlinCsvVersion")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")

    implementation("com.gurobi:gurobi:$gurobiVersion")

    implementation("com.google.ortools:ortools-java:$ortoolsVersion")


    testImplementation(kotlin("test"))
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}