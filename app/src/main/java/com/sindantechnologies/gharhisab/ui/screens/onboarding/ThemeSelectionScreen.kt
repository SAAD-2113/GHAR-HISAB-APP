package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.components.GharHisabButton
import com.sindantechnologies.gharhisab.ui.components.SelectionCard
import com.sindantechnologies.gharhisab.ui.theme.Dimens

/**
 * Theme selection onboarding screen with responsive layout.
 */
@Composable
fun ThemeSelectionScreen(
    viewModel: ThemeSelectionViewModel,
    onContinue: () -> Unit,
) {
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()

    val themeLabels = mapOf(
        ThemeMode.CLASSIC to stringResource(R.string.theme_classic),
        ThemeMode.MIDNIGHT to stringResource(R.string.theme_midnight),
        ThemeMode.FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
    )

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
            ) {
                GharHisabButton(
                    text = stringResource(R.string.continue_button),
                    onClick = {
                        viewModel.saveTheme()
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                text = stringResource(R.string.theme_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            ) {
                ThemeMode.entries.forEach { theme ->
                    SelectionCard(
                        label = themeLabels[theme] ?: theme.displayName,
                        isSelected = selectedTheme == theme,
                        onClick = { viewModel.selectTheme(theme) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}
