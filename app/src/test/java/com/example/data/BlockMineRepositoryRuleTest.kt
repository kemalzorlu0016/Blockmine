package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockMineRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateProfile_and_updateProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = BlockMineRepository(firestore)

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateProfile("AliceMiner").getOrThrow()
        }
        assertEquals(uid, created.userId)

        val updated = created.copy(
            username = "AliceArchitect",
            blocksPlaced = 55L,
            coins = 500L
        )
        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateProfile(updated)
        }
        assertTrue(updateRes.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null && it.blocksPlaced == 55L }
        }
        assertNotNull(observed)
        assertEquals("AliceArchitect", observed?.username)
    }

    @Test
    fun getProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BlockMineRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateProfile("AliceMiner").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BlockMineRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileById(aliceUid)
        }
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun addAndObserveFriends_authenticatedOwner_emitsFriend() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = BlockMineRepository(firestore)

        val friendId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addFriend(
                friendName = "EfeCraft",
                friendTag = "#TR99",
                status = "online",
                serverRoom = "Anadolu Survival #1"
            ).getOrThrow()
        }
        assertTrue(friendId.isNotBlank())

        val friends = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFriends(uid).first { list -> list.any { it.id == friendId } }
        }
        assertTrue(friends.any { it.friendName == "EfeCraft" })
    }

    @Test
    fun createServerRoom_and_observeMyServers_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = BlockMineRepository(firestore)

        val serverId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createServerRoom("SkyBlock TR", "creative", "EU-TR").getOrThrow()
        }
        assertTrue(serverId.isNotBlank())

        val servers = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyServers(uid).first { list -> list.any { it.id == serverId } }
        }
        assertTrue(servers.any { it.roomName == "SkyBlock TR" })
    }

    @Test
    fun observeFriends_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BlockMineRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeFriends("some_user_123").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@blockmine.test"
        const val BOB_EMAIL = "bob@blockmine.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
