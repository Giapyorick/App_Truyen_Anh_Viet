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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.first_project.Author
import com.example.first_project.AdminBg
import com.example.first_project.DarkGreen
import com.example.first_project.SuccessGreen
import com.example.first_project.SuccessText
import com.example.first_project.AlertRed
import com.example.first_project.AlertText
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Colors from AdminDashboard
// Using colors from AdminScreen.kt

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
                    if (success) Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
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
                    if (success) Toast.makeText(context, "Imported successfully!", Toast.LENGTH_SHORT).show()
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
                        Text("Stitch Reader", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
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
        },
//        bottomBar = {
//            NavigationBar(containerColor = Color.White) {
//                NavigationBarItem(icon = { Icon(Icons.Default.Explore, null) }, label = { Text("Explore") }, selected = false, onClick = {})
//                NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) }, label = { Text("Library") }, selected = false, onClick = {})
//                NavigationBarItem(icon = { Icon(Icons.Default.Translate, null) }, label = { Text("Words") }, selected = false, onClick = {})
//                NavigationBarItem(icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("Account") }, selected = true, onClick = {})
//                NavigationBarItem(icon = { Icon(Icons.Default.Stars, null) }, label = { Text("Premium") }, selected = false, onClick = {})
//            }
//        }
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
                "Authors Directory",
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
                placeholder = { Text("Search by name, email, or country...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
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
                            viewModel.deleteAuthor(author.id) { }
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
}

@Preview(showBackground = true)
@Composable
fun AuthorAdminScreenPreview() {
    MaterialTheme {
        AuthorAdminScreen(onBack = {})
    }
}

@Composable
fun AuthorItem(author: Author, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = author.resolveImagePath(),
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




@Composable
fun IconInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                        else Text("Save Author", color = Color.White)
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
                if (author == null) "Create Author Details" else "Edit Author Details",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Configure the metadata and literary taxonomy for this entry.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            LabelText("FULL NAME")
            TransparentTextField(value = name, onValueChange = { name = it }, placeholder = "Enter author full name...")

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
                    AsyncImage(
                        model = (author?.copy(img = imgUrl) ?: Author(img = imgUrl)).resolveImagePath(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Text("Add Profile Photo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            LabelText("COUNTRY")
            TransparentTextField(value = country, onValueChange = { country = it }, placeholder = "e.g. Vietnam, UK, Japan...")

            LabelText("EMAIL ADDRESS")
            TransparentTextField(value = email, onValueChange = { email = it }, placeholder = "author@stitch.edu")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("BIRTH DATE")
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
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    LabelText("STATUS")
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
                            listOf("Active", "On Leave", "Inactive").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
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
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
