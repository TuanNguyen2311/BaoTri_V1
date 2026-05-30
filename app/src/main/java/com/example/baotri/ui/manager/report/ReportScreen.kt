package com.example.baotri.ui.manager.report

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
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

// ── Screen ─────────────────────────────────────────────────────


@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    vm: ReportViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val periods = listOf("Tuần", "Tháng", "Quý", "Năm")

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Báo cáo",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = {}) {
                        Icon(Icons.Default.FileDownload, null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xuất PDF", color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Period tabs
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Row {
                        ReportPeriod.values().forEachIndexed { i, p ->
                            val sel = state.period == p
                            Box(
                                modifier = Modifier.weight(1f).height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.onPeriodChange(p) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(periods[i], fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (sel) androidx.compose.ui.graphics.Color.White else TextSecondary)
                            }
                        }
                    }
                }
            }

            // Summary strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                ) {
                    SummaryCell("${state.summary["total"] ?: 0}", "Tổng log", PurplePrimary, Modifier.weight(1f))
                    SummaryCell("${state.summary["urgent"] ?: 0}", "Khẩn cấp", RedColor, Modifier.weight(1f))
                    SummaryCell("${state.summary["waiting"] ?: 0}", "Chờ xử lý", AmberColor, Modifier.weight(1f))
                    SummaryCell("${state.summary["resolved"] ?: 0}", "Hoàn thành", GreenPrimary, Modifier.weight(1f))
                }
            }

            // Bar chart
            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Log theo tuần", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("So sánh tháng trước", fontSize = 11.sp, color = TextTertiary)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (state.weeklyData.isNotEmpty()) {
                            val maxVal = state.weeklyData.maxOf { maxOf(it.current, it.previous) }.coerceAtLeast(1)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(70.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                state.weeklyData.forEach { w ->
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
                                            val ch = (w.current.toFloat() / maxVal * 60).coerceAtLeast(4f).dp
                                            val ph = (w.previous.toFloat() / maxVal * 60).coerceAtLeast(4f).dp
                                            Box(modifier = Modifier.width(10.dp).height(ch)
                                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                .background(PurplePrimary))
                                            Box(modifier = Modifier.width(10.dp).height(ph)
                                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                .background(PurpleLight))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("T${w.week}", fontSize = 10.sp, color = TextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top devices
            item {
                Spacer(Modifier.height(14.dp))
                SectionTitle("Thiết bị nhiều sự cố nhất")
                Spacer(Modifier.height(8.dp))
            }

            items(state.topDevices.take(5).mapIndexed { i, d -> Pair(i + 1, d) }) { (rank, incident) ->
                val maxCount = state.topDevices.firstOrNull()?.count?.coerceAtLeast(1) ?: 1
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                            .background(if (rank <= 2) PurpleLight else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (rank <= 2) PurplePrimary else BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$rank", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (rank <= 2) PurplePrimary else TextSecondary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(incident.device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(incident.device.location, fontSize = 11.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(80.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp)).background(BorderColor)
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth(incident.count.toFloat() / maxCount)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp)).background(PurplePrimary))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("${incident.count} log", fontSize = 11.sp, color = TextTertiary)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SummaryCell(value: String, label: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier = modifier.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}
