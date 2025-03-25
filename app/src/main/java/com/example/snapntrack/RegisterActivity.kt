package com.example.snapntrack

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snapntrack.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        binding.signUpButton.setOnClickListener {
            registerUser()
        }

    }

    private fun registerUser() {
        val name = binding.editTextNme.text.toString().trim()
        val username = binding.editTextUsername.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (validateInputs(name, username, email, password)) {
            binding.signUpButton.isEnabled = false
            binding.progressBar.visibility = android.view.View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val userData = hashMapOf(
                                "name" to name,
                                "username" to username,
                                "email" to email,
                                "id" to user.uid
                            )

                            // Set default budgets
                            val defaultBudgets = hashMapOf(
                                "weekly" to 100,
                                "monthly" to 500,
                                "yearly" to 6000
                            )

                            database.child("users").child(user.uid).setValue(userData)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        database.child("users").child(user.uid).child("budgets")
                                            .setValue(defaultBudgets)
                                            .addOnCompleteListener { budgetTask ->
                                                if (budgetTask.isSuccessful) {
                                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                    startActivity(Intent(this, MainActivity::class.java))
                                                    finish()
                                                } else {
                                                    Toast.makeText(this, "Failed to set default budgets: ${budgetTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                                binding.signUpButton.isEnabled = true
                                                binding.progressBar.visibility = android.view.View.INVISIBLE
                                            }
                                    } else {
                                        Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        binding.signUpButton.isEnabled = true
                                        binding.progressBar.visibility = android.view.View.GONE
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        binding.signUpButton.isEnabled = true
                        binding.progressBar.visibility = android.view.View.INVISIBLE
                    }
                }
        }
    }
//
    private fun validateInputs(name: String, username: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.editTextNme.error = "Name is required"
            binding.editTextNme.requestFocus()
            return false
        }

        if (username.isEmpty()) {
            binding.editTextUsername.error = "Username is required"
            binding.editTextUsername.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.editTextEmail.error = "Email is required"
            binding.editTextEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Please enter a valid email"
            binding.editTextEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "Password is required"
            binding.editTextPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "Password must be at least 6 characters"
            binding.editTextPassword.requestFocus()
            return false
        }

        return true
    }
}