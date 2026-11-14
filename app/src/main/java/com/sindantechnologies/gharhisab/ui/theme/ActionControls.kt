package com.sindantechnologies.gharhisab.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

val ColorScheme.isMidnight: Boolean
    get() = background == MidnightBackground

/** Filled controls use the brand red; actions drawn directly on dark surfaces stay readable. */
val ColorScheme.actionText: Color
    get() = if (isMidnight) onSurface else primary

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.actionText),
        content = content,
    )
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        colors = if (MaterialTheme.colorScheme.isMidnight) FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ) else FilterChipDefaults.filterChipColors(),
    )
}

/** Preserve Classic outlined actions while giving Midnight actions the requested solid brand fill. */
@Composable
fun AppQuickActionButton(
    onClick: () -> Unit,
    shape: Shape,
    content: @Composable RowScope.() -> Unit,
) {
    if (MaterialTheme.colorScheme.isMidnight) {
        Button(onClick = onClick, shape = shape, content = content)
    } else {
        OutlinedButton(onClick = onClick, shape = shape, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appSegmentedButtonColors() = if (MaterialTheme.colorScheme.isMidnight) {
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
    )
} else {
    SegmentedButtonDefaults.colors()
}

/** Focus indicators are foregrounds, so Midnight uses its readable neutral color. */
@Composable
fun appOutlinedTextFieldColors() = if (MaterialTheme.colorScheme.isMidnight) {
    OutlinedTextFieldDefaults.colors(
        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.onSurface,
    )
} else {
    OutlinedTextFieldDefaults.colors()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appDatePickerColors() = if (MaterialTheme.colorScheme.isMidnight) {
    DatePickerDefaults.colors(
        todayContentColor = MaterialTheme.colorScheme.onSurface,
        todayDateBorderColor = MaterialTheme.colorScheme.onSurface,
        currentYearContentColor = MaterialTheme.colorScheme.onSurface,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
        dateTextFieldColors = appOutlinedTextFieldColors(),
    )
} else {
    DatePickerDefaults.colors()
}
