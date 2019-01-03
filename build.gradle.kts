import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sdp-kanal"
version = "1.3.0-SNAPSHOT"

description = """Application for routing Secure Digital Post to DIFI"s message dispatcher"""

val artemisVersion = "2.6.2"
val atomikosVersion = "4.0.6"
val camelVersion = "2.22.2"
val commonsIOVersion = "2.6"
val glassfishJaxbVersion = "2.3.1"
val jaxwsApiVersion = "2.1"
val jacksonVersion = "2.9.7"
val jschVersion = "0.1.54"
val jtaApiVersion = "1.1"
val kluentVersion = "1.43"
val konfigVersion = "1.6.10.0"
val ktorVersion = "1.0.0"
val logbackVersion = "1.2.3"
val mqVersion = "9.1.0.0"
val prometheusVersion = "0.5.0"
val saajVersion = "1.5.0"
//val sdpSharedVersion = "2.6"
val spekVersion = "2.0.0-rc.1"
val sdpClientVersion = "5.3"
val woodstoxVersion = "5.2.0"
val xmlSecVersion = "2.1.2"

plugins {
    java
    kotlin("jvm") version "1.3.10"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/spekframework/spek-dev") }
}

tasks.create("printVersion") {
    println(project.version)
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
    testLogging {
        showStandardStreams = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "no.nav.kanal.SdpKanalApplicationKt"
}

dependencies {
    // Communication libraries
    implementation("com.atomikos:transactions:$atomikosVersion")
    implementation("com.fasterxml.woodstox:woodstox-core:$woodstoxVersion")
    implementation("com.ibm.mq:com.ibm.mq.allclient:$mqVersion")
    implementation("com.jcraft:jsch:$jschVersion")
    //implementation("com.sun.xml.messaging.saaj:saaj-impl:$saajVersion")
    // Temporary, the version of mimepull saaj depends on has been removed from maven central
    //implementation("org.jvnet.mimepull:mimepull:1.9.10")
    implementation("commons-io:commons-io:$commonsIOVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("javax.transaction:jta:$jtaApiVersion")
    //implementation("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    implementation("no.difi.sdp:sikker-digital-post-klient-java:$sdpClientVersion") {
    //    exclude(module = "jaxb2-basics-runtime")
        exclude(module = "spring-security-core")
        exclude(module = "wss4j-ws-security-dom")
        exclude(module = "wss4j-ws-security-common")
    }
    //implementation("no.digipost:sdp-api-client:$sdpSharedVersion")
    implementation("org.apache.santuario:xmlsec:$xmlSecVersion")
    //implementation("org.glassfish.jaxb:jaxb-runtime:$glassfishJaxbVersion")
    //implementation("org.jvnet.jaxb2_commons:jaxb2-basics-runtime:1.11.1") {
    //    exclude(module = "jaxb-runtime")
    //    exclude(module = "jaxb-api")
    //}

    // Camel
    implementation("org.apache.camel:camel-core:$camelVersion")
    implementation("org.apache.camel:camel-jaxb:$camelVersion")
    implementation("org.apache.camel:camel-jms:$camelVersion")
    implementation("org.apache.camel:camel-metrics:$camelVersion")

    // Logging and configuration
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.natpryce:konfig:$konfigVersion")
    implementation("org.springframework.security:spring-security-core:4.1.0.RELEASE")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("javax.xml.soap:javax.xml.soap-api:1.4.0")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("javax.jws:jsr181-api:1.0-MR1")

    // Testing
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runtime-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")

    testImplementation("org.apache.cxf:cxf-rt-frontend-jaxws:3.1.17")
    testImplementation("org.apache.cxf:cxf-rt-transports-http-jetty:3.1.17")
    testImplementation("org.apache.cxf:cxf-rt-ws-security:3.1.17")
    testImplementation("org.apache.cxf:cxf-rt-features-logging:3.1.17")

    testImplementation("org.apache.activemq:artemis-server:$artemisVersion")
    testImplementation("org.apache.activemq:artemis-jms-client:$artemisVersion")
    testImplementation("org.apache.wss4j:wss4j-ws-security-dom:2.1.8")
    testImplementation("org.apache.wss4j:wss4j-ws-security-common:2.1.8")
}
