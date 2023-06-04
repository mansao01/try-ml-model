package com.example.tryml

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.tryml.databinding.ActivityMainBinding
import com.example.tryml.ml.LiteModelAiyVisionClassifierBirdsV13
import com.example.tryml.ml.ModelV1
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.metadata.schema.Content
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer

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


    private fun outputGenerator(bitmap: Bitmap) {
////      declaring tensor flow lite model variable
        val birdModel = LiteModelAiyVisionClassifierBirdsV13.newInstance(this)

//// converting bitmap into tensor flow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val tfImage = TensorImage.fromBitmap(newBitmap)

//// process the image using trained model and sort it ind descending order
        val outputs = birdModel.process(tfImage)
            .probabilityAsCategoryList.apply {
                sortByDescending { it.score }
           }

////       getting result having high probability
        val highProbabilityOutput = outputs[0]
        binding.tvResult.text = highProbabilityOutput.label
        Log.i("TAG", "outputGenerator $highProbabilityOutput")




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

    private fun Bitmap.convertToByteBuffer(): ByteBuffer {
        // Get the byte count of the bitmap.
        val byteCount = byteCount

        // Allocate a byte buffer with the same size as the bitmap.
        val byteBuffer = ByteBuffer.allocate(byteCount)

        // Copy the pixels from the bitmap to the byte buffer.
        copyPixelsToBuffer(byteBuffer)

        // Rewind the byte buffer so that it can be read from.
        byteBuffer.rewind()

        // Return the byte buffer.
        return byteBuffer
    }

}