package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Category
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CategoryAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    init {
        fetchCategories()
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
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateCategory(category: Category, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("categories").document(category.id).set(category).await()
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteCategory(categoryId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("categories").document(categoryId).delete().await()
                fetchCategories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
