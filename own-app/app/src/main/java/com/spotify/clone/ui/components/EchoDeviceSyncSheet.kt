package com.spotify.clone.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.sync.DeviceSyncManager
import com.spotify.clone.core.sync.DeviceType
import com.spotify.clone.core.sync.EchoDevice

/**
 * ECHO Connect & Multi-Device Sync Bottom Sheet:
 * - Simultaneous multi-room wireless playback across multiple speakers, TVs, and PCs
 * - Real-time ECHO Jam session (listen synchronously with friends, shared queue)
 * - Individual and master volume controls with ultra-low latency timing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoDeviceSyncSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val syncManager = remember { DeviceSyncManager.getInstance(context) }
    val syncState by syncManager.syncState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val sheetBg = if (isDark) Color(0xFF14131D) else Color(0xFFF9F7F4)
    val cardBg = if (isDark) Color(0xFF1F1D2B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2E2C3D) else Color(0xFFE6E2DC)
    val textPrimary = if (isDark) Color(0xFFF6F3FA) else Color(0xFF17171C)
    val textSecondary = if (isDark) Color(0xFFA6A2B3) else Color(0xFF777680)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ECHO Connect & Multi-Device",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = syncState.listeningStatusDescription,
                        color = activeTheme.primaryColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tabs: Devices & Multi-Room | Group Jam
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = activeTheme.primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = activeTheme.primaryColor,
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Connected Devices",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Group Jam (SharePlay)",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTabIndex == 0) {
                // TAB 1: CONNECTED DEVICES & SIMULTANEOUS MULTI-ROOM AUDIO
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Multi-Room Simultaneous Playback Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            activeTheme.primaryColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                                            activeTheme.gradientEnd.copy(alpha = if (isDark) 0.20f else 0.10f)
                                        )
                                    )
                                )
                                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sync to Multiple Devices",
                                            color = textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Play sound simultaneously across selected speakers in sync.",
                                            color = textSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Switch(
                                        checked = syncState.isMultiRoomAudioEnabled,
                                        onCheckedChange = { enabled ->
                                            syncManager.toggleMultiRoomAudio(enabled)
                                            val msg = if (enabled) "Multi-Room audio activated on ${syncState.activeDeviceCount} devices"
                                            else "Switched to single device output"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = activeTheme.primaryColor,
                                            uncheckedThumbColor = textSecondary,
                                            uncheckedTrackColor = cardBorder
                                        )
                                    )
                                }

                                if (syncState.isMultiRoomAudioEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Podcasts,
                                            contentDescription = null,
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ultra-low latency audio synchronization active (12ms buffer)",
                                            color = activeTheme.primaryColor,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Devices Header
                    item {
                        Text(
                            text = "AVAILABLE OUTPUTS (${syncState.devices.size})",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Device Items List
                    items(syncState.devices) { device ->
                        DeviceCard(
                            device = device,
                            isMultiRoom = syncState.isMultiRoomAudioEnabled,
                            onSelect = { syncManager.selectSingleDevice(device.id) },
                            onToggleMultiRoom = { syncManager.toggleDeviceInMultiRoom(device.id) },
                            onVolumeChange = { syncManager.setDeviceVolume(device.id, it) },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentColor = activeTheme.primaryColor
                        )
                    }
                }
            } else {
                // TAB 2: GROUP JAM & SOCIAL SHAREPLAY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val activeSession = syncState.activeSession
                    if (activeSession != null) {
                        // Active Session Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "LIVE GROUP JAM",
                                            color = activeTheme.primaryColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(activeTheme.primaryColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${activeSession.participants.size} Listening Together",
                                            color = activeTheme.primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Session Code Box
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF12111A) else Color(0xFFF1EFEA))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "SESSION JOIN CODE",
                                            color = textSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = activeSession.sessionCode,
                                            color = textPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp
                                        )
                                    }

                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ECHO Jam Code", activeSession.sessionCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Session code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = activeTheme.primaryColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "PARTICIPANTS & COLLABORATORS",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                activeSession.participants.forEach { participant ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(participant.avatarColorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = participant.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = participant.name,
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (participant.isHost) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(cardBorder)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "HOST",
                                                    color = textSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        syncManager.leaveJamSession()
                                        Toast.makeText(context, "Left Group Jam session", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                                ) {
                                    Text("Leave Group Jam", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Start New Jam Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Group,
                                        contentDescription = null,
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Start an ECHO Jam",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Invite friends nearby or online to listen together in perfect sync and add songs to the shared queue.",
                                    color = textSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                ElevatedButton(
                                    onClick = {
                                        val session = syncManager.startJamSession("Theja")
                                        Toast.makeText(context, "Group Jam started: ${session.sessionCode}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = activeTheme.primaryColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start a Jam Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { showJoinDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Outlined.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Friend's Jam with Code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join ECHO Jam Session", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the 6-character session code shown on your friend's screen (e.g. ECHO-4821):",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        placeholder = { Text("ECHO-XXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            syncManager.joinJamSession(joinCodeInput, "Theja")
                            Toast.makeText(context, "Joined session $joinCodeInput successfully!", Toast.LENGTH_SHORT).show()
                            showJoinDialog = false
                            selectedTabIndex = 1
                        }
                    },
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = activeTheme.primaryColor, contentColor = Color.White)
                ) {
                    Text("Join Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceCard(
    device: EchoDevice,
    isMultiRoom: Boolean,
    onSelect: () -> Unit,
    onToggleMultiRoom: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color
) {
    val isActive = if (isMultiRoom) (device.isSyncedInMultiRoom || device.isCurrent) else device.isCurrent
    val borderColor = if (isActive) accentColor.copy(alpha = 0.5f) else cardBorder

    val icon: ImageVector = when (device.type) {
        DeviceType.PHONE -> Icons.Filled.PhoneAndroid
        DeviceType.SPEAKER -> Icons.Filled.Speaker
        DeviceType.COMPUTER -> Icons.Filled.Computer
        DeviceType.TV -> Icons.Filled.Cast
        DeviceType.CAR -> Icons.Filled.DirectionsCar
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                if (isMultiRoom) onToggleMultiRoom() else onSelect()
            }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) accentColor.copy(alpha = 0.18f) else cardBorder.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) accentColor else textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = if (isActive) accentColor else textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${device.connectionType} • ${device.latencyMs}ms delay",
                        color = textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isMultiRoom) {
                    Switch(
                        checked = device.isSyncedInMultiRoom || device.isCurrent,
                        onCheckedChange = { onToggleMultiRoom() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = textSecondary,
                            uncheckedTrackColor = cardBorder
                        )
                    )
                } else {
                    if (device.isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = accentColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Per-device volume slider when active
            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = device.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = cardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(device.volume * 100).toInt()}%",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}
