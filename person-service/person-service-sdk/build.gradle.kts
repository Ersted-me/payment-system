plugins {
    `java-library`
    `maven-publish`
    id("org.openapi.generator")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.3"))

    api("org.springframework:spring-web")
    api("org.springframework:spring-context")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("jakarta.validation:jakarta.validation-api")
    api("jakarta.annotation:jakarta.annotation-api")
    api("io.swagger.core.v3:swagger-annotations:2.2.20")
}

openApiGenerate {
    generatorName.set("spring")

    inputSpec.set("$rootDir/openapi/person-service-api.yaml")

    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)

    apiPackage.set("com.ersted.personservice.sdk.api")
    modelPackage.set("com.ersted.personservice.sdk.model")
    invokerPackage.set("com.ersted.personservice.sdk")

    configOptions.set(mapOf(
        "library"               to "spring-http-interface",

        "useSpringBoot4"        to "true",
        "useJackson3"           to "true",

        "interfaceOnly"         to "true",
        "skipDefaultInterface"  to "true",
        "openApiNullable"       to "false",
        "documentationProvider" to "none",

        "useTags"               to "true",
        "dateLibrary"           to "java8",
    ))

    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

publishing {
    publications {
        create<MavenPublication>("person-service-sdk") {
            from(components["java"])

            groupId    = project.group.toString()
            artifactId = "person-service-sdk"
            version    = project.version.toString()
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "nexus"
            val nexusUrl = (findProperty("nexusUrl") as String?)
                ?: System.getenv("NEXUS_URL")
                ?: return@maven

            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "$nexusUrl/repository/maven-snapshots/"
                else
                    "$nexusUrl/repository/maven-releases/"
            )

            isAllowInsecureProtocol = true

            credentials {
                username = (findProperty("nexusUsername") as String?)
                    ?: System.getenv("NEXUS_USERNAME")
                            ?: error("nexusUsername not set")
                password = (findProperty("nexusPassword") as String?)
                    ?: System.getenv("NEXUS_PASSWORD")
                            ?: error("nexusPassword not set")
            }
        }
    }
}