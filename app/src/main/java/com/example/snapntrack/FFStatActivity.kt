package com.example.snapntrack

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.snapntrack.util.PieChartView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class FFStatActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var progreeBar: ProgressBar
    private lateinit var pieChartView: PieChartView
    private lateinit var total:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ffstat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
        pieChartView = findViewById(R.id.pieChartView)
       total=findViewById(R.id.textTotal)

        progreeBar=findViewById(R.id.progressBars)

        loadItemFromDatabase()
    }

    private fun loadItemFromDatabase() {
        progreeBar.visibility = View.VISIBLE
        val userId = auth.currentUser?.uid ?: return
        val scannedRef = database.child("scanned").child(userId)

        scannedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    progreeBar.visibility = View.GONE
                    pieChartView.visibility = View.VISIBLE

                    val itemMap = mutableMapOf<String, Int>()
                    var totalItems = 0

                    for (receiptSnapshot in snapshot.children) {
                        val items = receiptSnapshot.child("items").children
                        for (itemSnapshot in items) {
                            val itemName = itemSnapshot.child("name").getValue(String::class.java)
                            if (itemName != null) {
                                itemMap[itemName] = itemMap.getOrDefault(itemName, 0) + 1

                                totalItems++

                                total.text = "Total Buy Item: $totalItems"
                            }
                        }
                    }
                    val top2Items = getTop2Items(itemMap)
                    displayPieChart(top2Items, totalItems)


                } else {
                    progreeBar.visibility = View.GONE
                    Toast.makeText(this@FFStatActivity, "No data found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progreeBar.visibility = View.GONE
                Toast.makeText(this@FFStatActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayPieChart(itemMap: Map<String, Int>, totalItems: Int) {
        pieChartView.setData(itemMap, totalItems)
    }

    private fun getTop2Items(itemMap: Map<String, Int>): Map<String, Int> {
        val sortedItems = itemMap.toList().sortedByDescending { (_, value) -> value }

        val top2Items = sortedItems.take(3).toMap()

        return top2Items
    }

}