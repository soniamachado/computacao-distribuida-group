plugins {
    application
}

val grpcVersion = "1.68.0"

dependencies {
    implementation(project(":contracts"))
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
}

application {
    mainClass.set("isel.cd.manager.ManagerServer")
}