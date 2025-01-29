package com.example.genshin_shop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AdminEditProductActivity : AppCompatActivity() {

    // UI Components
    private lateinit var productNameInput: EditText
    private lateinit var productPriceInput: EditText
    private lateinit var productDescriptionInput: EditText
    private lateinit var productQuantityInput: EditText
    private lateinit var productCategorySpinner: Spinner
    private lateinit var selectImageButton: Button
    private lateinit var updateButton: Button
    private lateinit var productImageView: ImageView
    private lateinit var additionalImagesRecyclerView: RecyclerView
    private lateinit var addAdditionalImageButton: Button
    private lateinit var progressBar: ProgressBar

    // Firebase References
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    // Data Variables
    private var productId: String? = null
    private var mainImageUri: Uri? = null
    private var currentMainImageUrl: String = ""
    private val additionalImageUris = mutableListOf<Uri>()
    private val additionalImageUrls = mutableListOf<String>()

    // Adapters
    private lateinit var additionalImagesAdapter: AdminEditAdditionalImagesAdapter

    // Launchers
    private lateinit var mainImagePickerLauncher: ActivityResultLauncher<Intent>
    private val additionalImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            additionalImageUris.addAll(uris)
            additionalImagesAdapter.addUris(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_edit_product)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        storage = FirebaseStorage.getInstance()

        // Initialize UI Components
        initUI()

        // Get Product ID from Intent
        productId = intent.getStringExtra("productId")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Product ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load Categories and Product Data
        loadCategories { categoriesList ->
            loadProductData(categoriesList)
        }

        // Initialize Main Image Picker
        mainImagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                mainImageUri = result.data?.data
                productImageView.setImageURI(mainImageUri)
            }
        }

        // Set Click Listeners
        selectImageButton.setOnClickListener { openImagePicker() }
        addAdditionalImageButton.setOnClickListener { additionalImagePickerLauncher.launch("image/*") }
        updateButton.setOnClickListener { updateProduct() }
    }

    // Initialize UI components and RecyclerView
    private fun initUI() {
        productNameInput = findViewById(R.id.productNameInput)
        productPriceInput = findViewById(R.id.productPriceInput)
        productDescriptionInput = findViewById(R.id.productDescriptionInput)
        productQuantityInput = findViewById(R.id.productQuantityInput)
        productCategorySpinner = findViewById(R.id.productCategorySpinner)
        selectImageButton = findViewById(R.id.selectImageButton)
        updateButton = findViewById(R.id.updateButton)
        productImageView = findViewById(R.id.productImageView)
        additionalImagesRecyclerView = findViewById(R.id.additionalImagesRecyclerView)
        addAdditionalImageButton = findViewById(R.id.addAdditionalImageButton)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.GONE

        additionalImagesAdapter = AdminEditAdditionalImagesAdapter(mutableListOf()) { position ->
            additionalImageUrls.removeAt(position)
            additionalImagesAdapter.notifyItemRemoved(position)
        }

        additionalImagesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        additionalImagesRecyclerView.adapter = additionalImagesAdapter
    }

    // Load categories into the spinner
    private fun loadCategories(onCategoriesLoaded: (List<String>) -> Unit) {
        val categoriesList = mutableListOf<String>()
        database.child("categories").get().addOnSuccessListener { snapshot ->
            for (categorySnapshot in snapshot.children) {
                categorySnapshot.getValue(String::class.java)?.let { category ->
                    categoriesList.add(category)
                }
            }

            if (categoriesList.isEmpty()) {
                categoriesList.add("No Categories Available")
            }

            val spinnerAdapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesList)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            productCategorySpinner.adapter = spinnerAdapter

            onCategoriesLoaded(categoriesList)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
        }
    }

    // Load product data and populate the UI
    private fun loadProductData(categoriesList: List<String>) {
        database.child("products").child(productId!!).get().addOnSuccessListener { snapshot ->
            val product = snapshot.getValue(Product::class.java)
            product?.let {
                productNameInput.setText(it.name)
                productPriceInput.setText(it.price)
                productDescriptionInput.setText(it.description)
                productQuantityInput.setText(it.quantity.toString())
                currentMainImageUrl = it.mainImage

                Glide.with(this).load(currentMainImageUrl).into(productImageView)

                val categoryPosition = categoriesList.indexOf(it.category)
                if (categoryPosition >= 0) {
                    productCategorySpinner.setSelection(categoryPosition)
                }

                additionalImageUrls.clear()
                additionalImageUrls.addAll(it.images)
                additionalImagesAdapter.setImageUrls(it.images)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load product data", Toast.LENGTH_SHORT).show()
        }
    }

    // Open image picker for main image
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        mainImagePickerLauncher.launch(intent)
    }

    // Update the product in Firebase
    private fun updateProduct() {
        val name = productNameInput.text.toString().trim()
        val price = productPriceInput.text.toString().trim()
        val description = productDescriptionInput.text.toString().trim()
        val quantity = productQuantityInput.text.toString().toIntOrNull() ?: 0
        val category = productCategorySpinner.selectedItem?.toString()?.trim() ?: ""

        if (!validateInput(name, price, description, quantity, category)) return

        progressBar.visibility = View.VISIBLE
        if (mainImageUri != null) {
            uploadMainImage(name, price, description, quantity, category)
        } else {
            uploadAdditionalImages(name, price, description, currentMainImageUrl, quantity, category)
        }
    }

    // Upload main image to Firebase Storage
    private fun uploadMainImage(name: String, price: String, description: String, quantity: Int, category: String) {
        val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}")
        mainImageUri?.let { uri ->
            storageRef.putFile(uri).addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                    uploadAdditionalImages(name, price, description, downloadUrl.toString(), quantity, category)
                }
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to upload main image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Upload additional images to Firebase Storage
    private fun uploadAdditionalImages(name: String, price: String, description: String, mainImageUrl: String, quantity: Int, category: String) {
        val uploadedImageUrls = mutableListOf<String>()
        if (additionalImageUris.isEmpty()) {
            saveProduct(name, price, description, mainImageUrl, quantity, category, additionalImageUrls)
        } else {
            additionalImageUris.forEach { uri ->
                val ref = storage.reference.child("product_images/${UUID.randomUUID()}")
                ref.putFile(uri).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        uploadedImageUrls.add(downloadUrl.toString())
                        if (uploadedImageUrls.size == additionalImageUris.size) {
                            saveProduct(name, price, description, mainImageUrl, quantity, category, uploadedImageUrls)
                        }
                    }
                }
            }
        }
    }

    // Save product to Firebase
    private fun saveProduct(
        name: String,
        price: String,
        description: String,
        mainImageUrl: String,
        quantity: Int,
        category: String,
        images: List<String>
    ) {
        val updatedProduct = Product(
            id = productId!!,
            name = name,
            price = price,
            originalPrice = price,
            description = description,
            mainImage = mainImageUrl,
            quantity = quantity,
            category = category,
            available = quantity > 0,
            images = images
        )

        // Save the product in Firebase
        database.child("products").child(productId!!).setValue(updatedProduct)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show()
            }
    }

    // Validate input fields
    private fun validateInput(name: String, price: String, description: String, quantity: Int, category: String): Boolean {
        return when {
            name.isEmpty() -> {
                productNameInput.error = "Name required"
                false
            }
            price.isEmpty() -> {
                productPriceInput.error = "Price required"
                false
            }
            description.isEmpty() -> {
                productDescriptionInput.error = "Description required"
                false
            }
            quantity <= 0 -> {
                productQuantityInput.error = "Quantity must be greater than zero"
                false
            }
            category.isEmpty() -> {
                Toast.makeText(this, "Please select a product category", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}
