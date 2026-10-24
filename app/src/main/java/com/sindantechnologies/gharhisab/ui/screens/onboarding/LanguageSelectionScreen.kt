package com.sindantechnologies.gharhisab.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.sindantechnologies.gharhisab.domain.model.Language
import com.sindantechnologies.gharhisab.ui.components.GharHisabButton
import com.sindantechnologies.gharhisab.ui.components.SelectionCard
import com.sindantechnologies.gharhisab.ui.theme.Dimens

/**
 * Language selection onboarding screen with responsive layout.
 */
@Composable
fun LanguageSelectionScreen(
    viewModel: LanguageSelectionViewModel,
    onContinue: () -> Unit,
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

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
                        viewModel.saveLanguage()
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
                text = stringResource(R.string.language_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            ) {
                Language.entries.forEach { language ->
                    SelectionCard(
                        label = "${language.displayName} — ${language.nativeName}",
                        isSelected = selectedLanguage == language,
                        onClick = { viewModel.selectLanguage(language) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}
