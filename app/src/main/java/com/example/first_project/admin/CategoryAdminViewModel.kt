package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Category
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

class CategoryAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    init {
        fetchCategories()
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

    fun addCategory(category: Category, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ref = db.collection("categories").document()
                val newCategory = category.copy(id = ref.id)
                ref.set(newCategory).await()
                logActivity("Thêm thể loại", newCategory.name)
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Thêm thể loại thất bại", category.name, false)
                onResult(false)
            }
        }
    }

    fun updateCategory(category: Category, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("categories").document(category.id).set(category).await()
                logActivity("Cập nhật thể loại", category.name)
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Cập nhật thể loại thất bại", category.name, false)
                onResult(false)
            }
        }
    }

    fun deleteCategory(categoryId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val categoryName = _categories.value.find { it.id == categoryId }?.name ?: "Unknown"
            try {
                db.collection("categories").document(categoryId).delete().await()
                logActivity("Xóa thể loại", categoryName)
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Xóa thể loại thất bại", categoryName, false)
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
            val sheet = workbook.createSheet("Categories")
            
            val headerRow = sheet.createRow(0)
            val headers = listOf("ID", "Name", "Description", "Status")
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            _categories.value.forEachIndexed { index, category ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(category.id)
                row.createCell(1).setCellValue(category.name)
                row.createCell(2).setCellValue(category.description)
                row.createCell(3).setCellValue(category.status)
            }

            workbook.write(outputStream)
            workbook.close()
            logActivity("Xuất file Excel", "Danh sách thể loại")
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
            val newCategories = mutableListOf<Category>()

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                val name = getCellValue(row, 1)
                if (name.isEmpty()) continue

                val rawId = getCellValue(row, 0)
                val categoryId = if (rawId.isEmpty() || rawId.lowercase() == "null") "" else rawId

                val category = Category(
                    id = categoryId,
                    name = name,
                    description = getCellValue(row, 2),
                    status = getCellValue(row, 3).ifEmpty { "Active" }
                )
                newCategories.add(category)
            }

            if (newCategories.isNotEmpty()) {
                val batch = db.batch()
                newCategories.forEach { category ->
                    val ref = if (category.id.isEmpty()) db.collection("categories").document() else db.collection("categories").document(category.id)
                    val finalCategory = if (category.id.isEmpty()) category.copy(id = ref.id) else category
                    batch.set(ref, finalCategory)
                }
                batch.commit().await()
                logActivity("Nhập file Excel", "${newCategories.size} thể loại")
                fetchCategories()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
