val versions = mapOf(
	"spring-boot-dependencies" to "4.0.3",
	"springdoc-openapi-starter-webflux-ui" to "3.0.2",
	"swagger-annotations" to "2.2.45",

	"person-service-sdk" to "1.0.0-SNAPSHOT",

	"mapstruct" to "1.6.3",
	"logstash-logback-encoder" to "9.0",

	"mockwebserver" to "5.3.2",
	"junit-jupiter" to "1.21.4",
	"wiremock" to "3.13.0",

	"lombok-mapstruct-binding" to "0.2.0",
	"mapstruct-processor" to "1.6.3",
)

plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.20.0"
}

group = "com.ersted"
version = "0.0.1"
description = "Individuals API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenLocal()
	maven {
		url = uri(System.getenv("NEXUS_URL")
			?: "http://localhost:8082/repository/maven-snapshots/")

		credentials {
			username = System.getenv("NEXUS_USERNAME") ?: "admin"
			password = System.getenv("NEXUS_PASSWORD") ?: "admin"
		}
		isAllowInsecureProtocol = true
	}
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:${versions["spring-boot-dependencies"]}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")


//	Observability
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

//	Swagger + OpenApiPlugin
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${versions["springdoc-openapi-starter-webflux-ui"]}")
	implementation("com.fasterxml.jackson.core:jackson-databind")
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("io.swagger.core.v3:swagger-annotations:${versions["swagger-annotations"]}")

//	SDK
	implementation("com.ersted:person-service-sdk:${versions["person-service-sdk"]}")

//	Helpers
	implementation("org.mapstruct:mapstruct:${versions["mapstruct"]}")
	implementation("org.projectlombok:lombok")
	implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash-logback-encoder"]}")


//	Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("com.squareup.okhttp3:mockwebserver:${versions["mockwebserver"]}")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:${versions["junit-jupiter"]}")
	testImplementation("org.wiremock:wiremock-jetty12:${versions["wiremock"]}")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	//	Annotation processor
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${versions["lombok-mapstruct-binding"]}")
	annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstruct-processor"]}")
}

configurations.testRuntimeClasspath {
	resolutionStrategy.eachDependency {
		if (requested.group == "org.eclipse.jetty" ||
			requested.group == "org.eclipse.jetty.http2" ||
			requested.group == "org.eclipse.jetty.http3") {
			useVersion("12.0.16")
			because("WireMock Jetty12 requires Jetty 12.0.x — incompatible with Spring Boot 4 managed 12.1.x")
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

openApiGenerate {
	generatorName.set("spring")

	inputSpec.set("$rootDir/openapi/individuals-api.yaml")

	outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)

	apiPackage.set("com.ersted.individualsapi.api")
	modelPackage.set("com.ersted.individualsapi.dto")
	invokerPackage.set("com.ersted.individualsapi")

	configOptions.set(mapOf(
		"library"               to "spring-boot",

		"useSpringBoot4"        to "true",
		"useJackson3"           to "true",
		"reactive" 				to "true",

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

tasks.named("compileJava") {
	dependsOn(tasks.openApiGenerate)
}
