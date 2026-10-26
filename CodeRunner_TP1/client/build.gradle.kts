plugins {
    application
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