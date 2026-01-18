/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.jte)
    alias(libs.plugins.jib)
}

version = "0.1.0"
group = "dev.brice.fancymail"

repositories {
    mavenCentral()
}

dependencies {
    // Micronaut annotation processors - managed by plugin
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("info.picocli:picocli-codegen:${libs.versions.picocli.get()}")

    // Micronaut core
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.views:micronaut-views-jte")

    // Picocli for CLI
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation(libs.picocli)
    compileOnly(libs.picocli)

    // HTML parsing and Markdown conversion
    implementation(libs.jsoup)
    implementation(libs.flexmark)

    // Caching
    implementation(libs.caffeine)

    // Merkle tree
    implementation(libs.merkle.tree)

    // JTE templates runtime
    implementation(libs.jte)

    // Logging
    runtimeOnly(libs.logback.classic)

    // YAML configuration support
    runtimeOnly("org.yaml:snakeyaml")

    // Testing
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core:3.27.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass = "dev.brice.fancymail.Application"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jte {
    sourceDirectory = file("src/main/jte").toPath()
    contentType = gg.jte.ContentType.Html
    generate()
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("dev.brice.fancymail.*")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Fix task dependency for JTE and Micronaut's classpath inspector
tasks.named("inspectRuntimeClasspath") {
    dependsOn(tasks.named("generateJte"))
}

// Jib configuration for Google Cloud Run deployment
val gcpProjectId = providers.gradleProperty("gcpProjectId")

jib {
    from {
        image = "azul/zulu-openjdk:25-jre"
    }
    to {
        image = gcpProjectId.map { "gcr.io/$it/fancy-mail" }.getOrElse("fancy-mail")
        tags = setOf("latest", version.toString())
    }
    container {
        ports = listOf("8080")
        environment = mapOf(
            "MICRONAUT_ENVIRONMENTS" to "cloud"
        )
        jvmFlags = listOf(
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0"
        )
    }
}
