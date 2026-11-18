package com.example.uwbextracredit4377.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uwbextracredit4377.data.TrackingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDashboard(
    uiState: TrackerUiState,
    onToggleMode: (Boolean) -> Unit,
    onTogglePhoneRole: (Boolean) -> Unit,
    onPeerAddressChanged: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    hasPermissions: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "UWB Proximity Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Qorvo DWM3001CDK • FiRa DS-TWR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Card
            ModeToggleCard(
                mode = uiState.mode,
                isPhoneController = uiState.isPhoneController,
                onToggle = { isChecked ->
                    if (isChecked && !hasPermissions) {
                        onRequestPermissions()
                    }
                    onToggleMode(isChecked)
                },
                onRoleToggle = onTogglePhoneRole
            )

            // Warning Banner if permission or hardware error occurred
            if (uiState.errorMessage != null) {
                ErrorMessageBanner(message = uiState.errorMessage)
            }

            // Addresses Card (Phone Local MAC & Qorvo Peer MAC)
            AddressesCard(
                localAddress = uiState.localAddress,
                peerAddress = uiState.peerAddressInput,
                onPeerAddressChanged = onPeerAddressChanged,
                onDone = { focusManager.clearFocus() }
            )

            // Primary Live Distance Readout Card
            LiveDistanceCard(
                smoothedMeters = uiState.smoothedDistanceMeters,
                rawMeters = uiState.rawDistanceMeters,
                distanceCm = uiState.distanceInCm,
                zone = uiState.proximityZone,
                azimuth = uiState.azimuthDegrees
            )

            // Live Distance Trend Sparkline
            DistanceSparkline(
                history = uiState.distanceHistory,
                currentZone = uiState.proximityZone
            )

            // Session Configuration & Status Card
            HardwareInfoCard(
                statusMessage = uiState.statusMessage
            )
        }
    }
}

@Composable
private fun ModeToggleCard(
    mode: TrackingMode,
    isPhoneController: Boolean,
    onToggle: (Boolean) -> Unit,
    onRoleToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (mode == TrackingMode.LIVE_HARDWARE) Color(0xFF2E7D32) else Color(0xFF1976D2)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mode == TrackingMode.LIVE_HARDWARE) "Live Qorvo UWB" else "Simulated Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (mode == TrackingMode.LIVE_HARDWARE) {
                            "DWM3001CDK (Channel 9, Preamble 9, Session 42)"
                        } else {
                            "Default: Cycling 0.3m – 3.5m at 5 Hz (No board needed)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = mode == TrackingMode.LIVE_HARDWARE,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            if (mode == TrackingMode.LIVE_HARDWARE) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Phone Role: ${if (isPhoneController) "Controller (Initiator)" else "Controlee (Responder)"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPhoneController) "Phone sends Poll -> Board replies" else "Board sends Poll -> Phone replies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isPhoneController,
                        onCheckedChange = onRoleToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressesCard(
    localAddress: String,
    peerAddress: String,
    onPeerAddressChanged: (String) -> Unit,
    onDone: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "UWB Network Addresses",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phone Local Address (Controlee)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = localAddress,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "2-BYTE HEX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column {
                Text(
                    text = "Qorvo DWM3001CDK Peer Address",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = peerAddress,
                    onValueChange = onPeerAddressChanged,
                    placeholder = { Text("00:00") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Text(
                    text = "Format: XX:XX (e.g. 00:00 or 12:34)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveDistanceCard(
    smoothedMeters: Float?,
    rawMeters: Float?,
    distanceCm: Int?,
    zone: ProximityZone?,
    azimuth: Float?
) {
    val activeZone = zone ?: ProximityZone.NEAR

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = activeZone.color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zone Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(activeZone.containerColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Zone: ${activeZone.label} • ${activeZone.description}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = activeZone.color
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Live Distance Readout (Meters)
            Text(
                text = if (smoothedMeters != null) "%.2f".format(smoothedMeters) else "--",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeZone.color,
                letterSpacing = (-1).sp
            )

            Text(
                text = "METERS (m)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Centimeters Readout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (distanceCm != null) "$distanceCm cm" else "-- cm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (rawMeters != null && smoothedMeters != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "(Raw: %.2fm)".format(rawMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (azimuth != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Angle of Arrival (AoA): %.1f°".format(azimuth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proximity Zone Legend Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ZoneIndicatorPill(zone = ProximityZone.IMMEDIATE, isCurrent = activeZone == ProximityZone.IMMEDIATE)
                ZoneIndicatorPill(zone = ProximityZone.NEAR, isCurrent = activeZone == ProximityZone.NEAR)
                ZoneIndicatorPill(zone = ProximityZone.FAR, isCurrent = activeZone == ProximityZone.FAR)
            }
        }
    }
}

@Composable
private fun ZoneIndicatorPill(zone: ProximityZone, isCurrent: Boolean) {
    val alpha = if (isCurrent) 1.0f else 0.4f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(zone.containerColor.copy(alpha = alpha))
            .border(
                width = if (isCurrent) 2.dp else 0.dp,
                color = if (isCurrent) zone.color else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = zone.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = zone.color.copy(alpha = alpha)
        )
    }
}

@Composable
private fun HardwareInfoCard(
    statusMessage: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "FiRa Profile Specifications",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Ranging Profile:", style = MaterialTheme.typography.bodySmall)
                Text(text = "FiRa DS-TWR (Unicast)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "RF Channel / Preamble:", style = MaterialTheme.typography.bodySmall)
                Text(text = "Channel 9 / Preamble 9", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Session ID:", style = MaterialTheme.typography.bodySmall)
                Text(text = "42", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Filter:", style = MaterialTheme.typography.bodySmall)
                Text(text = "5-Sample Rolling Average", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Status: $statusMessage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorMessageBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Start
        )
    }
}
