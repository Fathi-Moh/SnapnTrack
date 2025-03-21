package com.example.snapntrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.util.ReceiptLogAdapter
import com.example.snapntrack.util.ReceiptLogItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReceiptLogActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReceiptLogAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button
    private lateinit var database: DatabaseReference
    private val receiptList = mutableListOf<ReceiptLogItem>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receipt_log)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBars)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReceiptLogAdapter(receiptList) { receipt -> openReceiptPreview(receipt) }
        recyclerView.adapter = adapter
        btnBack.setOnClickListener {
            finish()
        }

        database = FirebaseDatabase.getInstance().reference
        fetchReceipts()
    }

    private fun fetchReceipts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        database.child("scanned").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    receiptList.clear()
                    for (receiptSnapshot in snapshot.children) {
                        val key = receiptSnapshot.key ?: continue
                        val shopName = receiptSnapshot.child("shopName").getValue(String::class.java) ?: "Unknown Shop"
                        val issueDate = receiptSnapshot.child("issueDate").getValue(String::class.java) ?: "Unknown Shop"

                        val totalCost = receiptSnapshot.child("totalCost").getValue(Double::class.java) ?: 0.0
                        val discount = receiptSnapshot.child("discount").getValue(Double::class.java) ?: 0.0

                        receiptList.add(ReceiptLogItem(key, shopName,issueDate, totalCost, discount))
                    }
                    adapter.notifyDataSetChanged()
                    progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ReceiptLogActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            })
    }

    private fun openReceiptPreview(receipt: ReceiptLogItem) {
        val intent = Intent(this, ReviewPActivity::class.java)
        intent.putExtra("receiptId", receipt.id)
        startActivity(intent)
    }

}