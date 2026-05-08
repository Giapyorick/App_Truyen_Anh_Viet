package com.example.first_project

data class Paragraph(
    val paragraphOrder: Int = 0,
    val english: String = "",
    val vietnamese: String = ""
)

data class Chapter(
    var id: String = "",
    val chapterNumber: Int = 0,
    val title: String = "",
    val createdDate: String = "",
    val paragraphs: List<Paragraph> = emptyList()
)

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

data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "Active"
)
