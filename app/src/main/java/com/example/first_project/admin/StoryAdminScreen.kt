package com.example.first_project.admin

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
import com.example.first_project.SuccessGreen
import com.example.first_project.SuccessText
import com.example.first_project.AlertRed
import com.example.first_project.InfoBlue
import com.example.first_project.InfoText
import com.example.first_project.LabelText
import com.example.first_project.TransparentTextField
import com.example.first_project.StatusBadge
import com.example.first_project.FilterButton
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryAdminScreen(
    onBack: () -> Unit,
    onManageChapters: (String) -> Unit,
    viewModel: StoryAdminViewModel = viewModel()
) {
    val stories by viewModel.stories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }




    var selectedStory by remember { mutableStateOf<Story?>(null) }




    var searchQuery by remember { mutableStateOf("") }





    val filteredStories = stories.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
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
                "MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Stories Directory",
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
                placeholder = { Text("Search stories...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                            viewModel.deleteStory(story.id) { }
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
}










@Composable
fun StoryItem(story: Story, onEdit: () -> Unit, onDelete: () -> Unit, onManageChapters: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = story.resolveImagePath(),
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

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { onManageChapters() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Chapters", tint = DarkGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                    categoryIdsStrings = selectedCategoryIds.distinct(),
                    img = localPath
                ) ?: Story(
                    title = title,
                    description = description,
                    publicationDate = publicationDate,
                    status = status,
                    authorId = authorId,
                    categoryIds = selectedCategoryIds.distinct(),
                    categoryIdsStrings = selectedCategoryIds.distinct(),
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
                categoryIdsStrings = selectedCategoryIds.distinct(),
                img = imgUrl
            ) ?: Story(
                title = title,
                description = description,
                publicationDate = publicationDate,
                status = status,
                authorId = authorId,
                categoryIds = selectedCategoryIds.distinct(),
                categoryIdsStrings = selectedCategoryIds.distinct(),
                img = imgUrl
            ))
            onConfirm(updated)
        }
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
                        shape = RoundedCornerShape(4.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save Story", color = Color.White)
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
                if (story == null) "Create Story Details" else "Edit Story Details",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Configure the metadata and literary taxonomy for this bilingual entry.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            LabelText("TITLE")
            TransparentTextField(value = title, onValueChange = { title = it }, placeholder = "Enter story title in English or Vietnamese...")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
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
                    AsyncImage(
                        model = (story?.copy(img = imgUrl) ?: Story(img = imgUrl)).resolveImagePath(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Text("Add Story Cover", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            LabelText("DESCRIPTION")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Provide a brief synopsis of the work...", color = Color.LightGray) },
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = DarkGreen
                )
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        LabelText("Publication Date")
                    }




                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        TransparentTextField(
                            value = publicationDate,
                            onValueChange = { },
                            placeholder = "yyyy/mm/dd",
                            enabled = false
                        )
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
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }




                    }




                ) {
                    DatePicker(state = datePickerState)
                }




            }





            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        LabelText("Status")
                    }




                    var expanded by remember { mutableStateOf(false) }




                    Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(status)
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }




                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("In-progress", "Completed", "Hiatus", "Draft").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        LabelText("Category selection")
                    }




                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chunks = categories.chunked(2) // Display 2 categories per row for stability
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { cat ->
                                    val catId = cat.id
                                    val isSelected = selectedCategoryIds.map { it.toString() }.contains(catId.toString())
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = isSelected,
                                        onClick = {
                                            val currentList = selectedCategoryIds.map { it.toString() }




                                            selectedCategoryIds = if (isSelected) {
                                                currentList.filter { it != catId.toString() }




                                            } else {
                                                currentList + catId.toString()
                                            }




                                        },
                                        label = {
                                            Text(
                                                cat.name,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DarkGreen,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }




                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }




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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp), tint = DarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        LabelText("Author assignment")
                    }





                    var expanded by remember { mutableStateOf(false) }




                    val currentAuthor = authors.find { it.id == authorId }





                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (currentAuthor != null) {
                                    AsyncImage(
                                        model = currentAuthor.resolveImagePath(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(currentAuthor.authorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(currentAuthor.country, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }




                                } else {
                                    Text("Select Author", modifier = Modifier.weight(1f), color = Color.Gray)
                                }




                                Icon(Icons.Default.SwapHoriz, null, tint = Color.Gray)
                            }




                        }





                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            authors.forEach { author ->
                                DropdownMenuItem(
                                    text = { Text(author.authorName) },
                                    onClick = {
                                        authorId = author.id
                                        expanded = false
                                    }




                                )
                            }




                        }




                    }




                }




            }





            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Delete Draft", color = Color.Gray, modifier = Modifier.clickable { onDismiss() })
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.6f),
                    enabled = !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Confirm Changes")
                }




            }




            Spacer(modifier = Modifier.height(40.dp))
        }




    }




}




