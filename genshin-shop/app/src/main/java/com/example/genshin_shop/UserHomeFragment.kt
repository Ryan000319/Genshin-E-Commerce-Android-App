package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class UserHomeFragment : Fragment() {


    private lateinit var advertisementViewPager: ViewPager2
    private lateinit var itemAdvertisementAdapter: ItemAdvertisementAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var shoppingCartIcon: ImageView
    private lateinit var searchBar: EditText

    private val advertisements = mutableListOf<ItemAdvertisement>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var slideRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_home, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize Views
        advertisementViewPager = view.findViewById(R.id.advertisement_view_pager)
        dotsIndicator = view.findViewById(R.id.dots_indicator)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        shoppingCartIcon = view.findViewById(R.id.shopping_cart)
        searchBar = view.findViewById(R.id.search_bar)

        advertisementViewPager.isNestedScrollingEnabled = false
        viewPager.isNestedScrollingEnabled = false


        // Set up click listener for shopping cart icon
        shoppingCartIcon.setOnClickListener {
            navigateToCart()
        }
        setupSearchListener()

        setupViewPagerWithTabs()
        fetchAdvertisements()
        setupAutoSlide()

        return view
    }

    private fun setupViewPagerWithTabs() {
        val adapter = UserHomePagerAdapter(requireActivity())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Characters"
                1 -> "All Products"
                2 -> "Wishlisted"
                else -> ""
            }
        }.attach()
    }

    private fun navigateToCart() {
        // Navigate to CartFragment
        parentFragmentManager.commit {
            replace(R.id.fragment_container, UserCartFragment())
            addToBackStack(null) // Add to backstack
        }
    }

    private fun fetchAdvertisements() {
        itemAdvertisementAdapter = ItemAdvertisementAdapter(
            advertisements,
            isAdmin = false,
            onAdClick = { keywords ->
                openSearchResults(keywords)
            }
        )
        advertisementViewPager.adapter = itemAdvertisementAdapter
        dotsIndicator.attachTo(advertisementViewPager)

        database.child("advertisements").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                advertisements.clear()
                for (adSnapshot in snapshot.children) {
                    val ad = adSnapshot.getValue(ItemAdvertisement::class.java) ?: ItemAdvertisement()
                    advertisements.add(ad)
                }
                itemAdvertisementAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openSearchResults(keywords: List<String>) {
        val query = keywords.joinToString(", ")
        val intent = Intent(requireContext(), UserSearchResultsActivity::class.java)
        intent.putExtra("keyword", query)
        startActivity(intent)
    }

    private fun setupAutoSlide() {
        slideRunnable = Runnable {
            if (advertisements.isNotEmpty()) {
                val currentItem = advertisementViewPager.currentItem
                advertisementViewPager.currentItem = (currentItem + 1) % advertisements.size
            }
            handler.postDelayed(slideRunnable, 3000)
        }
        handler.postDelayed(slideRunnable, 3000)
    }

    private fun setupSearchListener() {
        searchBar.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val query = searchBar.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchProducts(query)
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun searchProducts(query: String) {
        database.child("products").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchedProducts = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
                    .filter { product ->
                        product.name.contains(query, ignoreCase = true) || product.description.contains(
                            query,
                            ignoreCase = true
                        )
                    }

                if (matchedProducts.isNotEmpty()) {
                    val intent = Intent(requireContext(), UserSearchResultsActivity::class.java)
                    intent.putParcelableArrayListExtra("products", ArrayList(matchedProducts))
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(slideRunnable)
    }


}
