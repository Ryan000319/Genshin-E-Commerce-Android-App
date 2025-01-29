package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlin.collections.mutableSetOf

class AdminManageProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var database: DatabaseReference
    private lateinit var addProductButton: Button
    private lateinit var categoryFilterSpinner: Spinner

    private lateinit var addProductLauncher: ActivityResultLauncher<Intent>
    private lateinit var editProductLauncher: ActivityResultLauncher<Intent>

    private val productList = mutableListOf<Product>()
    private val displayedList = mutableListOf<Product>()
    private val categoryList = mutableSetOf<String>() // Unique categories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_manage_products)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize Views
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        addProductButton = findViewById(R.id.addProductButton)
        recyclerView = findViewById(R.id.productsRecyclerView)

        // Initialize RecyclerView and Adapter
        productAdapter = ProductAdapter(displayedList, isAdmin = true,
            onProductEdit = { product -> editProduct(product) },
            onProductDelete = { product -> deleteProduct(product) })

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = productAdapter

        // Set up category filter spinner
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyCategoryFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Launchers for adding/editing products
        addProductLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) loadProducts()
        }
        editProductLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) loadProducts()
        }

        // Load products and categories
        loadProducts()

        addProductButton.setOnClickListener {
            val intent = Intent(this, AdminAddProductActivity::class.java)
            addProductLauncher.launch(intent)
        }

        val searchBar: EditText = findViewById(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchQuery = s.toString().trim()
                filterProductsBySearch(searchQuery)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterProductsBySearch(query: String) {
        val selectedCategory = categoryFilterSpinner.selectedItem?.toString() ?: "All"

        // Filter products by search query and category
        val filteredProducts = productList.filter { product ->
            val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
            val matchesSearch = product.name.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        // Update displayed list and notify the adapter
        displayedList.clear()
        displayedList.addAll(filteredProducts)
        productAdapter.notifyDataSetChanged()
    }


    private fun loadProducts() {
        // Listen for real-time updates on products
        database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(productSnapshot: DataSnapshot) {
                productList.clear()
                categoryList.clear()
                categoryList.add("All") // Default category option

                for (productNode in productSnapshot.children) {
                    val product = productNode.getValue(Product::class.java)
                    product?.let {
                        productList.add(it)
                        categoryList.add(it.category) // Collect unique categories
                    }
                }

                // After loading products, fetch wishlisted counts
                fetchWishlistCounts()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AdminManageProductsActivity,
                    "Failed to load products: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun fetchWishlistCounts() {
        database.child("wishlists").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(wishlistSnapshot: DataSnapshot) {
                val wishlistCounts = mutableMapOf<String, Int>()

                // Count occurrences of product IDs in all wishlists
                for (userWishlist in wishlistSnapshot.children) {
                    for (productNode in userWishlist.children) {
                        val productId = productNode.key
                        productId?.let {
                            wishlistCounts[it] = wishlistCounts.getOrDefault(it, 0) + 1
                        }
                    }
                }

                // Update each product with its wishlist count
                productList.forEach { product ->
                    product.wishlistCount = wishlistCounts[product.id] ?: 0
                }

                setupCategorySpinner() // Update spinner with categories
                applyCategoryFilter() // Display products by selected category
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminManageProductsActivity, "Failed to load wishlist data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = adapter
    }

    private fun applyCategoryFilter() {
        val selectedCategory = categoryFilterSpinner.selectedItem?.toString() ?: "All"

        displayedList.clear()
        displayedList.addAll(
            if (selectedCategory == "All") productList
            else productList.filter { it.category == selectedCategory }
        )

        productAdapter.notifyDataSetChanged()
    }

    private fun editProduct(product: Product) {
        val intent = Intent(this, AdminEditProductActivity::class.java)
        intent.putExtra("productId", product.id)
        editProductLauncher.launch(intent)
    }

    private fun deleteProduct(product: Product) {
        database.child("products").child(product.id).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
            loadProducts()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show()
        }
    }
}
