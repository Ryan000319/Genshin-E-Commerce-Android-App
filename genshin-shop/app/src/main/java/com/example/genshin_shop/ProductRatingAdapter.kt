package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class ProductRatingAdapter(
    private val ratings: List<ProductRating>,
    private val database: DatabaseReference // Firebase database reference
) : RecyclerView.Adapter<ProductRatingAdapter.RatingViewHolder>() {

    inner class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userNameTextView)
        val starContainer: LinearLayout = itemView.findViewById(R.id.starContainer)
        val userComment: TextView = itemView.findViewById(R.id.userCommentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_rating, parent, false)
        return RatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        val rating = ratings[position]

        database.child("users").child(rating.userId).child("username")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.getValue(String::class.java)
                    holder.userName.text = username ?: "Unknown User"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.userName.text = "Unknown User"
                }
            })

        // Display star rating
        holder.starContainer.removeAllViews()
        for (i in 1..5) {
            val starView = ImageView(holder.starContainer.context).apply {
                setImageResource(
                    if (i <= rating.rating) R.drawable.ic_star_rating else R.drawable.ic_star_rating_outline
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(4, 0, 4, 0) }
            }
            holder.starContainer.addView(starView)
        }

        // Set the user comment
        holder.userComment.text = rating.comment
    }




    override fun getItemCount(): Int = ratings.size
}
