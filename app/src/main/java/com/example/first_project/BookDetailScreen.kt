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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.google.firebase.firestore.FirebaseFirestore

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
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<User?>(null) }

    var isFavorite by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    var userStoryData by remember { mutableStateOf<Story?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storyId) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Check library (Following)
            db.collection("users").document(user.uid)
                .collection("library").document(storyId)
                .addSnapshotListener { doc, error ->
                    if (doc != null && doc.exists()) {
                        isFollowing = true
                        userStoryData = Story.fromSnapshot(doc)
                    } else {
                        isFollowing = false
                        userStoryData = null
                    }
                }
            
            // Check favorites
            db.collection("users").document(user.uid)
                .collection("favorites").document(storyId)
                .addSnapshotListener { doc, error ->
                    isFavorite = doc != null && doc.exists()
                }

            // Fetch Current User Profile for commenting
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    currentUserProfile = doc.toObject(User::class.java)?.copy(id = doc.id)
                }
        }

        // Fetch Comments
        db.collection("stories").document(storyId)
            .collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    comments = value.documents.mapNotNull { it.toObject(Comment::class.java)?.apply { id = it.id } }
                }
            }

        db.collection("stories").document(storyId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val s = Story.fromSnapshot(document)
                    story = s
                    chapters = s?.chapters?.sortedBy { it.chapterNumber } ?: emptyList()
                    
                    // Fetch Author Name
                    s?.authorId?.let { uid ->
                        if (uid.isNotEmpty()) {
                            db.collection("authors").document(uid).get()
                                .addOnSuccessListener { authDoc ->
                                    authorName = authDoc.getString("authorName") ?: "Ẩn danh"
                                }
                                .addOnFailureListener {
                                    authorName = "Chưa rõ"
                                }
                        } else {
                            authorName = "Chưa rõ"
                        }
                    }

                    // Fetch Category Names
                    val catIds = s?.getCategoryIdsAsStrings()?.distinct()
                    if (catIds?.isNotEmpty() == true) {
                        db.collection("categories")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), catIds.take(10))
                            .get()
                            .addOnSuccessListener { catSnap ->
                                categoryNames = catSnap.documents.mapNotNull { it.getString("name") }
                            }
                    }
                    
                    // Nếu đã có chapters trong story (nhúng), chúng ta đã gán ở trên.
                    // Chỉ tải thêm từ sub-collection nếu thực sự cần thiết hoặc nếu danh sách nhúng rỗng.
                    if (chapters.isEmpty()) {
                        db.collection("stories").document(storyId).collection("chapters")
                            .orderBy("chapterNumber")
                            .get()
                            .addOnSuccessListener { value ->
                                if (value != null && !value.isEmpty) {
                                    val fetchedChapters = value.documents.mapNotNull { doc ->
                                        try {
                                            doc.toObject(Chapter::class.java)?.apply { id = doc.id }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    chapters = fetchedChapters
                                    story = story?.copy(chapters = fetchedChapters)
                                }
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                } else {
                    errorMessage = "Không tìm thấy truyện này"
                    isLoading = false
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
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
                val firstChapter = chapters.firstOrNull()
                val lastChapterId = userStoryData?.lastChapterId
                val lastChapterNum = userStoryData?.lastChapterNumber

                DetailBottomBar(
                    onContinueReading = if (lastChapterId != null) { { onChapterClick(lastChapterId) } } else null,
                    onStartFromBeginning = { firstChapter?.id?.let { onChapterClick(it) } },
                    continueText = if (lastChapterNum != null) "Tiếp tục C.$lastChapterNum" else "Tiếp tục",
                    story = story,
                    isFollowing = isFollowing,
                    isFavorite = isFavorite,
                    onToggleFollow = {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            val ref = db.collection("users").document(user.uid).collection("library").document(storyId)
                            if (isFollowing) {
                                ref.delete().addOnSuccessListener { isFollowing = false }
                            } else {
                                story?.let { s -> 
                                    ref.set(s, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isFollowing = true } 
                                }
                            }
                        }
                    },
                    onToggleFavorite = {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            val ref = db.collection("users").document(user.uid).collection("favorites").document(storyId)
                            if (isFavorite) {
                                ref.delete().addOnSuccessListener { isFavorite = false }
                            } else {
                                story?.let { s -> 
                                    ref.set(s, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isFavorite = true } 
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReadingDarkGreen)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage!!, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen)) {
                        Text("Quay lại")
                    }
                }
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
                    item {
                        CommentSection(
                            comments = comments,
                            onAddComment = { content, rating ->
                                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (user != null && currentUserProfile != null) {
                                    val newComment = Comment(
                                        userId = user.uid,
                                        userName = currentUserProfile?.name ?: "Người dùng",
                                        userImage = currentUserProfile?.image ?: "",
                                        content = content,
                                        rating = rating,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    db.collection("stories").document(storyId)
                                        .collection("comments").add(newComment)
                                        .addOnSuccessListener {
                                            // Update story rating stats (simplified)
                                            val newCount = s.count_rate + 1
                                            val newRate = (s.rate * s.count_rate + rating) / newCount
                                            db.collection("stories").document(storyId).update(
                                                "rate", newRate,
                                                "count_rate", newCount
                                            )
                                        }
                                }
                            }
                        )
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
                    .data(story.resolveImagePath())
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

@Composable
fun CategoryChipsSection(categoryNames: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Standardized chunked layout to avoid FlowRow binary incompatibilities
        val chunks = categoryNames.chunked(3)
        chunks.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { name ->
                    Surface(
                        color = ReadingDarkGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ReadingDarkGreen,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
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
fun CommentSection(
    comments: List<Comment>,
    onAddComment: (String, Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Đánh giá & Bình luận (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = ReadingDarkGreen
            )
            TextButton(onClick = { showDialog = true }) {
                Text("Viết đánh giá", color = ReadingDarkGreen)
            }
        }

        if (comments.isEmpty()) {
            Text(
                "Chưa có bình luận nào. Hãy là người đầu tiên đánh giá!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            comments.take(3).forEach { comment ->
                CommentItem(comment)
            }
            if (comments.size > 3) {
                TextButton(onClick = { /* TODO: Show all comments */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Xem tất cả bình luận", color = Color.Gray)
                }
            }
        }
    }

    if (showDialog) {
        AddCommentDialog(
            onDismiss = { showDialog = false },
            onConfirm = { content, rating ->
                onAddComment(content, rating)
                showDialog = false
            }
        )
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = if (comment.userImage.isNotEmpty()) comment.userImage else R.drawable.ic_launcher_foreground, // Replace with default avatar
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (index < comment.rating) Color(0xFFFFB300) else Color.LightGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(comment.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun AddCommentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết đánh giá của bạn") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index < rating) Color(0xFFFFB300) else Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Cảm nhận của bạn về tác phẩm...") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content, rating) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen)
            ) {
                Text("Gửi đánh giá")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
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
fun DetailBottomBar(
    onContinueReading: (() -> Unit)? = null,
    onStartFromBeginning: () -> Unit,
    continueText: String,
    story: Story?,
    isFollowing: Boolean,
    isFavorite: Boolean,
    onToggleFollow: () -> Unit,
    onToggleFavorite: () -> Unit
) {
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
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (onContinueReading != null) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartFromBeginning,
                        modifier = Modifier.weight(0.45f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Đọc từ đầu", fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = onContinueReading,
                        modifier = Modifier.weight(0.55f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(continueText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            } else {
                Button(
                    onClick = onStartFromBeginning,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bắt đầu đọc", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onToggleFollow,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isFollowing) Color.White else ReadingDarkGreen,
                    containerColor = if (isFollowing) ReadingDarkGreen else Color.Transparent
                )
            ) {
                Icon(
                    if (isFollowing) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Follow"
                )
            }
        }
    }
}
