package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserAllProductsFragment : Fragment() {

    private lateinit var categorySpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private val productAdapter = ProductAdapter(mutableListOf(), isAdmin = false)
    private var productListener: ValueEventListener? = null
    private var allProducts: MutableList<Product> = mutableListOf() // Cache for all products
    private val categoryList = mutableSetOf<String>() // Cache for categories

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_all_products, container, false)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Find views
        categorySpinner = view.findViewById(R.id.category_spinner)
        recyclerView = view.findViewById(R.id.all_products_recycler_view)

        productAdapter.setupWishlistRealtimeListener()

        setupRecyclerView()
        fetchCategories()
        fetchProducts()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(context, 2) // Grid with 2 columns
        recyclerView.adapter = productAdapter

        productAdapter.onProductClick = { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra("product", product) // Pass the product object to the activity
            startActivity(intent)
        }
    }

    private fun fetchCategories() {
        database.child("products").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                categoryList.add("All") // Default category

                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { categoryList.add(it.category) } // Collect unique categories
                }

                setupCategorySpinner() // Update spinner with categories
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserAllProductsFragment", "Failed to fetch categories: ${error.message}")
            }
        })
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, categoryList.toList())
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                filterProducts(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchProducts() {
        productListener = database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                allProducts.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { allProducts.add(it) }
                }

                productAdapter.updateProducts(allProducts) // Display all products initially
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserAllProductsFragment", "Failed to load products: ${error.message}")
            }
        })
    }

    private fun filterProducts(category: String) {
        val filteredProducts = if (category == "All") {
            allProducts
        } else {
            allProducts.filter { it.category.equals(category, ignoreCase = true) }
        }

        productAdapter.updateProducts(filteredProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        productListener?.let { database.child("products").removeEventListener(it) }
        productAdapter.removeWishlistListener()
    }
}
