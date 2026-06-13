package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Author
import com.example.first_project.Category
import com.example.first_project.Story
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    fun fetchStories() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("stories").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    Story.fromSnapshot(doc)
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
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "story_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
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
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateStory(story: Story, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(story.id).set(story).await()
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteStory(storyId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("stories").document(storyId).delete().await()
                fetchStories()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
