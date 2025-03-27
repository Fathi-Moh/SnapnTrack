package com.example.snapntrack

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.util.ReceiptAdapterTwo
import com.example.snapntrack.util.ReceiptItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReviewPActivity : AppCompatActivity() {
    private lateinit var txtShopName: TextView
    private lateinit var txtTotalCost: TextView
    private lateinit var txtDiscount: TextView
    private lateinit var txtVat: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReceiptAdapterTwo
    private lateinit var progressBar: ProgressBar
    private lateinit var layout: ConstraintLayout
    private lateinit var database: DatabaseReference
    private val itemList = mutableListOf<ReceiptItem>()
    private lateinit var btnBack: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_review_pactivity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        txtShopName = findViewById(R.id.shopName)
        txtTotalCost = findViewById(R.id.totalCost)
        layout = findViewById(R.id.mainLayout)
        txtVat = findViewById(R.id.textVat)
        txtDiscount = findViewById(R.id.textDiscount)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBars)
        btnBack = findViewById(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReceiptAdapterTwo(itemList)
        recyclerView.adapter = adapter

        val receiptId = intent.getStringExtra("receiptId")
        if (receiptId != null) {
            fetchReceiptData(receiptId)
        } else {
            Toast.makeText(this, "Invalid receipt ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun fetchReceiptData(receiptId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        database = FirebaseDatabase.getInstance().reference.child("scanned").child(userId).child(receiptId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopName = snapshot.child("shopName").getValue(String::class.java) ?: "Unknown Shop"
                val totalCost = snapshot.child("totalCost").getValue(Double::class.java) ?: 0.0
                val vat = snapshot.child("vat").getValue(Double::class.java) ?: 0.0
                val discount = snapshot.child("discount").getValue(Double::class.java) ?: 0.0
                itemList.clear()

                for (itemSnapshot in snapshot.child("items").children) {
                    val name = itemSnapshot.child("name").getValue(String::class.java) ?: "Unknown Item"
                    val price = itemSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                    val qua = itemSnapshot.child("qua").getValue(Int::class.java) ?: 1

                    itemList.add(ReceiptItem(name, price,qua))
                }

                txtShopName.text = shopName
                txtTotalCost.text = "£${" %.2f".format(totalCost)}"
                txtDiscount.text = "£${" %.2f".format(discount)}"
                txtVat.text = "£${" %.2f".format(vat)}"
                adapter.notifyDataSetChanged()
                layout.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ReviewPActivity, "Failed to load receipt", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        })
    }

}