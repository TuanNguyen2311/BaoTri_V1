package com.example.baotri.ui.ktv.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.domain.model.MaintenanceLog
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil

@Composable
fun KtvDashboardScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    vm: KtvDashboardViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        bottomBar = {
            KtvBottomNav(
                current = 0,
                onHome = {},
                onScan = onNavigateToScan,
                onHistory = onNavigateToHistory,
                onSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Xin chào,", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(state.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(GreenLight).border(1.dp, GreenPrimary.copy(.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }

            // ── Stats ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "${state.stats.scannedToday}",
                        label = "Scan hôm nay",
                        icon = Icons.Default.QrCodeScanner,
                        iconBg = GreenLight, iconTint = GreenPrimary,
                        valueColor = GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${state.stats.pendingLogs}",
                        label = "Log chờ xử lý",
                        icon = Icons.Default.PendingActions,
                        iconBg = AmberLight, iconTint = AmberColor,
                        valueColor = AmberColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Scan Button ───────────────────────────────────
            item {
                Button(
                    onClick = onNavigateToScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Scan QR / Barcode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Recent Logs ───────────────────────────────────
            item {
                SectionTitle("Scan gần đây", action = "Xem tất cả", onAction = onNavigateToHistory)
                Spacer(Modifier.height(8.dp))
            }

            if (state.recentLogs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.History,
                        message = "Chưa có lịch sử scan"
                    )
                }
            } else {
                items(state.recentLogs) { log ->
                    RecentLogItem(log = log, onClick = { onNavigateToDetail(log.deviceId) })
                }
            }
        }
    }
}

@Composable
private fun RecentLogItem(log: MaintenanceLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(if (log.deviceName.isNotBlank()) log.deviceName else "Thiết bị #${log.deviceId}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(DateUtil.relativeTime(log.performedAt),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        StatusBadge(status = log.status)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun KtvBottomNav(
    current: Int,
    onHome: () -> Unit,
    onScan: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = current == 0, onClick = onHome,
            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Trang chủ") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary, selectedTextColor = GreenPrimary)
        )
        NavigationBarItem(
            selected = current == 1, onClick = onScan,
            icon = { Icon(Icons.Default.QrCodeScanner, null) }, label = { Text("Scan") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary, selectedTextColor = GreenPrimary)
        )
        NavigationBarItem(
            selected = current == 2, onClick = onHistory,
            icon = { Icon(Icons.Default.History, null) }, label = { Text("Lịch sử") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary, selectedTextColor = GreenPrimary)
        )
        NavigationBarItem(
            selected = current == 3, onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Cài đặt") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLight,
                selectedIconColor = GreenPrimary, selectedTextColor = GreenPrimary)
        )
    }
}
