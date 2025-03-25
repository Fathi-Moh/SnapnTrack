package com.example.snapntrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StatisticsActivity : AppCompatActivity() {
    private lateinit var btnSpend: Button
    private lateinit var btnFF: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnSpend = findViewById(R.id.btnSpending)
        btnFF = findViewById(R.id.btnffC)

        btnBack = findViewById(R.id.btnBack)


        btnSpend.setOnClickListener {
            startActivity(Intent(this, ExpenseStaticsActivity::class.java))
        }
        btnFF.setOnClickListener {
            startActivity(Intent(this, FFStatActivity::class.java))
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

}