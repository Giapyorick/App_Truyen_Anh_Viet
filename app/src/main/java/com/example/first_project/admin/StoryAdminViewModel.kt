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
