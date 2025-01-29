package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserSearchResultsActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private var filteredProducts: MutableList<Product> = mutableListOf()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_search_results)

        // Initialize Firebase database
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference.child("products")

        // Initialize views
        searchBar = findViewById(R.id.search_bar)
        recyclerView = findViewById(R.id.recycler_view)

        // Set up the RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        productAdapter = ProductAdapter(filteredProducts, isAdmin = false) { selectedProduct ->
            openProductDetail(selectedProduct)
        }
        recyclerView.adapter = productAdapter

        // Retrieve the keyword from the intent
        val keyword = intent.getStringExtra("keyword") ?: ""

        if (keyword.isNotEmpty()) {
            searchProducts(keyword)
        } else {
            Toast.makeText(this, "No keyword provided for search.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchProducts(keyword: String) {
        // Split the keyword string into a list of keywords
        val keywords = keyword.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchedProducts = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
                    .filter { product ->
                        // Check if product matches any of the keywords
                        keywords.any { keyword ->
                            product.name.contains(keyword, ignoreCase = true) ||
                                    product.description.contains(keyword, ignoreCase = true)
                        }
                    }

                if (matchedProducts.isNotEmpty()) {
                    filteredProducts.clear()
                    filteredProducts.addAll(matchedProducts)
                    productAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@UserSearchResultsActivity, "No products match your search.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserSearchResultsActivity, "Error occurred: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun openProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra("product", product) // Pass the selected product to the detail activity
        startActivity(intent)
    }
}
