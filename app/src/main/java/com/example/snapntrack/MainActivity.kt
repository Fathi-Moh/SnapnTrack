package com.example.snapntrack

import android.content.Intent

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snapntrack.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        binding.scanReciptButton.setOnClickListener {
            startActivity(Intent(this, ScanReceiptActivity::class.java))
        }

        binding.receiptLogButton.setOnClickListener {
            startActivity(Intent(this, ReceiptLogActivity::class.java))
        }

        binding.staticsButton.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.setBudgetButton.setOnClickListener {
            startActivity(Intent(this, SetBudgetActivity::class.java))
        }

        binding.logutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }


//
    private fun loadBudgetFromDatabase() {

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = SimpleDateFormat("MM", Locale.getDefault()).format(calendar.time)
        val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
        val weeklyKey = "$year-W$weekNumber"
        val monthlyKey = "$year-$month"
        val yearlyKey = "$year"
        val database = FirebaseDatabase.getInstance().reference

        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)


        database.child("users").child(userId).child("budgets").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val weeklyBudget = snapshot.child("weekly").getValue(Int::class.java) ?: 100
                val monthlyBudget = snapshot.child("monthly").getValue(Int::class.java) ?: 500
                val yearlyBudget = snapshot.child("yearly").getValue(Int::class.java) ?: 6000

                userRef.child("weeklySpend").child(weeklyKey).get().addOnSuccessListener { weeklySnapshot ->
                    val weeklySpend = weeklySnapshot.getValue(Double::class.java) ?: 0.0

                    if (weeklySpend>=weeklyBudget){
                        val message = "Keep an eye on your spending! Your weekly budget for Week $weeklyKey is exceeded."
                        showBudgetExceededDialog(message)                    }

                    userRef.child("monthlySpend").child(monthlyKey).get().addOnSuccessListener { monthlySnapshot ->
                        val monthlySpend = monthlySnapshot.getValue(Double::class.java) ?: 0.0
                        if (monthlySpend>=monthlyBudget){
                            val message = "Keep an eye on your spending! Your monthly budget for $monthlyKey is exceeded."
                            showBudgetExceededDialog(message)
                            }
                        userRef.child("yearlySpend").child(yearlyKey).get().addOnSuccessListener { yearlySnapshot ->
                            val yearlySpend = yearlySnapshot.getValue(Double::class.java) ?: 0.0
                            if (yearlySpend>=yearlyBudget){
                                val message = "Keep an eye on your spending! Your yearly budget for $yearlyKey is exceeded."
                                showBudgetExceededDialog(message)

                            }

                        }
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load budget: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        }

    private fun showBudgetExceededDialog(message: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Alert Budget Exceeded")
            .setMessage(message)
            .setPositiveButton("Set Budget") { dialog, _ ->
                // Navigate to SetBudgetActivity
                startActivity(Intent(this, SetBudgetActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadBudgetFromDatabase()

    }
    }