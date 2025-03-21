package com.example.snapntrack

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var signUpButton: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private lateinit var request: GetCredentialRequest
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth =FirebaseAuth.getInstance()
        database = Firebase.database.reference

        val loginButton = findViewById<AppCompatButton>(R.id.loginButton)
         signUpButton = findViewById<AppCompatButton>(R.id.signUpButton)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        val googleSignInButton = findViewById<SignInButton>(R.id.googleSignInButton)
         progressBar = findViewById<ProgressBar>(R.id.progressBars)


        loginButton.setOnClickListener {

            startActivity(Intent(this, LoginActivityTwo::class.java))

        }

        signUpButton.setOnClickListener {

            startActivity(Intent(this, RegisterActivity::class.java))

        }

        forgotPasswordText.setOnClickListener {
          showForgotPasswordDialog()
        }

        credentialManager = CredentialManager.create(baseContext)


      //  createRequest()

        googleSignInButton.setOnClickListener {
            launchCredentialManager()

        }


    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)

        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")
        builder.setView(dialogView)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Failed to send reset email: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun launchCredentialManager() {
        signUpButton.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(true)
            .build()

         request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request
                )

                handleSignIn(result.credential)
                Toast.makeText(this@LoginActivity, ""+result, Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Cccouldn't retrieve user's credentials: ${e.localizedMessage}")
            }
        }

    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            signUpButton.isEnabled = true
            progressBar.visibility = android.view.View.GONE
            Log.w(TAG, "Cccredential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    signUpButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Check if the user already exists in the database
            database.child("users").child(user.uid).get().addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    signUpButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                    val userData = dbTask.result?.value as? Map<*, *>

                    if (userData == null) {
                        val newUserData = hashMapOf(
                            "name" to user.displayName,
                            "username" to "",
                            "email" to user.email,
                            "id" to user.uid,
                            "budgets" to hashMapOf(
                                "weekly" to 100,
                                "monthly" to 500,
                                "yearly" to 6000
                            )
                        )

                        database.child("users").child(user.uid).setValue(newUserData)
                            .addOnCompleteListener { saveTask ->
                                if (saveTask.isSuccessful) {
                                    Toast.makeText(this, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to save user data: ${saveTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Welcome back, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    signUpButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Failed to check user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}