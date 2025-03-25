package com.example.snapntrack

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ScanReceiptActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView

    private lateinit var btnCapture: AppCompatButton
    private lateinit var btnNext: AppCompatButton
    private var currentPhotoPath: String? = null
    private lateinit var progressBar: ProgressBar

    private var issuerName: String? = null
    private var totalCost: Double? = null
    private var discount: Double? = null
    private var issueDate: String? = null
    private var vat: Double? = null

    private val itemNames = mutableListOf<String>()
    private val itemTotalCosts = mutableListOf<String>()
    private val itemQua = mutableListOf<String>()

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val REQUEST_CAMERA_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_receipt)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.imageView)
        btnCapture = findViewById(R.id.btnCapture)
        btnNext = findViewById(R.id.btnNext)
        progressBar = findViewById(R.id.progressBars)

        btnNext.visibility = View.GONE
        progressBar.visibility = View.GONE


        btnCapture.setOnClickListener {
            checkCameraPermission()
        }

        btnNext.setOnClickListener {
            val intent = Intent(this, EditActivity::class.java).apply {
                putExtra("issuerName", issuerName)
                putExtra("totalCost", totalCost)
                putExtra("discount", discount)
                putExtra("issueDate", issueDate)
                putExtra("vat", vat)
                putStringArrayListExtra("itemNames", ArrayList(itemNames))
                putStringArrayListExtra("itemTotalCosts", ArrayList(itemTotalCosts))
                putStringArrayListExtra("itemQua", ArrayList(itemQua))
            }
            startActivity(intent)

        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile() ?: return
        val photoURI: Uri =
            FileProvider.getUriForFile(this, "com.example.snapntrack.fileprovider", photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let {
                imageView.setImageURI(Uri.fromFile(File(it)))
                processImageFile(File(it))
            }
        }
    }

    private fun processImageFile(imageFile: File) {
        //  val client = OkHttpClient()
        progressBar.visibility = View.VISIBLE
        btnCapture.isEnabled = false
        btnNext.visibility = View.GONE

        val client = OkHttpClient.Builder()
            .connectTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val apiKey = "jHSviLtYraebPuFghoNibuIftA9wot7A"
        val url = "https://api.iapp.co.th/ocr/v3/receipt/file"


        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                imageFile.name,
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
            )
            .addFormDataPart("return_image", "false")
            .addFormDataPart("return_ocr", "false")
            .build()


        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("apikey", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {

                progressBar.visibility = View.GONE
                btnCapture.isEnabled = true
                }
                Log.e("TAG", "onFailure: ${e.message}")

            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("TAG", "onResponse: $responseBody")
                response.use {
                    if (!response.isSuccessful) {

                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            btnCapture.isEnabled = true
                            Toast.makeText(this@ScanReceiptActivity, "Failed to process receipt", Toast.LENGTH_SHORT).show()
                        }

                        throw IOException("Unexpected code $response")

                    } else {
                        try {
                            val jsonObject = JSONObject(responseBody ?: "{}")
                            val processed = jsonObject.getJSONObject("processed")

                            issuerName = processed.optString("issuerName", "N/A")
                            totalCost = processed.optDouble("grandTotal", 0.0)
                            if (totalCost == 0.0) {
                                totalCost = processed.optDouble("totalCost", 0.0)
                            }
                            vat = processed.optDouble("vat", 0.0)


                            discount = processed.optDouble("discount", 0.0)
                            issueDate = processed.optString("invoiceDate", "N/A")
                            val itemsArray = processed.getJSONArray("items")
                            itemNames.clear()
                            itemTotalCosts.clear()
                            itemQua.clear()
                            for (i in 0 until itemsArray.length()) {
                                val item = itemsArray.getJSONObject(i)
                                itemNames.add(item.optString("itemName", "Unknown"))
                                itemTotalCosts.add(item.optString("itemTotalCost", "0.0"))
                                itemQua.add(item.optString("itemUnit", "1"))

                            }




                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                btnCapture.isEnabled = true
                                btnNext.visibility = View.VISIBLE

                                Toast.makeText(this@ScanReceiptActivity, "OCR Successful!", Toast.LENGTH_SHORT).show()
                            }



                        } catch (e: Exception) {


                                Log.e("TAG", "onResponse1:"+e.message)
                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                btnCapture.isEnabled = true
                                Toast.makeText(this@ScanReceiptActivity, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }

//
}