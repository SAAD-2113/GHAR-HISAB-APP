package com.sindantechnologies.gharhisab.ui.screens.transaction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.sindantechnologies.gharhisab.ui.theme.appOutlinedTextFieldColors
import com.sindantechnologies.gharhisab.ui.theme.AppFilterChip as FilterChip
import com.sindantechnologies.gharhisab.ui.theme.AppTextButton as TextButton
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.data.local.room.entity.TransactionEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.domain.model.TransactionType
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.ui.theme.ExpenseRed
import com.sindantechnologies.gharhisab.ui.theme.IncomeGreen
import com.sindantechnologies.gharhisab.util.Constants
import com.sindantechnologies.gharhisab.util.CurrencyFormatter
import com.sindantechnologies.gharhisab.util.IconMapper
import com.sindantechnologies.gharhisab.util.VoiceEntryDraft
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.ui.theme.isMidnight
import com.sindantechnologies.gharhisab.ui.theme.appSegmentedButtonColors
import com.sindantechnologies.gharhisab.ui.theme.actionText
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import com.sindantechnologies.gharhisab.util.ParsedVoiceTransaction
import com.sindantechnologies.gharhisab.util.VoiceTransactionParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceEntryDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSaveTransaction: suspend (TransactionEntity) -> Unit,
    onFallbackManualEntry: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVoiceLanguage by remember { mutableStateOf(context.resources.configuration.locales[0].language.takeIf { it in listOf("en", "ur", "pa") } ?: "en") } // "en", "ur", "pa"
    var isListening by remember { mutableStateOf(false) }
    var isRecognizing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var parsedTransaction by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Confirmation form state
    var draft by remember { mutableStateOf(VoiceEntryDraft()) }
    val editType = draft.type
    val editAmountText = draft.amountText
    val editCategory = draft.category
    val editDescription = draft.description
    var isSaving by remember { mutableStateOf(false) }

    // Check permission
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordAudioPermission = granted
        if (!granted) {
            errorMessage = "Microphone permission is required for voice entry."
        }
    }

    // SpeechRecognizer reference
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun startListening() {
        if (!hasRecordAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (speechRecognizer == null) {
            errorMessage = "Speech recognition is not available on this device. You can use manual entry."
            return
        }

        errorMessage = null
        recognizedText = ""
        isListening = true
        isRecognizing = true

        val bcp47 = when (selectedVoiceLanguage) {
            "ur" -> "ur-PK"
            "pa" -> "pa-PK"
            else -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, bcp47)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, bcp47)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transaction...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                isRecognizing = false
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> context.getString(R.string.voice_language_unsupported)
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> context.getString(R.string.voice_language_unavailable)
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please speak clearly and try again."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for selected speech model. Check your connection."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission was denied."
                    else -> "Could not capture voice input. Please try again or enter manually."
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                isRecognizing = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    recognizedText = text
                    val parsed = VoiceTransactionParser.parse(text, categories, selectedVoiceLanguage)
                    parsedTransaction = parsed
                    draft = draft.merge(parsed)
                } else {
                    errorMessage = "Could not recognize speech. Please try again."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { recognizedText = it }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try { speechRecognizer.startListening(intent) }
        catch (_: Exception) {
            isListening = false
            isRecognizing = false
            errorMessage = context.getString(R.string.operation_failed)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(Dimens.RadiusLg),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Dimens.ElevationLg,
        ) {
            Column(
                modifier = Modifier.padding(Dimens.SpacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                // Language Selection Chips
                Text(
                    text = "Speaking Language:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                    modifier = Modifier.padding(bottom = Dimens.SpacingSm)
                ) {
                    FilterChip(
                        selected = selectedVoiceLanguage == "en",
                        onClick = { selectedVoiceLanguage = "en" }, enabled = !isRecognizing && !isSaving,
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = selectedVoiceLanguage == "ur",
                        onClick = { selectedVoiceLanguage = "ur" }, enabled = !isRecognizing && !isSaving,
                        label = { Text("اردو (Urdu)") }
                    )
                    FilterChip(
                        selected = selectedVoiceLanguage == "pa",
                        onClick = { selectedVoiceLanguage = "pa" }, enabled = !isRecognizing && !isSaving,
                        label = { Text("ਪੰਜਾਬੀ (Punjabi)") }
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                Text(stringResource(R.string.voice_incremental_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                // Microphone Button with Pulse
                Box(contentAlignment = Alignment.Center) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                        )
                    }

                    Surface(
                        enabled = !isSaving && (!isRecognizing || isListening),
                        onClick = {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                startListening()
                            }
                        },
                        shape = CircleShape,
                        color = if (isListening || MaterialTheme.colorScheme.isMidnight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp),
                        shadowElevation = Dimens.ElevationMd
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.MicOff,
                                contentDescription = if (isListening) "Listening..." else "Tap to speak",
                                tint = if (isListening || MaterialTheme.colorScheme.isMidnight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(Dimens.IconLg)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                Text(
                    text = when {
                        isListening -> "Listening... speak now"
                        isRecognizing -> stringResource(R.string.voice_processing)
                        else -> "Tap microphone to speak"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) MaterialTheme.colorScheme.actionText else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.SpacingMd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Recognized transcript card
                if (recognizedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(Dimens.RadiusMd),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Dimens.SpacingMd)) {
                            Text(
                                text = "Transcribed:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingXxs))
                            Text(
                                text = "\"$recognizedText\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Parsed / Editable Confirmation Section
                if (parsedTransaction != null) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                    Text(
                        text = "Review & Confirm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                    // Type Selector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            colors = appSegmentedButtonColors(),
                            selected = editType == TransactionType.EXPENSE,
                            onClick = { draft = draft.copy(type = TransactionType.EXPENSE, manualType = true, category = draft.category?.takeIf { it.transactionType == TransactionType.EXPENSE }) },
                            enabled = !isSaving,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Expense")
                        }
                        SegmentedButton(
                            colors = appSegmentedButtonColors(),
                            selected = editType == TransactionType.INCOME,
                            onClick = { draft = draft.copy(type = TransactionType.INCOME, manualType = true, category = draft.category?.takeIf { it.transactionType == TransactionType.INCOME }) },
                            enabled = !isSaving,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Income")
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    // Amount input
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { draft = draft.copy(amountText = it, manualAmount = it.isNotBlank()) },
                        enabled = !isSaving,
                        label = { Text("Amount") },
                        prefix = { Text("${Constants.CURRENCY_SYMBOL} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = appOutlinedTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    // Description input
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { draft = draft.copy(description = it, manualDescription = it.isNotBlank()) },
                        enabled = !isSaving,
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = appOutlinedTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                    // Category selection
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                    ) {
                        val filteredCategories = categories.filter { it.transactionType == editType }
                        filteredCategories.forEach { cat ->
                            FilterChip(
                                selected = editCategory?.id == cat.id,
                                onClick = { draft = draft.copy(category = cat, manualCategory = true) },
                                enabled = !isSaving,
                                label = { Text(cat.name, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                    // Absolute dates require manual entry; never silently save them as today.
                    if (draft.requiresManualDate) {
                        Text(stringResource(R.string.voice_absolute_date_manual),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                    }
                    // Confirm Save Button
                    Button(
                        onClick = {
                            val amount = CurrencyFormatter.parseRupeesToPaisa(editAmountText)
                            val cat = editCategory
                            if (amount != null && amount > 0L && cat != null) {
                                isSaving = true
                                scope.launch {
                                    val transaction = TransactionEntity(
                                        type = editType,
                                        amount = amount,
                                        categoryId = cat.id,
                                        description = editDescription.ifBlank { cat.name },
                                        timestamp = draft.timestamp ?: System.currentTimeMillis(),
                                        paymentMethod = PaymentMethod.CASH,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    try {
                                        onSaveTransaction(transaction)
                                        onDismiss()
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (_: Exception) { errorMessage = context.getString(R.string.operation_failed) }
                                    finally { isSaving = false }
                                }
                            } else {
                                errorMessage = "Please provide a valid amount and category."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && !isRecognizing && !draft.requiresManualDate && (CurrencyFormatter.parseRupeesToPaisa(editAmountText) ?: 0) > 0 && editCategory?.transactionType == editType
                    ) {
                        Text(if (isSaving) "Saving..." else "Confirm & Save")
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Fallback to manual entry
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onDismiss()
                        onFallbackManualEntry()
                    }
                ) {
                    Text("Or enter transaction manually")
                }
            }
        }
    }
}
