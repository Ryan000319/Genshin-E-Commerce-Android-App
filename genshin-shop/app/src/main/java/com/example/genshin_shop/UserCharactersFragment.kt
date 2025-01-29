package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserCharactersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var characterAdapter: UserCharacterAdapter
    private val characters = mutableListOf<Character>()
    private lateinit var database: DatabaseReference
    private var recyclerViewState: Parcelable? = null // Save RecyclerView state
    private val exoPlayerPool = mutableMapOf<Int, ExoPlayer>() // Shared ExoPlayer pool

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_characters, container, false)

        recyclerView = view.findViewById(R.id.character_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        characterAdapter = UserCharacterAdapter(requireContext(), characters, exoPlayerPool) { character ->
            openSearchOutput(character.keyword)
        }
        recyclerView.adapter = characterAdapter

        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference.child("characters")

        loadCharacters()
        return view
    }

    private fun loadCharacters() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                characters.clear()
                for (data in snapshot.children) {
                    val character = data.getValue(Character::class.java)
                    character?.let { characters.add(it) }
                }
                characterAdapter.notifyDataSetChanged()
                restoreRecyclerViewState()
            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    private fun openSearchOutput(keyword: String) {
        val intent = Intent(requireContext(), UserSearchResultsActivity::class.java)
        intent.putExtra("keyword", keyword)
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
        characterAdapter.pauseAllPlayers() // Pause all videos
        characterAdapter.detachAllPlayers(recyclerView) // Detach players
    }

    override fun onResume() {
        super.onResume()
        restoreRecyclerViewState()
        characterAdapter.rebindPlayers(recyclerView) // Rebind players to views
    }

    private fun restoreRecyclerViewState() {
        recyclerViewState?.let {
            recyclerView.layoutManager?.onRestoreInstanceState(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        characterAdapter.detachAllPlayers(recyclerView) // Detach players
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release all players on app destroy
        exoPlayerPool.values.forEach { it.release() }
        exoPlayerPool.clear()
    }
}
