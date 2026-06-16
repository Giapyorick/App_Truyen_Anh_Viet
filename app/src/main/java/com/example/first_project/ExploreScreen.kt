package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.first_project.ui.theme.ReadingDarkGreen
import com.example.first_project.ui.theme.LightGreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Khám phá",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ReadingDarkGreen
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = ReadingDarkGreen)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onNavigateToHome,
                onLibraryClick = onNavigateToLibrary,
                onExploreClick = { },
                selectedItem = "EXPLORE"
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeSearchBar(query = "", onQueryChange = {})
            }

            item {
                ExploreCategoriesSection()
            }

            item {
                Text(
                    "Xu hướng",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = ReadingDarkGreen
                )
            }

            item {
                val trendingStories = listOf(
                    Story(id = "1", title = "The Da Vinci Code", rate = 4.8, likes = 1200, status = "Completed", img = "assets/Davinci_code.jpg"),
                    Story(id = "2", title = "Harry Potter", rate = 4.9, likes = 3500, status = "Completed", img = "assets/Harry_Potter.jpg"),
                    Story(id = "3", title = "Sherlock Holmes", rate = 4.9, likes = 2800, status = "Completed", img = "assets/Sherlock_holmes.jpg")
                )
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(trendingStories) { story ->
                        TrendingBookCard(story = story, onClick = { onNavigateToDetail(story.id) })
                    }
                }
            }

            item {
                Text(
                    "Đề xuất cho bạn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = ReadingDarkGreen
                )
            }

            // Mock list of recommended stories
            val mockStories = listOf(
                Story(id = "4", title = "Norwegian Wood", rate = 4.6, likes = 1500, status = "Completed", img = "assets/Norwegian_Wood.jpg"),
                Story(id = "5", title = "Me Before You", rate = 4.7, likes = 2100, status = "Completed", img = "assets/me_before_you.jpg"),
                Story(id = "6", title = "The Fault In Our Stars", rate = 4.8, likes = 3200, status = "Completed", img = "assets/The_Fault_In_Our_Stars.jpg")
            )

            items(mockStories) { story ->
                HomeBookItem(story = story, onClick = { onNavigateToDetail(story.id) })
            }
            
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TrendingBookCard(story: Story, onClick: () -> Unit) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else {
        story.img
    }

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LightGreenBg)
        ) {
            if (story.img.isNotEmpty()) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = ReadingDarkGreen.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            story.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = Color.Black
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFB300))
            Text(
                " ${story.rate}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ExploreCategoriesSection() {
    val categories = listOf(
        "Kinh dị", "Lãng mạn", "Hành động", "Phiêu lưu", 
        "Viễn tưởng", "Trinh thám", "Hài hước", "Lịch sử"
    )

    Column {
        Text(
            "Thể loại",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = ReadingDarkGreen,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Using a custom FlowRow-like layout or a Grid within LazyColumn item is tricky, 
        // for "UI only" we'll use a simple approach with rows or a fixed height grid.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { category ->
                        CategoryCard(name = category, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(60.dp)
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = LightGreenBg.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = ReadingDarkGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExplorePreview() {
    ExploreScreen()
}
