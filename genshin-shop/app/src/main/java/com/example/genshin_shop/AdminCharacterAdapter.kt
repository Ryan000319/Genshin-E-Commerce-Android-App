package com.example.genshin_shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdminCharacterAdapter(
    private val characters: List<Character>, // List of characters to display
    private val onDelete: (Character) -> Unit // Callback for handling character deletion
) : RecyclerView.Adapter<AdminCharacterAdapter.CharacterViewHolder>() {

    inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val characterSticker: ImageView = itemView.findViewById(R.id.character_sticker) // Sticker ImageView
        val characterName: TextView = itemView.findViewById(R.id.character_name) // Name TextView
        val deleteButton: TextView = itemView.findViewById(R.id.delete_button) // Delete Button
    }

    // Inflates the layout for a character item and creates a ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_character_card, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = characters[position]

        // Load sticker into ImageView using Glide
        Glide.with(holder.itemView.context)
            .load(character.stickerUrl)
            .into(holder.characterSticker)

        // Set character name
        holder.characterName.text = character.name

        // Set delete button click listener
        holder.deleteButton.setOnClickListener { onDelete(character) }
    }

    // Returns the total number of characters to display
    override fun getItemCount(): Int = characters.size
}
