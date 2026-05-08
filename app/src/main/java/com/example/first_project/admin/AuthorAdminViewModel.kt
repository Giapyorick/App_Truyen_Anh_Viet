package com.example.first_project.admin

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Author
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
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

import java.io.File
import java.io.FileOutputStream

class AuthorAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _authors = MutableStateFlow<List<Author>>(emptyList())
    val authors: StateFlow<List<Author>> = _authors

    init {
        fetchAuthors()
    }

    fun saveImageLocally(context: Context, uri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                // Tạo tên file duy nhất
                val fileName = "author_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                
                // Copy dữ liệu từ Uri vào file cục bộ
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
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

    fun addAuthor(author: Author, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ref = db.collection("authors").document()
                val newAuthor = author.copy(id = ref.id)
                ref.set(newAuthor).await()
                fetchAuthors()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateAuthor(author: Author, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("authors").document(author.id).set(author).await()
                fetchAuthors()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteAuthor(authorId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("authors").document(authorId).delete().await()
                fetchAuthors()
                onResult(true)
            } catch (e: Exception) {
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
            val sheet = workbook.createSheet("Authors")
            val headerRow = sheet.createRow(0)
            val headers = listOf("ID", "Name", "Email", "DOB", "Gender", "Country", "Status", "Image")
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            _authors.value.forEachIndexed { index, author ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(author.id)
                row.createCell(1).setCellValue(author.authorName)
                row.createCell(2).setCellValue(author.email)
                row.createCell(3).setCellValue(author.dob)
                row.createCell(4).setCellValue(author.gender)
                row.createCell(5).setCellValue(author.country)
                row.createCell(6).setCellValue(author.status)
                row.createCell(7).setCellValue(author.img)
            }

            workbook.write(outputStream)
            workbook.close()
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
            val newAuthors = mutableListOf<Author>()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                val name = getCellValue(row, 1)
                if (name.isEmpty()) continue // Bỏ qua dòng trống

                // Đọc ID, nếu là "null" hoặc trống thì coi như không có ID
                val rawId = getCellValue(row, 0)
                val authorId = if (rawId.isEmpty() || rawId.lowercase() == "null") "" else rawId

                val author = Author(
                    id = authorId,
                    authorName = name,
                    email = getCellValue(row, 2),
                    dob = getCellValue(row, 3),
                    gender = getCellValue(row, 4),
                    country = getCellValue(row, 5),
                    status = getCellValue(row, 6).ifEmpty { "Active" },
                    img = getCellValue(row, 7)
                )
                newAuthors.add(author)
            }

            if (newAuthors.isNotEmpty()) {
                val batch = db.batch()
                newAuthors.forEach { author ->
                    val ref = if (author.id.isEmpty()) db.collection("authors").document() else db.collection("authors").document(author.id)
                    val finalAuthor = if (author.id.isEmpty()) author.copy(id = ref.id) else author
                    batch.set(ref, finalAuthor)
                }
                batch.commit().await()
                fetchAuthors()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
