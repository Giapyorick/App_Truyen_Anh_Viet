package com.example.first_project.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.first_project.User
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
                _users.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSecondaryAuth(context: Context): FirebaseAuth {
        val options = FirebaseOptions.fromResource(context) ?: throw Exception("Could not load Firebase options")
        val secondaryApp = try {
            FirebaseApp.getInstance("Secondary")
        } catch (e: Exception) {
            FirebaseApp.initializeApp(context, options, "Secondary")
        }
        return FirebaseAuth.getInstance(secondaryApp)
    }

    fun saveImageLocally(context: Context, uri: android.net.Uri, onSuccess: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "user_${System.currentTimeMillis()}.jpg")
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

    fun addUser(context: Context, user: User, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val secondaryAuth = getSecondaryAuth(context)
                val authResult = secondaryAuth.createUserWithEmailAndPassword(user.email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("Failed to get UID")
                
                val newUser = user.copy(id = uid)
                db.collection("users").document(uid).set(newUser).await()
                
                secondaryAuth.signOut()
                
                fetchUsers()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun updateUser(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users").document(user.id).set(user).await()
                fetchUsers()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun deleteUser(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).delete().await()
                fetchUsers()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
