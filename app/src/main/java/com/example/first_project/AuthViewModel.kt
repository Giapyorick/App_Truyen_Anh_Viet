package com.example.first_project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * AuthViewModel: Lớp quản lý logic xác thực và phân quyền người dùng.
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    val currentUser get() = auth.currentUser

    /**
     * Hàm xử lý đăng nhập vào ứng dụng.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // Trim email để tránh lỗi "badly formatted" do khoảng trắng thừa
        val cleanEmail = email.trim()
        
        if (cleanEmail.isBlank() || password.isBlank()) {
            onResult(false, "Vui lòng nhập đầy đủ thông tin")
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            try {
                                val doc = db.collection("users").document(uid).get().await()
                                isAdmin = doc.getString("role") == "admin"
                                onResult(true, null)
                            } catch (e: Exception) {
                                isAdmin = false
                                onResult(true, null)
                            }
                        }
                    } else {
                        onResult(true, null)
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    /**
     * Hàm xử lý đăng ký tài khoản mới cho người dùng.
     */
    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val cleanEmail = email.trim()
        
        if (cleanEmail.isBlank() || password.isBlank()) {
            onResult(false, "Vui lòng nhập đầy đủ thông tin")
            return
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            try {
                                val userMap = mapOf(
                                    "email" to cleanEmail,
                                    "role" to "user",
                                    "status" to "Active"
                                )
                                db.collection("users").document(uid).set(userMap).await()
                                onResult(true, null)
                            } catch (e: Exception) {
                                onResult(false, "Lỗi tạo thông tin người dùng: ${e.message}")
                            }
                        }
                    } else {
                        onResult(true, null)
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    var isAdmin by mutableStateOf(false)
        private set

    fun checkUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                isAdmin = doc.getString("role") == "admin"
            } catch (e: Exception) {
                isAdmin = false
            }
        }
    }
}
