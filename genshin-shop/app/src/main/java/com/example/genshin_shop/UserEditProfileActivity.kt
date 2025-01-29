package com.example.genshin_shop

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserEditProfileActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var avatarRecyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var avatarPreviewImageView: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val avatarList = listOf(
        R.drawable.user_avatar_1,
        R.drawable.user_avatar_2,
        R.drawable.user_avatar_3,
        R.drawable.user_avatar_9,
        R.drawable.user_avatar_10
    )

    private var selectedAvatarResId = R.drawable.user_avatar_1 // Default selected avatar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText)
        addressEditText = findViewById(R.id.addressEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        avatarRecyclerView = findViewById(R.id.avatarRecyclerView)
        saveButton = findViewById(R.id.saveButton)
        avatarPreviewImageView = findViewById(R.id.avatarImageView)

        // Setup RecyclerView for avatar selection
        setupAvatarRecyclerView()

        // Load current user data
        loadCurrentUserData()

        // Save profile changes
        saveButton.setOnClickListener {
            saveUserData()
        }
    }

    private fun setupAvatarRecyclerView() {
        avatarRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = UserAvatarAdapter(avatarList) { selectedAvatar ->
            selectedAvatarResId = selectedAvatar
            avatarPreviewImageView.setImageResource(selectedAvatar) // Update preview
        }
        avatarRecyclerView.adapter = adapter
    }

    private fun loadCurrentUserData() {
        val userId = auth.currentUser?.uid
        userId?.let {
            database.child("users").child(it).get()
                .addOnSuccessListener { snapshot ->
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        usernameEditText.setText(it.username)
                        addressEditText.setText(it.address)
                        phoneEditText.setText(it.phone)

                        // Set selected avatar based on current user profile
                        val avatarIndex = avatarList.indexOfFirst { resId -> resId == getDrawableIdFromUrl(it.avatarUrl) }
                        if (avatarIndex >= 0) {
                            selectedAvatarResId = avatarList[avatarIndex]
                            avatarPreviewImageView.setImageResource(selectedAvatarResId)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid
        val selectedAvatarUrl = getAvatarUrlFromDrawableId(selectedAvatarResId)
        val userData = mapOf(
            "username" to usernameEditText.text.toString(),
            "address" to addressEditText.text.toString(),
            "phone" to phoneEditText.text.toString(),
            "avatarUrl" to selectedAvatarUrl
        )

        userId?.let {
            database.child("users").child(it).updateChildren(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getDrawableIdFromUrl(avatarUrl: String?): Int {
        return when (avatarUrl) {
            "avatar1" -> R.drawable.user_avatar_1
            "avatar2" -> R.drawable.user_avatar_2
            "avatar3" -> R.drawable.user_avatar_3
            "avatar9" -> R.drawable.user_avatar_9
            "avatar10" -> R.drawable.user_avatar_10
            else -> R.drawable.user_avatar_1 // Default avatar
        }
    }

    private fun getAvatarUrlFromDrawableId(drawableId: Int): String {
        return when (drawableId) {
            R.drawable.user_avatar_1 -> "avatar1"
            R.drawable.user_avatar_2 -> "avatar2"
            R.drawable.user_avatar_3 -> "avatar3"
            R.drawable.user_avatar_9 -> "avatar9"
            R.drawable.user_avatar_10 -> "avatar10"
            else -> "avatar1" // Default avatar
        }
    }
}
