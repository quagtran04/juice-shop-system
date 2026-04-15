package com.example.juiceshop.data

import com.example.juiceshop.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    // AUTH

    suspend fun register(email: String, password: String, username: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID is null")
            val user = User(uid = uid, username = username, email = email)
            usersRef.document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID is null")
            val user = getUserById(uid) ?: throw Exception("User data not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // PROFILE

    suspend fun getUserById(uid: String): User? {
        val doc = usersRef.document(uid).get().await()
        return doc.toObject(User::class.java)?.copy(uid = doc.id)
    }

    suspend fun updateProfile(uid: String, phone: String, address: String): Boolean {
        return try {
            usersRef.document(uid).update(
                mapOf("phone" to phone, "address" to address)
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun changePassword(newPassword: String): Boolean {
        return try {
            auth.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}