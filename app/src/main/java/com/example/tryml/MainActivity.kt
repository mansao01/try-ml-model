package com.example.tryml

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.tryml.databinding.ActivityMainBinding
import com.example.tryml.ml.LiteModelAiyVisionClassifierBirdsV13
import com.example.tryml.ml.ModelV1
import com.example.tryml.ml.Modelv1Revise2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.metadata.schema.Content
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val GALLERY_REQUEST_CODE = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        takePicture()
        loadGallery()
        binding.tvResult.setOnClickListener {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${binding.tvResult.text}")
            ).apply {
                startActivity(this)
            }
        }
    }


    private fun outputGenerator(mBitmap: Bitmap) {

        try {
            val model = Modelv1Revise2.newInstance(this)
            val bitmap = Bitmap.createScaledBitmap(mBitmap, 224, 224, true)

            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            val byteBuffer = tensorImage.buffer

            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            model.close()
            Toast.makeText(this, outputFeature0.floatArray[0].toString(), Toast.LENGTH_LONG).show()
//            binding.tvResult.text = outputFeature0.toString()
//            Log.i("Result", outputFeature0.floatArray[0].toString())
//            Log.i("Result", outputFeature0.floatArray[1].toString())
            outputFeature0.floatArray.forEach {
                Log.i("Result", it.toString())
            }


        } catch (e: IllegalArgumentException) {
            Log.e("TAG", e.toString())

        }
    }

    private fun onResultReceived(requestCode: Int, result: ActivityResult?) {
        when (requestCode) {
            GALLERY_REQUEST_CODE -> {
                if (result?.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let {
                        Log.i("TAG", "onResultReceived: $it")
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it))
                        binding.ivImage.setImageBitmap(bitmap)
                        outputGenerator(bitmap)
                    }
                } else {
                    Log.e("TAG", "onActivityResult : error in selecting image")
                }
            }
        }
    }

    //    to get image from gallery
    private val onResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.i("TAG", "this is the result: ${it.data} ${it.resultCode}")
            onResultReceived(GALLERY_REQUEST_CODE, it)
        }

    //        request camera permission
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                takePicturePreview.launch(null)
            } else {
                Toast.makeText(this, "Permission denied!!, Try again", Toast.LENGTH_SHORT).show()
            }
        }

    //        launch camera and take picture
    private val takePicturePreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            if (it != null) {
                binding.ivImage.setImageBitmap(it)
                outputGenerator(it)
            }
        }


    private fun loadGallery() {
        binding.btnLoadImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                val mimeTypes = arrayOf("image/jpeg", "image/png", "image/")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                onResult.launch(intent)
            } else {
                requestPermission.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun takePicture() {
        binding.btnTakePict.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePicturePreview.launch(null)
            } else {
                requestPermission.launch(android.Manifest.permission.CAMERA)
            }
        }
    }
}