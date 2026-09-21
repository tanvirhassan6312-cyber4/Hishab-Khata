package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFaqScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("সব") }
    var userQueryInput by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }
    var expandedFaqId by remember { mutableStateOf<String?>(null) }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "ai",
                text = "আসসালামু আলাইকুম! আমি আপনার অ্যাপ এআই সহকারী। এই অ্যাপের যেকোনো ফিচার (যেমন: বিক্রি, বাকি খাতা, কিউআর কোড, অ্যাটোমিক প্রক্সিমিটি পে, ভয়েস লেজার) সম্পর্কে না বুঝলে নিচে প্রশ্ন করুন। আমি বাংলায় বুঝিয়ে দেব।"
            )
        )
    }

    val staticFaqs = remember {
        listOf(
            FaqItem(
                id = "faq_proximity",
                category = "অ্যাটোমিক প্রক্সিমিটি পে",
                question = "অ্যাটোমিক প্রক্সিমিটি পে কীভাবে কাজ করে?",
                answer = "এই সিস্টেমে বাকির টাকা পরিশোধের জন্য কাস্টমারকে কোনো অ্যাপ ইনস্টল করতে হবে না বা কোনো কিউআর কোড স্ক্যান করতে হবে না। কাস্টমার দোকানে আসলেই ব্যাকগ্রাউন্ড ব্লুটুথ ফ্রিকোয়েন্সিতে শনাক্ত হবে। আপনি 'Clear Due' চাপলে অ্যাপ মানুষের কানে শোনা যায় না এমন আল্ট্রাসাউন্ড অডিও হ্যান্ডশেক দিয়ে ২ সেকেন্ডে বাকি পরিশোধ নিশ্চিত করবে ও স্বয়ংক্রিয় বাংলায় উচ্চস্বরে ঘোষণা দেবে।"
            ),
            FaqItem(
                id = "faq_voice",
                category = "ভয়েস লেজার",
                question = "বাংলায় কথা বলে কীভাবে বিক্রি বা বাকি রেকর্ড করব?",
                answer = "হোম স্ক্রিনে 'ভয়েস হিসাব খাতা' বাটনে চাপ দিয়ে বলুন 'করিম ভাইকে ২০ টাকার ডিম বাকি' বা 'রহিম ভাই ৫০ টাকার চা ক্যাশ'। এআই স্বয়ংক্রিয়ভাবে নাম, টাকা এবং পণ্যের বিবরণ বুঝে নিয়ে সাথে সাথে লেজারে সেভ করে নেবে।"
            ),
            FaqItem(
                id = "faq_pos",
                category = "বিক্রি ও মেমো",
                question = "নতুন বিক্রি বা মেমো কীভাবে তৈরি করব?",
                answer = "নিচের নেভিগেশন বার থেকে 'মেমো' (POS) অপশনে যান। প্রোডাক্টে ট্যাপ করে কার্টে যোগ করুন বা উপরে কিউআর স্ক্যানার দিয়ে স্ক্যান করুন। এরপর 'পরিশোধ ও রসিদ' চাপলে ক্যাশ বা বাকি সিলেক্ট করে প্রিন্ট করতে পারবেন।"
            ),
            FaqItem(
                id = "faq_due",
                category = "বাকি ও তাগাদা",
                question = "কাস্টমারকে বাকির ডিজিটাল তাগাদা কীভাবে পাঠাব?",
                answer = "হোম বা 'বাকি রিমাইন্ডার' ট্যাবে যান। কাস্টমারের নামের পাশে হোয়াটসঅ্যাপ বা মেসেজ আইকনে ট্যাপ করলেই স্বয়ংক্রিয় বাংলা তাগাদা মেসেজ ও পরিশোধের লিংক চলে যাবে।"
            ),
            FaqItem(
                id = "faq_qr",
                category = "কিউআর হাব",
                question = "বিকাশ, নগদ বা রকেটের কিউআর কোড কীভাবে তৈরি করব?",
                answer = "'কিউআর হাব' মেন্যুতে গিয়ে আপনার ব্যক্তিগত বা মার্চেন্ট নম্বর ও টাকার পরিমাণ দিলে স্বয়ংক্রিয় কিউআর তৈরি হবে। এটি কাস্টমারকে দেখালে তারা সহজে টাকা পাঠাতে পারবে।"
            ),
            FaqItem(
                id = "faq_permission",
                category = "পারমিশন ও নিরাপত্তা",
                question = "ক্যামেরা ও মাইক্রোফোন পারমিশন কেন প্রয়োজন?",
                answer = "দ্রুত বারকোড/কিউআর স্ক্যান করার জন্য ক্যামেরা পারমিশন এবং মুখে বলে হিসাব রাখা (ভয়েস লেজার) ও টাচলেস অফলাইন অডিও হ্যান্ডশেকের জন্য মাইক্রোফোন পারমিশন একবারের জন্য নেওয়া হয়।"
            ),
            FaqItem(
                id = "faq_dev",
                category = "ডেভেলপার তথ্য",
                question = "এই অ্যাপটি কারা তৈরি করেছেন?",
                answer = "এই স্মার্ট রিটেল শপ ম্যানেজার অ্যাপটির ডেভলপমেন্ট করেছেন তাফসির এবং তানভির।"
            )
        )
    }

    val categories = listOf("সব", "অ্যাটোমিক প্রক্সিমিটি পে", "ভয়েস লেজার", "বিক্রি ও মেমো", "বাকি ও তাগাদা", "কিউআর হাব", "পারমিশন ও নিরাপত্তা")

    val filteredFaqs = if (selectedCategory == "সব") staticFaqs else staticFaqs.filter { it.category == selectedCategory }
    val chatListState = rememberLazyListState()

    fun handleAskAi(query: String) {
        if (query.isBlank() || isAiGenerating) return
        val userText = query.trim()
        userQueryInput = ""
        chatMessages.add(ChatMessage(sender = "user", text = userText))
        isAiGenerating = true

        coroutineScope.launch {
            chatListState.animateScrollToItem((chatMessages.size - 1).coerceAtLeast(0))
            delay(600)

            // AI Knowledge Base Engine constrained strictly to this app
            val aiResponse = generateAppFaqResponse(userText)
            chatMessages.add(ChatMessage(sender = "ai", text = aiResponse))
            isAiGenerating = false
            chatListState.animateScrollToItem((chatMessages.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "অ্যাপ FAQ ও AI সহকারী",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkCharcoal
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Emerald500
                            ) {
                                Text(
                                    text = "বাংলা এআই",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "শুধু এই অ্যাপ সম্পর্কিত প্রশ্নের তাৎক্ষণিক সমাধান",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = CharcoalLight
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে যান", tint = DarkCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = CharcoalDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Emerald700 else CardBorder
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Main Content: Split into FAQs and Interactive AI Chat
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Developer Attribution Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RoyalBlue50),
                        border = BorderStroke(1.dp, RoyalBlue200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DeepIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "অ্যাপ ডেভেলপার: তাফসির এবং তানভির",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DeepIndigo,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "এই অ্যাপের যেকোনো ফিচার বুঝতে নিচে এআইকে সরাসরি প্রশ্ন করুন।",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CharcoalDark.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // FAQ Accordions Header
                item {
                    Text(
                        text = "📌 সচরাচর জিজ্ঞাসিত প্রশ্নাবলী (FAQ)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // FAQ Accordion Items
                items(filteredFaqs, key = { it.id }) { faq ->
                    val isExpanded = expandedFaqId == faq.id
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isExpanded) Emerald500 else CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedFaqId = if (isExpanded) null else faq.id
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = faq.question,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CharcoalDark
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = CharcoalMuted
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp, start = 26.dp)) {
                                    Divider(color = Slate100, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = faq.answer,
                                        fontSize = 12.sp,
                                        color = CharcoalDark,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Live Q&A Section Header
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🤖 এআই প্রশ্নোত্তর চ্যাট (Live App Assistant)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalDark,
                            fontSize = 14.sp
                        )
                    )
                }

                // Chat Messages
                items(chatMessages, key = { it.id }) { msg ->
                    val isAi = msg.sender == "ai"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        if (isAi) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Emerald700),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isAi) 2.dp else 14.dp,
                                bottomEnd = if (isAi) 14.dp else 2.dp
                            ),
                            color = if (isAi) Slate100 else Emerald700,
                            border = BorderStroke(1.dp, if (isAi) CardBorder else Emerald700),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isAi) CharcoalDark else Color.White,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                if (isAiGenerating) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Emerald700,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এআই উত্তর প্রস্তুত করছে...",
                                fontSize = 12.sp,
                                color = CharcoalMuted
                            )
                        }
                    }
                }

                // Quick Prompt Suggestions
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 দ্রুত প্রশ্ন করতে ট্যাপ করুন:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickQuestions = listOf(
                            "অ্যাটোমিক প্রক্সিমিটি পে কীভাবে ব্যবহার করব?",
                            "ভয়েস লেজারে বাকি কীভাবে লিখব?",
                            "কাস্টমারের বাকির তাগাদা মেসেজ কীভাবে পাঠায়?",
                            "প্রিন্ট ছাড়াই কি রসিদ দেওয়া যায়?",
                            "এই অ্যাপের ডেভেলপার কারা?"
                        )
                        items(quickQuestions) { q ->
                            SuggestionChip(
                                onClick = { handleAskAi(q) },
                                label = { Text(text = q, fontSize = 11.sp, color = DarkCharcoal) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CardBorder),
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Input Bar at bottom
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userQueryInput,
                        onValueChange = { userQueryInput = it },
                        placeholder = { Text("অ্যাপ সম্পর্কে প্রশ্ন লিখুন...", fontSize = 12.5.sp, color = CharcoalMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("faq_query_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { handleAskAi(userQueryInput) },
                        enabled = userQueryInput.isNotBlank() && !isAiGenerating,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (userQueryInput.isNotBlank() && !isAiGenerating) Emerald700 else Slate200)
                            .testTag("faq_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "পাঠান",
                            tint = if (userQueryInput.isNotBlank() && !isAiGenerating) Color.White else CharcoalMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Intelligent Bangla FAQ Engine strictly restricted to this application's features
 */
private fun generateAppFaqResponse(userQuery: String): String {
    val q = userQuery.lowercase().trim()

    // Enforce strict app domain constraint
    val isOffTopic = listOf(
        "weather", "আবহাওয়া", "cricket", "ফুটবল", "রাজনীতি", "প্রেম", "গান",
        "movie", "সিনেমা", "joke", "রান্না", "রেসিপি", "news", "ভারত", "পাকিস্তান"
    ).any { q.contains(it) }

    if (isOffTopic) {
        return "⚠️ আমি শুধুমাত্র এই 'আমার দোকান' রিটেল শপ ম্যানেজার অ্যাপ সম্পর্কিত প্রশ্নের উত্তর দিতে পারি। দয়া করে অ্যাপের বিক্রি, বাকি, কিউআর হাব, ভয়েস লেজার বা প্রক্সিমিটি পে সম্পর্কিত প্রশ্ন করুন।"
    }

    return when {
        q.contains("প্রক্সিমিটি") || q.contains("proximity") || q.contains("অ্যাটোমিক") || q.contains("atomic") || q.contains("টাচলেস") || q.contains("অফলাইন সাউন্ড") -> {
            "⚡ 'অ্যাটোমিক প্রক্সিমিটি পে':\n" +
            "১. কাস্টমার দোকানে আসলে ব্লুটুথ ফ্রিকোয়েন্সিতে তার ফোন শনাক্ত হয়ে হালকা ভাইব্রেশন দিয়ে পপ-আপ উঠবে।\n" +
            "২. 'Clear Due' চাপলে মানুষের কানে শোনা যায় না এমন আল্ট্রাসাউন্ড অডিও দিয়ে ২ সেকেন্ডে গোপন এনক্রিপ্টেড হ্যান্ডশেক হবে।\n" +
            "৩. কোনো ইন্টারনেট বা কিউআর কোড ছাড়াই ডিজিটাল রসিদ পুশ হবে এবং অ্যাপ বাংলায় উচ্চস্বরে ঘোষণা দেবে: 'রহিম ভাইয়ের ৪০০ টাকা বাকি সম্পূর্ণ পরিশোধিত হয়েছে'।"
        }
        q.contains("ভয়েস") || q.contains("কথা বলে") || q.contains("মাইক") || q.contains("voice") || q.contains("রেকর্ড") -> {
            "🎙️ 'ভয়েস হিসাব খাতা':\n" +
            "হোম স্ক্রিনে মাইক আইকন ট্যাপ করে বাংলায় বলুন (যেমন: 'করিম ভাইকে ৫০ টাকার চিনি বাকি' বা '১০০ টাকা ক্যাশ বিক্রি')। অ্যাপের এআই স্বয়ংক্রিয়ভাবে নাম, টাকা ও পণ্যের বিবরণ আলাদা করে হিসাব খাতায় সেভ করে নেবে।"
        }
        q.contains("ডেভেলপার") || q.contains("তৈরি") || q.contains("বানিয়েছে") || q.contains("developer") || q.contains("author") || q.contains("তাফসির") || q.contains("তানভির") -> {
            "👨‍💻 এই অ্যাপটির সম্মানিত ডেভেলপার হলেন: তাফসির এবং তানভির। অ্যাপ সংক্রান্ত যেকোনো কারিগরি সহায়তায় এই FAQ সেকশন ব্যবহার করতে পারেন।"
        }
        q.contains("বাকি") || q.contains("তাগাদা") || q.contains("due") || q.contains("খাতা") -> {
            "📒 'বাকি ও তাগাদা ব্যবস্থাপনা':\n" +
            "বাকি স্ক্রিনে প্রতিটি কাস্টমারের মোট বাকি, তারিখ ও রিমাইন্ডার দেখতে পাবেন। হোয়াটসঅ্যাপ বা এসএমএস বাটনে ট্যাপ করলে তাৎক্ষণিক বাংলায় স্বয়ংক্রিয় হিসাবের তাগাদা চলে যাবে।"
        }
        q.contains("কিউআর") || q.contains("qr") || q.contains("বিকাশ") || q.contains("নগদ") || q.contains("রকেট") || q.contains("পেমেন্ট") -> {
            "📱 'কিউআর হাব':\n" +
            "বিকাশ, নগদ, রকেট ও ব্যাংক একাউন্টের জন্য ডাইনামিক কিউআর কোড তৈরি করা যায়। টাকার পরিমাণ লিখে দিলে কাস্টমার এক ক্লিকে সঠিক টাকা স্ক্যান করে পাঠিয়ে দিতে পারবেন।"
        }
        q.contains("বিক্রি") || q.contains("মেমো") || q.contains("রসিদ") || q.contains("memo") || q.contains("pos") || q.contains("কার্ট") -> {
            "🛒 'বিক্রি ও ডিজিটাল মেমো':\n" +
            "মেমো স্ক্রিন থেকে প্রোডাক্ট ট্যাপ করে বা ক্যামেরা দিয়ে স্ক্যান করে কার্টে নিন। কাস্টমারের নাম, ক্যাশ বা বাকি সিলেক্ট করে সরাসরি ব্লুটুথ থার্মাল প্রিন্টারে প্রিন্ট বা হোয়াটসঅ্যাপে ই-মেমো শেয়ার করতে পারবেন।"
        }
        q.contains("পারমিশন") || q.contains("permission") || q.contains("ক্যামেরা") || q.contains("মাইক্রোফোন") -> {
            "🔒 'ক্যামেরা ও মাইক্রোফোন পারমিশন':\n" +
            "ক্যামেরা পারমিশন দ্রুত কিউআর/বারকোড স্ক্যান করার জন্য এবং মাইক্রোফোন পারমিশন ভয়েস লেজার ও টাচলেস অডিও হ্যান্ডশেকের জন্য প্রয়োজন। অ্যাপে ঢোকার সময় একবার অনুমোদন দিলেই হবে।"
        }
        q.contains("বিজ্ঞাপন") || q.contains("ad") || q.contains("start.io") || q.contains("startio") -> {
            "📢 এই অ্যাপে নিরাপদ Start.io বিজ্ঞাপন নেটওয়ার্ক ইন্টিগ্রেট করা রয়েছে।"
        }
        else -> {
            "ℹ️ আপনার প্রশ্নটি পেয়েছি। এই অ্যাপের প্রধান ফিচারসমূহ:\n" +
            "• অ্যাটোমিক প্রক্সিমিটি পে (কন্টাক্টলেস বাকি পরিশোধ)\n" +
            "• স্মার্ট ভয়েস লেজার (কথা বলে হিসাব)\n" +
            "• ডিজিটাল মেমো ও থার্মাল প্রিন্টিং\n" +
            "• স্বয়ংক্রিয় বাকি তাগাদা ও কিউআর হাব\n" +
            "কোন ফিচারটি নিয়ে বিস্তারিত জানতে চান দয়া করে লিখুন।"
        }
    }
}
