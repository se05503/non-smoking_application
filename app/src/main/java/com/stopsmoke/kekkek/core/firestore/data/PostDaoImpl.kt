package com.stopsmoke.kekkek.core.firestore.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import com.stopsmoke.kekkek.common.Result
import com.stopsmoke.kekkek.core.firestorage.dao.StorageDao
import com.stopsmoke.kekkek.core.firestore.COMMENT_COLLECTION
import com.stopsmoke.kekkek.core.firestore.POST_COLLECTION
import com.stopsmoke.kekkek.core.firestore.dao.PostDao
import com.stopsmoke.kekkek.core.firestore.model.PostEntity
import com.stopsmoke.kekkek.core.firestore.model.UserEntity
import com.stopsmoke.kekkek.core.firestore.pager.FireStorePagingSource
import com.stopsmoke.kekkek.core.firestore.whereNotNullEqualTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.Calendar
import javax.inject.Inject

internal class PostDaoImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageDao: StorageDao
) : PostDao {

    override fun getPost(category: String?): Flow<PagingData<PostEntity>> {
        val query = firestore.collection(POST_COLLECTION)
            .whereNotNullEqualTo("category", category)
            .orderBy("date_time.created", Query.Direction.DESCENDING)

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

    override fun getPostForWrittenUid(writtenUid: String): Flow<PagingData<PostEntity>> {
        return try {
            val query = firestore.collection(POST_COLLECTION)
                .whereEqualTo("written.uid", writtenUid)
                .orderBy("date_time.created", Query.Direction.DESCENDING)
                .limit(10)

            return Pager(
                config = PagingConfig(PAGE_LIMIT)
            ) {
                FireStorePagingSource(
                    query = query,
                    limit = PAGE_LIMIT.toLong(),
                    clazz = PostEntity::class.java
                )

            }.flow
        } catch (e: Exception) {
            e.printStackTrace()
            //빈 페이저
            Pager(
                config = PagingConfig(PAGE_LIMIT)
            ) {
                object : PagingSource<Int, PostEntity>() {
                    override fun getRefreshKey(state: PagingState<Int, PostEntity>): Int? = null

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostEntity> {
                        return LoadResult.Page(
                            data = emptyList(),
                            prevKey = null,
                            nextKey = null
                        )
                    }
                }
            }.flow
        }
    }

    override fun getBookmark(postIdList: List<String>): Flow<PagingData<PostEntity>> {
        return try {
            val query = firestore.collection(POST_COLLECTION)
                .whereIn("id", postIdList)

            Pager(
                config = PagingConfig(PAGE_LIMIT)
            ) {
                FireStorePagingSource(
                    query = query,
                    limit = PAGE_LIMIT.toLong(),
                    clazz = PostEntity::class.java
                )

            }.flow
        } catch (e: Exception) {
            e.printStackTrace()
            //빈 페이저
            Pager(
                config = PagingConfig(PAGE_LIMIT)
            ) {
                object : PagingSource<Int, PostEntity>() {
                    override fun getRefreshKey(state: PagingState<Int, PostEntity>): Int? = null

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostEntity> {
                        return LoadResult.Page(
                            data = emptyList(),
                            prevKey = null,
                            nextKey = null
                        )
                    }
                }
            }.flow
        }
    }

    override fun getPostUserFilter(uid: String): Flow<PagingData<PostEntity>> {
        val query = firestore.collection(POST_COLLECTION)
            .whereEqualTo("written.uid", uid)
            .orderBy("date_time.created", Query.Direction.DESCENDING)

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

    override fun getPostItem(postId: String): Flow<PostEntity> {
        return firestore.collection(POST_COLLECTION)
            .document(postId)
            .dataObjects<PostEntity>()
            .mapNotNull { it }
    }

    override suspend fun addPost(postEntity: PostEntity) {
        firestore.collection(POST_COLLECTION).document().let { document ->
            document.set(postEntity.copy(id = document.id))
                .await()
            document.update("date_time.created", FieldValue.serverTimestamp())
                .await()
            document.update("date_time.modified", FieldValue.serverTimestamp())
                .await()
        }
    }

    override suspend fun addPost(postEntity: PostEntity, inputStream: InputStream) {
        firestore.collection(POST_COLLECTION).document().let { document ->
            val uploadUrl = storageDao.uploadFile(inputStream, "posts/${document.id}/image.jpeg")

            document.set(postEntity.copy(id = document.id, imagesUrl = listOf(uploadUrl)))
                .addOnFailureListener { throw it }
                .addOnCanceledListener { throw CancellationException() }
                .await()
        }

    }

    override suspend fun editPost(postEntity: PostEntity) {
        val updateMap = mapOf(
            "category" to postEntity.category,
            "title" to postEntity.title,
            "text" to postEntity.text,
            "date_time.modified" to FieldValue.serverTimestamp(),
        )
        firestore.collection(POST_COLLECTION)
            .document(postEntity.id!!)
            .update(updateMap)
            .await()
    }

    override suspend fun editPost(postEntity: PostEntity, inputStream: InputStream) {
        val uploadUrl = storageDao.uploadFile(inputStream, "posts/${postEntity.id}/image.jpeg")

        val updateMap = mapOf(
            "category" to postEntity.category,
            "title" to postEntity.title,
            "text" to postEntity.text,
            "date_time.modified" to FieldValue.serverTimestamp(),
            "images_url" to listOf(uploadUrl)
        )

        firestore.collection(POST_COLLECTION)
            .document(postEntity.id!!)
            .update(updateMap)
            .await()
    }

    override suspend fun updateOrInsertPost(postEntity: PostEntity) {
        firestore.collection(POST_COLLECTION)
            .document(postEntity.id!!)
            .set(postEntity)
            .addOnFailureListener { throw it }
            .addOnCanceledListener { throw CancellationException() }
            .await()
    }

    override suspend fun deletePost(postId: String) {
        firestore.collection(POST_COLLECTION)
            .document(postId)
            .delete()
            .addOnFailureListener { throw it }
            .addOnCanceledListener { throw CancellationException() }
            .await()
    }

    override fun getTopNotice(limit: Long): Flow<List<PostEntity>> {
        return firestore.collection(POST_COLLECTION)
            .whereEqualTo("category", "notice")
            .orderBy("date_time.created", Query.Direction.DESCENDING)
            .limit(limit)
            .dataObjects<PostEntity>()
    }

    override suspend fun getPopularPostList(): Flow<List<PostEntity>> {
            // 현재 시간에서 7일 전 시간 계산
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgo = Timestamp(calendar.time)

            val query = firestore.collection(POST_COLLECTION)
                .whereGreaterThan("date_time.created", sevenDaysAgo)
                .orderBy("views", Query.Direction.DESCENDING)
                .limit(10)

            // Firestore 쿼리 결과를 PostEntity 리스트로 변환
            return query.dataObjects<PostEntity>()
    }

    override suspend fun getPopularPostListNonPeriod(): Flow<List<PostEntity>> {
        val query = firestore.collection(POST_COLLECTION)
            .orderBy("views", Query.Direction.DESCENDING)
            .limit(10)

        return query.dataObjects<PostEntity>()
    }

    override suspend fun getPostForPostId(postId: String): PostEntity {
        return try {
            val query = firestore.collection(POST_COLLECTION)
                .whereEqualTo("id", postId)
                .get()
                .await()

            val document = query.documents.firstOrNull()
            document?.toObject<PostEntity>() ?: PostEntity()
        } catch (e: Exception) {
            e.printStackTrace()
            PostEntity()
        }
    }

    override fun getCommentCount(postId: String): Flow<Long> = callbackFlow {
        firestore.collection(COMMENT_COLLECTION)
            .whereEqualTo("post_data.post_id", postId)
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener {
                trySend(it.count)
            }
            .addOnFailureListener {
                trySend(-1)
            }
            .await()

        awaitClose()
    }

    override fun getLikeCount(postId: String): Flow<Long> = callbackFlow {
        firestore.collection(POST_COLLECTION)
            .whereEqualTo("post_data.post_id", postId)
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener {
                trySend(it.count)
            }
            .addOnFailureListener {
                trySend(-1)
            }
            .await()

        awaitClose()
    }

    override suspend fun addLike(postId: String, uid: String): Result<Unit> {
        val task = firestore.collection(POST_COLLECTION)
            .document(postId)
            .update("like_user", FieldValue.arrayUnion(uid))
            .also { it.await() }

        if (task.isSuccessful) {
            return Result.Success(Unit)
        }
        return Result.Error(task.exception)
    }

    override suspend fun deleteLike(postId: String, uid: String): Result<Unit> {
        val task = firestore.collection(POST_COLLECTION)
            .document(postId)
            .update("like_user", FieldValue.arrayRemove(uid))
            .also { it.await() }

        if (task.isSuccessful) {
            return Result.Success(Unit)
        }
        return Result.Error(task.exception)
    }

    override suspend fun addViews(postId: String): Result<Unit> {
        val task = firestore.collection(POST_COLLECTION)
            .document(postId)
            .collection("_counter_shards_")
            .document()
            .set(mapOf("views" to 1))
            .also { it.await() }

        if (task.isSuccessful) {
            return Result.Success(Unit)
        }
        return Result.Error(task.exception)
    }

    override suspend fun setProfileImage(userId: String, imgUrl: String) {
        try {
            val getQuery = firestore.collection(POST_COLLECTION)
                .whereEqualTo("written.uid", userId)
                .get()
                .await()

            val postList = getQuery.documents.mapNotNull { document ->
                document.toObject<PostEntity>()?.apply {
                    written?.profileImage = imgUrl
                }
            }

            postList.forEach { post ->
                firestore.collection(POST_COLLECTION)
                    .document(post.id!!)
                    .set(post)
                    .await()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun setUserDataForName(userEntity: UserEntity, name: String) {
        try {
            val getQuery = firestore.collection(POST_COLLECTION)
                .whereEqualTo("written.uid", userEntity.uid!!)
                .get()
                .await()

            val postList = getQuery.documents.mapNotNull { document ->
                document.toObject<PostEntity>()?.apply {
                    written?.name = name
                }
            }

            postList.forEach { post ->
                firestore.collection(POST_COLLECTION)
                    .document(post.id!!)
                    .set(post)
                    .await()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun addBookmark(postId: String, uid: String): Result<Unit> {
        val task = firestore.collection(POST_COLLECTION)
            .document(postId)
            .update("bookmark_user", FieldValue.arrayUnion(uid))
            .also { it.await() }

        if (task.isSuccessful) {
            return Result.Success(Unit)
        }
        return Result.Error(task.exception)
    }

    override suspend fun deleteBookmark(postId: String, uid: String): Result<Unit> {
        val task = firestore.collection(POST_COLLECTION)
            .document(postId)
            .update("bookmark_user", FieldValue.arrayRemove(uid))
            .also { it.await() }

        if (task.isSuccessful) {
            return Result.Success(Unit)
        }
        return Result.Error(task.exception)
    }

    override fun getBookmarkItems(uid: String): Flow<PagingData<PostEntity>> {
        val query = firestore.collection(POST_COLLECTION)
            .whereArrayContains("bookmark_user", uid)
            .limit(30)

        return Pager(
            config = PagingConfig(PAGE_LIMIT)
        ) {
            FireStorePagingSource(
                query = query,
                limit = PAGE_LIMIT.toLong(),
                clazz = PostEntity::class.java
            )

        }
            .flow
    }

    companion object {
        private const val PAGE_LIMIT = 30
    }
}