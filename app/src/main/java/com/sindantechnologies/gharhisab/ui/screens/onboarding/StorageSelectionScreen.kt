package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.StorageOption
import com.sindantechnologies.gharhisab.ui.components.ComingSoonDialog
import com.sindantechnologies.gharhisab.ui.components.GharHisabButton
import com.sindantechnologies.gharhisab.ui.theme.Dimens

/**
 * Storage selection onboarding screen with responsive layout.
 */
@Composable
fun StorageSelectionScreen(
    viewModel: StorageSelectionViewModel,
    onContinue: () -> Unit,
) {
    val selectedStorage by viewModel.selectedStorage.collectAsStateWithLifecycle()
    val showDialog by viewModel.showComingSoonDialog.collectAsStateWithLifecycle()

    if (showDialog) {
        ComingSoonDialog(
            onContinueLocal = {
                viewModel.continueWithLocal()
                onContinue()
            },
            onDismiss = { viewModel.dismissDialog() },
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = Dimens.ElevationMd,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
                ) {
                    GharHisabButton(
                        text = stringResource(R.string.continue_button),
                        onClick = {
                            viewModel.onContinue(onContinue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            Text(
                text = stringResource(R.string.storage_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            // Local Storage Card
            StorageOptionCard(
                icon = Icons.Filled.PhoneAndroid,
                title = stringResource(R.string.storage_local_title),
                features = listOf(
                    stringResource(R.string.storage_local_works_offline),
                    stringResource(R.string.storage_local_faster),
                    stringResource(R.string.storage_local_secure),
                    stringResource(R.string.storage_local_recommended),
                ),
                isSelected = selectedStorage == StorageOption.LOCAL,
                onClick = { viewModel.selectStorage(StorageOption.LOCAL) },
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Google Drive Card
            StorageOptionCard(
                icon = Icons.Filled.Cloud,
                title = stringResource(R.string.storage_google_drive_title),
                features = emptyList(),
                isSelected = selectedStorage == StorageOption.GOOGLE_DRIVE,
                onClick = { viewModel.selectStorage(StorageOption.GOOGLE_DRIVE) },
                badgeText = stringResource(R.string.storage_coming_soon_badge),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}

/**
 * A storage option card with icon, title, feature list, and selection indicator.
 */
@Composable
private fun StorageOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    features: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 200),
        label = "storage_border",
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "storage_container",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMd),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpacingLg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconLg),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (badgeText != null) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = Dimens.SpacingXs),
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = Dimens.SpacingLg, bottom = Dimens.SpacingXs),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ),
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
