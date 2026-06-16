package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Author
import com.example.first_project.Category
import com.example.first_project.Story
import com.example.first_project.Activity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class StoryAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _authors = MutableStateFlow<List<Author>>(emptyList())
    val authors: StateFlow<List<Author>> = _authors

    init {
        fetchStories()
        fetchCategories()
        fetchAuthors()
    }

    private fun logActivity(action: String, target: String, isSuccess: Boolean = true) {
        viewModelScope.launch {
            try {
                val ref = db.collection("activities").document()
                val activity = Activity(
                    id = ref.id,
                    action = action,
                    target = target,
                    timestamp = System.currentTimeMillis(),
                    status = if (isSuccess) "SUCCESS" else "ALERT",
                    isSuccess = isSuccess
                )
                ref.set(activity).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchStories() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("stories").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Story::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        val data = doc.data
                        if (data != null) {
                            Story(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                authorId = data["authorId"] as? String ?: "",
                                description = data["description"] as? String ?: "",
                                status = data["status"] as? String ?: "",
                                img = data["img"] as? String ?: "",
                                likes = (data["likes"] as? Long)?.toInt() ?: 0,
                                rate = (data["rate"] as? Number)?.toDouble() ?: 0.0,
                                count_follower = (data["count_follower"] as? Long)?.toInt() ?: 0,
                                count_rate = (data["count_rate"] as? Long)?.toInt() ?: 0,
                                categoryIds = (data["categoryIds"] as? List<*>)?.filterNotNull() ?: emptyList(),
                                publicationDate = data["publicationDate"] as? String ?: "",
                                lastChapterId = data["lastChapterId"] as? String,
                                lastChapterNumber = (data["lastChapterNumber"] as? Long)?.toInt(),
                                lastReadTime = data["lastReadTime"] as? Long,
                                scrollIndex = (data["scrollIndex"] as? Long)?.toInt() ?: 0
                            )
                        } else null
                    }
                }
                _stories.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("categories").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                }
                _categories.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAuthors() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("authors").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Author::class.java)?.copy(id = doc.id)
                }
                _authors.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveImageLocally(context: android.content.Context, uri: android.net.Uri, onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val fileName = "story_${UUID.randomUUID()}.jpg"
                val file = java.io.File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }

    fun addStory(story: Story, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ref = db.collection("stories").document()
                val newStory = story.copy(id = ref.id)
                ref.set(newStory).await()
                logActivity("Đăng truyện mới", newStory.title)
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Đăng truyện thất bại", story.title, false)
                onResult(false)
            }
        }
    }

    fun updateStory(story: Story, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(story.id).set(story).await()
                logActivity("Cập nhật truyện", story.title)
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Cập nhật truyện thất bại", story.title, false)
                onResult(false)
            }
        }
    }

    fun deleteStory(storyId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val storyTitle = _stories.value.find { it.id == storyId }?.title ?: "Unknown"
            try {
                val chapters = db.collection("stories").document(storyId).collection("chapters").get().await()
                if (!chapters.isEmpty) {
                    val batch = db.batch()
                    chapters.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                db.collection("stories").document(storyId).delete().await()
                logActivity("Xóa truyện", storyTitle)
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Xóa truyện thất bại", storyTitle, false)
                onResult(false)
            }
        }
    }

    private fun getCellValue(row: org.apache.poi.ss.usermodel.Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    val value = cell.numericCellValue
                    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim()
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }

    suspend fun exportToExcel(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Stories")
            
            val headerRow = sheet.createRow(0)
            val headers = listOf("ID", "Title", "AuthorID", "Description", "Status", "Image", "CategoryIDs", "PubDate")
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            _stories.value.forEachIndexed { index, story ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(story.id)
                row.createCell(1).setCellValue(story.title)
                row.createCell(2).setCellValue(story.authorId)
                row.createCell(3).setCellValue(story.description)
                row.createCell(4).setCellValue(story.status)
                row.createCell(5).setCellValue(story.img)
                row.createCell(6).setCellValue(story.getCategoryIdsStrings().joinToString(","))
                row.createCell(7).setCellValue(story.publicationDate)
            }

            workbook.write(outputStream)
            workbook.close()
            logActivity("Xuất file Excel", "Danh mục truyện")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromExcel(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val newStories = mutableListOf<Story>()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                val title = getCellValue(row, 1)
                if (title.isEmpty()) continue

                val rawId = getCellValue(row, 0)
                val storyId = if (rawId.isEmpty() || rawId.lowercase() == "null") "" else rawId

                val catIdsRaw = getCellValue(row, 6)
                val catIds = if (catIdsRaw.isEmpty()) emptyList<String>() else catIdsRaw.split(",").map { it.trim() }

                val story = Story(
                    id = storyId,
                    title = title,
                    authorId = getCellValue(row, 2),
                    description = getCellValue(row, 3),
                    status = getCellValue(row, 4).ifEmpty { "In-progress" },
                    img = getCellValue(row, 5),
                    categoryIds = catIds,
                    publicationDate = getCellValue(row, 7)
                )
                newStories.add(story)
            }

            if (newStories.isNotEmpty()) {
                val batch = db.batch()
                newStories.forEach { story ->
                    val ref = if (story.id.isEmpty()) db.collection("stories").document() else db.collection("stories").document(story.id)
                    val finalStory = if (story.id.isEmpty()) story.copy(id = ref.id) else story
                    batch.set(ref, finalStory)
                }
                batch.commit().await()
                logActivity("Nhập file Excel", "${newStories.size} truyện")
                fetchStories()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
