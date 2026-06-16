package com.example.first_project

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.FlowPreview
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.first_project.network.GroqApi
import com.example.first_project.network.GroqRequest
import com.example.first_project.network.Message
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.example.first_project.ui.theme.LightGreenBg
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ReadingTheme { LIGHT, DARK, SEPIA }

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReadingScreen(
    storyId: String,
    chapterId: String,
    onBack: () -> Unit,
    onChapterChange: (String) -> Unit
) {
    val literataFont = FontFamily.Serif

    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    
    var storyData by remember { mutableStateOf<Story?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reading Settings
    var fontSize by remember { mutableStateOf(18) }
    var readingTheme by remember { mutableStateOf(ReadingTheme.LIGHT) }
    var showFontSizeSheet by remember { mutableStateOf(false) }
    var showToCSheet by remember { mutableStateOf(false) }
    var showAISheet by remember { mutableStateOf(false) }

    val bgColor = when (readingTheme) {
        ReadingTheme.LIGHT -> Color(0xFFFDFDF5)
        ReadingTheme.DARK -> Color(0xFF121212)
        ReadingTheme.SEPIA -> Color(0xFFF4ECD8)
    }
    val textColor = when (readingTheme) {
        ReadingTheme.LIGHT -> Color(0xFF1A1A1A)
        ReadingTheme.DARK -> Color(0xFFE0E0E0)
        ReadingTheme.SEPIA -> Color(0xFF5B4636)
    }
    val secondaryTextColor = when (readingTheme) {
        ReadingTheme.LIGHT -> Color(0xFF7F8C8D)
        ReadingTheme.DARK -> Color.Gray
        ReadingTheme.SEPIA -> Color(0xFF8C7E6A)
    }

    LaunchedEffect(storyId, chapterId) {
        isLoading = true
        try {
            if (storyId.isEmpty() || chapterId.isEmpty()) {
                errorMessage = "ID không hợp lệ"
                isLoading = false
                return@LaunchedEffect
            }

            val storyDoc = db.collection("stories").document(storyId).get().await()
            storyData = storyDoc.toObject(Story::class.java)
            
            val chaptersSnapshot = db.collection("stories")
                .document(storyId)
                .collection("chapters")
                .orderBy("chapterNumber")
                .get()
                .await()
            chapters = chaptersSnapshot.documents.mapNotNull { it.toObject(Chapter::class.java)?.apply { id = it.id } }

            val chapterDoc = db.collection("stories")
                .document(storyId)
                .collection("chapters")
                .document(chapterId)
                .get()
                .await()
            
            val chapter = chapterDoc.toObject(Chapter::class.java)
            if (chapter != null) {
                currentChapter = chapter
            } else {
                errorMessage = "Không tìm thấy nội dung chương"
            }
        } catch (e: Exception) {
            errorMessage = "Lỗi kết nối: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val displayChapter = currentChapter ?: Chapter()
    val isVietnameseMap = remember(chapterId) { mutableStateMapOf<Int, Boolean>() }

    val listState = rememberLazyListState()

    // Save progress to Firestore
    val saveProgress = { index: Int ->
        val user = FirebaseAuth.getInstance().currentUser
        val currentChap = currentChapter
        if (user != null && storyId.isNotEmpty() && chapterId.isNotEmpty() && currentChap != null) {
            val progressData = mapOf(
                "lastChapterId" to chapterId,
                "lastChapterNumber" to currentChap.chapterNumber,
                "lastReadTime" to System.currentTimeMillis(),
                "scrollIndex" to index,
                // Optional: Store basic story info if it doesn't exist yet
                "id" to storyId,
                "title" to (storyData?.title ?: "")
            )
            
            db.collection("users").document(user.uid)
                .collection("library").document(storyId)
                .set(progressData, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener { e ->
                    android.util.Log.e("ReadingScreen", "Error saving progress", e)
                }
        }
    }

    LaunchedEffect(listState, chapterId, currentChapter) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(2000L) // Đợi 2 giây sau khi ngừng cuộn để lưu
            .distinctUntilChanged()
            .collectLatest { index ->
                if (currentChapter != null) {
                    saveProgress(index)
                }
            }
    }


    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    var isFirstEntry by remember { mutableStateOf(true) }

    // Xử lý cuộn trang khi load chương mới hoặc khôi phục tiến độ
    LaunchedEffect(isLoading, currentChapter) {
        if (!isLoading && currentChapter != null) {
            if (isFirstEntry) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    try {
                        val doc = db.collection("users").document(user.uid)
                            .collection("library").document(storyId)
                            .get().await()
                        val savedChapterId = doc.getString("lastChapterId")
                        val savedIndex = doc.getLong("scrollIndex")?.toInt() ?: 0
                        
                        if (savedChapterId == chapterId && savedIndex > 0) {
                            if (savedIndex < (currentChapter?.paragraphs?.size ?: 0) + 1) {
                                listState.scrollToItem(savedIndex)
                            }
                        } else {
                            listState.scrollToItem(0)
                        }
                    } catch (e: Exception) {
                        listState.scrollToItem(0)
                    }
                } else {
                    listState.scrollToItem(0)
                }
                isFirstEntry = false
            } else {
                // Khi người dùng bấm sang chương khác trong cùng một phiên đọc
                listState.scrollToItem(0)
            }
        }
    }

    val isDarkMode = readingTheme == ReadingTheme.DARK

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            storyData?.title ?: "Đang tải...",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = literataFont,
                                color = if (isDarkMode) Color.White else ReadingDarkGreen
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (readingTheme == ReadingTheme.DARK) Color.White else ReadingDarkGreen)
                    }
                },
                actions = {
                    // Removed global translation button
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            ReadingControlsBar(
                onFontSizeClick = { showFontSizeSheet = true },
                onThemeToggle = { 
                    readingTheme = when(readingTheme) {
                        ReadingTheme.LIGHT -> ReadingTheme.SEPIA
                        ReadingTheme.SEPIA -> ReadingTheme.DARK
                        ReadingTheme.DARK -> ReadingTheme.LIGHT
                    }
                },
                onToCClick = { showToCSheet = true },
                onTranslateAllClick = {
                    val anyEnglish = displayChapter.paragraphs.indices.any { isVietnameseMap[it] != true }
                    displayChapter.paragraphs.indices.forEach { idx ->
                        isVietnameseMap[idx] = anyEnglish
                    }
                },
                readingTheme = readingTheme
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAISheet = true },
                containerColor = ReadingDarkGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp) // Tránh đè lên BottomBar
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        val scrollProgress = remember {
            derivedStateOf {
                if (listState.layoutInfo.totalItemsCount == 0) 0f
                else {
                    val firstVisible = listState.firstVisibleItemIndex.toFloat()
                    val total = listState.layoutInfo.totalItemsCount.toFloat()
                    (firstVisible / (total - 1)).coerceIn(0f, 1f)
                }
            }
        }

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Idea 5: Progress Bar
            LinearProgressIndicator(
                progress = { scrollProgress.value },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                color = ReadingDarkGreen,
                trackColor = Color.Transparent
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ReadingDarkGreen)
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            } else {
                // Idea 7: Transition Animation
                AnimatedContent(
                    targetState = chapterId,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it }).togetherWith(fadeOut() + slideOutHorizontally { -it })
                    },
                    label = "ChapterTransition"
                ) { targetChapterId ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                        state = listState
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Chương ${displayChapter.chapterNumber}".uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = secondaryTextColor,
                                    letterSpacing = 3.sp,
                                    fontFamily = literataFont,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    displayChapter.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = literataFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (readingTheme == ReadingTheme.DARK) Color.White else Color(0xFF2C3E50)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            val annotatedString = buildAnnotatedString {
                                displayChapter.paragraphs.forEachIndexed { index, paragraph ->
                                    val isVietnamese = isVietnameseMap[index] == true
                                    val rawText = if (isVietnamese) paragraph.vietnamese else paragraph.english
                                    val cleanedText = rawText.replace(Regex("\\s+"), " ").trim()
                                    
                                    if (cleanedText.isEmpty()) return@forEachIndexed

                                    pushStringAnnotation(tag = "para", annotation = index.toString())
                                    
                                    withStyle(style = SpanStyle(
                                        fontFamily = literataFont,
                                        fontSize = fontSize.sp,
                                        color = if (isVietnamese) {
                                            if (readingTheme == ReadingTheme.DARK) Color(0xFF81C784) else ReadingDarkGreen
                                        } else textColor,
                                        fontStyle = if (isVietnamese) FontStyle.Italic else FontStyle.Normal,
                                    )) {
                                        append(" ")
                                        append(cleanedText)
                                    }
                                    pop()
                                }
                            }

                            val clipboardManager = LocalClipboardManager.current
                            val scope = rememberCoroutineScope()
                            Text(
                                text = annotatedString,
                                onTextLayout = { textLayoutResult = it },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = (fontSize * 1.8).sp,
                                    textAlign = TextAlign.Justify,
                                    letterSpacing = 0.2.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                val layoutResult = textLayoutResult ?: return@detectTapGestures
                                                val position = layoutResult.getOffsetForPosition(offset)
                                                annotatedString.getStringAnnotations(tag = "para", start = position, end = position)
                                                    .firstOrNull()?.let { annotation ->
                                                        val idx = annotation.item.toInt()
                                                        isVietnameseMap[idx] = !(isVietnameseMap[idx] ?: false)
                                                    }
                                            },
                                            onDoubleTap = { offset ->
                                                val layoutResult = textLayoutResult ?: return@detectTapGestures
                                                val position = layoutResult.getOffsetForPosition(offset)
                                                annotatedString.getStringAnnotations(tag = "para", start = position, end = position)
                                                    .firstOrNull()?.let { annotation ->
                                                        val idx = annotation.item.toInt()
                                                        val paragraph = displayChapter.paragraphs.getOrNull(idx)
                                                        if (paragraph != null) {
                                                            val isVietnamese = isVietnameseMap[idx] == true
                                                            val textToCopy = if (isVietnamese) paragraph.vietnamese else paragraph.english
                                                            clipboardManager.setText(AnnotatedString(textToCopy))
                                                            Toast.makeText(context, "Đã sao chép đoạn văn", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            }
                                        )
                                    }
                            )
                        }

                        val prevChapter = chapters.find { it.chapterNumber == displayChapter.chapterNumber - 1 }
                        val nextChapter = chapters.find { it.chapterNumber == displayChapter.chapterNumber + 1 }

                        if (prevChapter != null || nextChapter != null) {
                            item {
                                Spacer(modifier = Modifier.height(48.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (prevChapter != null) {
                                        Button(
                                            onClick = { onChapterChange(prevChapter.id) },
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("CHƯƠNG TRƯỚC", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                                        }
                                    }

                                    if (nextChapter != null) {
                                        Button(
                                            onClick = { onChapterChange(nextChapter.id) },
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("CHƯƠNG TIẾP", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        } else {
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            }

            // Bottom Gradient Overlay for depth
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor.copy(alpha = 0.9f))
                        )
                    )
            )
        }
    }

    if (showFontSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFontSizeSheet = false },
            containerColor = bgColor
        ) {
            FontSizeControl(
                currentSize = fontSize,
                onSizeChange = { fontSize = it },
                readingTheme = readingTheme
            )
        }
    }

    if (showToCSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToCSheet = false },
            containerColor = bgColor
        ) {
            TableOfContents(
                chapters = chapters,
                currentChapterId = chapterId,
                onChapterSelect = { newChapterId ->
                    showToCSheet = false
                    onChapterChange(newChapterId)
                },
                readingTheme = readingTheme
            )
        }
    }

    if (showAISheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAISheet = false },
            sheetState = sheetState,
            containerColor = bgColor,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AIChatAssistant(readingTheme = readingTheme)
        }
    }
}

