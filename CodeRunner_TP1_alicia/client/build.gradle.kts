
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.gradleup.shadow") version "9.3.1"
}

val grpcVersion = "1.68.0"

dependencies {
    implementation(project(":contracts"))
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

application {
    mainClass.set("isel.cd.client.ClientApp")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("Demo.jar")
    manifest {
        attributes["Main-Class"] = "isel.cd.client.Demo"
    }
    mergeServiceFiles()
}
