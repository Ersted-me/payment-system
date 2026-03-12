val versions = mapOf(
	"spring-boot-dependencies" to "4.0.3",
	"springdoc-openapi-starter-webflux-ui" to "3.0.2",
	"swagger-annotations" to "2.2.45",
	"logstash-logback-encoder" to "9.0",

	"mockwebserver" to "5.3.2", //5.3.2
	"junit-jupiter" to "1.21.4"
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

//	Helpers
	implementation("org.projectlombok:lombok")
	implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash-logback-encoder"]}")


//	Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("com.squareup.okhttp3:mockwebserver:${versions["mockwebserver"]}")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:${versions["junit-jupiter"]}")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	//	Annotation processor
	annotationProcessor("org.projectlombok:lombok")
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
