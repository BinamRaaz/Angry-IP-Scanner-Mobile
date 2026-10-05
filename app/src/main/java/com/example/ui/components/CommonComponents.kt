package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.models.HostStatus
import com.example.ui.theme.StatusAliveDark
import com.example.ui.theme.StatusAliveLight
import com.example.ui.theme.StatusDeadDark
import com.example.ui.theme.StatusDeadLight
import com.example.ui.theme.StatusUnknownDark
import com.example.ui.theme.StatusUnknownLight

@Composable
fun HostStatusBadge(
    status: HostStatus,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.hashCode() < 0 // Simplified check or theme color
    val (bgColor, textColor, label) = when (status) {
        HostStatus.ALIVE -> Triple(
            if (isDark) StatusAliveDark.copy(alpha = 0.2f) else StatusAliveLight.copy(alpha = 0.15f),
            if (isDark) StatusAliveDark else StatusAliveLight,
            "Alive"
        )
        HostStatus.DEAD -> Triple(
            if (isDark) StatusDeadDark.copy(alpha = 0.2f) else StatusDeadLight.copy(alpha = 0.15f),
            if (isDark) StatusDeadDark else StatusDeadLight,
            "Dead"
        )
        HostStatus.UNKNOWN -> Triple(
            if (isDark) StatusUnknownDark.copy(alpha = 0.2f) else StatusUnknownLight.copy(alpha = 0.15f),
            if (isDark) StatusUnknownDark else StatusUnknownLight,
            "Unknown"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("host_status_badge_${status.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = " $label",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}
