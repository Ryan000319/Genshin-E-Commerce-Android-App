package com.example.genshin_shop

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserCharacterAdapter(
    private val context: Context,
    private val characters: List<Character>,
    private val exoPlayerPool: MutableMap<Int, ExoPlayer>, // Shared pool of ExoPlayers
    private val onCharacterClick: (Character) -> Unit
) : RecyclerView.Adapter<UserCharacterAdapter.CharacterViewHolder>() {

    inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerView: PlayerView = itemView.findViewById(R.id.player_view)
        val characterName: TextView = itemView.findViewById(R.id.character_name_text_view)
        val characterSticker: ImageView = itemView.findViewById(R.id.character_sticker_image)
        val placeholderImage: ImageView = itemView.findViewById(R.id.video_placeholder_image)
        val goButton: ImageView = itemView.findViewById(R.id.go_button)
        val videoSection: View = itemView.findViewById(R.id.character_video_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_character_card, parent, false)
        return CharacterViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = characters[position]

        // Set character name and sticker
        holder.characterName.text = character.name
        Glide.with(context).load(character.stickerUrl).into(holder.characterSticker)

        // Get or initialize ExoPlayer for this position
        val exoPlayer = exoPlayerPool[position] ?: ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(character.videoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false // Keep paused initially
            exoPlayerPool[position] = this // Add to the pool
        }

        holder.playerView.player = exoPlayer
        holder.playerView.useController = false

        // Hide placeholder when video is ready
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    holder.placeholderImage.visibility = View.GONE
                }
            }
        })

        // Handle video play/pause on touch
        holder.videoSection.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    exoPlayer.playWhenReady = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    exoPlayer.playWhenReady = false
                    true
                }
                else -> false
            }
        }

        // Handle Go Button click
        holder.goButton.setOnClickListener {
            onCharacterClick(character)
        }
    }

    override fun onViewRecycled(holder: CharacterViewHolder) {
        super.onViewRecycled(holder)
        holder.playerView.player = null // Detach player, but do not release
    }

    //Pause all ExoPlayers.
    fun pauseAllPlayers() {
        exoPlayerPool.values.forEach { it.playWhenReady = false }
    }

    //Rebind all ExoPlayers to their respective PlayerViews.
    fun rebindPlayers(recyclerView: RecyclerView) {
        for ((position, exoPlayer) in exoPlayerPool) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? CharacterViewHolder
            viewHolder?.playerView?.player = exoPlayer
        }
    }

    //Detach all ExoPlayers from PlayerViews without releasing them.
    fun detachAllPlayers(recyclerView: RecyclerView) {
        for ((position, _) in exoPlayerPool) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? CharacterViewHolder
            viewHolder?.playerView?.player = null
        }
    }

    override fun getItemCount(): Int = characters.size
}
