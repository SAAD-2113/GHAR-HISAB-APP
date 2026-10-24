package com.sindantechnologies.gharhisab.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sindantechnologies.gharhisab.R

/**
 * Material Design dialog shown when the user selects Google Drive storage.
 *
 * @param onContinueLocal Callback when "Continue with Local Storage" is pressed.
 * @param onDismiss Callback when "Cancel" is pressed or dialog is dismissed.
 */
@Composable
fun ComingSoonDialog(
    onContinueLocal: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.storage_coming_soon_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.storage_coming_soon_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinueLocal) {
                Text(
                    text = stringResource(R.string.storage_continue_local),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.storage_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
