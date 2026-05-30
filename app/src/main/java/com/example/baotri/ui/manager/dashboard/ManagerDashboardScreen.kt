package com.example.baotri.ui.manager.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.domain.model.AlertType
import com.example.baotri.domain.model.DeviceAlert
import com.example.baotri.domain.model.WeeklyLogCount
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

// ── Screen ─────────────────────────────────────────────────────


@Composable
fun ManagerDashboardScreen(
    onNavigateToDeviceDetail: (Long) -> Unit,
    vm: ManagerDashboardViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tổng quan hệ thống",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(state.fullName,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(PurpleLight).border(1.dp, PurplePrimary.copy(.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, null,
                            tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }

            // Stats 2x2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "${state.stats.totalDevices}",
                        label = "Tổng thiết bị",
                        icon = Icons.Default.Build,
                        iconBg = GreenLight, iconTint = GreenPrimary, valueColor = GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${state.stats.pendingCount}",
                        label = "Chờ xử lý",
                        icon = Icons.Default.Warning,
                        iconBg = AmberLight, iconTint = AmberColor, valueColor = AmberColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        value = "${state.stats.expiredWarrantyCount}",
                        label = "Hết bảo hành",
                        icon = Icons.Default.EventBusy,
                        iconBg = RedLight, iconTint = RedColor, valueColor = RedColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${state.stats.logsThisMonth}",
                        label = "Log tháng này",
                        icon = Icons.Default.Assignment,
                        iconBg = PurpleLight, iconTint = PurplePrimary, valueColor = PurplePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly chart
            item {
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Log bảo trì theo tuần",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Tháng này", fontSize = 11.sp, color = GreenPrimary)
                        }
                        Spacer(Modifier.height(12.dp))
                        WeeklyBarChart(data = state.weeklyData)
                    }
                }
            }

            // Alerts
            item {
                Spacer(Modifier.height(14.dp))
                SectionTitle("Cần chú ý")
                Spacer(Modifier.height(8.dp))
            }

            if (state.alerts.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.CheckCircle, message = "Không có cảnh báo nào")
                }
            } else {
                items(state.alerts) { alert ->
                    AlertItem(alert = alert, onClick = { onNavigateToDeviceDetail(alert.device.id) })
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(data: List<WeeklyLogCount>) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { maxOf(it.current, it.previous) }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(70.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { w ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Current month bar
                    val curH = (w.current.toFloat() / maxVal * 60).coerceAtLeast(4f).dp
                    Box(modifier = Modifier.width(10.dp).height(curH)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(GreenPrimary))
                    // Previous month bar
                    val prevH = (w.previous.toFloat() / maxVal * 60).coerceAtLeast(4f).dp
                    Box(modifier = Modifier.width(10.dp).height(prevH)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(GreenLight))
                }
                Spacer(Modifier.height(4.dp))
                Text("T${w.week}", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot(color = GreenPrimary, label = "Tháng này")
        LegendDot(color = GreenLight, label = "Tháng trước")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun AlertItem(alert: DeviceAlert, onClick: () -> Unit) {
    val (dotColor, badgeBg, badgeFg, badgeLabel) = when (alert.alertType) {
        AlertType.URGENT   -> listOf(RedColor, RedLight, RedColor, "Khẩn")
        AlertType.WAITING  -> listOf(AmberColor, AmberLight, AmberColor, "Chờ")
        AlertType.WARRANTY -> listOf(AmberColor, AmberLight, AmberColor, "Bảo hành")
        AlertType.STABLE   -> listOf(GreenPrimary, GreenLight, GreenPrimary, "Ổn định")
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor as Color))
        Column(modifier = Modifier.weight(1f)) {
            Text(alert.device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${alert.device.location}", fontSize = 11.sp, color = TextSecondary)
        }
        Box(
            modifier = Modifier.clip(CircleShape).background(badgeBg as Color)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(badgeLabel as String, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = badgeFg as Color)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun ManagerBottomNav(
    current: Int,
    onDashboard: () -> Unit,
    onDevices: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        val colors = NavigationBarItemDefaults.colors(
            indicatorColor = PurpleLight,
            selectedIconColor = PurplePrimary,
            selectedTextColor = PurplePrimary
        )
        NavigationBarItem(selected = current == 0, onClick = onDashboard,
            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Tổng quan") }, colors = colors)
        NavigationBarItem(selected = current == 1, onClick = onDevices,
            icon = { Icon(Icons.Default.Build, null) }, label = { Text("Thiết bị") }, colors = colors)
        NavigationBarItem(selected = current == 2, onClick = onReports,
            icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Báo cáo") }, colors = colors)
        NavigationBarItem(selected = current == 3, onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Cài đặt") }, colors = colors)
    }
}
