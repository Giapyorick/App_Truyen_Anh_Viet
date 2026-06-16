package com.example.first_project

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.DocumentSnapshot

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
    val categoryIdsStrings: List<String> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val publicationDate: String = "",
    val lastChapterId: String? = null,
    val lastChapterNumber: Int? = null,
    val lastReadTime: Long? = null,
    val scrollIndex: Int = 0
) {
    @Exclude
    fun getCategoryIdsAsStrings(): List<String> {
        return categoryIds.map { it.toString() }
    }

    @Exclude
    fun resolveImagePath(): Any {
        return when {
            img.startsWith("assets/") -> "file:///android_asset/${img.removePrefix("assets/")}"
            img.startsWith("/") -> java.io.File(img)
            else -> img
        }
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): Story? {
            return try {
                doc.toObject(Story::class.java)?.apply { id = doc.id }
            } catch (e: Exception) {
                val data = doc.data ?: return null
                val chaptersData = data["chapters"] as? List<Map<String, Any>> ?: emptyList()
                val chaptersList = chaptersData.map { c ->
                    val paragraphsData = c["paragraphs"] as? List<Map<String, Any>> ?: emptyList()
                    val paragraphsList = paragraphsData.map { p ->
                        Paragraph(
                            paragraphOrder = (p["paragraphOrder"] as? Long)?.toInt() ?: 0,
                            english = p["english"] as? String ?: "",
                            vietnamese = p["vietnamese"] as? String ?: ""
                        )
                    }
                    Chapter(
                        id = c["id"] as? String ?: "",
                        chapterNumber = (c["chapterNumber"] as? Long)?.toInt() ?: 0,
                        title = c["title"] as? String ?: "",
                        createdDate = c["createdDate"] as? String ?: "",
                        paragraphs = paragraphsList
                    )
                }
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
                    categoryIdsStrings = (data["categoryIdsStrings"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    chapters = chaptersList,
                    publicationDate = data["publicationDate"] as? String ?: "",
                    lastChapterId = data["lastChapterId"] as? String,
                    lastChapterNumber = (data["lastChapterNumber"] as? Long)?.toInt(),
                    lastReadTime = data["lastReadTime"] as? Long,
                    scrollIndex = (data["scrollIndex"] as? Long)?.toInt() ?: 0
                )
            }
        }
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
) {
    @Exclude
    fun resolveImagePath(): Any {
        return when {
            img.startsWith("assets/") -> "file:///android_asset/${img.removePrefix("assets/")}"
            img.startsWith("/") -> java.io.File(img)
            else -> img
        }
    }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "Active"
)

data class User(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val image: String = "",
    val gender: String = "",
    val role: String = "user",
    val status: String = "Active"
) {
    @Exclude
    fun resolveImagePath(): Any {
        return when {
            image.startsWith("assets/") -> "file:///android_asset/${image.removePrefix("assets/")}"
            image.startsWith("/") -> java.io.File(image)
            else -> image
        }
    }
}

data class Comment(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val content: String = "",
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
