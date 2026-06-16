package com.example.first_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.first_project.admin.AuthorAdminScreen
import com.example.first_project.admin.CategoryAdminScreen
import com.example.first_project.admin.StoryAdminScreen
import com.example.first_project.admin.ChapterAdminScreen
import com.example.first_project.admin.UserAdminScreen
import com.example.first_project.ui.theme.First_ProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            First_ProjectTheme {
                val authViewModel: AuthViewModel = viewModel()
                var currentScreen by remember { mutableStateOf("login") }
                var selectedStoryId by remember { mutableStateOf("") }
                var selectedChapterId by remember { mutableStateOf("") }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        onNavigateToRegister = { currentScreen = "register" },
                        onLoginSuccess = {
                            currentScreen = "home"
                        },
                        viewModel = authViewModel
                    )
                    "admin" -> AdminDashboard(
                        onBack = { currentScreen = "home" },
                        onManageStories = { currentScreen = "admin_stories" },
                        onManageAuthors = { currentScreen = "admin_authors" },
                        onManageCategories = { currentScreen = "admin_categories" },
                        onManageUsers = { currentScreen = "admin_users" }
                    )
                    "admin_authors" -> AuthorAdminScreen(
                        onBack = { currentScreen = "admin" }
                    )
                    "admin_stories" -> StoryAdminScreen(
                        onBack = { currentScreen = "admin" },
                        onManageChapters = { storyId ->
                            selectedStoryId = storyId
                            currentScreen = "admin_chapters"
                        }
                    )
                    "admin_categories" -> CategoryAdminScreen(
                        onBack = { currentScreen = "admin" }
                    )
                    "admin_chapters" -> ChapterAdminScreen(
                        storyId = selectedStoryId,
                        onBack = { currentScreen = "admin_stories" }
                    )
                    "admin_users" -> UserAdminScreen(
                        onBack = { currentScreen = "admin" }
                    )
                    "register" -> RegisterScreen(
                        onNavigateToLogin = { currentScreen = "login" },
                        onRegisterSuccess = { currentScreen = "home" }
                    )
                    "home" -> HomeScreen(
                        isAdmin = authViewModel.isAdmin,
                        onNavigateToAdmin = { currentScreen = "admin" },
                        onNavigateToDetail = { storyId ->
                            selectedStoryId = storyId
                            currentScreen = "detail"
                        },
                        onNavigateToReading = { storyId, chapterId ->
                            selectedStoryId = storyId
                            selectedChapterId = chapterId
                            currentScreen = "reading"
                        },
                        onNavigateToLibrary = { currentScreen = "library" },
                        onNavigateToExplore = { /* TODO */ }
                    )
                    "library" -> LibraryScreen(
                        onNavigateToHome = { currentScreen = "home" },
                        onNavigateToExplore = { /* TODO */ },
                        onNavigateToDetail = { storyId ->
                            selectedStoryId = storyId
                            currentScreen = "detail"
                        }
                    )
                    "detail" -> BookDetailScreen(
                        storyId = selectedStoryId,
                        onBack = { currentScreen = "home" },
                        onChapterClick = { chapterId ->
                            selectedChapterId = chapterId
                            currentScreen = "reading"
                        }
                    )
                    "reading" -> ReadingScreen(
                        storyId = selectedStoryId,
                        chapterId = selectedChapterId,
                        onBack = { currentScreen = "detail" },
                        onChapterChange = { newChapterId: String ->
                            selectedChapterId = newChapterId
                        }
                    )
                }
            }
        }
    }
}
