package com.example.snapntrack

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapntrack.util.ReceiptAdapter
import com.example.snapntrack.util.ReceiptItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EditActivity : AppCompatActivity() {
    private lateinit var itemQua: ArrayList<String>
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var txtShopName: TextView
    private lateinit var editTotalCost: EditText
    private lateinit var editDiscount: EditText
    private lateinit var adapter: ReceiptAdapter
    private lateinit var shopName: String
    private var totalCost: Double = 0.0
    private var discount: Double = 0.0
    private var vat: Double = 0.0
    private lateinit var issueDate: String
    private var itemList: MutableList<ReceiptItem> = mutableListOf()
    private var isSaved = false
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = findViewById(R.id.recyclerView)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        txtShopName = findViewById(R.id.shopName)
        editTotalCost = findViewById(R.id.editTotalCost)
        editDiscount = findViewById(R.id.editDiscount)
        progressBar = findViewById(R.id.progressBars)
        editTotalCost.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editDiscount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        // Retrieve data from intent

         shopName = intent.getStringExtra("issuerName") ?: "Unknown Shop"
         val totalCosts = intent.getDoubleExtra("totalCost", 0.0)
        vat = intent.getDoubleExtra("discount", 0.0)
         val  discounts = intent.getDoubleExtra("discount", 0.0)
        issueDate = intent.getStringExtra("issueDate") ?: "Unknown Date"
        val itemNames = intent.getStringArrayListExtra("itemNames") ?: arrayListOf()
        val itemTotalCosts = intent.getStringArrayListExtra("itemTotalCosts") ?: arrayListOf()
         itemQua = intent.getStringArrayListExtra("itemQua") ?: arrayListOf()

        // Combine item names and costs into a list of pairs
        for (i in itemNames.indices) {
            val itemCost = itemTotalCosts.getOrNull(i)?.toDoubleOrNull() ?: 0.0
            val itemQuan = itemQua.getOrNull(i)?.toIntOrNull() ?: 1

            itemList.add(ReceiptItem(itemNames[i], itemCost,itemQuan))
        }

        txtShopName.text = shopName
        editTotalCost.setText(totalCosts.toString())
        editDiscount.setText(discounts.toString())



        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReceiptAdapter(itemList) { position ->
            showEditItemDialog(position)
        }
        recyclerView.adapter = adapter

        btnSave.setOnClickListener {
            saveReceiptToFirebase()
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showEditItemDialog(position: Int) {
        val item = itemList[position]

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_item, null)
        val editItemName = dialogView.findViewById<EditText>(R.id.editItemName)
        val editItemCost = dialogView.findViewById<EditText>(R.id.editItemCost)

        editItemName.setText(item.name)
        editItemCost.setText(item.cost.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = editItemName.text.toString()
                val newCost = editItemCost.text.toString().toDoubleOrNull() ?: 0.0
                val itemQuan = itemQua.getOrNull(position)?.toIntOrNull() ?: 1
                itemList[position] = ReceiptItem(newName, newCost,itemQuan)
                adapter.notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveReceiptToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        totalCost = editTotalCost.text.toString().toDoubleOrNull() ?: 0.0
        discount = editDiscount.text.toString().toDoubleOrNull() ?: 0.0

        btnSave.isEnabled = false
        btnBack.isEnabled = false
        recyclerView.isEnabled = false
        editTotalCost.isEnabled = false
        editDiscount.isEnabled = false
        progressBar.visibility = View.VISIBLE
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = SimpleDateFormat("MM", Locale.getDefault()).format(calendar.time)
       val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
        val weeklyKey = "$year-W$weekNumber"
        val monthlyKey = "$year-$month"
        val yearlyKey = "$year"

        val database = FirebaseDatabase.getInstance().reference
        val receiptRef = database.child("scanned").child(userId).push()
        val userRef = database.child("users").child(userId)

        userRef.child("totalSpendAmount").get().addOnSuccessListener { snapshot ->
            val storedTotalAmount = snapshot.getValue(Double::class.java) ?: 0.0
            val updatedTotalAmount = storedTotalAmount + totalCost
            userRef.child("totalSpendAmount").setValue(updatedTotalAmount)
        }

        // Update Weekly Spend
        userRef.child("weeklySpend").child(weeklyKey).get().addOnSuccessListener { snapshot ->
            val storedWeeklyAmount = snapshot.getValue(Double::class.java) ?: 0.0
            val updatedWeeklyAmount = storedWeeklyAmount + totalCost
            userRef.child("weeklySpend").child(weeklyKey).setValue(updatedWeeklyAmount)
        }

        // Update Monthly Spend
        userRef.child("monthlySpend").child(monthlyKey).get().addOnSuccessListener { snapshot ->
            val storedMonthlyAmount = snapshot.getValue(Double::class.java) ?: 0.0
            val updatedMonthlyAmount = storedMonthlyAmount + totalCost
            userRef.child("monthlySpend").child(monthlyKey).setValue(updatedMonthlyAmount)
        }

        // Update Yearly Spend
        userRef.child("yearlySpend").child(yearlyKey).get().addOnSuccessListener { snapshot ->
            val storedYearlyAmount = snapshot.getValue(Double::class.java) ?: 0.0
            val updatedYearlyAmount = storedYearlyAmount + totalCost
            userRef.child("yearlySpend").child(yearlyKey).setValue(updatedYearlyAmount)
        }



                val receiptData = mapOf(
                    "shopName" to shopName,
                    "totalCost" to totalCost,
                    "discount" to discount,
                    "issueDate" to issueDate,
                    "vat" to vat,
                    "items" to itemList.map { mapOf("name" to it.name, "price" to it.cost,"qua" to it.qua) }
                )

                receiptRef.setValue(receiptData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Receipt saved successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save receipt", Toast.LENGTH_SHORT).show()
                        enableEditing()
                    }





    }

    private fun enableEditing() {
        progressBar.visibility = View.GONE
        btnSave.isEnabled = true
        btnBack.isEnabled = true
        recyclerView.isEnabled = true
        editTotalCost.isEnabled = true
        editDiscount.isEnabled = true
    }

}