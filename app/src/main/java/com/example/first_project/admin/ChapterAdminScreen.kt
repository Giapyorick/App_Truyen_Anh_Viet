package com.example.first_project.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.first_project.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterAdminScreen(
    storyId: String,
    onBack: () -> Unit,
    viewModel: ChapterAdminViewModel = viewModel()
) {
    val chapters by viewModel.chapters.collectAsState()
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredChapters = chapters.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.chapterNumber.toString().contains(searchQuery)
    }

    LaunchedEffect(storyId) {
        viewModel.fetchChapters(storyId)
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stitch Reader", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJsonDialog = true }) {
                        Icon(Icons.Default.Code, contentDescription = "Import JSON", tint = DarkGreen)
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
                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
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
                "Chapters Directory",
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
                placeholder = { Text("Search chapters...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                    ChapterAdminItem(
                        chapter = chapter,
                        onEdit = {
                            selectedChapter = chapter
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteChapter(storyId, chapter.id) {}
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        ChapterEditDialog(
            chapter = selectedChapter,
            onDismiss = { showEditDialog = false },
            onConfirm = { chapter ->
                if (selectedChapter == null) {
                    viewModel.addChapter(storyId, chapter) { showEditDialog = false }
                } else {
                    viewModel.updateChapter(storyId, chapter) { showEditDialog = false }
                }
            },
            viewModel = viewModel
        )
    }

    if (showJsonDialog) {
        JsonImportDialog(
            onDismiss = { showJsonDialog = false },
            onConfirm = { json ->
                viewModel.importChaptersFromJson(storyId, json) { success, error ->
                    if (success) {
                        showJsonDialog = false
                    } else {
                        android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun JsonImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Chapters from JSON") },
        text = {
            Column {
                Text("Paste your JSON structure below:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    placeholder = { Text("{ \"chapters\": [ ... ] }") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(jsonText) },
                enabled = jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChapterAdminItem(chapter: Chapter, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F1F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chapter.chapterNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "${chapter.paragraphs.size} paragraphs • Updated recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditDialog(
    chapter: Chapter?,
    onDismiss: () -> Unit,
    onConfirm: (Chapter) -> Unit,
    viewModel: ChapterAdminViewModel
) {
    var title by remember { mutableStateOf(chapter?.title ?: "") }
    var chapterNumber by remember { mutableStateOf(chapter?.chapterNumber?.toString() ?: "") }
    var paragraphs by remember { mutableStateOf(chapter?.paragraphs ?: emptyList()) }

    val context = LocalContext.current
    var enUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var viUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val enPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { enUri = it }
    val viPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viUri = it }

    val onSave = {
        val newChapter = (chapter?.copy(
            title = title,
            chapterNumber = chapterNumber.toIntOrNull() ?: 0,
            paragraphs = paragraphs
        ) ?: Chapter(
            title = title,
            chapterNumber = chapterNumber.toIntOrNull() ?: 0,
            createdDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
            paragraphs = paragraphs
        ))
        onConfirm(newChapter)
    }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TopAppBar(
                title = { Text("Stitch Reader", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = {
                    TextButton(onClick = {}) { Text("Draft", color = Color.Gray) }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Save Chapter", color = Color.White)
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
                if (chapter == null) "Create Chapter Details" else "Edit Chapter Details",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Configure the bilingual content and metadata for this chapter.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            LabelText("CHAPTER TITLE")
            TransparentTextField(value = title, onValueChange = { title = it }, placeholder = "Enter chapter title...")

            LabelText("CHAPTER NUMBER")
            TransparentTextField(value = chapterNumber, onValueChange = { chapterNumber = it }, placeholder = "e.g. 1, 2, 3...")

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        LabelText("Bulk Import (Excel)")
                    }

                    Text(
                        "Select English and Vietnamese spreadsheets to pair paragraphs automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { enPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (enUri != null) SuccessGreen else Color.White,
                                contentColor = if (enUri != null) SuccessText else Color.Black
                            ),
                            border = BorderStroke(1.dp, if (enUri != null) SuccessText else Color.LightGray)
                        ) {
                            Text(if (enUri != null) "EN Ready" else "Select EN", fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { viPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viUri != null) SuccessGreen else Color.White,
                                contentColor = if (viUri != null) SuccessText else Color.Black
                            ),
                            border = BorderStroke(1.dp, if (viUri != null) SuccessText else Color.LightGray)
                        ) {
                            Text(if (viUri != null) "VI Ready" else "Select VI", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (enUri != null && viUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.importParagraphsFromExcel(context, enUri!!, viUri!!,
                                    onSuccess = {
                                        paragraphs = it
                                        isProcessing = false
                                        enUri = null
                                        viUri = null
                                    },
                                    onError = { error ->
                                        isProcessing = false
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                            shape = RoundedCornerShape(4.dp),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            else Text("Process & Pair Rows")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paragraphs (${paragraphs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    paragraphs = paragraphs + Paragraph(paragraphOrder = paragraphs.size + 1, english = "", vietnamese = "")
                }) {
                    Icon(Icons.Default.Add, modifier = Modifier.size(16.dp), contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual", color = DarkGreen)
                }
            }

            paragraphs.forEachIndexed { index, p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = DarkGreen,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "P${p.paragraphOrder}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (index > 0) {
                                    paragraphs = paragraphs.toMutableList().apply {
                                        val temp = this[index]
                                        this[index] = this[index - 1]
                                        this[index - 1] = temp
                                    }.mapIndexed { idx, item -> item.copy(paragraphOrder = idx + 1) }
                                }
                            }, enabled = index > 0) {
                                Icon(Icons.Default.ArrowUpward, null, tint = if (index > 0) DarkGreen else Color.Gray, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                if (index < paragraphs.size - 1) {
                                    paragraphs = paragraphs.toMutableList().apply {
                                        val temp = this[index]
                                        this[index] = this[index + 1]
                                        this[index + 1] = temp
                                    }.mapIndexed { idx, item -> item.copy(paragraphOrder = idx + 1) }
                                }
                            }, enabled = index < paragraphs.size - 1) {
                                Icon(Icons.Default.ArrowDownward, null, tint = if (index < paragraphs.size - 1) DarkGreen else Color.Gray, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                paragraphs = paragraphs.toMutableList().apply { removeAt(index) }
                                    .mapIndexed { idx, item -> item.copy(paragraphOrder = idx + 1) }
                            }) {
                                Icon(Icons.Default.DeleteOutline, null, tint = AlertText, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text("English", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        TransparentTextField(
                            value = p.english,
                            onValueChange = { newVal ->
                                paragraphs = paragraphs.toMutableList().apply {
                                    this[index] = this[index].copy(english = newVal)
                                }
                            },
                            placeholder = "English text..."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Vietnamese", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        TransparentTextField(
                            value = p.vietnamese,
                            onValueChange = { newVal ->
                                paragraphs = paragraphs.toMutableList().apply {
                                    this[index] = this[index].copy(vietnamese = newVal)
                                }
                            },
                            placeholder = "Vietnamese text..."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Discard Changes", color = Color.Gray, modifier = Modifier.clickable { onDismiss() })
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.6f)
                ) {
                    Text("Confirm Changes")
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
