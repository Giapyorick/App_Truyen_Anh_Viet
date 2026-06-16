package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Chapter
import com.example.first_project.Paragraph
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChapterAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

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

    fun fetchChapters(storyId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("stories").document(storyId)
                    .collection("chapters").orderBy("chapterNumber").get().await()
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
                val newChapter = chapter.copy(id = ref.id, createdDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()))
                ref.set(newChapter).await()
                
                // Update story's last chapter info
                db.collection("stories").document(storyId).update(
                    "lastChapterId", ref.id,
                    "lastChapterNumber", newChapter.chapterNumber
                )
                
                logActivity("Thêm chương mới", "Chương ${newChapter.chapterNumber} (ID: $storyId)")
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                logActivity("Thêm chương thất bại", "Truyện ID: $storyId", false)
                onResult(false)
            }
        }
    }

    fun updateChapter(storyId: String, chapter: Chapter, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(storyId).collection("chapters").document(chapter.id).set(chapter).await()
                logActivity("Cập nhật chương", "Chương ${chapter.chapterNumber} (ID: $storyId)")
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                logActivity("Cập nhật chương thất bại", "Chương ${chapter.chapterNumber}", false)
                onResult(false)
            }
        }
    }

    fun deleteChapter(storyId: String, chapterId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val chapterNum = _chapters.value.find { it.id == chapterId }?.chapterNumber ?: 0
            try {
                db.collection("stories").document(storyId).collection("chapters").document(chapterId).delete().await()
                logActivity("Xóa chương", "Chương $chapterNum (ID: $storyId)")
                fetchChapters(storyId)
                onResult(true)
            } catch (e: Exception) {
                logActivity("Xóa chương thất bại", "Chương $chapterNum", false)
                onResult(false)
            }
        }
    }

    private fun getCellValue(row: org.apache.poi.ss.usermodel.Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                val value = cell.numericCellValue
                if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
            }
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim()
            else -> ""
        }
    }

    suspend fun exportToExcel(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Chapters")
            
            val headerRow = sheet.createRow(0)
            val headers = listOf("ChapterNumber", "Title", "ParagraphOrder", "English", "Vietnamese")
            headers.forEachIndexed { index, title -> headerRow.createCell(index).setCellValue(title) }

            var currentRow = 1
            _chapters.value.forEach { chapter ->
                if (chapter.paragraphs.isEmpty()) {
                    val row = sheet.createRow(currentRow++)
                    row.createCell(0).setCellValue(chapter.chapterNumber.toDouble())
                    row.createCell(1).setCellValue(chapter.title)
                } else {
                    chapter.paragraphs.forEach { p ->
                        val row = sheet.createRow(currentRow++)
                        row.createCell(0).setCellValue(chapter.chapterNumber.toDouble())
                        row.createCell(1).setCellValue(chapter.title)
                        row.createCell(2).setCellValue(p.paragraphOrder.toDouble())
                        row.createCell(3).setCellValue(p.english)
                        row.createCell(4).setCellValue(p.vietnamese)
                    }
                }
            }

            workbook.write(outputStream)
            workbook.close()
            logActivity("Xuất file Excel", "Danh sách chương")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromExcel(storyId: String, inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            
            val chapterData = mutableMapOf<Int, Pair<String, MutableList<Paragraph>>>()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val chapNum = getCellValue(row, 0).toDoubleOrNull()?.toInt() ?: continue
                val chapTitle = getCellValue(row, 1)
                val pOrder = getCellValue(row, 2).toDoubleOrNull()?.toInt() ?: 0
                val english = getCellValue(row, 3)
                val vietnamese = getCellValue(row, 4)

                val pair = chapterData.getOrPut(chapNum) { chapTitle to mutableListOf() }
                if (english.isNotEmpty() || vietnamese.isNotEmpty()) {
                    pair.second.add(Paragraph(pOrder, english, vietnamese))
                }
            }

            if (chapterData.isNotEmpty()) {
                val batch = db.batch()
                chapterData.forEach { (num, pair) ->
                    val ref = db.collection("stories").document(storyId).collection("chapters").document()
                    val chapter = Chapter(
                        id = ref.id,
                        chapterNumber = num,
                        title = pair.first,
                        createdDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()),
                        paragraphs = pair.second.sortedBy { it.paragraphOrder }
                    )
                    batch.set(ref, chapter)
                }
                batch.commit().await()
                logActivity("Nhập file Excel", "${chapterData.size} chương (ID: $storyId)")
                fetchChapters(storyId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
