
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


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation ("org.springframework.boot:spring-boot-starter-validation")

//	Observability
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("net.logstash.logback:logstash-logback-encoder:9.0")
	implementation("io.micrometer:micrometer-tracing-bridge-otel")
	implementation("io.opentelemetry:opentelemetry-exporter-otlp")
	runtimeOnly("io.grpc:grpc-netty-shaded:1.78.0")


//	OpenApi
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")
	implementation("org.openapitools:jackson-databind-nullable:0.2.8")
	implementation("io.swagger.core.v3:swagger-annotations:2.2.41")

//	Helpers
	implementation("org.projectlombok:lombok:1.18.42")
	annotationProcessor ("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
	testImplementation("org.testcontainers:testcontainers:2.0.3")
	testImplementation("org.testcontainers:junit-jupiter:1.21.4")

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


