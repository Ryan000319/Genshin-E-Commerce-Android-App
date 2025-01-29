package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserProfileFragment : Fragment() {

    private lateinit var avatarImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var logoutButton: Button

    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            loadUserProfile()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        avatarImageView = view.findViewById(R.id.avatarImageView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        addressTextView = view.findViewById(R.id.addressTextView)
        phoneTextView = view.findViewById(R.id.phoneTextView)
        coinsTextView = view.findViewById(R.id.coinsTextView)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        logoutButton = view.findViewById(R.id.logoutButton)

        setupViewPager()
        loadUserProfile()

        editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), UserEditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            navigateToMain()
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    usernameTextView.text = "Name: ${user.username}"
                    addressTextView.text = "Address: ${user.address ?: "No Address"}"
                    phoneTextView.text = "Phone: ${user.phone ?: "No Phone Number"}"
                    coinsTextView.text = "Coins: ${user.coins ?: 0}"
                    avatarImageView.setImageResource(getAvatarResource(user.avatarUrl))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "Error loading profile data", error.toException())
            }
        })
    }

    private fun setupViewPager() {
        val adapter = UserPurchasesPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "To Ship"
                1 -> "To Receive"
                2 -> "Completed"
                else -> null
            }
        }.attach()
    }

    private fun getAvatarResource(avatarUrl: String?): Int {
        return when (avatarUrl) {
            "avatar1" -> R.drawable.user_avatar_1
            "avatar2" -> R.drawable.user_avatar_2
            "avatar3" -> R.drawable.user_avatar_3
            "avatar9" -> R.drawable.user_avatar_9
            "avatar10" -> R.drawable.user_avatar_10
            else -> R.drawable.user_avatar_1
        }
    }

    private fun navigateToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }
}
