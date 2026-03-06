plugins {
    id("org.openapi.generator") version "7.20.0" apply false
    id("org.springframework.boot") version "4.0.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.ersted"
    version = "1.0.0-SNAPSHOT"
    description = "Person Service"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}