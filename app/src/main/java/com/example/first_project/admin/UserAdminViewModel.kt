package com.example.first_project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.User
import com.example.first_project.Activity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserAdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    init {
        fetchUsers()
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

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
                _users.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateUserRole(uid: String, newRole: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userEmail = _users.value.find { it.uid == uid }?.email ?: "Unknown"
            try {
                db.collection("users").document(uid).update("role", newRole).await()
                logActivity("Cập nhật vai trò", "$userEmail -> $newRole")
                fetchUsers()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Cập nhật vai trò thất bại", userEmail, false)
                onResult(false)
            }
        }
    }

    fun updateUserStatus(uid: String, newStatus: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userEmail = _users.value.find { it.uid == uid }?.email ?: "Unknown"
            try {
                db.collection("users").document(uid).update("status", newStatus).await()
                logActivity("Cập nhật trạng thái người dùng", "$userEmail -> $newStatus")
                fetchUsers()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Cập nhật trạng thái người dùng thất bại", userEmail, false)
                onResult(false)
            }
        }
    }

    fun deleteUser(uid: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userEmail = _users.value.find { it.uid == uid }?.email ?: "Unknown"
            try {
                db.collection("users").document(uid).delete().await()
                logActivity("Xóa người dùng", userEmail)
                fetchUsers()
                onResult(true)
            } catch (e: Exception) {
                logActivity("Xóa người dùng thất bại", userEmail, false)
                onResult(false)
            }
        }
    }
}
