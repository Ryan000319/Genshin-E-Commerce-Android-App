package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserWishlistedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter

    private val database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val wishlistRef = database.getReference("wishlists")
    private val productsRef = database.getReference("products")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var allProducts: MutableList<Product> = mutableListOf() // Cache for all products
    private var wishlistProductIds: MutableSet<String> = mutableSetOf() // Set for wishlist product IDs

    private var productsListener: ValueEventListener? = null
    private var wishlistListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_wishlisted, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recommended_products_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize adapter with click handling
        productAdapter = ProductAdapter(mutableListOf(), isAdmin = false, onProductClick = { product ->
            navigateToProductDetail(product)
        })
        recyclerView.adapter = productAdapter

        productAdapter.setupWishlistRealtimeListener()

        // Fetch both wishlist and products
        observeUserWishlist()
        fetchAllProducts()

        return view
    }

    private fun fetchAllProducts() {
        productsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allProducts.clear()

                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null && product.quantity > 0) { // Exclude products with zero stock
                        allProducts.add(product)
                    }
                }

                filterWishlistProducts()
                Log.d("UserWishlistedFragment", "All products fetched: ${allProducts.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserWishlistedFragment", "Error fetching products: ${error.message}", error.toException())
            }
        }
        productsRef.addValueEventListener(productsListener!!)
    }

    private fun observeUserWishlist() {
        userId?.let { uid ->
            wishlistListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wishlistProductIds.clear()
                    for (itemSnapshot in snapshot.children) {
                        itemSnapshot.key?.let { wishlistProductIds.add(it) }
                    }

                    filterWishlistProducts()
                    Log.d("UserWishlistedFragment", "Wishlist product IDs: $wishlistProductIds")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserWishlistedFragment", "Error fetching wishlist: ${error.message}", error.toException())
                }
            }
            wishlistRef.child(uid).addValueEventListener(wishlistListener!!)
        }
    }

    private fun filterWishlistProducts() {
        val filteredProducts = allProducts.filter { it.id in wishlistProductIds }
        productAdapter.updateProducts(filteredProducts)
        Log.d("UserWishlistedFragment", "Filtered wishlist products: ${filteredProducts.size}")
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        intent.putExtra("product", product) // Pass the product object
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        wishlistListener?.let { wishlistRef.child(userId!!).removeEventListener(it) }
        productsListener?.let { productsRef.removeEventListener(it) }
        productAdapter.removeWishlistListener()
    }
}
