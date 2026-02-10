val versions = mapOf(
	"logstash-logback-encoder" to "9.0",
	"springdoc-openapi-starter-webflux-ui" to "2.8.9",
	"jackson-databind-nullable" to "0.2.8",
	"swagger-annotations" to "2.2.41",
	"lombok" to "1.18.42",
	"mockwebserver" to "4.12.0",
	"testcontainers" to "2.0.3",
	"junit-jupiter" to "1.21.4"
)

plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.19.0"
}

group = "com.ersted"
version = "0.0.1"
description = "Individuals API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation ("org.springframework.boot:spring-boot-starter-validation")

//	Observability
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-aop")

//	Metrics (needs AOP) + Logs
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash-logback-encoder"]}")

//	Trace (needs AOP)
	implementation("io.micrometer:micrometer-tracing-bridge-otel")
	implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

//	OpenApi
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${versions["springdoc-openapi-starter-webflux-ui"]}")
	implementation("org.openapitools:jackson-databind-nullable:${versions["jackson-databind-nullable"]}")
	implementation("io.swagger.core.v3:swagger-annotations:${versions["swagger-annotations"]}")

//	Helpers
	implementation("org.projectlombok:lombok:${versions["lombok"]}")
	annotationProcessor ("org.projectlombok:lombok")

//	Tests
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("com.squareup.okhttp3:mockwebserver:${versions["mockwebserver"]}")
	testImplementation("org.testcontainers:testcontainers:${versions["testcontainers"]}")
	testImplementation("org.testcontainers:junit-jupiter:${versions["junit-jupiter"]}")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set("$rootDir/openapi/individuals-api.yaml")
	outputDir.set("$buildDir/generated-sources/openapi")
	apiPackage.set("com.ersted.api")
	modelPackage.set("com.ersted.dto")
	configOptions.set(mapOf(
		"dateLibrary" to "java8",
		"interfaceOnly" to "true",
		"useSpringBoot3" to "true",
		"reactive" to "true",
		"delegatePattern" to "true"
	))
}

sourceSets {
	main {
		java {
			srcDir("$buildDir/generated-sources/openapi/src/main/java")
		}
	}
}

tasks.named("compileJava") {
	dependsOn("openApiGenerate")
}
