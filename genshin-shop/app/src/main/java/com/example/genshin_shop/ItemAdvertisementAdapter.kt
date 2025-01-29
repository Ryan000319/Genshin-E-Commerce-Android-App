package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ItemAdvertisementAdapter(
    private val advertisements: List<ItemAdvertisement>,
    private val isAdmin: Boolean,
    private val onDeleteClick: ((ItemAdvertisement) -> Unit)? = null,
    private val onAdClick: ((List<String>) -> Unit)? = null // Callback for advertisement click
) : RecyclerView.Adapter<ItemAdvertisementAdapter.AdViewHolder>() {

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adImage: ImageView = itemView.findViewById(R.id.ad_image)
        val deleteButton: ImageButton? = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val layout = if (isAdmin) R.layout.admin_item_advertisement else R.layout.user_item_advertisement
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val advertisement = advertisements[position]

        // Load advertisement image
        Glide.with(holder.adImage.context).load(advertisement.imageUrl).into(holder.adImage)

        // Handle advertisement click to open search results with keywords
        holder.adImage.setOnClickListener {
            if (advertisement.keywords.isNotEmpty()) {
                onAdClick?.invoke(advertisement.keywords) // Pass keywords to the callback
            } else {
                Toast.makeText(holder.itemView.context, "No keywords available", Toast.LENGTH_SHORT).show()
            }
        }

        // Show delete button if isAdmin is true
        holder.deleteButton?.apply {
            visibility = if (isAdmin) View.VISIBLE else View.GONE
            setOnClickListener {
                onDeleteClick?.invoke(advertisement)
            }
        }
    }

    override fun getItemCount(): Int = advertisements.size
}
