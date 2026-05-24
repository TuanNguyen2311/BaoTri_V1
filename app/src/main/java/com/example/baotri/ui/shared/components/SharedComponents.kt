package com.example.baotri.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baotri.domain.model.MaintenanceStatus
import com.example.baotri.ui.shared.theme.*

// ── Status Badge ─────────────────────────────────────────────
@Composable
fun StatusBadge(status: MaintenanceStatus) {
    val (bg, fg, label) = when (status) {
        MaintenanceStatus.RESOLVED      -> Triple(GreenLight, GreenPrimary, "Đã xử lý")
        MaintenanceStatus.WAITING_PARTS -> Triple(AmberLight, AmberColor, "Chờ phụ tùng")
        MaintenanceStatus.UNRESOLVED    -> Triple(RedLight, RedColor, "Chưa xử lý")
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Priority Dot ─────────────────────────────────────────────
@Composable
fun PriorityDot(status: MaintenanceStatus?, modifier: Modifier = Modifier) {
    val color = when (status) {
        MaintenanceStatus.UNRESOLVED    -> RedColor
        MaintenanceStatus.WAITING_PARTS -> AmberColor
        MaintenanceStatus.RESOLVED      -> GreenPrimary
        null                             -> BorderColor
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Loading Button ────────────────────────────────────────────
@Composable
fun LoadingButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = GreenPrimary
) {
    Button(
        onClick = {if (!loading) onClick()},
        enabled = enabled && !loading,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ── Section Title ─────────────────────────────────────────────
@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 0.6.sp
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(action, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Info Box ──────────────────────────────────────────────────
@Composable
fun InfoBox(
    message: String,
    icon: ImageVector? = null,
    type: InfoBoxType = InfoBoxType.INFO,
    modifier: Modifier = Modifier
) {
    val (bg, border, textColor) = when (type) {
        InfoBoxType.INFO    -> Triple(PurpleLight, PurplePrimary.copy(alpha = .4f), PurplePrimary)
        InfoBoxType.WARNING -> Triple(AmberLight, AmberColor.copy(alpha = .4f), AmberColor)
        InfoBoxType.ERROR   -> Triple(RedLight, RedColor.copy(alpha = .4f), RedColor)
        InfoBoxType.SUCCESS -> Triple(GreenLight, GreenPrimary.copy(alpha = .4f), GreenPrimary)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        }
        Text(message, color = textColor, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

enum class InfoBoxType { INFO, WARNING, ERROR, SUCCESS }

// ── Stat Card ─────────────────────────────────────────────────
@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ── Empty State ───────────────────────────────────────────────
@Composable
fun EmptyState(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null,
            modifier = Modifier.size(56.dp), tint = BorderColor)
        Spacer(Modifier.height(12.dp))
        Text(message, color = TextTertiary, fontSize = 14.sp)
    }
}

// ── Top App Bar ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaoTriTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
