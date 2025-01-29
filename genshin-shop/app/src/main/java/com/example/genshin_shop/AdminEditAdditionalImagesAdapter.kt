package com.example.genshin_shop

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdminEditAdditionalImagesAdapter(
    private val additionalImagesList: MutableList<String>, // List of image URLs or URIs as Strings
    private val onRemoveImage: (Int) -> Unit // Callback for when an image is removed
) : RecyclerView.Adapter<AdminEditAdditionalImagesAdapter.AdditionalImageViewHolder>() {

    // ViewHolder for individual image items
    inner class AdditionalImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val additionalImageView: ImageView = itemView.findViewById(R.id.imageView)
        val removeImageButton: ImageView = itemView.findViewById(R.id.removeImageButton)
    }

    // Inflate the layout for each item and create a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdditionalImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_additional_image, parent, false)
        return AdditionalImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdditionalImageViewHolder, position: Int) {
        val imagePath = additionalImagesList[position]

        // Load the image into the ImageView using Glide
        Glide.with(holder.additionalImageView.context)
            .load(imagePath)
            .placeholder(R.drawable.merch_1)
            .error(R.drawable.merch_1)
            .into(holder.additionalImageView)

        // Set click listener for the remove button
        holder.removeImageButton.setOnClickListener {
            if (position >= 0 && position < additionalImagesList.size) {
                additionalImagesList.removeAt(position)  // Remove the image from the list
                notifyItemRemoved(position) // Notify RecyclerView of the removed item
                notifyItemRangeChanged(position, additionalImagesList.size) // Update subsequent items
                onRemoveImage.invoke(position) // Trigger the remove callback
            }
        }
    }
    // Returns the total number of images
    override fun getItemCount(): Int = additionalImagesList.size

    // Add new images from a list of URIs
    fun addUris(uris: List<Uri>) {
        additionalImagesList.addAll(uris.map { it.toString() })
        notifyDataSetChanged()
    }

    // Replace the current list with a new list of image URLs
    fun setImageUrls(urls: List<String>) {
        additionalImagesList.clear()
        additionalImagesList.addAll(urls)
        notifyDataSetChanged()
    }
}
