package com.stopsmoke.kekkek.firestore.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.stopsmoke.kekkek.common.Result
import com.stopsmoke.kekkek.firestore.dao.UserDao
import com.stopsmoke.kekkek.firestore.model.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class UserDaoImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserDao {
    override fun getUser(): Flow<UserEntity> {
        return firestore.collection(COLLECTION)
            .document("default")
            .dataObjects<UserEntity>()
            .mapNotNull { it }
    }

    override fun getUser(uid: String): Flow<UserEntity> {
        return firestore.collection(COLLECTION)
            .document(uid)
            .dataObjects<UserEntity>()
            .mapNotNull { it }
    }

    override suspend fun setUser(userEntity: UserEntity) {
        firestore.collection(COLLECTION).document(userEntity.uid!!)
            .set(userEntity)
            .addOnFailureListener { throw it }
            .await()
    }

    override suspend fun startQuitSmokingTimer(uid: String): Result<Unit> {
        var result: Result<Unit> = Result.Loading

        firestore.collection(COLLECTION)
            .document(uid)
            .set(UserEntity())
            .addOnSuccessListener {
                result = Result.Success(Unit)
            }
            .addOnFailureListener {
                result = Result.Error(it)
            }
            .await()

        return result
    }

    override suspend fun stopQuitSmokingTimer(uid: String): Result<Unit> {
        var result: Result<Unit> = Result.Loading

        firestore.collection(COLLECTION)
            .document(uid)
            .set(UserEntity())
            .addOnSuccessListener {
                result = Result.Success(Unit)
            }
            .addOnFailureListener {
                result = Result.Error(it)
            }
            .await()

        return result
    }

    companion object {
        private const val COLLECTION = "user"
    }
}