package com.example.first_project.admin

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.first_project.Author
import com.example.first_project.Category
import com.example.first_project.Story
import com.example.first_project.AdminBg
import com.example.first_project.DarkGreen
import com.example.first_project.AlertText
import com.example.first_project.LabelText
import com.example.first_project.TransparentTextField
import com.example.first_project.StatusBadge
import com.example.first_project.ConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryAdminScreen(
    onBack: () -> Unit,
    onManageChapters: (String) -> Unit,
    viewModel: StoryAdminViewModel = viewModel()
) {
    val stories by viewModel.stories.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var storyToDelete by remember { mutableStateOf<Story?>(null) }

    val filteredStories = stories.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
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
                        Text("Quản lý Truyện", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("stories.xlsx") }) {
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
                    selectedStory = null
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
                "Danh mục Truyện",
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
                placeholder = { Text("Tìm kiếm truyện...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                items(filteredStories) { story ->
                    StoryItem(
                        story = story,
                        onEdit = {
                            selectedStory = story
                            showDialog = true
                        },
                        onDelete = {
                            storyToDelete = story
                            showDeleteConfirm = true
                        },
                        onManageChapters = { onManageChapters(story.id) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        BackHandler { showDialog = false }
        StoryEditDialog(
            story = selectedStory,
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            onConfirm = { story ->
                if (selectedStory == null) {
                    viewModel.addStory(story) { showDialog = false }
                } else {
                    viewModel.updateStory(story) { showDialog = false }
                }
            }
        )
    }

    if (showDeleteConfirm && storyToDelete != null) {
        ConfirmationDialog(
            title = "Xóa truyện",
            message = "Bạn có chắc chắn muốn xóa truyện \"${storyToDelete?.title}\"? Tất cả các chương của truyện này cũng sẽ bị xóa.",
            onConfirm = {
                viewModel.deleteStory(storyToDelete!!.id) {
                    showDeleteConfirm = false
                    storyToDelete = null
                }
            },
            onDismiss = {
                showDeleteConfirm = false
                storyToDelete = null
            }
        )
    }
}

@Composable
fun StoryItem(story: Story, onEdit: () -> Unit, onDelete: () -> Unit, onManageChapters: () -> Unit) {
    val imagePath = if (story.img.startsWith("assets/")) {
        "file:///android_asset/${story.img.removePrefix("assets/")}"
    } else if (story.img.startsWith("/")) {
        java.io.File(story.img)
    } else {
        story.img
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(2 / 3f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        story.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    StatusBadge(story.status)
                }

                Text(story.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onManageChapters) {
                        Text("Quản lý Chương", style = MaterialTheme.typography.bodySmall, color = DarkGreen)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditDialog(
    story: Story?,
    viewModel: StoryAdminViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Story) -> Unit
) {
    var title by remember { mutableStateOf(story?.title ?: "") }
    var description by remember { mutableStateOf(story?.description ?: "") }
    var publicationDate by remember { mutableStateOf(story?.publicationDate ?: "") }
    var status by remember { mutableStateOf(story?.status ?: "In-progress") }
    var authorId by remember { mutableStateOf(story?.authorId ?: "") }
    var imgUrl by remember { mutableStateOf(story?.img ?: "") }
    var selectedCategoryIds by remember { mutableStateOf(story?.categoryIds?.map { it.toString() } ?: emptyList<String>()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    
    val categories by viewModel.categories.collectAsState()
    val authors by viewModel.authors.collectAsState()
    
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    val onSave = {
        if (selectedImageUri != null) {
            isUploading = true
            viewModel.saveImageLocally(context, selectedImageUri!!, onSuccess = { localPath ->
                isUploading = false
                val updated = (story?.copy(
                    title = title,
                    description = description,
                    publicationDate = publicationDate,
                    status = status,
                    authorId = authorId,
                    categoryIds = selectedCategoryIds.distinct(),
                    img = localPath
                ) ?: Story(
                    title = title,
                    description = description,
                    publicationDate = publicationDate,
                    status = status,
                    authorId = authorId,
                    categoryIds = selectedCategoryIds.distinct(),
                    img = localPath
                ))
                onConfirm(updated)
            }, onError = { isUploading = false })
        } else {
            val updated = (story?.copy(
                title = title,
                description = description,
                publicationDate = publicationDate,
                status = status,
                authorId = authorId,
                categoryIds = selectedCategoryIds.distinct(),
                img = imgUrl
            ) ?: Story(
                title = title,
                description = description,
                publicationDate = publicationDate,
                status = status,
                authorId = authorId,
                categoryIds = selectedCategoryIds.distinct(),
                img = imgUrl
            ))
            onConfirm(updated)
        }
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết Truyện", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (story == null) "Tạo Truyện mới" else "Chỉnh sửa Truyện",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )

            LabelText("TIÊU ĐỀ")
            TransparentTextField(value = title, onValueChange = { title = it }, placeholder = "Nhập tiêu đề truyện...")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 80.dp)
                    .aspectRatio(2 / 3f)
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
                        Text("Thêm ảnh bìa", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            LabelText("MÔ TẢ")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = DarkGreen
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("NGÀY XUẤT BẢN")
                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = publicationDate,
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledContainerColor = Color(0xFFF1F1F1),
                                disabledBorderColor = Color.Transparent
                            )
                        )
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
                                disabledContainerColor = Color(0xFFF1F1F1),
                                disabledBorderColor = Color.Transparent
                            )
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("In-progress", "Completed", "Hiatus").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
                            }
                        }
                    }
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                publicationDate = dateFormatter.format(Date(it))
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
                ) { DatePicker(state = datePickerState) }
            }

            LabelText("TÁC GIẢ")
            var authorExpanded by remember { mutableStateOf(false) }
            val currentAuthor = authors.find { it.id == authorId }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentAuthor?.authorName ?: "Chọn Tác giả",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { authorExpanded = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (currentAuthor != null) Color.Black else Color.Gray,
                        disabledContainerColor = Color(0xFFF1F1F1),
                        disabledBorderColor = Color.Transparent
                    )
                )
                DropdownMenu(expanded = authorExpanded, onDismissRequest = { authorExpanded = false }) {
                    authors.forEach { author ->
                        DropdownMenuItem(text = { Text(author.authorName) }, onClick = { authorId = author.id; authorExpanded = false })
                    }
                }
            }

            LabelText("THỂ LOẠI")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryIds.contains(cat.id),
                                onClick = {
                                    selectedCategoryIds = if (selectedCategoryIds.contains(cat.id)) {
                                        selectedCategoryIds - cat.id
                                    } else {
                                        selectedCategoryIds + cat.id
                                    }
                                },
                                label = { Text(cat.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
