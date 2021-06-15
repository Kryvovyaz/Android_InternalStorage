package com.vlad_kryvovyaz.androidinternalstorage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.vlad_kryvovyaz.androidinternalstorage.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        internalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            val isDeletionSuccessful =
                deletePhotoFromInternalStorage(it.name)//delete photo on longClick()
            if (isDeletionSuccessful) {
                loadPhotosFromInternalStorageIntoRecyclerView()
                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete photo.", Toast.LENGTH_SHORT).show()
            }
        }
        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            val isPrivate = binding.switchPrivate.isChecked
            if (isPrivate) {
                val isSavedSuccessfully =
                    savePhotoToInternalStorage(UUID.randomUUID().toString(), it)
                if (isSavedSuccessfully) {
                    loadPhotosFromInternalStorageIntoRecyclerView()
                    Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnTakePhoto.setOnClickListener {
            takePhoto.launch()
        }
        setupInternalStorageRecyclerView()
        loadPhotosFromInternalStorageIntoRecyclerView()
    }

    private fun setupInternalStorageRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = internalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }


    private fun loadPhotosFromInternalStorageIntoRecyclerView() {
        lifecycleScope.launch {
            val photos = loadPhotosFromInternalStorage()
            internalStoragePhotoAdapter.submitList(photos)

        }
    }

    private fun deletePhotoFromInternalStorage(fileName: String): Boolean {
        return try {
            deleteFile(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadPhotosFromInternalStorage(): List<InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            } ?: listOf()
        }
    }

    private fun savePhotoToInternalStorage(fileName: String, bmp: Bitmap): Boolean {
        return try {
            //this created file can only be accssesed by this application
            openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                if (!bmp.compress((Bitmap.CompressFormat.JPEG), 95, stream)) {
                    throw  IOException("Could not save bitmap.")
                }
            }//use is extension fun that will imidiatly close our stream after we wrote to it or exception occured
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}