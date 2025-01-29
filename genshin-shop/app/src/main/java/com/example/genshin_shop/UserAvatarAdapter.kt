package com.example.genshin_shop

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class UserAvatarAdapter(
    private val avatarList: List<Int>,
    private val onAvatarSelected: (Int) -> Unit
) : RecyclerView.Adapter<UserAvatarAdapter.AvatarViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_avatar_choice, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val avatarResId = avatarList[position]
        holder.avatarImageView.setImageResource(avatarResId)

        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setOnClickListener {
            selectedPosition = position
            onAvatarSelected(avatarResId)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = avatarList.size

    class AvatarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
    }
}
