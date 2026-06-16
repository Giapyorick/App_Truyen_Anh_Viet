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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterAdminScreen(
    storyId: String,
    onBack: () -> Unit,
    viewModel: ChapterAdminViewModel = viewModel()
) {
    val chapters by viewModel.chapters.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }

    LaunchedEffect(storyId) {
        viewModel.fetchChapters(storyId)
    }

    val filteredChapters = chapters.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.chapterNumber.toString().contains(searchQuery)
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
                                viewModel.importFromExcel(storyId, inputStream)
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
                        Text("Quản lý Chương", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("chapters_${storyId}.xlsx") }) {
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
                    selectedChapter = null
                    showEditDialog = true
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
                "MÃ TRUYỆN: $storyId",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Danh sách Chương",
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
                placeholder = { Text("Tìm kiếm chương...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                items(filteredChapters) { chapter ->
                    ChapterItemAdmin(
                        chapter = chapter,
                        onEdit = {
                            selectedChapter = chapter
                            showEditDialog = true
                        },
                        onDelete = {
                            chapterToDelete = chapter
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        BackHandler { showEditDialog = false }
        ChapterEditDialog(
            chapter = selectedChapter,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedChapter ->
                if (selectedChapter == null) {
                    viewModel.addChapter(storyId, updatedChapter) { showEditDialog = false }
                } else {
                    viewModel.updateChapter(storyId, updatedChapter) { showEditDialog = false }
                }
            }
        )
    }

    if (showDeleteConfirm && chapterToDelete != null) {
        ConfirmationDialog(
            title = "Xóa chương",
            message = "Bạn có chắc chắn muốn xóa Chương ${chapterToDelete?.chapterNumber}: ${chapterToDelete?.title}? Thao tác này không thể hoàn tác.",
            onConfirm = {
                viewModel.deleteChapter(storyId, chapterToDelete!!.id) {
                    showDeleteConfirm = false
                    chapterToDelete = null
                }
            },
            onDismiss = {
                showDeleteConfirm = false
                chapterToDelete = null
            }
        )
    }
}

@Composable
fun ChapterItemAdmin(chapter: Chapter, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Chương ${chapter.chapterNumber}: ${chapter.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                )
                Text(chapter.createdDate, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text("${chapter.paragraphs.size} đoạn văn", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
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
fun ChapterEditDialog(
    chapter: Chapter?,
    onDismiss: () -> Unit,
    onConfirm: (Chapter) -> Unit
) {
    var number by remember { mutableStateOf(chapter?.chapterNumber?.toString() ?: "") }
    var title by remember { mutableStateOf(chapter?.title ?: "") }
    var paragraphs by remember { mutableStateOf(chapter?.paragraphs ?: emptyList<Paragraph>()) }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết Chương", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = {
                    Button(
                        onClick = {
                            onConfirm(chapter?.copy(
                                chapterNumber = number.toIntOrNull() ?: 0,
                                title = title,
                                paragraphs = paragraphs
                            ) ?: Chapter(
                                chapterNumber = number.toIntOrNull() ?: 0,
                                title = title,
                                paragraphs = paragraphs
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Lưu chương", color = Color.White)
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
                if (chapter == null) "Tạo Chương mới" else "Chỉnh sửa Chương",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(0.3f)) {
                    LabelText("SỐ CHƯƠNG")
                    TransparentTextField(value = number, onValueChange = { number = it }, placeholder = "VD: 1")
                }
                Column(modifier = Modifier.weight(0.7f)) {
                    LabelText("TIÊU ĐỀ")
                    TransparentTextField(value = title, onValueChange = { title = it }, placeholder = "Nhập tiêu đề chương...")
                }
            }

            LabelText("NỘI DUNG ĐOẠN VĂN")
            
            paragraphs.forEachIndexed { index, p ->
                ParagraphEditItem(
                    paragraph = p,
                    onUpdate = { updatedP ->
                        val newList = paragraphs.toMutableList()
                        newList[index] = updatedP
                        paragraphs = newList
                    },
                    onDelete = {
                        paragraphs = paragraphs.filterIndexed { i, _ -> i != index }
                    }
                )
            }

            Button(
                onClick = {
                    paragraphs = paragraphs + Paragraph(paragraphOrder = paragraphs.size + 1)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm đoạn văn", color = Color.Black)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ParagraphEditItem(paragraph: Paragraph, onUpdate: (Paragraph) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Thứ tự: ${paragraph.paragraphOrder}", style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            
            OutlinedTextField(
                value = paragraph.english,
                onValueChange = { onUpdate(paragraph.copy(english = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tiếng Anh (English)", fontSize = 12.sp) },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreen)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = paragraph.vietnamese,
                onValueChange = { onUpdate(paragraph.copy(vietnamese = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tiếng Việt", fontSize = 12.sp) },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkGreen)
            )
        }
    }
}
