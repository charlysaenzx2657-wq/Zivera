package com.verdor.musica.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Thin wrapper around Firebase Auth (email/password). Google/other
 * providers can be added later, but email/password needs no extra OAuth
 * console setup beyond enabling it once in the Firebase console. */
class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    /** Google Sign-In: the UI gets an ID token from Google's own sign-in
     * flow (GoogleSignInClient), then hands it here to finish linking it
     * to Firebase Auth. Requires the "Google" provider enabled in the
     * Firebase console (already done) — no extra Firestore/Auth code
     * needed beyond this. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Unit
    }

    fun signOut() = auth.signOut()

    fun uidOrNull(): String? = auth.currentUser?.uid
}
