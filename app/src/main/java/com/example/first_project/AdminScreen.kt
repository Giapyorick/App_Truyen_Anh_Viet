package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Custom Colors based on the image
val AdminBg = Color(0xFFF9F9F7)
val DarkGreen = Color(0xFF2D4338)
val SuccessGreen = Color(0xFFE8F5E9)
val SuccessText = Color(0xFF2E7D32)
val AlertRed = Color(0xFFFFEBEE)
val AlertText = Color(0xFFC62828)
val InfoBlue = Color(0xFFE3F2FD)
val InfoText = Color(0xFF1976D2)
val WarningOrange = Color(0xFFFFF3E0)
val WarningText = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onBack: () -> Unit,
    onManageStories: () -> Unit,
    onManageAuthors: () -> Unit,
    onManageCategories: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stitch Reader", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
            )
        },
        containerColor = AdminBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "System Overview",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                )
                Text(
                    "Monitoring the heartbeat of the bilingual library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // System Health Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SYSTEM HEALTH", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                        Text("Operational", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Server Latency", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("24ms", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { 0.8f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = Color.Black,
                            trackColor = Color.LightGray
                        )
                    }
                }
            }

            // Stats Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Authors Stats (Dark Green)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                            Text("142", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("TOTAL AUTHORS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    // Stories Stats (Light Grey)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
                            Text("3,892", style = MaterialTheme.typography.headlineSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("TOTAL STORIES", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Management Items
            item {
                ManagementItem(
                    title = "Manage Authors",
                    subtitle = "Onboard, verify, and curate literary contributors.",
                    icon = Icons.Default.Groups,
                    onClick = onManageAuthors
                )
            }
            item {
                ManagementItem(
                    title = "Manage Stories",
                    subtitle = "Review translations and publish new bilingual editions.",
                    icon = Icons.Default.Description,
                    onClick = onManageStories
                )
            }
            item {
                ManagementItem(
                    title = "Manage Categories",
                    subtitle = "Organize stories into thematic genres and collections.",
                    icon = Icons.Default.Category,
                    onClick = onManageCategories
                )
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {}) {
                        Text("View All", style = MaterialTheme.typography.labelLarge, color = Color.Black)
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header for table
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("ACTIVITY", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("TARGET", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("TIMESTAMP", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("STATUS", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    
                    ActivityRow("New Story Published", "\"The Quiet Forest\"", "2 mins ago", "SUCCESS", isSuccess = true)
                    ActivityRow("New Author Verified", "Trần Minh Anh", "15 mins ago", "SUCCESS", isSuccess = true)
                    ActivityRow("Translation Conflict", "\"Echoes of Silence\"", "1 hour ago", "ALERT", isSuccess = false)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ManagementItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F1F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun TransparentTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, enabled: Boolean = true) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.LightGray) },
        enabled = enabled,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = DarkGreen,
            unfocusedIndicatorColor = Color.LightGray,
            disabledIndicatorColor = Color.LightGray,
            disabledTextColor = Color.Black
        ),
        singleLine = true
    )
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "COMPLETED" -> SuccessGreen to SuccessText
        "IN-PROGRESS" -> InfoBlue to InfoText
        "HIATUS" -> AlertRed to AlertText
        "ACTIVE" -> SuccessGreen to SuccessText
        "INACTIVE" -> AlertRed to AlertText
        "VERIFIED" -> InfoBlue to InfoText
        "POSTING" -> WarningOrange to WarningText
        else -> Color.LightGray to Color.DarkGray
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            status.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun ActivityRow(activity: String, target: String, time: String, status: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isSuccess) Icons.Default.FileUpload else Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSuccess) Color.Black else AlertText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(activity, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        Text(target, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(time, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Surface(
            modifier = Modifier.weight(0.8f),
            color = if (isSuccess) SuccessGreen else AlertRed,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                status,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = if (isSuccess) SuccessText else AlertText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
