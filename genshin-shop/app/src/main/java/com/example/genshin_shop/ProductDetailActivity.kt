package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var database: DatabaseReference

    private lateinit var product: Product
    private var selectedQuantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_product_detail)

        product = intent.getParcelableExtra("product") ?: return


        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference

        setupImageSlider()
        setupViewPager()

        findViewById<ImageView>(R.id.increaseQuantityButton).setOnClickListener {
            selectedQuantity++
            updateQuantityUI()
        }

        findViewById<ImageView>(R.id.decreaseQuantityButton).setOnClickListener {
            if (selectedQuantity > 1) {
                selectedQuantity--
                updateQuantityUI()
            } else {
                Toast.makeText(this, "Minimum quantity is 1", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.addToCartButton).setOnClickListener { addToCart() }
        findViewById<TextView>(R.id.buyNowButton).setOnClickListener { buyNow() }

        updateQuantityUI()
    }

    private fun setupImageSlider() {
        val productImages = product.images
        if (productImages.isNotEmpty()) {
            val imageAdapter = ProductImageAdapter(productImages)
            val viewPager = findViewById<ViewPager2>(R.id.productImageSlider)
            val dotsIndicator = findViewById<DotsIndicator>(R.id.dots_indicator)

            viewPager.adapter = imageAdapter
            dotsIndicator.attachTo(viewPager)
        } else {
            Toast.makeText(this, "No product images available", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupViewPager() {
        val fragments = listOf(
            ProductDetailDescriptionFragment.newInstance(product),
            ProductDetailReviewsFragment.newInstance(product.id)
        )
        val titles = listOf("Description", "Reviews")

        val adapter = ProductDetailPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun updateQuantityUI() {
        findViewById<TextView>(R.id.quantityTextView).text = selectedQuantity.toString()
    }

    private fun addToCart() {
        if (product.quantity >= selectedQuantity) {
            val orderItem = OrderItem(
                itemId = product.id,
                name = product.name,
                quantity = selectedQuantity,
                price = product.price.toDouble()
            )
            UserCartManager.addToCart(orderItem)
            Toast.makeText(this, "Added to Cart", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buyNow() {
        if (product.quantity >= selectedQuantity) {
            // Deduct stock and update sold count
            updateStockAndSoldCount(product.id, selectedQuantity)

            val orderItem = OrderItem(
                itemId = product.id,
                name = product.name,
                quantity = selectedQuantity,
                price = product.price.toDouble()
            )
            navigateToCheckout(orderItem)
        } else {
            Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStockAndSoldCount(productId: String, quantityPurchased: Int) {
        val productRef = database.child("products").child(productId)
        productRef.get().addOnSuccessListener { snapshot ->
            val currentStock = snapshot.child("quantity").getValue(Int::class.java) ?: 0
            val soldCount = snapshot.child("soldCount").getValue(Int::class.java) ?: 0

            val newStock = currentStock - quantityPurchased
            val newSoldCount = soldCount + quantityPurchased

            productRef.child("quantity").setValue(newStock)
            productRef.child("soldCount").setValue(newSoldCount)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update stock", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCheckout(orderItem: OrderItem) {
        val intent = Intent(this, UserCheckoutActivity::class.java)
        intent.putExtra("orderItem", orderItem)
        startActivity(intent)
    }
}
