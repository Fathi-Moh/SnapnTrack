package com.example.snapntrack

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class SetBudgetActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var budgetTypeRadioGroup: RadioGroup
    private lateinit var budgetAmountEditText: AppCompatEditText
    private lateinit var saveBudgetButton: AppCompatButton
    private lateinit var progreeBar: ProgressBar

    private lateinit var headingTextView: TextView
    private lateinit var weeklyBudgetText: TextView
    private lateinit var weeklySpentText: TextView
    private lateinit var monthlyBudgetText: TextView
    private lateinit var monthlySpentText: TextView
    private lateinit var yearlyBudgetText: TextView
    private lateinit var yearlySpentText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_budget)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        budgetTypeRadioGroup = findViewById(R.id.budgetTypeRadioGroup)
        budgetAmountEditText = findViewById(R.id.budgetAmountEditText)
        saveBudgetButton = findViewById(R.id.saveBudgetButton)
        weeklyBudgetText = findViewById(R.id.weeklyBudget)
        weeklySpentText = findViewById(R.id.weeklySpent)
        monthlyBudgetText = findViewById(R.id.monthlyBudget)
        monthlySpentText = findViewById(R.id.monthlySpent)
        yearlyBudgetText = findViewById(R.id.yearlyBudget)
        yearlySpentText = findViewById(R.id.yearlySpent)

        headingTextView = findViewById(R.id.textView2)

        progreeBar = findViewById(R.id.progressBars)
        loadBudgetFromDatabase()

        saveBudgetButton.setOnClickListener {
            saveBudgetToDatabase()
        }
    }

    private fun saveBudgetToDatabase() {
        val selectedRadioButtonId = budgetTypeRadioGroup.checkedRadioButtonId
        val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
        val budgetType = selectedRadioButton.text.toString().lowercase()
        val budgetAmount = budgetAmountEditText.text.toString().toIntOrNull() ?: 0

        if (budgetAmount <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        saveBudgetButton.isEnabled = false
        progreeBar.visibility = android.view.View.VISIBLE

        val userId = auth.currentUser?.uid ?: return
        val budgetData = hashMapOf(
            budgetType to budgetAmount
        )

        database.child("users").child(userId).child("budgets")
            .updateChildren(budgetData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                saveBudgetButton.isEnabled = true
                progreeBar.visibility = android.view.View.GONE
                loadBudgetFromDatabase()

            }
            .addOnFailureListener {
                saveBudgetButton.isEnabled = true
                progreeBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Failed to save budget: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

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


        database.child("users").child(userId).child("budgets")
            .addListenerForSingleValueEvent(object :
                ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val weeklyBudget = snapshot.child("weekly").getValue(Int::class.java) ?: 100
                    val monthlyBudget = snapshot.child("monthly").getValue(Int::class.java) ?: 500
                    val yearlyBudget = snapshot.child("yearly").getValue(Int::class.java) ?: 6000

                    userRef.child("weeklySpend").child(weeklyKey).get()
                        .addOnSuccessListener { weeklySnapshot ->
                            val weeklySpend = weeklySnapshot.getValue(Double::class.java) ?: 0.0

                            userRef.child("monthlySpend").child(monthlyKey).get()
                                .addOnSuccessListener { monthlySnapshot ->
                                    val monthlySpend =
                                        monthlySnapshot.getValue(Double::class.java) ?: 0.0

                                    userRef.child("yearlySpend").child(yearlyKey).get()
                                        .addOnSuccessListener { yearlySnapshot ->
                                            val yearlySpend =
                                                yearlySnapshot.getValue(Double::class.java) ?: 0.0

                                            weeklyBudgetText.text = "$weeklyBudget"
                                            weeklySpentText.text = "%.2f".format(weeklySpend)

                                            monthlyBudgetText.text = "$monthlyBudget"
                                            monthlySpentText.text = "%.2f".format(monthlySpend)

                                            yearlyBudgetText.text = "$yearlyBudget"
                                            yearlySpentText.text = "%.2f".format(yearlySpend)

                                            headingTextView.text =
                                                "Current Budget and Expense for Week $weekNumber, $month, $year"

                                        }
                                }
                        }

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SetBudgetActivity,
                        "Failed to load budget: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })


    }
}