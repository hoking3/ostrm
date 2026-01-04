/**
 * Ostrm Backend - Quarkus + GraalVM Native Image
 * @author hienao
 * @date 2025-12-31
 */

val quarkusVersion by extra("3.17.5")
val flywayVersion by extra("11.4.0")
val testcontainersVersion by extra("1.20.6")

plugins {
    java
    `java-library`
    jacoco
    id("io.quarkus") version "3.17.5"
    id("pmd")
    // id("com.diffplug.spotless") version "7.0.2" // Disabled to prevent corruption
}

group = "com.hienao.openlist2strm"
version = "3.0.0"
description = "openlist to strm - Quarkus Native"
java.sourceCompatibility = JavaVersion.VERSION_21

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Quarkus 核心依赖
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    
    // RESTful API
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    
    // 安全和 JWT
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    
    // 数据库和 ORM
    implementation("io.quarkiverse.jdbc:quarkus-jdbc-sqlite:3.0.11") // SQLite Native Image 支持
    implementation("io.quarkiverse.mybatis:quarkus-mybatis:2.2.2")
    
    // 数据库迁移
    implementation("io.quarkus:quarkus-flyway")
    
    // 缓存
    implementation("io.quarkus:quarkus-cache")
    
    // 定时任务
    implementation("io.quarkus:quarkus-scheduler")
    
    // WebSocket
    implementation("io.quarkus:quarkus-websockets-next")
    
    // OpenAPI 文档
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.25")
    
    // 健康检查
    implementation("io.quarkus:quarkus-smallrye-health")
    
    // 工具类
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-io:commons-io:2.15.1")
    
    // Lombok (开发时使用，Native 编译时需要注意)
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    
    // 测试依赖
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    
    // JSR 305 注解
    api("org.jspecify:jspecify:1.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

jacoco {
    toolVersion = "0.8.12"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

pmd {
    sourceSets = listOf(java.sourceSets.findByName("main"))
    isConsoleOutput = true
    toolVersion = "7.9.0"
    rulesMinimumPriority.set(5)
    ruleSetFiles = files("pmd-rules.xml")
}
