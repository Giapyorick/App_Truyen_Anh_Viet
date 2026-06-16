package com.example.first_project.admin

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.first_project.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAdminScreen(
    onBack: () -> Unit,
    viewModel: UserAdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    val filteredUsers = users.filter {
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quản lý Người dùng", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "HỆ THỐNG QUẢN TRỊ",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Danh sách Người dùng",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Tìm theo email hoặc tên...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = DarkGreen
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredUsers) { user ->
                    UserItem(
                        user = user,
                        onRoleChange = { newRole ->
                            viewModel.updateUserRole(user.uid, newRole) { success ->
                                if (success) Toast.makeText(context, "Đã cập nhật vai trò", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStatusChange = { newStatus ->
                            viewModel.updateUserStatus(user.uid, newStatus) { success ->
                                if (success) Toast.makeText(context, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            userToDelete = user
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && userToDelete != null) {
        ConfirmationDialog(
            title = "Xóa người dùng",
            message = "Bạn có chắc chắn muốn xóa người dùng \"${userToDelete?.email}\"? Lưu ý: Thao tác này chỉ xóa thông tin trong cơ sở dữ liệu, không xóa tài khoản đăng nhập.",
            onConfirm = {
                viewModel.deleteUser(userToDelete!!.uid) {
                    showDeleteConfirm = false
                    userToDelete = null
                }
            },
            onDismiss = {
                showDeleteConfirm = false
                userToDelete = null
            }
        )
    }
}

@Composable
fun UserItem(
    user: User,
    onRoleChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (user.displayName.isNotEmpty()) user.displayName else "Người dùng mới",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(user.role)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(user.status)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Role Change button
                    TextButton(
                        onClick = { onRoleChange(if (user.role == "admin") "user" else "admin") },
                        colors = ButtonDefaults.textButtonColors(contentColor = DarkGreen)
                    ) {
                        Text(if (user.role == "admin") "Hạ quyền User" else "Nâng quyền Admin", fontSize = 12.sp)
                    }
                    
                    // Status Change button
                    TextButton(
                        onClick = { onStatusChange(if (user.status == "Active") "Banned" else "Active") },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (user.status == "Active") AlertText else InfoText)
                    ) {
                        Text(if (user.status == "Active") "Chặn" else "Bỏ chặn", fontSize = 12.sp)
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
