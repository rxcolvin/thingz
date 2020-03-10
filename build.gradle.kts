val derbyVersion = "10.14.2.0"
val http4kVersion = "3.189.0"

plugins {
    kotlin("jvm") version "1.3.61"
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}



dependencies {
    implementation(kotlin(module = "stdlib-jdk8"))
    implementation(
        group = "org.apache.derby",
        name = "derby",
        version = derbyVersion
    )
    implementation(
        "thingz:thingz-core-all-jvm:0.0.1"
    )
    listOf(
        "http4k-core",
        "http4k-server-jetty",
        "http4k-client-apache",
        "http4k-server-ktorcio"

    ).forEach {
        compile(
            group = "org.http4k",
            name = it,
            version = http4kVersion
        )
    }

}


val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    suppressWarnings = true
    jvmTarget = "1.8"

}

