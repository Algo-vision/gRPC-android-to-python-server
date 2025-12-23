package com.instacart.sample.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.instacart.sample.ui.theme.Colors
import org.koin.androidx.compose.koinViewModel

@Composable
fun GrpcSampleScreen(
    viewModel: GrpcServiceViewModel = koinViewModel(),
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Colors.background,
        ) {
            val state by viewModel.state.collectAsState()
            ServiceView(
                state = state,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onStartCamera = viewModel::startCamera,
                onStopCamera = viewModel::stopCamera,
                onSendTestFrame = viewModel::sendTestFrame,
            )
        }
    }
}