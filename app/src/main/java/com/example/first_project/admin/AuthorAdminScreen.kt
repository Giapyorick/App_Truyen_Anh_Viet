package com.example.first_project.admin

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.first_project.Author
import com.example.first_project.AdminBg
import com.example.first_project.DarkGreen
import com.example.first_project.AlertText
import com.example.first_project.LabelText
import com.example.first_project.TransparentTextField
import com.example.first_project.StatusBadge
import com.example.first_project.ConfirmationDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorAdminScreen(
    onBack: () -> Unit,
    viewModel: AuthorAdminViewModel = viewModel()
) {
    val authors by viewModel.authors.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedAuthor by remember { mutableStateOf<Author?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var authorToDelete by remember { mutableStateOf<Author?>(null) }

    val filteredAuthors = authors.filter {
        it.authorName.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.country.contains(searchQuery, ignoreCase = true)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                viewModel.exportToExcel(outputStream)
                            } ?: false
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (success) Toast.makeText(context, "Đã xuất file thành công!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(it)?.use { inputStream ->
                                viewModel.importFromExcel(inputStream)
                            } ?: false
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (success) Toast.makeText(context, "Đã nạp dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quản lý Tác giả", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("authors.xlsx") }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    IconButton(onClick = { importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedAuthor = null
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
                "HỆ THỐNG QUẢN TRỊ",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Danh mục Tác giả",
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
                placeholder = { Text("Tìm theo tên, email, quốc gia...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                items(filteredAuthors) { author ->
                    AuthorItem(
                        author = author,
                        onEdit = {
                            selectedAuthor = author
                            showDialog = true
                        },
                        onDelete = {
                            authorToDelete = author
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        BackHandler { showDialog = false }
        AuthorEditDialog(
            author = selectedAuthor,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onConfirm = { author ->
                if (selectedAuthor == null) {
                    viewModel.addAuthor(author) { showDialog = false }
                } else {
                    viewModel.updateAuthor(author) { showDialog = false }
                }
            }
        )
    }

    if (showDeleteConfirm && authorToDelete != null) {
        ConfirmationDialog(
            title = "Xóa tác giả",
            message = "Bạn có chắc chắn muốn xóa tác giả \"${authorToDelete?.authorName}\"? Thao tác này không thể hoàn tác.",
            onConfirm = {
                viewModel.deleteAuthor(authorToDelete!!.id) {
                    showDeleteConfirm = false
                    authorToDelete = null
                }
            },
            onDismiss = {
                showDeleteConfirm = false
                authorToDelete = null
            }
        )
    }
}

@Composable
fun AuthorItem(author: Author, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = if (author.img.startsWith("/")) java.io.File(author.img) else author.img,
                    contentDescription = null,
                    modifier = Modifier
                        .width(60.dp)
                        .aspectRatio(4 / 6f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            author.authorName,
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                        )
                        StatusBadge(author.status)
                    }
                    Text(author.country, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    IconInfoRow(Icons.Default.MailOutline, author.email)
                    Spacer(modifier = Modifier.height(4.dp))
                    IconInfoRow(Icons.Default.Cake, author.dob)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorEditDialog(
    author: Author?,
    viewModel: AuthorAdminViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Author) -> Unit
) {
    var name by remember { mutableStateOf(author?.authorName ?: "") }
    var email by remember { mutableStateOf(author?.email ?: "") }
    var dob by remember { mutableStateOf(author?.dob ?: "") }
    var gender by remember { mutableStateOf(author?.gender ?: "Male") }
    var country by remember { mutableStateOf(author?.country ?: "") }
    var status by remember { mutableStateOf(author?.status ?: "Active") }
    var imgUrl by remember { mutableStateOf(author?.img ?: "") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    val onSave = {
        if (selectedImageUri != null) {
            isUploading = true
            viewModel.saveImageLocally(context, selectedImageUri!!, onSuccess = { localPath ->
                isUploading = false
                val updated = (author?.copy(authorName = name, email = email, dob = dob, gender = gender, country = country, status = status, img = localPath)
                    ?: Author(authorName = name, email = email, dob = dob, gender = gender, country = country, status = status, img = localPath))
                onConfirm(updated)
            }, onError = { isUploading = false })
        } else {
            val updated = (author?.copy(authorName = name, email = email, dob = dob, gender = gender, country = country, status = status, img = imgUrl)
                ?: Author(authorName = name, email = email, dob = dob, gender = gender, country = country, status = status, img = imgUrl))
            onConfirm(updated)
        }
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết Tác giả", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Lưu thay đổi", color = Color.White)
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (author == null) "Thêm Tác giả mới" else "Chỉnh sửa Tác giả",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )

            LabelText("HỌ VÀ TÊN")
            TransparentTextField(value = name, onValueChange = { name = it }, placeholder = "Nhập tên tác giả...")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 80.dp)
                    .aspectRatio(4 / 6f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (imgUrl.isNotEmpty()) {
                    AsyncImage(model = if (imgUrl.startsWith("/")) java.io.File(imgUrl) else imgUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Text("Thêm ảnh chân dung", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            LabelText("QUỐC GIA")
            TransparentTextField(value = country, onValueChange = { country = it }, placeholder = "Ví dụ: Việt Nam, Mỹ...")

            LabelText("ĐỊA CHỈ EMAIL")
            TransparentTextField(value = email, onValueChange = { email = it }, placeholder = "example@email.com")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("NGÀY SINH")
                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("yyyy/mm/dd") },
                            trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledTrailingIconColor = DarkGreen,
                                disabledContainerColor = Color(0xFFF1F1F1),
                                disabledBorderColor = Color.Transparent,
                                disabledPlaceholderColor = Color.Gray
                            )
                        )
                    }
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    dob = dateFormatter.format(Date(it))
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("TRẠNG THÁI")
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledTrailingIconColor = Color.Gray,
                                disabledContainerColor = Color(0xFFF1F1F1),
                                disabledBorderColor = Color.Transparent
                            )
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Active", "Inactive").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun IconInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
