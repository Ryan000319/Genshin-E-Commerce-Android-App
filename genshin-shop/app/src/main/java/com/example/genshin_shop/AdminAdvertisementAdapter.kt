package com.example.genshin_shop

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdminAdvertisementAdapter(
    private val advertisements: List<ItemAdvertisement>,  // List of advertisements to display
    private val onDeleteClick: (ItemAdvertisement) -> Unit, // Callback for handling delete actions
) : RecyclerView.Adapter<AdminAdvertisementAdapter.AdViewHolder>() {

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adImage: ImageView = itemView.findViewById(R.id.ad_image)
        val keywordsText: TextView = itemView.findViewById(R.id.product_id_text)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_item_advertisement, parent, false)
        return AdViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val advertisement = advertisements[position]

        // Load advertisement image
        Glide.with(holder.adImage.context)
            .load(advertisement.imageUrl)
            .into(holder.adImage)

        // Display keywords
        holder.keywordsText.text = "Keywords: ${advertisement.keywords.joinToString(", ")}"

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteClick(advertisement)
        }

    }

    // Returns the total number of advertisement items
    override fun getItemCount(): Int = advertisements.size
}
