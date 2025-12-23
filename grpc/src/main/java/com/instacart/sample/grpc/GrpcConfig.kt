package com.instacart.sample.grpc

data class GrpcConfig(
    val host: String,
    val port: Int,
    val useTls: Boolean = false,
)
