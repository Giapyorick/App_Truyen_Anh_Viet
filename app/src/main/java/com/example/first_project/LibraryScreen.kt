package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.example.first_project.ui.theme.LightGreenBg
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val user = remember { FirebaseAuth.getInstance().currentUser }
    var libraryStories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid)
                .collection("library")
                .addSnapshotListener { value, error ->
                    if (value != null) {
                        libraryStories = value.documents.mapNotNull { it.toObject(Story::class.java)?.apply { id = it.id } }
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Thư viện của tôi",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = ReadingDarkGreen
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ReadingDarkGreen)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = ReadingDarkGreen)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onLibraryClick = { },
                onExploreClick = onNavigateToExplore,
                selectedItem = "LIBRARY"
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReadingDarkGreen)
            }
        } else if (user == null) {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vui lòng đăng nhập để xem thư viện")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                item {
                    LibrarySearchBar()
                }
                
                if (libraryStories.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("Thư viện còn trống. Hãy lưu những câu chuyện bạn yêu thích!", color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Text(
                            "Danh sách lưu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ReadingDarkGreen,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    items(libraryStories) { story ->
                        val progressValue = if (story.chapters.isNotEmpty() && story.lastChapterNumber != null) {
                            story.lastChapterNumber.toFloat() / story.chapters.size.toFloat()
                        } else 0f
                        
                        val progressText = if (story.lastChapterNumber != null) {
                            "Đã đọc ${story.lastChapterNumber}/${story.chapters.size} chương"
                        } else {
                            "Chưa bắt đầu (${story.chapters.size} chương)"
                        }

                        ReadingItem(
                            story = story,
                            progressValue = progressValue,
                            progressText = progressText,
                            onClick = { onNavigateToDetail(story.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySearchBar() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        placeholder = { Text("Tìm trong thư viện...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ReadingDarkGreen) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LightGreenBg.copy(alpha = 0.3f),
            unfocusedContainerColor = LightGreenBg.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun ReadingItem(story: Story, progressValue: Float, progressText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = story.resolveImagePath(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp, 85.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    story.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1
                )
                Text(
                    if (story.status.isNotEmpty()) "Tình trạng: ${story.status}" else "Sách điện tử",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(progressText, fontSize = 11.sp, color = ReadingDarkGreen, fontWeight = FontWeight.Medium)
                    Text("${(progressValue * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ReadingDarkGreen,
                    trackColor = LightGreenBg
                )
            }
        }
    }
}
