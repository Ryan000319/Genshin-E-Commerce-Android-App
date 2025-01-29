package com.example.genshin_shop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AdminAddProductActivity : AppCompatActivity() {

    private lateinit var productNameInput: EditText
    private lateinit var productPriceInput: EditText
    private lateinit var productDescriptionInput: EditText
    private lateinit var productQuantityInput: EditText
    private lateinit var productCategorySpinner: Spinner
    private lateinit var selectMainImageButton: Button
    private lateinit var selectImagesButton: Button
    private lateinit var addProductButton: Button
    private lateinit var addCategoryButton: Button
    private lateinit var productImageView: ImageView
    private lateinit var selectedImagesRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var mainImageUri: Uri? = null
    private val imageUris = mutableListOf<Uri>()
    private lateinit var adminImagePreviewAdapter: AdminImagePreviewAdapter
    private lateinit var database: DatabaseReference
    private lateinit var mainImagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var multipleImagePickerLauncher: ActivityResultLauncher<Intent>
    private val categories = mutableListOf<String>() // To dynamically store categories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_add_product)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize Views
        productNameInput = findViewById(R.id.productNameInput)
        productPriceInput = findViewById(R.id.productPriceInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productQuantityInput = findViewById(R.id.productQuantityInput)
        productCategorySpinner = findViewById(R.id.productCategorySpinner)
        selectMainImageButton = findViewById(R.id.selectMainImageButton)
        selectImagesButton = findViewById(R.id.selectImagesButton)
        addProductButton = findViewById(R.id.addProductButton)
        addCategoryButton = findViewById(R.id.addCategoryButton)
        productImageView = findViewById(R.id.productImageView)
        selectedImagesRecyclerView = findViewById(R.id.selectedImagesRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = ProgressBar.INVISIBLE

        // Fetch categories from Firebase
        addInitialCategories()
        fetchCategories()

        // Set up RecyclerView for additional images preview
        adminImagePreviewAdapter = AdminImagePreviewAdapter(imageUris)
        selectedImagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        selectedImagesRecyclerView.adapter = adminImagePreviewAdapter

        // Initialize image picker launchers
        mainImagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                mainImageUri = result.data?.data
                productImageView.setImageURI(mainImageUri)
            } else {
                Toast.makeText(this, "Main image selection canceled", Toast.LENGTH_SHORT).show()
            }
        }

        multipleImagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val clipData = result.data?.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val imageUri = clipData.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else {
                    result.data?.data?.let { uri -> imageUris.add(uri) }
                }
                adminImagePreviewAdapter.notifyDataSetChanged()  // Refresh the RecyclerView
                Toast.makeText(this, "Selected ${imageUris.size} images", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listeners
        selectMainImageButton.setOnClickListener { selectMainImage() }
        selectImagesButton.setOnClickListener { selectMultipleImages() }
        addProductButton.setOnClickListener { uploadProductData() }
        addCategoryButton.setOnClickListener { showAddCategoryDialog() }
    }

    // Fetch existing categories from Firebase
    private fun fetchCategories() {
        database.child("categories").get().addOnSuccessListener { snapshot ->
            categories.clear() // Clear existing categories to avoid duplicates
            snapshot.children.mapNotNullTo(categories) { it.getValue(String::class.java) }
            populateCategorySpinner() // Call spinner update here
        }
    }

    // Populate the Spinner with the list of categories
    private fun populateCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productCategorySpinner.adapter = adapter
    }

    // Show dialog to add a new category
    private fun showAddCategoryDialog() {
        val dialogView = EditText(this).apply {
            hint = "Enter Category Name"
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newCategory = dialogView.text.toString().trim()
                if (newCategory.isNotEmpty() && newCategory !in categories) {
                    addCategoryToFirebase(newCategory)
                } else {
                    Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Preload categories into Firebase if not already added
    private fun addInitialCategories() {
        val initialCategories = listOf("Plushies", "Figurines", "Clothing", "Accessories")
        val categoriesRef = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference.child("categories")

        categoriesRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Add categories if they don't already exist
                initialCategories.forEach { category ->
                    categoriesRef.push().setValue(category)
                }
                Toast.makeText(this, "Initial categories added successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Categories already exist in the database", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to check categories: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Add a new category to Firebase
    private fun addCategoryToFirebase(newCategory: String) {
        val newCategoryRef = database.child("categories").push()
        newCategoryRef.setValue(newCategory).addOnSuccessListener {
            categories.add(newCategory)
            populateCategorySpinner()
            Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
        }
    }

    // Launch image picker for main image
    private fun selectMainImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        mainImagePickerLauncher.launch(intent)
    }

    // Launch image picker for multiple images
    private fun selectMultipleImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        multipleImagePickerLauncher.launch(intent)
    }

    // Upload product details and images to Firebase
    private fun uploadProductData() {
        val productName = productNameInput.text.toString().trim()
        val productPrice = productPriceInput.text.toString().trim()
        val productDescription = productDescriptionInput.text.toString().trim()
        val productQuantity = productQuantityInput.text.toString().trim().toIntOrNull() ?: 0
        val productCategory = productCategorySpinner.selectedItem.toString()

        if (validateForm(productName, productPrice, productDescription, productQuantity)) {
            uploadImages(productName, productPrice, productDescription, productQuantity, productCategory)
        } else {
            Toast.makeText(this, "Please correct errors and try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImages(name: String, price: String, description: String, quantity: Int, category: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        Log.d("AddProductActivity", "Starting image upload. Main Image Uri: $mainImageUri, Additional Images Count: ${imageUris.size}")

        val storageRef = FirebaseStorage.getInstance().reference
        val imageUrls = mutableListOf<String>()

        // Upload main image
        mainImageUri?.let { uri ->
            val mainImageRef = storageRef.child("product_images/${UUID.randomUUID()}")
            mainImageRef.putFile(uri).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { mainImageUrl ->
                    Log.d("AddProductActivity", "Main image uploaded successfully: $mainImageUrl")
                    uploadAdditionalImages(name, price, description, quantity, category, mainImageUrl.toString(), imageUrls)
                }
            }.addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        } ?: Toast.makeText(this, "Please select a main image", Toast.LENGTH_SHORT).show()
    }

    private fun uploadAdditionalImages(
        name: String,
        price: String,
        description: String,
        quantity: Int,
        category: String,
        mainImageUrl: String,
        imageUrls: MutableList<String>
    ) {
        if (imageUris.isEmpty()) {
            addProductToDatabase(name, price, description, mainImageUrl, imageUrls, quantity, category)
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
        var uploadedCount = 0
        Log.d("AddProductActivity", "Starting upload for additional images...")

        for (uri in imageUris) {
            val imageRef = storageRef.child("product_images/${UUID.randomUUID()}")
            imageRef.putFile(uri).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { url ->
                    Log.d("AddProductActivity", "Additional image uploaded: $url")
                    imageUrls.add(url.toString())
                    uploadedCount++
                    if (uploadedCount == imageUris.size) {
                        addProductToDatabase(name, price, description, mainImageUrl, imageUrls, quantity, category)
                    }
                }
            }.addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.INVISIBLE
            }
        }
    }

    private fun addProductToDatabase(
        name: String,
        price: String,
        description: String,
        mainImageUrl: String,
        imageUrls: List<String>,
        quantity: Int,
        category: String
    ) {
        Log.d("AddProductActivity", "Adding product to database...")

        val productId = database.child("products").push().key ?: run {
            Log.e("AddProductActivity", "Error: Could not generate product ID")
            Toast.makeText(this, "Error: Could not generate product ID", Toast.LENGTH_SHORT).show()
            progressBar.visibility = ProgressBar.INVISIBLE
            return
        }

        Log.d("AddProductActivity", "Generated Product ID: $productId")

        val product = Product(
            id = productId,
            name = name,
            price = price,
            description = description,
            mainImage = mainImageUrl,
            quantity = quantity,
            category = category,
            available = quantity > 0,
            images = imageUrls
        )

        Log.d("AddProductActivity", "Product object: $product")

        database.child("products").child(productId).setValue(product)
            .addOnSuccessListener {
                Log.d("AddProductActivity", "Product added successfully to the database")
                Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.INVISIBLE
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AddProductActivity", "Failed to add product to the database: ${e.message}")
                Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.INVISIBLE
            }
    }

    // Validate form input
    private fun validateForm(name: String, price: String, description: String, quantity: Int): Boolean {
        return when {
            name.isEmpty() -> {
                productNameInput.error = "Product name required"
                false
            }
            price.isEmpty() -> {
                productPriceInput.error = "Product price required"
                false
            }
            !price.matches("\\d+(\\.\\d{1,2})?".toRegex()) -> {
                productPriceInput.error = "Invalid price format"
                false
            }
            description.isEmpty() -> {
                productDescriptionInput.error = "Product description required"
                false
            }
            quantity <= 0 -> {
                productQuantityInput.error = "Product quantity required"
                false
            }
            mainImageUri == null -> {
                Toast.makeText(this, "Please select a main image", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}
