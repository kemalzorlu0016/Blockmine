package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BlockMineRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrCreateProfile(defaultUsername: String = "BlockMiner"): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) {
                    Result.success(existing)
                } else {
                    Result.failure(IllegalStateException("Failed to deserialize profile"))
                }
            } else {
                val cleanName = defaultUsername.trim().take(40).ifEmpty { "BlockMiner" }
                val payload = mapOf(
                    "userId" to uid,
                    "username" to cleanName,
                    "titleBadge" to "Usta Mimar",
                    "skinPreset" to "diamond_knight",
                    "hairColorHex" to "#3B271A",
                    "tunicColorHex" to "#00B4D8",
                    "pantsColorHex" to "#1D3557",
                    "capeStyle" to "royal_gold",
                    "level" to 1L,
                    "xp" to 100L,
                    "coins" to 250L,
                    "gems" to 25L,
                    "dailyStreak" to 1L,
                    "lastClaimDay" to 0L,
                    "blocksPlaced" to 0L,
                    "blocksMined" to 0L,
                    "templatesBuilt" to 0L,
                    "languageCode" to "tr",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val createdSnap = docRef.get().await()
                val createdProfile = createdSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid, username = cleanName)
                Result.success(createdProfile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getProfileById(targetUserId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val updates = mapOf(
                "username" to profile.username.trim().take(40).ifEmpty { "BlockMiner" },
                "titleBadge" to profile.titleBadge.trim().take(50).ifEmpty { "Usta Mimar" },
                "skinPreset" to profile.skinPreset.take(30).ifEmpty { "diamond_knight" },
                "hairColorHex" to profile.hairColorHex.take(16).ifEmpty { "#3B271A" },
                "tunicColorHex" to profile.tunicColorHex.take(16).ifEmpty { "#00B4D8" },
                "pantsColorHex" to profile.pantsColorHex.take(16).ifEmpty { "#1D3557" },
                "capeStyle" to profile.capeStyle.take(30).ifEmpty { "royal_gold" },
                "level" to profile.level.coerceIn(1L, 9999L),
                "xp" to profile.xp.coerceAtLeast(0L),
                "coins" to profile.coins.coerceAtLeast(0L),
                "gems" to profile.gems.coerceAtLeast(0L),
                "dailyStreak" to profile.dailyStreak.coerceIn(0L, 3650L),
                "lastClaimDay" to profile.lastClaimDay.coerceAtLeast(0L),
                "blocksPlaced" to profile.blocksPlaced.coerceAtLeast(0L),
                "blocksMined" to profile.blocksMined.coerceAtLeast(0L),
                "templatesBuilt" to profile.templatesBuilt.coerceAtLeast(0L),
                "languageCode" to profile.languageCode.take(10).ifEmpty { "tr" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeFriends(userId: String = requireUserId()): Flow<List<FriendEntry>> = flow {
        val path = "users/$userId/friends"
        emitAll(
            db.collection("users").document(userId).collection("friends")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FriendEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addFriend(
        friendName: String,
        friendTag: String = "#TR01",
        status: String = "online",
        serverRoom: String = "Anadolu Survival #1",
        skinPreset: String = "emerald_explorer"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("friends").document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "friendName" to friendName.trim().take(40).ifEmpty { "Oyuncu" },
                "friendTag" to friendTag.trim().take(30).ifEmpty { "#TR01" },
                "status" to if (status in listOf("online", "in_game", "offline")) status else "online",
                "serverRoom" to serverRoom.trim().take(60),
                "skinPreset" to skinPreset.trim().take(30).ifEmpty { "emerald_explorer" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("friends").document(friendId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMyServers(userId: String = requireUserId()): Flow<List<ServerRoom>> = flow {
        val path = "servers"
        emitAll(
            db.collection("servers")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ServerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createServerRoom(
        roomName: String,
        gameMode: String = "creative",
        region: String = "EU-TR"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("servers").document()
        return try {
            val validMode = if (gameMode in listOf("creative", "survival", "exploration")) gameMode else "creative"
            val payload = mapOf(
                "ownerId" to uid,
                "roomName" to roomName.trim().take(50).ifEmpty { "BlockMine Sunucusu" },
                "gameMode" to validMode,
                "region" to region.trim().take(20).ifEmpty { "EU-TR" },
                "playerCount" to 1L,
                "maxPlayers" to 16L,
                "memberUids" to listOf(uid),
                "lastChatMessage" to "Sunucu oluşturuldu! Keşfe başla.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendServerChatMessage(serverId: String, message: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("servers").document(serverId)
        return try {
            docRef.update(
                mapOf(
                    "lastChatMessage" to message.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
