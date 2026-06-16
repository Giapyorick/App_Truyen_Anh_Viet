package com.example.first_project.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.first_project.User
import com.example.first_project.AdminBg
import com.example.first_project.DarkGreen
import com.example.first_project.AlertText
import com.example.first_project.LabelText
import com.example.first_project.TransparentTextField
import com.example.first_project.StatusBadge
import com.example.first_project.FilterButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAdminScreen(
    onBack: () -> Unit,
    viewModel: UserAdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stitch Reader", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedUser = null
                    showDialog = true
                },
                containerColor = DarkGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Users Directory",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterButton("Filter", Icons.Default.FilterList)
                FilterButton("Sort", Icons.AutoMirrored.Filled.Sort)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Search users...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                        onEdit = {
                            selectedUser = user
                            showDialog = true
                        },
                        onDelete = {
                            viewModel.deleteUser(user.id) {
                                Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
    if (showDialog) {
        BackHandler { showDialog = false }
        UserEditDialog(
            user = selectedUser,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onConfirm = { user, password ->
                if (selectedUser == null) {
                    if (password.isNullOrBlank()) {
                        Toast.makeText(context, "Password is required for new users", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addUser(context, user, password) { success, msg ->
                            if (success) {
                                showDialog = false
                                Toast.makeText(context, "User added successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    viewModel.updateUser(user) { success, msg ->
                        if (success) {
                            showDialog = false
                            Toast.makeText(context, "User updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun UserItem(user: User, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.resolveImagePath(),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    StatusBadge(user.status)
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(user.role.uppercase(), style = MaterialTheme.typography.labelSmall, color = if(user.role == "admin") DarkGreen else Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditDialog(
    user: User?,
    viewModel: UserAdminViewModel,
    onDismiss: () -> Unit,
    onConfirm: (User, String?) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(user?.gender ?: "Male") }
    var role by remember { mutableStateOf(user?.role ?: "user") }
    var status by remember { mutableStateOf(user?.status ?: "Active") }
    var imageUrl by remember { mutableStateOf(user?.image ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    val onSave = {
        val finalizeSave: (String) -> Unit = { finalImagePath ->
            val updated = (user?.copy(
                name = name,
                phone = phone,
                email = email,
                gender = gender,
                role = role,
                status = status,
                image = finalImagePath
            ) ?: User(
                name = name,
                phone = phone,
                email = email,
                gender = gender,
                role = role,
                status = status,
                image = finalImagePath
            ))
            onConfirm(updated, if (user == null) password else null)
        }

        if (selectedImageUri != null) {
            isUploading = true
            viewModel.saveImageLocally(context, selectedImageUri!!, onSuccess = { localPath ->
                isUploading = false
                finalizeSave(localPath)
            }, onError = { isUploading = false })
        } else {
            finalizeSave(imageUrl)
        }
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text(if (user == null) "Add New User" else "User Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save", color = Color.White)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = (user?.copy(image = imageUrl) ?: User(image = imageUrl)).resolveImagePath(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(32.dp), tint = Color.LightGray)
                }
            }

            LabelText("FULL NAME")
            TransparentTextField(value = name, onValueChange = { name = it }, placeholder = "Enter name...")

            LabelText("PHONE")
            TransparentTextField(value = phone, onValueChange = { phone = it }, placeholder = "Enter phone number...")

            LabelText("EMAIL")
            TransparentTextField(value = email, onValueChange = { email = it }, placeholder = "Enter email...", enabled = user == null)

            if (user == null) {
                LabelText("PASSWORD")
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter password...", color = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = DarkGreen
                    ),
                    singleLine = true
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LabelText("Gender")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = g }) {
                                RadioButton(selected = gender == g, onClick = { gender = g })
                                Text(g, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LabelText("Role")
                    var expandedRole by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().clickable { expandedRole = true }.padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(role.uppercase())
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                        DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                            listOf("admin", "user").forEach { r ->
                                DropdownMenuItem(text = { Text(r.uppercase()) }, onClick = { role = r; expandedRole = false })
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LabelText("Status")
                    var expandedStatus by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().clickable { expandedStatus = true }.padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(status)
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                            listOf("Active", "Inactive").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expandedStatus = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
