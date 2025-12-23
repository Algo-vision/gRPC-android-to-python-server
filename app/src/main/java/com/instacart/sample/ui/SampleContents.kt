package com.instacart.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.instacart.sample.logger.Logger
import com.instacart.sample.ui.GrpcServiceViewModel.ServiceState
import com.instacart.sample.ui.theme.Colors
import com.instacart.sample.ui.theme.Typography

@Composable
internal fun ServiceView(
    state: ServiceState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onSendTestFrame: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ServiceStatusCard(
                state,
                modifier = Modifier.weight(1f),
            )
            ServiceControls(
                state = state,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onStartCamera = onStartCamera,
                onStopCamera = onStopCamera,
                onSendTestFrame = onSendTestFrame,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            "Logs (recent first)",
            style = Typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        LogsView(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ServiceStatusCard(
    state: ServiceState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Colors.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusText("Service status: ${state.serviceStatus}")
            StatusText("Service config: ${state.config}")
            StatusText("Camera status: ${state.cameraStatus}")
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = Typography.bodyLarge,
        color = Colors.onPrimaryContainer
    )
}

@Composable
internal fun ServiceControls(
    state: ServiceState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onSendTestFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        ControlButton(
            text = "Connect to service",
            onClick = onConnect,
            enabled = !state.isConnected,
        )
        ControlButton(
            text = "Disconnect from service",
            onClick = onDisconnect,
            enabled = state.isConnected,
        )
        ControlButton(
            text = "Start camera",
            onClick = onStartCamera,
            enabled = !state.isCameraStarted,
        )
        ControlButton(
            text = "Stop camera",
            onClick = onStopCamera,
            enabled = state.isCameraStarted,
        )
        ControlButton(
            text = "Send test frame",
            onClick = onSendTestFrame,
            enabled = state.isConnected,
        )
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text, style = Typography.bodyMedium)
    }
}

@Composable
internal fun LogsView(
    modifier: Modifier = Modifier,
) {
    val logs by Logger.logs.collectAsState()
    val listState = rememberLazyListState()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Colors.surfaceVariant),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
