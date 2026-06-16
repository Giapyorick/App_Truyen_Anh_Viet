package com.example.first_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.first_project.admin.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Colors
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
    onManageCategories: () -> Unit,
    onManageUsers: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val storyCount by viewModel.storyCount.collectAsState()
    val authorCount by viewModel.authorCount.collectAsState()
    val categoryCount by viewModel.categoryCount.collectAsState()
    val userCount by viewModel.userCount.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stitch Reader Admin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.refreshStats()
                        viewModel.fetchRecentActivities()
                    }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
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
                    "Tổng quan hệ thống",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                )
                Text(
                    "Theo dõi nhịp đập của thư viện song ngữ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Stats Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(
                            modifier = Modifier.weight(1f),
                            count = authorCount.toString(),
                            label = "TÁC GIẢ",
                            icon = Icons.Default.Groups,
                            containerColor = DarkGreen,
                            contentColor = Color.White
                        )
                        StatsCard(
                            modifier = Modifier.weight(1f),
                            count = storyCount.toString(),
                            label = "TRUYỆN",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            containerColor = Color(0xFFF1F1F1),
                            contentColor = Color.Black
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatsCard(
                            modifier = Modifier.weight(1f),
                            count = categoryCount.toString(),
                            label = "THỂ LOẠI",
                            icon = Icons.Default.Category,
                            containerColor = InfoBlue,
                            contentColor = InfoText
                        )
                        StatsCard(
                            modifier = Modifier.weight(1f),
                            count = userCount.toString(),
                            label = "NGƯỜI DÙNG",
                            icon = Icons.Default.Person,
                            containerColor = WarningOrange,
                            contentColor = WarningText
                        )
                    }
                }
            }

            // Management Items
            item {
                Text("Quản lý danh mục", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ManagementItem(
                        title = "Quản lý Tác giả",
                        subtitle = "Thêm, sửa, xóa và xác minh thông tin tác giả.",
                        icon = Icons.Default.PersonSearch,
                        onClick = onManageAuthors
                    )
                    ManagementItem(
                        title = "Quản lý Truyện",
                        subtitle = "Đăng truyện mới, duyệt bản dịch song ngữ.",
                        icon = Icons.Default.Description,
                        onClick = onManageStories
                    )
                    ManagementItem(
                        title = "Quản lý Thể loại",
                        subtitle = "Tổ chức truyện theo các chủ đề và bộ sưu tập.",
                        icon = Icons.Default.Category,
                        onClick = onManageCategories
                    )
                    ManagementItem(
                        title = "Quản lý Người dùng",
                        subtitle = "Phân quyền admin và quản lý trạng thái tài khoản.",
                        icon = Icons.Default.ManageAccounts,
                        onClick = onManageUsers
                    )
                }
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hoạt động gần đây", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.fetchRecentActivities() }) {
                        Text("Xem tất cả", style = MaterialTheme.typography.labelLarge, color = Color.Black)
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (recentActivities.isEmpty()) {
                            Text("Chưa có hoạt động nào.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            recentActivities.forEach { activity ->
                                ActivityRow(
                                    activity = activity.action,
                                    target = activity.target,
                                    time = formatTimestamp(activity.timestamp),
                                    status = activity.status,
                                    isSuccess = activity.isSuccess
                                )
                                if (activity != recentActivities.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Vừa xong"
        diff < 3600000 -> "${diff / 60000} phút trước"
        diff < 86400000 -> "${diff / 3600000} giờ trước"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    count: String,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Text(count, style = MaterialTheme.typography.headlineSmall, color = contentColor, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ManagementItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun LabelText(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
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
        "COMPLETED", "HOÀN THÀNH" -> SuccessGreen to SuccessText
        "IN-PROGRESS", "ĐANG TIẾN HÀNH" -> InfoBlue to InfoText
        "HIATUS", "TẠM NGƯNG" -> AlertRed to AlertText
        "ACTIVE", "HOẠT ĐỘNG" -> SuccessGreen to SuccessText
        "INACTIVE", "NGƯNG HOẠT ĐỘNG" -> AlertRed to AlertText
        "VERIFIED", "XÁC MINH" -> InfoBlue to InfoText
        "POSTING", "ĐANG ĐĂNG" -> WarningOrange to WarningText
        "ADMIN" -> DarkGreen to Color.White
        "USER" -> Color.LightGray to Color.DarkGray
        "BANNED" -> Color.Black to Color.White
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isSuccess) Icons.Default.FileUpload else Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSuccess) Color.Black else AlertText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(activity, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Text(target, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        Text(time, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
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

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertText)
            ) {
                Text("Xác nhận", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White
    )
}
