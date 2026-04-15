val versions = mapOf(
	"spring-boot-dependencies" to "4.0.4",
	"mapstruct" to "1.6.3",
	"logstash-logback-encoder" to "9.0",
	"springdoc-openapi-starter-webmvc-ui" to "3.0.2",
	"swagger-annotations" to "2.2.45",
	"lombok-mapstruct-binding" to "0.2.0",
	"mapstruct-processor" to "1.6.3",
	"testcontainers" to "1.21.4",
	"avro" to "1.12.1",
)

plugins {
	java
	idea
	id("org.springframework.boot") version "4.0.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.20.0"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.ersted"
version = "0.0.1-SNAPSHOT"

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
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//	implementation ("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-kafka")
	implementation("org.apache.avro:avro:${versions["avro"]}")

	//Observability
//	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
//	implementation("org.springframework.boot:spring-boot-starter-aspectj")
//	implementation("org.springframework.boot:spring-boot-starter-actuator")
//	implementation("io.micrometer:micrometer-registry-prometheus")

	// Migration
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")

	//Helpers
	implementation("org.mapstruct:mapstruct:${versions["mapstruct"]}")
	compileOnly("org.projectlombok:lombok")
	implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash-logback-encoder"]}")

	// Swagger + OpenApiPlugin
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${versions["springdoc-openapi-starter-webmvc-ui"]}")
	implementation("com.fasterxml.jackson.core:jackson-databind")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("io.swagger.core.v3:swagger-annotations:${versions["swagger-annotations"]}")

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
//	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito:mockito-core")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:${versions["testcontainers"]}")
	testImplementation("org.testcontainers:postgresql:${versions["testcontainers"]}")
	testImplementation("org.testcontainers:kafka:${versions["testcontainers"]}")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	//	Annotation processor
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${versions["lombok-mapstruct-binding"]}")
	annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstruct-processor"]}")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

openApiGenerate {
	generatorName.set("spring")

	inputSpec.set("$rootDir/openapi/wallet-service-api.yaml")

	outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)

	apiPackage.set("com.ersted.walletservice.api")
	modelPackage.set("com.ersted.walletservice.model")
	invokerPackage.set("com.ersted.walletservice")

	configOptions.set(mapOf(
		"library"               to "spring-boot",

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

avro {
	isCreateSetters.set(true)
	isCreateOptionalGetters.set(false)
	isGettersReturnOptional.set(false)
	isOptionalGettersForNullableFieldsOnly.set(false)
	fieldVisibility.set("PRIVATE")
	outputCharacterEncoding.set("UTF-8")
	stringType.set("String")
	isEnableDecimalLogicalType.set(true)
}

sourceSets {
	main {
		java {
			srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
			srcDir(layout.buildDirectory.dir("generated-main-avro-java"))
		}
	}
}

tasks.compileJava {
	dependsOn(tasks.openApiGenerate)
}
