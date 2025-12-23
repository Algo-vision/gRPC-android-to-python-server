package com.instacart.sample.di

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraManager
import com.instacart.sample.grpc.GrpcConfig
import com.instacart.sample.grpc.SideCameraGrpcClient
import com.instacart.sample.imagestream.ImageStream
import com.instacart.sample.imagestream.StreamConfig
import com.instacart.sample.imagestream.camera.CameraImageStream
import com.instacart.sample.ui.GrpcServiceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    /***************************************************************
     * Update the configurations here as needed
     ***************************************************************/
    single { GrpcConfig(host = "10.0.2.2", port = 50051, useTls = false) }
    single { StreamConfig() }

    // Provide application-level CoroutineScope for gRPC client
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Provide GrpcClient with CoroutineScope
    single { SideCameraGrpcClient(get(), get()) }

    // Provide Camera dependencies
    single<CameraManager> {
        val context = get<Context>()
        requireNotNull(context.getSystemService(CAMERA_SERVICE) as? CameraManager) {
            "CameraManager not available"
        }
    }
    single<ImageStream> { CameraImageStream(get(), get(), get()) }

    // Provide GrpcServiceViewModel
    viewModelOf(::GrpcServiceViewModel)
}
