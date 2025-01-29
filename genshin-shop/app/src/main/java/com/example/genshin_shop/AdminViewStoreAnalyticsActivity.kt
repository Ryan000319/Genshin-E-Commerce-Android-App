package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminViewStoreAnalyticsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categorySpinner: Spinner
    private lateinit var productAdapter: ProductAdapter
    private val database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    private lateinit var totalProductsTextView: TextView
    private lateinit var totalIncomeTextView: TextView
    private lateinit var completedOrdersTextView: TextView

    private val allProducts = mutableListOf<Product>()
    private val filteredProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_analytics)

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerViewPopularProducts)
        categorySpinner = findViewById(R.id.categorySpinner)
        totalProductsTextView = findViewById(R.id.totalProducts)
        totalIncomeTextView = findViewById(R.id.totalIncome)
        completedOrdersTextView = findViewById(R.id.completedOrders)

        // Initialize RecyclerView with callbacks
        productAdapter = ProductAdapter(
            products = filteredProducts,
            isAdmin = true,
            onProductEdit = { product -> editProduct(product) },
            onProductDelete = { product -> deleteProduct(product) }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = productAdapter

        // Setup Spinner
        setupCategorySpinner()

        // Fetch Initial Data
        fetchAnalytics()
    }

    private fun setupCategorySpinner() {
        val spinnerOptions = listOf("All Products", "Low Stock Products", "Wishlisted Products")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (spinnerOptions[position]) {
                    "All Products" -> fetchAllProducts()
                    "Low Stock Products" -> fetchLowStockProducts()
                    "Wishlisted Products" -> fetchWishlistedProducts()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchAnalytics() {
        fetchOverallStats()
        fetchTotalIncomeAndOrders()
    }

    private fun fetchOverallStats() {
        database.child("products").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allProducts.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { allProducts.add(it) }
                }
                totalProductsTextView.text = "Total Products: ${allProducts.size}"
                fetchWishlistCount()
                fetchAllProducts() // Default to All Products
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewStoreAnalyticsActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchWishlistCount() {
        database.child("wishlists").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wishlistCounts = mutableMapOf<String, Int>()

                // Count occurrences of product IDs in all wishlists
                for (userSnapshot in snapshot.children) {
                    for (productSnapshot in userSnapshot.children) {
                        val productId = productSnapshot.key
                        productId?.let {
                            wishlistCounts[it] = wishlistCounts.getOrDefault(it, 0) + 1
                        }
                    }
                }

                // Update wishlist count for each product
                allProducts.forEach { product ->
                    product.wishlistCount = wishlistCounts[product.id] ?: 0
                }

                fetchAllProducts() // Refresh RecyclerView with updated wishlist counts
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewStoreAnalyticsActivity, "Failed to load wishlist data", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchTotalIncomeAndOrders() {
        database.child("completed_orders").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalIncome = 0.0
                var completedOrders = 0

                for (userOrders in snapshot.children) {
                    for (orderSnapshot in userOrders.children) {
                        val orderAmount = orderSnapshot.child("totalAmount").getValue(Double::class.java) ?: 0.0
                        totalIncome += orderAmount
                        completedOrders++
                    }
                }

                totalIncomeTextView.text = "Total Income: RM%.2f".format(totalIncome)
                completedOrdersTextView.text = "Completed Orders: $completedOrders"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewStoreAnalyticsActivity, "Failed to load income data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAllProducts() {
        filteredProducts.clear()
        filteredProducts.addAll(allProducts)
        productAdapter.notifyDataSetChanged()
    }

    private fun fetchLowStockProducts() {
        val lowStockProducts = allProducts.filter { it.quantity in 1..9 }
        filteredProducts.clear()
        filteredProducts.addAll(lowStockProducts)
        productAdapter.notifyDataSetChanged()
    }

    private fun fetchWishlistedProducts() {
        database.child("wishlists").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wishlistedProductIds = mutableSetOf<String>()
                for (userSnapshot in snapshot.children) {
                    for (productSnapshot in userSnapshot.children) {
                        productSnapshot.key?.let { wishlistedProductIds.add(it) }
                    }
                }

                val wishlistedProducts = allProducts.filter { it.id in wishlistedProductIds }
                filteredProducts.clear()
                filteredProducts.addAll(wishlistedProducts)
                productAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminViewStoreAnalyticsActivity, "Failed to load wishlisted products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editProduct(product: Product) {
        val intent = Intent(this, AdminEditProductActivity::class.java)
        intent.putExtra("productId", product.id)
        startActivity(intent)
    }

    private fun deleteProduct(product: Product) {
        database.child("products").child(product.id).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
            fetchOverallStats()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show()
        }
    }
}
