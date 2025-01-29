package com.example.genshin_shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ProductDetailReviewsFragment : Fragment() {

    private lateinit var productId: String
    private val ratingsList = mutableListOf<ProductRating>()
    private lateinit var ratingsAdapter: ProductRatingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = arguments?.getString("productId") ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.user_fragment_product_reviews, container, false)

        // Use the same database URL for the adapter’s database reference
        val rootRef = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        val recyclerView = view.findViewById<RecyclerView>(R.id.reviewsRecyclerView)
        ratingsAdapter = ProductRatingAdapter(ratingsList, rootRef)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ratingsAdapter

        loadRatings(rootRef)
        return view
    }

    private fun loadRatings(rootRef: DatabaseReference) {
        val ratingsRef = rootRef.child("product_ratings").child(productId)

        println("Fetching ratings for productId: $productId")

        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ratingsList.clear()
                for (ratingNode in snapshot.children) {
                    val rating = ratingNode.getValue(ProductRating::class.java)
                    if (rating != null) {
                        println("Fetched rating: $rating")
                        ratingsList.add(rating)
                    } else {
                        println("Skipped invalid rating node: $ratingNode")
                    }
                }
                println("Total ratings loaded: ${ratingsList.size}")
                ratingsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error loading ratings: ${error.message}")
            }
        })
    }

    companion object {
        fun newInstance(productId: String): ProductDetailReviewsFragment {
            val fragment = ProductDetailReviewsFragment()
            val args = Bundle()
            args.putString("productId", productId)
            fragment.arguments = args
            return fragment
        }
    }
}
