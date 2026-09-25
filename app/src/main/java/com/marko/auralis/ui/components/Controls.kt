package com.marko.auralis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdjustableControlCard(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onValueClick: (() -> Unit)? = null,
    description: String? = null
) {
    var locked by rememberSaveable(title) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    valueLabel,
                    modifier = Modifier
                        .then(if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier)
                        .padding(6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onMinus,
                    enabled = !locked,
                    modifier = Modifier
                        .widthIn(min = 52.dp)
                        .semantics { contentDescription = "Decrease $title" }
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Slider(
                    value = value.coerceIn(range.start, range.endInclusive),
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    enabled = !locked,
                    valueRange = range,
                    steps = steps,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$title slider" }
                )
                FilledTonalButton(
                    onClick = onPlus,
                    enabled = !locked,
                    modifier = Modifier
                        .widthIn(min = 52.dp)
                        .semantics { contentDescription = "Increase $title" }
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                FilledTonalButton(
                    onClick = { locked = !locked },
                    modifier = Modifier.widthIn(min = 52.dp).semantics { contentDescription = if (locked) "Unlock $title" else "Lock $title" }
                ) { Text(if (locked) "🔒" else "🔓") }
            }
        }
    }
}
