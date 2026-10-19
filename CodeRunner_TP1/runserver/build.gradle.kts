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

    // Redis
    implementation("redis.clients:jedis:7.5.3")

    // Docker Java
    implementation("com.github.docker-java:docker-java-core:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

application {
    mainClass.set("isel.cd.runserver.RunServer")
}