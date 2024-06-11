package com.stopsmoke.kekkek.firestore.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.stopsmoke.kekkek.firestore.dao.PostDao
import com.stopsmoke.kekkek.firestore.data.pager.FireStorePagingSource
import com.stopsmoke.kekkek.firestore.model.PostEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class PostDaoImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PostDao {

    override fun getPost(category: String?): Flow<PagingData<PostEntity>> {
        val query = firestore.collection(COLLECTION)
            .whereNotNullEqualTo("category", category)
            .orderBy("date_time", Query.Direction.DESCENDING)


        return Pager(
            config = PagingConfig(PAGE_LIMIT)
        ) {
            FireStorePagingSource(
                query = query,
                limit = PAGE_LIMIT.toLong(),
                clazz = PostEntity::class.java
            )

        }.flow
    }

    override fun getPost(uid: String): Flow<PagingData<PostEntity>> {
        val query = firestore.collection(COLLECTION)
            .whereEqualTo("written.uid", uid)
            .orderBy("comment_user", Query.Direction.DESCENDING)

        return Pager(
            config = PagingConfig(PAGE_LIMIT)
        ) {
            FireStorePagingSource(
                query = query,
                limit = PAGE_LIMIT.toLong(),
                clazz = PostEntity::class.java
            )

        }.flow
    }

    override suspend fun addPost(postEntity: PostEntity) {
        firestore.collection(COLLECTION).document().let { document ->
            document.set(postEntity.copy(id = document.id))
                .addOnFailureListener { throw it }
                .addOnCanceledListener { throw CancellationException() }
                .await()
        }
    }

    override suspend fun updateOrInsertPost(postEntity: PostEntity) {
        firestore.collection(COLLECTION)
            .document(postEntity.id!!)
            .set(postEntity)
            .addOnFailureListener { throw it }
            .addOnCanceledListener { throw CancellationException() }
            .await()
    }

    override suspend fun deletePost(postId: String) {
        firestore.collection(COLLECTION)
            .document(postId)
            .delete()
            .addOnFailureListener { throw it }
            .addOnCanceledListener { throw CancellationException() }
            .await()
    }

    override suspend fun getPopularPostItems(): List<PostEntity> {
        return try {
            val query = firestore.collection(COLLECTION)
                .whereEqualTo("category", "popular")
                .limit(2)
                .get()
                .await()

            query.documents.mapNotNull { document ->
                document.toObject<PostEntity>()
            }
        } catch (e: Exception) {
            // 예외 처리
            emptyList()
        }
    }

    override suspend fun getTopNotice(): PostEntity {
        return try {
            val query = firestore.collection(COLLECTION)
                .whereEqualTo("category", "notice")
                .limit(1)
                .get()
                .await()

            val document = query.documents.firstOrNull()
            document?.toObject<PostEntity>() ?: PostEntity()
        } catch (e: Exception) {
            PostEntity()
        }
    }

    companion object {
        private const val COLLECTION = "post"
        private const val PAGE_LIMIT = 30
    }
}

private fun Query.whereNotNullEqualTo(field: String, value: Any?): Query {
    if (value == null) {
        return this
    }
    return whereEqualTo(field, value)
}