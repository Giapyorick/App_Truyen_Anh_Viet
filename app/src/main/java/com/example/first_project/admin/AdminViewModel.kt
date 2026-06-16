package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.Activity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {
    private val db = Firebase.firestore

    private val _storyCount = MutableStateFlow(0)
    val storyCount: StateFlow<Int> = _storyCount

    private val _authorCount = MutableStateFlow(0)
    val authorCount: StateFlow<Int> = _authorCount

    private val _categoryCount = MutableStateFlow(0)
    val categoryCount: StateFlow<Int> = _categoryCount

    private val _userCount = MutableStateFlow(0)
    val userCount: StateFlow<Int> = _userCount

    private val _recentActivities = MutableStateFlow<List<Activity>>(emptyList())
    val recentActivities: StateFlow<List<Activity>> = _recentActivities

    init {
        refreshStats()
        fetchRecentActivities()
    }

    fun refreshStats() {
        viewModelScope.launch {
            try {
                val stories = db.collection("stories").get().await()
                _storyCount.value = stories.size()

                val authors = db.collection("authors").get().await()
                _authorCount.value = authors.size()

                val categories = db.collection("categories").get().await()
                _categoryCount.value = categories.size()

                val users = db.collection("users").get().await()
                _userCount.value = users.size()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRecentActivities() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("activities")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()
                
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Activity::class.java)?.copy(id = doc.id)
                }
                _recentActivities.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
