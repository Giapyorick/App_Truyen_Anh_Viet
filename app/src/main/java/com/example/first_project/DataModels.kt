package com.example.first_project

/**
 * Paragraph: Đại diện cho một đoạn văn song ngữ.
 */
data class Paragraph(
    val paragraphOrder: Int = 0,
    val english: String = "",
    val vietnamese: String = ""
)

/**
 * Chapter: Đại diện cho một chương truyện.
 */
data class Chapter(
    var id: String = "",
    val chapterNumber: Int = 0,
    val title: String = "",
    val createdDate: String = "",
    val paragraphs: List<Paragraph> = emptyList()
)

/**
 * Story: Đại diện cho một tác phẩm truyện.
 */
data class Story(
    var id: String = "",
    val title: String = "",
    val authorId: String = "",
    val description: String = "",
    val status: String = "",
    val img: String = "",
    val likes: Int = 0,
    val rate: Double = 0.0,
    val count_follower: Int = 0,
    val count_rate: Int = 0,
    val categoryIds: List<Any> = emptyList(),
    val publicationDate: String = "",
    val lastChapterId: String? = null,
    val lastChapterNumber: Int? = null,
    val lastReadTime: Long? = null,
    val scrollIndex: Int = 0
) {
    fun getCategoryIdsStrings(): List<String> {
        return categoryIds.map { it.toString() }
    }
}

/**
 * Author: Đại diện cho một Tác giả.
 */
data class Author(
    val id: String = "",
    val authorName: String = "",
    val email: String = "",
    val dob: String = "",
    val gender: String = "",
    val country: String = "",
    val img: String = "",
    val status: String = "Active"
)

/**
 * Category: Định nghĩa một thể loại truyện.
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "Active"
)

/**
 * User: Đại diện cho người dùng trong hệ thống.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "user",
    val displayName: String = "",
    val status: String = "Active"
)

/**
 * Activity: Lưu lại nhật ký hoạt động của Admin.
 */
data class Activity(
    val id: String = "",
    val action: String = "",      // Ví dụ: "Thêm truyện mới", "Xóa tác giả"
    val target: String = "",      // Tên đối tượng bị tác động
    val timestamp: Long = 0,
    val status: String = "SUCCESS",
    val isSuccess: Boolean = true
)
