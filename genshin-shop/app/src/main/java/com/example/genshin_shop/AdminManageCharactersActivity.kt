package com.example.genshin_shop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AdminManageCharactersActivity : AppCompatActivity() {

    private lateinit var characterNameEditText: EditText
    private lateinit var keywordEditText: EditText
    private lateinit var selectVideoButton: Button
    private lateinit var selectStickerButton: Button
    private lateinit var uploadCharacterButton: Button
    private lateinit var characterRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    // Firebase References
    private val databaseRef = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference.child("characters")
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    // Data Variables
    private val characters = mutableListOf<Character>()
    private var selectedVideoUri: Uri? = null
    private var selectedStickerUri: Uri? = null
    private lateinit var adapter: AdminCharacterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_manage_characters)

        // Initialize UI components
        characterNameEditText = findViewById(R.id.character_name_edit_text)
        keywordEditText = findViewById(R.id.keyword_edit_text)
        selectVideoButton = findViewById(R.id.select_video_button)
        selectStickerButton = findViewById(R.id.select_sticker_button)
        uploadCharacterButton = findViewById(R.id.upload_character_button)
        characterRecyclerView = findViewById(R.id.character_recycler_view)
        progressBar = findViewById(R.id.upload_progress_bar)

        // RecyclerView setup
        characterRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminCharacterAdapter(characters) { character ->
            deleteCharacter(character)
        }
        characterRecyclerView.adapter = adapter

        // Load characters from Firebase
        loadCharacters()

        // Select video button
        selectVideoButton.setOnClickListener {
            openFilePicker("video/*", 101)
        }

        // Select sticker button
        selectStickerButton.setOnClickListener {
            openFilePicker("image/*", 102)
        }

        // Upload character button
        uploadCharacterButton.setOnClickListener {
            uploadCharacter()
        }
    }

    // Open file picker for selecting videos or images
    private fun openFilePicker(type: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode)
    }

    // Handle file picker result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                101 -> {
                    selectedVideoUri = data?.data
                    Toast.makeText(this, "Video selected!", Toast.LENGTH_SHORT).show()
                }
                102 -> {
                    selectedStickerUri = data?.data
                    Toast.makeText(this, "Sticker selected!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Upload character to Firebase
    private fun uploadCharacter() {
        val name = characterNameEditText.text.toString().trim()
        val keyword = keywordEditText.text.toString().trim()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(keyword) || selectedVideoUri == null || selectedStickerUri == null) {
            Toast.makeText(this, "All fields, video, and sticker are required", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE

        // Upload video first
        val videoFileName = UUID.randomUUID().toString() + ".mp4"
        val videoRef = storageRef.child("character_videos/$videoFileName")

        videoRef.putFile(selectedVideoUri!!)
            .addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { videoUrl ->
                    // Upload sticker after video upload
                    uploadSticker(name, keyword, videoUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload video", Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.GONE
            }
    }

    // Upload sticker to Firebase
    private fun uploadSticker(name: String, keyword: String, videoUrl: String) {
        val stickerFileName = UUID.randomUUID().toString() + ".png"
        val stickerRef = storageRef.child("character_stickers/$stickerFileName")

        stickerRef.putFile(selectedStickerUri!!)
            .addOnSuccessListener {
                stickerRef.downloadUrl.addOnSuccessListener { stickerUrl ->
                    saveCharacterToDatabase(name, videoUrl, stickerUrl.toString(), keyword)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload sticker", Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.GONE
            }
    }

    // Save character details to Firebase Database
    private fun saveCharacterToDatabase(name: String, videoUrl: String, stickerUrl: String, keyword: String) {
        val characterId = databaseRef.push().key ?: return
        val character = Character(id = characterId, name = name, videoUrl = videoUrl, stickerUrl = stickerUrl, keyword = keyword)

        databaseRef.child(characterId).setValue(character).addOnSuccessListener {
            Toast.makeText(this, "Character added successfully", Toast.LENGTH_SHORT).show()
            clearFields()
            loadCharacters()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add character", Toast.LENGTH_SHORT).show()
        }.addOnCompleteListener {
            progressBar.visibility = ProgressBar.GONE
        }
    }

    // Load characters from Firebase
    private fun loadCharacters() {
        databaseRef.get().addOnSuccessListener { snapshot ->
            characters.clear()
            for (characterSnapshot in snapshot.children) {
                val character = characterSnapshot.getValue(Character::class.java)
                if (character != null) {
                    characters.add(character)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    // Clear input fields after adding a character
    private fun clearFields() {
        characterNameEditText.text.clear()
        keywordEditText.text.clear()
        selectedVideoUri = null
        selectedStickerUri = null
    }

    // Delete character from Firebase
    private fun deleteCharacter(character: Character) {
        databaseRef.child(character.id).removeValue().addOnSuccessListener {
            Toast.makeText(this, "${character.name} deleted successfully", Toast.LENGTH_SHORT).show()
            loadCharacters()
        }
    }
}
