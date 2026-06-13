package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.example.first_project.ui.theme.LightGreenBg
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isAdmin: Boolean = false,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit,
    onNavigateToReading: (String, String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var continueStory by remember { mutableStateOf<Story?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Fetch Categories
        db.collection("categories").whereEqualTo("status", "Active").addSnapshotListener { value, error ->
            if (value != null) {
                categories = value.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                }
            }
        }

        // Fetch All Stories
        db.collection("stories").addSnapshotListener { value, error ->
            if (value != null) {
                stories = value.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Story::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        // Fallback: manually parse if automatic deserialization fails (e.g. categoryIds type mismatch)
                        val data = doc.data
                        if (data != null) {
                            Story(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                authorId = data["authorId"] as? String ?: "",
                                description = data["description"] as? String ?: "",
                                status = data["status"] as? String ?: "",
                                img = data["img"] as? String ?: "",
                                likes = (data["likes"] as? Long)?.toInt() ?: 0,
                                rate = (data["rate"] as? Number)?.toDouble() ?: 0.0,
                                count_follower = (data["count_follower"] as? Long)?.toInt() ?: 0,
                                count_rate = (data["count_rate"] as? Long)?.toInt() ?: 0,
                                categoryIds = (data["categoryIds"] as? List<*>)?.filterNotNull() ?: emptyList(),
                                publicationDate = data["publicationDate"] as? String ?: "",
                                lastChapterId = data["lastChapterId"] as? String,
                                lastChapterNumber = (data["lastChapterNumber"] as? Long)?.toInt(),
                                lastReadTime = data["lastReadTime"] as? Long,
                                scrollIndex = (data["scrollIndex"] as? Long)?.toInt() ?: 0
                            )
                        } else null
                    }
                }
            }
            isLoading = false
        }

        // Fetch Continue Reading Story
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .collection("library")
                .orderBy("lastReadTime", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { value, error ->
                    if (value != null && !value.isEmpty) {
                        val doc = value.documents.first()
                        val story = try {
                            doc.toObject(Story::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            val data = doc.data
                            if (data != null) {
                                Story(
                                    id = doc.id,
                                    title = data["title"] as? String ?: "",
                                    authorId = data["authorId"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    status = data["status"] as? String ?: "",
                                    img = data["img"] as? String ?: "",
                                    likes = (data["likes"] as? Long)?.toInt() ?: 0,
                                    rate = (data["rate"] as? Number)?.toDouble() ?: 0.0,
                                    count_follower = (data["count_follower"] as? Long)?.toInt() ?: 0,
                                    count_rate = (data["count_rate"] as? Long)?.toInt() ?: 0,
                                    categoryIds = (data["categoryIds"] as? List<*>)?.filterNotNull() ?: emptyList(),
                                    publicationDate = data["publicationDate"] as? String ?: "",
                                    lastChapterId = data["lastChapterId"] as? String,
                                    lastChapterNumber = (data["lastChapterNumber"] as? Long)?.toInt(),
                                    lastReadTime = data["lastReadTime"] as? Long,
                                    scrollIndex = (data["scrollIndex"] as? Long)?.toInt() ?: 0
                                )
                            } else null
                        }
                        if (story?.lastChapterId != null && story.lastChapterId.isNotEmpty()) {
                            continueStory = story
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Literary Serenity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ReadingDarkGreen
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = ReadingDarkGreen)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = ReadingDarkGreen)
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = ReadingDarkGreen)
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = ReadingDarkGreen)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = { },
                onLibraryClick = onNavigateToLibrary,
                onExploreClick = onNavigateToExplore,
                selectedItem = "HOME"
            )
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
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    HomeSearchBar(searchQuery) { searchQuery = it }
                }
                item {
                    if (stories.isNotEmpty()) {
                        FeaturedSection(story = stories.first(), onClick = { onNavigateToDetail(stories.first().id) })
                    }
                }
                item {
                    GenresSection(categories)
                }
                if (continueStory != null) {
                    item {
                        ContinueReadingSection(
                            story = continueStory!!,
                            onContinueClick = { 
                                onNavigateToReading(continueStory!!.id, continueStory!!.lastChapterId!!) 
                            }
                        )
                    }
                }
                item {
                    NewUpdatesSection(stories = stories, onBookClick = onNavigateToDetail)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun HomeSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(56.dp),
        placeholder = { Text("Tìm kiếm tác phẩm, tác giả...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ReadingDarkGreen) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LightGreenBg.copy(alpha = 0.4f),
            unfocusedContainerColor = LightGreenBg.copy(alpha = 0.4f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun FeaturedSection(story: Story, onClick: () -> Unit) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else {
        story.img
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ReadingDarkGreen)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = story.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
// ... rest of the code

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "ĐỀ XUẤT CHO BẠN",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                story.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                "Tác phẩm song ngữ đặc sắc",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text("Đọc ngay", color = ReadingDarkGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GenresSection(categories: List<Category>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            "Thể loại phổ biến",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = ReadingDarkGreen,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { category ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightGreenBg,
                    modifier = Modifier.clickable { /* TODO: Filter by category */ }
                ) {
                    Text(
                        category.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = ReadingDarkGreen,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueReadingSection(story: Story, onContinueClick: () -> Unit) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else {
        story.img
    }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            "Tiếp tục đọc",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = ReadingDarkGreen,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onContinueClick() },
            colors = CardDefaults.cardColors(containerColor = ReadingDarkGreen.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp, 75.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ReadingDarkGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.img.isNotEmpty()) {
                        AsyncImage(
                            model = imagePath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Book, contentDescription = null, tint = ReadingDarkGreen)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        story.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ReadingDarkGreen,
                        maxLines = 1
                    )
                    Text(
                        "Chương ${story.lastChapterNumber ?: 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Button(
                    onClick = onContinueClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ReadingDarkGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Đọc tiếp", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun NewUpdatesSection(stories: List<Story>, onBookClick: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mới cập nhật",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = ReadingDarkGreen
            )
            TextButton(onClick = { /* TODO */ }) {
                Text("Xem tất cả", color = ReadingDarkGreen)
            }
        }

        stories.take(5).forEach { story ->
            HomeBookItem(story = story, onClick = { onBookClick(story.id) })
        }
    }
}

@Composable
fun HomeBookItem(story: Story, onClick: () -> Unit) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else {
        story.img
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(65.dp, 95.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LightGreenBg),
            contentAlignment = Alignment.Center
        ) {
            if (story.img.isNotEmpty()) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Book, contentDescription = null, tint = ReadingDarkGreen.copy(alpha = 0.3f))
            }
        }
// ...
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                story.title,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
            Text(
                "Trạng thái: ${story.status}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFB300))
                Text(
                    " ${story.rate} • ${story.likes} lượt thích",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        IconButton(onClick = { /* TODO */ }) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = ReadingDarkGreen)
        }
    }
}

@Composable
fun BottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    selectedItem: String = "HOME"
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("TRANG CHỦ") },
            selected = selectedItem == "HOME",
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ReadingDarkGreen,
                selectedTextColor = ReadingDarkGreen,
                indicatorColor = LightGreenBg
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Library") },
            label = { Text("THƯ VIỆN") },
            selected = selectedItem == "LIBRARY",
            onClick = onLibraryClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ReadingDarkGreen,
                selectedTextColor = ReadingDarkGreen,
                indicatorColor = LightGreenBg
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("KHÁM PHÁ") },
            selected = selectedItem == "EXPLORE",
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ReadingDarkGreen,
                selectedTextColor = ReadingDarkGreen,
                indicatorColor = LightGreenBg
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    HomeScreen(
        onNavigateToDetail = {},
        onNavigateToReading = { _, _ -> },
        onNavigateToLibrary = {},
        onNavigateToExplore = {}
    )
}