@Composable
fun AIChatAssistant(readingTheme: ReadingTheme) {
    var userInput by remember { mutableStateOf("") }
    var chatResponse by remember { mutableStateOf("Chào bạn! Tôi là trợ lý AI. Hãy dán đoạn văn tiếng Anh bạn muốn dịch và tìm hiểu sâu hơn vào đây.") }
    var isThinking by remember { mutableStateOf(false) }

    val groqApi = remember { GroqApi.create() }
    val scope = rememberCoroutineScope()
    
    val isDarkMode = readingTheme == ReadingTheme.DARK

    // === BƯỚC QUAN TRỌNG: DÁN API KEY CỦA BẠN VÀO GIỮA DẤU "" DƯỚI ĐÂY ===
    val rawApiKey = "key"
    
    // Tự động xử lý Header để đảm bảo định dạng "Bearer <API_KEY>"
    val authHeader = remember(rawApiKey) {
        val trimmed = rawApiKey.trim()
        when {
            trimmed.isEmpty() || trimmed == "DÁN_MÃ_GSK_CỦA_BẠN_VÀO_ĐÂY" -> ""
            trimmed.startsWith("Bearer ") -> trimmed
            else -> "Bearer $trimmed"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "AI Giải Thích & Dịch Thuật",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else ReadingDarkGreen,
            fontFamily = FontFamily.Serif
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            if (isThinking) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ReadingDarkGreen
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            chatResponse,
                            color = if (isDarkMode) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập đoạn văn cần giải thích...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) Color.DarkGray else Color(0xFFF0F0F0),
                    unfocusedContainerColor = if (isDarkMode) Color.DarkGray else Color(0xFFF0F0F0),
                    focusedIndicatorColor = ReadingDarkGreen
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (userInput.isNotBlank() && !isThinking) {
                        if (authHeader.isEmpty()) {
                            chatResponse = "Lỗi: Bạn chưa nhập API Key! \nHãy mở file ReadingScreen.kt, tìm biến 'rawApiKey' và dán mã gsk_... của bạn vào."
                            return@IconButton
                        }

                        isThinking = true
                        val prompt = """
                            Dịch đoạn văn sau sang tiếng Việt, giải thích chi tiết các cấu trúc ngữ pháp hoặc từ vựng khó, 
                            và cho 2-3 ví dụ thực tế liên quan:
                            
                            "$userInput"
                        """.trimIndent()
                        
                        scope.launch {
                            try {
                                val request = GroqRequest(
                                    messages = listOf(
                                        Message(role = "system", content = "Bạn là một trợ lý học tiếng Anh thông minh."),
                                        Message(role = "user", content = prompt)
                                    )
                                )
                                val response = groqApi.getCompletion(authHeader, null, request)
                                chatResponse = response.choices.firstOrNull()?.message?.content ?: "Không có phản hồi từ AI."
                            } catch (e: retrofit2.HttpException) {
                                e.printStackTrace()
                                val errorCode = e.code()
                                val errorBody = e.response()?.errorBody()?.string()
                                chatResponse = when (errorCode) {
                                    401 -> "Lỗi 401: Unauthorized. API Key có thể không đúng hoặc đã bị thu hồi. \nChi tiết từ Server: $errorBody"
                                    403 -> "Lỗi 403: Forbidden. Bạn không có quyền truy cập tài nguyên này. \nChi tiết: $errorBody"
                                    429 -> "Lỗi 429: Rate Limit. Bạn đã gửi quá nhiều yêu cầu. Hãy thử lại sau. \nChi tiết: $errorBody"
                                    else -> "Lỗi HTTP $errorCode: $errorBody"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val errorMsg = e.localizedMessage ?: "Lỗi không xác định"
                                chatResponse = "Lỗi: $errorMsg. \n(Hãy kiểm tra kết nối mạng và thử lại)"
                            } finally {
                                isThinking = false
                                userInput = ""
                            }
                        }
                    }
                },
                enabled = userInput.isNotBlank() && !isThinking,
                colors = IconButtonDefaults.iconButtonColors(containerColor = ReadingDarkGreen, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ReadingControlsBar(
    onFontSizeClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onToCClick: () -> Unit,
    onTranslateAllClick: () -> Unit,
    readingTheme: ReadingTheme
) {
    val isDarkMode = readingTheme == ReadingTheme.DARK
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
        color = when(readingTheme) {
            ReadingTheme.LIGHT -> Color.White
            ReadingTheme.DARK -> Color(0xFF1E1E1E)
            ReadingTheme.SEPIA -> Color(0xFFE8DCC4)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ControlItem(Icons.Default.TextFields, "Cỡ chữ", readingTheme, onFontSizeClick)
            
            val themeIcon = when(readingTheme) {
                ReadingTheme.LIGHT -> Icons.Default.MenuBook
                ReadingTheme.SEPIA -> Icons.Default.DarkMode
                ReadingTheme.DARK -> Icons.Default.LightMode
            }
            val themeLabel = when(readingTheme) {
                ReadingTheme.LIGHT -> "Sepia"
                ReadingTheme.SEPIA -> "Tối"
                ReadingTheme.DARK -> "Sáng"
            }
            
            ControlItem(themeIcon, themeLabel, readingTheme, onThemeToggle)
            ControlItem(Icons.Default.FormatListBulleted, "Mục lục", readingTheme, onToCClick)
            ControlItem(Icons.Default.Translate, "Dịch hết", readingTheme, onTranslateAllClick)
        }
    }
}

@Composable
fun ControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    readingTheme: ReadingTheme,
    onClick: () -> Unit
) {
    val isDarkMode = readingTheme == ReadingTheme.DARK
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isDarkMode) Color.White else ReadingDarkGreen
        )
        Text(
            label,
            fontSize = 10.sp,
            color = if (isDarkMode) Color.LightGray else (if(readingTheme == ReadingTheme.SEPIA) Color(0xFF5B4636) else Color.Gray),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FontSizeControl(currentSize: Int, onSizeChange: (Int) -> Unit, readingTheme: ReadingTheme) {
    val isDarkMode = readingTheme == ReadingTheme.DARK
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            "Cỡ chữ",
            style = MaterialTheme.typography.titleMedium,
            color = if (isDarkMode) Color.White else ReadingDarkGreen,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { if (currentSize > 12) onSizeChange(currentSize - 2) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (isDarkMode) Color.White else ReadingDarkGreen)
            }
            Text(
                currentSize.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else ReadingDarkGreen
            )
            IconButton(onClick = { if (currentSize < 32) onSizeChange(currentSize + 2) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (isDarkMode) Color.White else ReadingDarkGreen)
            }
        }
    }
}

@Composable
fun TableOfContents(
    chapters: List<Chapter>,
    currentChapterId: String,
    onChapterSelect: (String) -> Unit,
    readingTheme: ReadingTheme
) {
    val isDarkMode = readingTheme == ReadingTheme.DARK
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(16.dp)
    ) {
        Text(
            "Mục lục",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = if (isDarkMode) Color.White else ReadingDarkGreen,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn {
            items(chapters) { chapter ->
                val isCurrent = chapter.id == currentChapterId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterSelect(chapter.id) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        chapter.chapterNumber.toString(),
                        modifier = Modifier.width(32.dp),
                        color = if (isCurrent) ReadingDarkGreen else (if (isDarkMode) Color.Gray else Color.LightGray),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrent) ReadingDarkGreen else (if (isDarkMode) Color.White else Color.Black),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
                HorizontalDivider(color = if (isDarkMode) Color.DarkGray else Color(0xFFF0F0F0))
            }
        }
    }
}
