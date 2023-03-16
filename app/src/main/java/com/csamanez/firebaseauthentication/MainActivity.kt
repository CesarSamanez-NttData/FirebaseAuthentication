package com.csamanez.firebaseauthentication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configAuth()


    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
            } else {
                // Provider Google Authentication
                // Apertura una UI generica para la bd del proyecto
                val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())

                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    val response = IdpResponse.fromResultIntent(it.data)

                    if (it.resultCode == RESULT_OK) {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            Toast.makeText(this, "Welcome...", Toast.LENGTH_SHORT).show()
                        }
                    }

                }.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

}