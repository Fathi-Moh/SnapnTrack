package com.example.snapntrack

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snapntrack.util.LineGraphView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ExpenseStaticsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var budgetTypeRadioGroup: RadioGroup

    private lateinit var progreeBar: ProgressBar
    private lateinit var lineGraphView: LineGraphView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_statics)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
        lineGraphView = findViewById(R.id.lineGraphView)

        budgetTypeRadioGroup = findViewById(R.id.budgetTypeRadioGroup)

        progreeBar=findViewById(R.id.progressBars)
        budgetTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.weeklyRadioButton -> fetchDataAndUpdateGraph("weeklySpend")
                R.id.monthlyRadioButton -> fetchDataAndUpdateGraph("monthlySpend")
                R.id.yearlyRadioButton -> fetchDataAndUpdateGraph("yearlySpend")
            }
        }

        // Load initial data (e.g., weekly by default)
        fetchDataAndUpdateGraph("weeklySpend")


    }

    private fun fetchDataAndUpdateGraph(path: String) {
        progreeBar.visibility=View.VISIBLE
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child(path)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    progreeBar.visibility=View.GONE
                    lineGraphView.visibility=View.VISIBLE

                    val entries = mutableListOf<Pair<Float, Float>>()
                    val labels = mutableListOf<String>()
                    var index = 0
entries.clear()
                    labels.clear()
                    labels.add("")
                    entries.add(Pair(0f,0f))
                    for (data in snapshot.children) {
                        val label = data.key ?: continue
                        val value = data.getValue(Double::class.java) ?: 0.0
                        Log.e("TAG", "onDataChangeLabel: $label")
                        Log.e("TAG", "onDataChange: $value")

                        entries.add(Pair(index.toFloat(), value.toFloat()))
                        labels.add(label)
                        index++
                    }

                    Log.d("TAG", "Entries: $entries")
                    Log.d("TAG", "Labels: $labels")
                   labels.add("")
                    entries.add(Pair(0f,0f))

                    val yMin = entries.minOfOrNull { it.second } ?: 0f
                    val yMax = (entries.maxOfOrNull { it.second } ?: (yMin + 1f)) * 1.1f



                    lineGraphView.setData(entries, labels, yMin, yMax)

                }else{
                    progreeBar.visibility=View.GONE
                    Toast.makeText(this@ExpenseStaticsActivity, "No Data avalaible", Toast.LENGTH_SHORT).show()

                }

            }

            override fun onCancelled(error: DatabaseError) {
                progreeBar.visibility=View.GONE
                Toast.makeText(this@ExpenseStaticsActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

}