package com.example.first_project.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Chapter
import com.example.first_project.Paragraph
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.apache.poi.ss.usermodel.WorkbookFactory

class ChapterAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

    fun fetchChapters(storyId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("stories").document(storyId)
                    .collection("chapters")
                    .orderBy("chapterNumber")
                    .get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chapter::class.java)?.apply { id = doc.id }
                }
                _chapters.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addChapter(storyId: String, chapter: Chapter, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ref = db.collection("stories").document(storyId).collection("chapters").document()
                val newChapter = chapter.copy(id = ref.id)
                ref.set(newChapter).await()
                syncChaptersToStory(storyId)
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun updateChapter(storyId: String, chapter: Chapter, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(storyId).collection("chapters").document(chapter.id).set(chapter).await()
                syncChaptersToStory(storyId)
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    private suspend fun syncChaptersToStory(storyId: String) {
        try {
            val snapshot = db.collection("stories").document(storyId)
                .collection("chapters")
                .orderBy("chapterNumber")
                .get().await()
            val chaptersList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Chapter::class.java)?.apply { id = doc.id }
            }
            // Đảm bảo dữ liệu được cập nhật vào mảng chapters của Story chính để hiển thị ở trang chi tiết
            db.collection("stories").document(storyId).update("chapters", chaptersList).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteChapter(storyId: String, chapterId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(storyId).collection("chapters").document(chapterId).delete().await()
                syncChaptersToStory(storyId)
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun importChaptersFromJson(storyId: String, jsonString: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, List<Chapter>>>() {}.type
                val root: Map<String, List<Chapter>> = gson.fromJson(jsonString, type)
                val chapterList = root["chapters"] ?: return@launch onResult(false, "Không tìm thấy danh sách chapters")

                val batch = db.batch()
                val chaptersRef = db.collection("stories").document(storyId).collection("chapters")

                chapterList.forEach { chapter ->
                    val docRef = if (chapter.id.isNotEmpty()) chaptersRef.document(chapter.id) else chaptersRef.document()
                    val finalChapter = chapter.copy(id = docRef.id)
                    batch.set(docRef, finalChapter)
                }

                batch.commit().await()
                syncChaptersToStory(storyId)
                fetchChapters(storyId)
                onResult(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun importParagraphsFromExcel(
        context: Context,
        enUri: Uri,
        viUri: Uri?,
        onSuccess: (List<Paragraph>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (viUri == null) {
                    val paragraphs = readSingleExcelFile(context, enUri)
                    if (paragraphs.isEmpty()) onError("Không tìm thấy dữ liệu.") else onSuccess(paragraphs)
                } else {
                    val enList = readExcelColumn(context, enUri)
                    val viList = readExcelColumn(context, viUri)

                    if (enList.size != viList.size) {
                        onError("Số lượng dòng không khớp: Anh (${enList.size}) vs Việt (${viList.size})")
                        return@launch
                    }

                    val paragraphs = enList.zip(viList).mapIndexed { index, pair ->
                        Paragraph(paragraphOrder = index + 1, english = pair.first, vietnamese = pair.second)
                    }
                    onSuccess(paragraphs)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi không xác định")
            }
        }
    }

    fun importParagraphsFromTxt(
        context: Context,
        enUri: Uri,
        viUri: Uri,
        onSuccess: (List<Paragraph>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val enText = readTextFromUri(context, enUri)
                val viText = readTextFromUri(context, viUri)

                val enSentences = splitIntoSentences(enText)
                val viSentences = splitIntoSentences(viText)

                if (enSentences.size != viSentences.size) {
                    onError("Số lượng câu không khớp: Anh (${enSentences.size}) vs Việt (${viSentences.size})")
                    return@launch
                }

                val paragraphs = enSentences.zip(viSentences).mapIndexed { index, pair ->
                    Paragraph(paragraphOrder = index + 1, english = pair.first, vietnamese = pair.second)
                }
                onSuccess(paragraphs)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi không xác định")
            }
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: ""
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Regex này cắt theo . ! ? và giữ lại dấu câu nếu cần, 
        // nhưng đơn giản nhất là cắt và trim.
        // Dùng lookbehind để giữ lại dấu câu: (?<=[.!?])
        val regex = Regex("(?<=[.!?])\\s+")
        return text.split(regex).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun readExcelColumn(context: Context, uri: Uri): List<String> {
        val list = mutableListOf<String>()
        val formatter = org.apache.poi.ss.usermodel.DataFormatter()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Tìm cột nội dung dựa trên header hoặc mặc định là cột B (index 1)
            var contentCol = 1
            val firstRow = sheet.getRow(0)
            if (firstRow != null) {
                for (c in 0 until firstRow.lastCellNum.toInt()) {
                    val headerText = firstRow.getCell(c)?.toString()?.lowercase() ?: ""
                    if (headerText.contains("english") || headerText.contains("vietnamese") || headerText.contains("content")) {
                        contentCol = c
                        break
                    }
                }
            }

            // Đọc từ dòng 1 trở đi để bỏ qua tiêu đề
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val cell = row.getCell(contentCol) ?: row.getCell(0)
                val text = formatter.formatCellValue(cell).trim()
                if (text.isNotEmpty()) {
                    list.add(text)
                }
            }
        }
        return list
    }

    private fun readSingleExcelFile(context: Context, uri: Uri): List<Paragraph> {
        val paragraphs = mutableListOf<Paragraph>()
        val formatter = org.apache.poi.ss.usermodel.DataFormatter()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0) ?: return emptyList()

            var enCol = -1
            var viCol = -1

            for (c in 0 until headerRow.lastCellNum.toInt()) {
                val h = headerRow.getCell(c)?.toString()?.lowercase() ?: ""
                if (h.contains("english")) enCol = c
                if (h.contains("vietnamese")) viCol = c
            }

            // Fallback nếu không tìm thấy tên cột
            if (enCol == -1) enCol = 1
            if (viCol == -1) viCol = 2

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val en = formatter.formatCellValue(row.getCell(enCol)).trim()
                val vi = formatter.formatCellValue(row.getCell(viCol)).trim()
                if (en.isNotEmpty() || vi.isNotEmpty()) {
                    paragraphs.add(Paragraph(
                        paragraphOrder = paragraphs.size + 1,
                        english = en,
                        vietnamese = vi
                    ))
                }
            }
        }
        return paragraphs
    }
}
