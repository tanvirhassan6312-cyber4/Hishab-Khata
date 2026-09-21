package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.local.ProductEntity
import com.example.ui.theme.*
import com.example.util.BengaliFormatters
import com.example.util.ParsedVoiceEntry
import com.example.util.VoiceToLedgerParser
import com.example.util.VoiceTransactionType
import java.util.Locale

@Composable
fun VoiceLedgerDialog(
    allProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirmEntry: (ParsedVoiceEntry) -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var parsedResult by remember { mutableStateOf<ParsedVoiceEntry?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Quick Sample Prompts that shopkeepers use
    val quickSamples = listOf(
        "রহিম ভাইকে ৫০০ টাকার চাল আর ২ কেজির তেল বাকিতে দাও, দুইদিন পর দিবে বলছে",
        "করিম সাহেব ১০০০ টাকা জমা দিলেন",
        "আরিফ কে ২০০ টাকার চিনি নগদ বিক্রি করলাম",
        "সুমন ৩০০ টাকার ডাল বাকি নিলো"
    )

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(
                context = context,
                onListeningStateChange = { isListening = it },
                onTextRecognized = { text ->
                    spokenText = text
                    parsedResult = VoiceToLedgerParser.parseBengaliVoiceText(text, allProducts)
                },
                setRecognizer = { speechRecognizer = it }
            )
        } else {
            Toast.makeText(context, "ভয়েস রেকর্ডের জন্য অডিও পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Dialog(onDismissRequest = {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_voice_ledger")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlue50,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = RoyalBlue700)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI মুখে বলা হিসাব",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DeepIndigo)
                            )
                            Text(
                                text = "বাংলায় কথা বলুন, AI হিসাব লিখে নেবে",
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CharcoalMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big Mic Button with Pulsing visual
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Brush.radialGradient(listOf(Red600, Red800))
                            else Brush.radialGradient(listOf(RoyalBlue600, DeepIndigo))
                        )
                        .clickable {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    startListening(
                                        context = context,
                                        onListeningStateChange = { isListening = it },
                                        onTextRecognized = { text ->
                                            spokenText = text
                                            parsedResult = VoiceToLedgerParser.parseBengaliVoiceText(text, allProducts)
                                        },
                                        setRecognizer = { speechRecognizer = it }
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("btn_mic_listen")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isListening) "🎙️ শুনছি... বলুন..." else "মাইক বাটনে চাপ দিয়ে কথা বলুন",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) Red600 else RoyalBlue700
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Editable / Spoken text display
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = {
                        spokenText = it
                        parsedResult = if (it.isNotBlank()) VoiceToLedgerParser.parseBengaliVoiceText(it, allProducts) else null
                    },
                    label = { Text("যা বলেছেন বা লিখুন") },
                    placeholder = { Text("যেমন: রহিম ভাই ৫০০ টাকার চাল বাকি নিলো...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_voice_text"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Sample Prompt Chips
                Text(
                    text = "দ্রুত টেস্ট করতে ট্যাপ করুন:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CharcoalMuted),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                quickSamples.forEach { sample ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoyalBlue50,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                spokenText = sample
                                parsedResult = VoiceToLedgerParser.parseBengaliVoiceText(sample, allProducts)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RoyalBlue700, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.bodySmall.copy(color = CharcoalDark, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // AI Parsed Structured Preview Card
                AnimatedVisibility(visible = parsedResult != null) {
                    parsedResult?.let { res ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (res.transactionType) {
                                    VoiceTransactionType.DUE_SALE -> Red50
                                    VoiceTransactionType.CASH_SALE -> Emerald50
                                    VoiceTransactionType.PAYMENT_IN -> RoyalBlue50
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                when (res.transactionType) {
                                    VoiceTransactionType.DUE_SALE -> Red200
                                    VoiceTransactionType.CASH_SALE -> Emerald200
                                    VoiceTransactionType.PAYMENT_IN -> RoyalBlue200
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✨ AI বিশ্লেষণ ফলাফল",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DeepIndigo)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (res.transactionType) {
                                            VoiceTransactionType.DUE_SALE -> Red700
                                            VoiceTransactionType.CASH_SALE -> Emerald700
                                            VoiceTransactionType.PAYMENT_IN -> RoyalBlue700
                                        }
                                    ) {
                                        Text(
                                            text = when (res.transactionType) {
                                                VoiceTransactionType.DUE_SALE -> "বাকি বিক্রি (Due)"
                                                VoiceTransactionType.CASH_SALE -> "নগদ বিক্রি (Cash)"
                                                VoiceTransactionType.PAYMENT_IN -> "বকেয়া জমা (Payment In)"
                                            },
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("👤 কাস্টমার:", style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted))
                                    Text(res.customerName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("📦 পণ্যের বিবরণ:", style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted))
                                    Text(
                                        res.items.joinToString(", ") { "${it.itemName} (${BengaliFormatters.toBanglaCurrency(it.total)}৳)" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CharcoalDark)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("💰 মোট পরিমাণ:", style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted))
                                    Text("৳${BengaliFormatters.toBanglaCurrency(res.totalAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Emerald700))
                                }

                                if (res.transactionType == VoiceTransactionType.DUE_SALE && res.promisedDueDateMillis != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("📅 প্রতিশ্রুত পরিশোধ:", style = MaterialTheme.typography.bodySmall.copy(color = CharcoalMuted))
                                        Text(BengaliFormatters.formatDateBangla(res.promisedDueDateMillis), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Red700))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onConfirmEntry(res) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("btn_confirm_voice_entry"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (res.transactionType) {
                                            VoiceTransactionType.DUE_SALE -> Red700
                                            VoiceTransactionType.CASH_SALE -> Emerald700
                                            VoiceTransactionType.PAYMENT_IN -> RoyalBlue700
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("হিসাব নিশ্চিত ও সংরক্ষণ করুন ✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startListening(
    context: android.content.Context,
    onListeningStateChange: (Boolean) -> Unit,
    onTextRecognized: (String) -> Unit,
    setRecognizer: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "ডিভাইসে স্পিচ রিকগনিশন সার্ভিস নেই, টেক্সট ইনপুট দিন", Toast.LENGTH_LONG).show()
        return
    }

    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        setRecognizer(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "দোকানের হিসাব বলুন...")
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStateChange(true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                onListeningStateChange(false)
            }

            override fun onError(error: Int) {
                onListeningStateChange(false)
            }

            override fun onResults(results: Bundle?) {
                onListeningStateChange(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextRecognized(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextRecognized(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "ভয়েস চালু করা যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        onListeningStateChange(false)
    }
}
