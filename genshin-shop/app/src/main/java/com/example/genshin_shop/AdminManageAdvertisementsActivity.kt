package com.example.genshin_shop

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AdminManageAdvertisementsActivity : AppCompatActivity() {

    // Firebase References
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage


    private lateinit var imageView: ImageView
    private lateinit var uploadButton: Button
    private lateinit var saveButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adAdapter: AdminAdvertisementAdapter

    private val advertisements = mutableListOf<ItemAdvertisement>()
    private var imageUri: Uri? = null

    // Image Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_manage_advertisements)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        storage = FirebaseStorage.getInstance()


        imageView = findViewById(R.id.ad_image_view)
        uploadButton = findViewById(R.id.upload_image_button)
        saveButton = findViewById(R.id.save_ad_button)
        recyclerView = findViewById(R.id.ads_recycler_view)

        // Initialize RecyclerView
        adAdapter = AdminAdvertisementAdapter(advertisements,
            onDeleteClick = { advertisement -> deleteAdvertisement(advertisement) },
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adAdapter

        // Set Click Listeners
        uploadButton.setOnClickListener { pickImageLauncher.launch("image/*") }
        saveButton.setOnClickListener { saveAdvertisement() }

        // Load existing advertisements
        fetchAdvertisements()
    }

    // Save a new advertisement
    private fun saveAdvertisement() {
        val keywordsInput: EditText = findViewById(R.id.keywords_input)
        val keywords = keywordsInput.text.toString()
            .split(",") // Split by comma
            .map { it.trim() }
            .filter { it.isNotEmpty() } // Clean up empty entries

        if (imageUri == null || keywords.isEmpty()) {
            Toast.makeText(this, "Please select an image and enter keywords", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload image to Firebase Storage
        val imageRef = storage.reference.child("advertisement_images/${UUID.randomUUID()}.jpg")
        imageRef.putFile(imageUri!!).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                val ad = ItemAdvertisement(imageUrl, keywords)

                // Save advertisement details to Firebase Database
                database.child("advertisements").push().setValue(ad)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Advertisement saved", Toast.LENGTH_SHORT).show()
                        imageView.setImageResource(0) // Clear the image view
                        keywordsInput.text.clear() // Clear input fields
                        fetchAdvertisements() // Refresh the list
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save advertisement", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Fetch existing advertisements from Firebase Database
    private fun fetchAdvertisements() {
        database.child("advertisements").get().addOnSuccessListener { snapshot ->
            advertisements.clear()
            for (adSnapshot in snapshot.children) {
                val imageUrl = adSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                val keywords = adSnapshot.child("keywords").getValue<List<String>>() ?: listOf()
                val ad = ItemAdvertisement(imageUrl, keywords, adSnapshot.key)
                advertisements.add(ad)
            }
            adAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load advertisements", Toast.LENGTH_SHORT).show()
        }
    }

    // Delete an advertisement
    private fun deleteAdvertisement(ad: ItemAdvertisement) {
        val adRef = database.child("advertisements").child(ad.id ?: return)
        adRef.removeValue()
            .addOnSuccessListener {
                val imageRef = storage.getReferenceFromUrl(ad.imageUrl)
                imageRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Advertisement deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
                    }
                fetchAdvertisements() // Refresh advertisement list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete advertisement", Toast.LENGTH_SHORT).show()
            }
    }

}
