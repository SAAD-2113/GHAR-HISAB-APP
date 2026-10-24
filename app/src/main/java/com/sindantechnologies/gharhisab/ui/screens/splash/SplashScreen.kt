package com.sindantechnologies.gharhisab.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.datastore.UserPreferencesRepository
import com.sindantechnologies.gharhisab.domain.model.ThemeMode
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.util.Constants
import kotlinx.coroutines.delay

/**
 * Splash screen with smooth fade-in animation and responsive layout.
 *
 * Displays the GharHisab illustration, app title, and Sindan Technologies
 * branding, adapting gracefully to small, normal, and landscape screens.
 */
@Composable
fun SplashScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_fade_in",
    )

    val onboardingComplete by userPreferencesRepository.onboardingCompleteFlow
        .collectAsState(initial = null)

    val themeMode by userPreferencesRepository.themeModeFlow
        .collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)

    val isDark = when (themeMode) {
        ThemeMode.CLASSIC -> false
        ThemeMode.MIDNIGHT -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Navigate once animation has played AND onboarding status is loaded
    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete == null) return@LaunchedEffect
        delay(Constants.SPLASH_DURATION_MS)
        if (onboardingComplete == true) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        val isCompactHeight = maxHeight < 500.dp
        val illustrationMaxSize = if (isCompactHeight) 110.dp else 170.dp
        val companyLogoHeight = if (isCompactHeight) 32.dp else Dimens.SplashCompanyLogoHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Center Content: Illustration + App Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isDark) {
                    Surface(
                        color = Color(0xFFF7F7F7),
                        shape = RoundedCornerShape(Dimens.RadiusLg),
                        shadowElevation = Dimens.ElevationMd,
                        modifier = Modifier.padding(Dimens.SpacingSm),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.splash_logo),
                            contentDescription = stringResource(R.string.splash_logo_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .sizeIn(maxWidth = illustrationMaxSize, maxHeight = illustrationMaxSize)
                                .padding(Dimens.SpacingMd),
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.splash_logo),
                        contentDescription = stringResource(R.string.splash_logo_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.sizeIn(
                            maxWidth = illustrationMaxSize,
                            maxHeight = illustrationMaxSize,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) Dimens.SpacingSm else Dimens.SpacingLg))

                Text(
                    text = stringResource(R.string.splash_app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (isCompactHeight) 24.sp else 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Branding: "A Product of" + Sindan Technologies logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = if (isCompactHeight) Dimens.SpacingSm else Dimens.SpacingLg),
            ) {
                Text(
                    text = stringResource(R.string.splash_product_of),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                if (isDark) {
                    Surface(
                        color = Color(0xFFF7F7F7),
                        shape = RoundedCornerShape(Dimens.RadiusSm),
                        shadowElevation = Dimens.ElevationSm,
                        modifier = Modifier.padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sindan_logo),
                            contentDescription = stringResource(R.string.splash_company_logo_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .heightIn(max = companyLogoHeight)
                                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs),
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.sindan_logo),
                        contentDescription = stringResource(R.string.splash_company_logo_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.heightIn(max = companyLogoHeight),
                    )
                }
            }
        }
    }
}
