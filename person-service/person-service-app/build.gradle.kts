val versions = mapOf(
	"spring-boot-dependencies" to "4.0.3",
	"mapstruct" to "1.6.3",
	"logstash-logback-encoder" to "9.0",
	"springdoc-openapi-starter-webmvc-ui" to "3.0.2",
	"swagger-annotations" to "2.2.45",
	"lombok-mapstruct-binding" to "0.2.0",
	"mapstruct-processor" to "1.6.3",
)

plugins {
	java
	idea
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	id("org.openapi.generator")
}


java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:${versions["spring-boot-dependencies"]}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	//Observability
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Audit
	implementation("org.hibernate.orm:hibernate-envers")

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
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("io.swagger.core.v3:swagger-annotations:${versions["swagger-annotations"]}")

	// Tests
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
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

	inputSpec.set("$rootDir/openapi/person-service-api.yaml")

	outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)

	apiPackage.set("com.ersted.personservice.api")
	modelPackage.set("com.ersted.personservice.model")
	invokerPackage.set("com.ersted.personservice")

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
