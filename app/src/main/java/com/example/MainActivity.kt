package com.example

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BlockMineRepository
import com.example.data.configureAppCheck
import com.example.ui.BlockMineAuthScreen
import com.example.ui.BlockMineGameScreen
import com.example.ui.BlockMineViewModel
import com.example.ui.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestMaxDisplayRefreshRate()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }

    private fun requestMaxDisplayRefreshRate() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val activeDisplay = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return
        val maxMode = activeDisplay.supportedModes.maxByOrNull { it.refreshRate } ?: return
        val params = window.attributes
        params.preferredDisplayModeId = maxMode.modeId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.preferredRefreshRate = maxMode.refreshRate
        }
        window.attributes = params
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        BlockMineAuthScreen(
            onAuthSuccess = {}
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "BlockMiner"
        key(uid) {
            AuthenticatedBlockMineApp(
                currentUserId = uid,
                initialDisplayName = displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedBlockMineApp(
    currentUserId: String,
    initialDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: BlockMineViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BlockMineViewModel(
                    repository = BlockMineRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    BlockMineGameScreen(
        viewModel = viewModel,
        onSignOut = {
            signOutUser(credentialManager, scope)
        }
    )
}
