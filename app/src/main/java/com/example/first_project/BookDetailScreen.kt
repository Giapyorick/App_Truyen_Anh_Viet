package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    storyId: String,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var story by remember { mutableStateOf<Story?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var authorName by remember { mutableStateOf("Đang tải...") }
    var categoryNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var userStoryData by remember { mutableStateOf<Story?>(null) }

    LaunchedEffect(storyId) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .collection("library").document(storyId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userStoryData = doc.toObject(Story::class.java)
                    }
                }
        }
        
        db.collection("stories").document(storyId).get()
            .addOnSuccessListener { document ->
                val s = document.toObject(Story::class.java)?.apply { id = document.id }
                story = s
                
                // Fetch Author Name
                s?.authorId?.let { uid ->
                    if (uid.isNotEmpty()) {
                        db.collection("authors").document(uid).get()
                            .addOnSuccessListener { authDoc ->
                                authorName = authDoc.getString("authorName") ?: "Ẩn danh"
                            }
                    } else {
                        authorName = "Chưa rõ"
                    }
                }

                // Fetch Category Names
                val catIds = s?.getCategoryIdsStrings()
                if (catIds?.isNotEmpty() == true) {
                    db.collection("categories")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), catIds)
                        .get()
                        .addOnSuccessListener { catSnap ->
                            categoryNames = catSnap.documents.mapNotNull { it.getString("name") }
                        }
                }
            }
        
        db.collection("stories").document(storyId).collection("chapters")
            .orderBy("chapterNumber")
            .addSnapshotListener { value, error ->
                if (value != null) {
                    chapters = value.documents.mapNotNull { doc ->
                        doc.toObject(Chapter::class.java)?.apply { id = doc.id }
                    }
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Literary Serenity",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = ReadingDarkGreen
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ReadingDarkGreen)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Translate, contentDescription = "Language", tint = ReadingDarkGreen)
                    }
                }
            )
        },
        bottomBar = {
            if (story != null && chapters.isNotEmpty()) {
                val startChapterId = userStoryData?.lastChapterId ?: chapters.first().id
                val startChapterNum = userStoryData?.lastChapterNumber ?: chapters.first().chapterNumber
                
                DetailBottomBar(
                    onReadNow = { onChapterClick(startChapterId) },
                    buttonText = if (userStoryData?.lastChapterId != null) "Tiếp tục đọc (Chương $startChapterNum)" else "Bắt đầu đọc",
                    story = story
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReadingDarkGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                story?.let { s ->
                    item {
                        BookHeaderSection(s, authorName)
                    }
                    item {
                        BookStatsSection(s, chapters.size)
                    }
                    if (categoryNames.isNotEmpty()) {
                        item {
                            CategoryChipsSection(categoryNames)
                        }
                    }
                    item {
                        BookSummarySection(s)
                    }
                }
                item {
                    Text(
                        "Danh sách chương",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ReadingDarkGreen,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                items(chapters) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        isLocked = false, // Implementation for locked chapters can be added later
                        onClick = { onChapterClick(chapter.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookHeaderSection(story: Story, authorName: String) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else {
        story.img
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp, 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ReadingDarkGreen)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = story.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay gradient to make text readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 300f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Text(
                        "Bilingual Edition",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ReadingDarkGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    story.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "Tác giả: $authorName",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipsSection(categoryNames: List<String>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoryNames.forEach { name ->
            Surface(
                color = ReadingDarkGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = ReadingDarkGreen
                )
            }
        }
    }
}

@Composable
fun BookStatsSection(story: Story, chapterCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(story.rate.toString(), "Đánh giá", Icons.Default.Star)
        Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color.LightGray))
        StatItem(chapterCount.toString(), "Chương", null)
        Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color.LightGray))
        StatItem("EN - VI", "Ngôn ngữ", null)
    }
}

@Composable
fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun BookSummarySection(story: Story) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            "Tóm tắt nội dung",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = ReadingDarkGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            story.description,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ChapterItem(chapter: Chapter, isLocked: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = !isLocked) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLocked) Color.LightGray.copy(alpha = 0.5f) else ReadingDarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chapter.chapterNumber.toString(),
                    color = if (isLocked) Color.Gray else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif
                )
                Text("Chapter ${chapter.chapterNumber}", color = Color.Gray, fontSize = 12.sp)
            }
            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = ReadingDarkGreen)
            }
        }
    }
}

@Composable
fun DetailBottomBar(onReadNow: () -> Unit, buttonText: String, story: Story?) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onReadNow,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(
                onClick = {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (user != null && story != null) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(user.uid)
                            .collection("library").document(story.id)
                            .set(story)
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ReadingDarkGreen)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark")
            }
        }
    }
}
