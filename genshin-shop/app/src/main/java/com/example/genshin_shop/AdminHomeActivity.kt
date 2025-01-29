package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth

class AdminHomeActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth


    private lateinit var productManagementSection: ConstraintLayout
    private lateinit var orderManagementSection: ConstraintLayout
    private lateinit var advertisementManagementSection: ConstraintLayout
    private lateinit var characterManagementSection: ConstraintLayout
    private lateinit var viewStoreAnalyticsSection: ConstraintLayout
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_home)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        productManagementSection = findViewById(R.id.manageProductsLayout)
        orderManagementSection = findViewById(R.id.manageOrdersLayout)
        advertisementManagementSection = findViewById(R.id.manageAdvertisementsLayout)
        characterManagementSection = findViewById(R.id.manageCharactersLayout)
        viewStoreAnalyticsSection = findViewById(R.id.viewAnalyticsLayout)
        logoutButton = findViewById(R.id.logoutButton)

        // Navigate to Product Management Section
        productManagementSection.setOnClickListener {
            startActivity(Intent(this, AdminManageProductsActivity::class.java))
        }

        // Navigate to Order Management Section
        orderManagementSection.setOnClickListener {
            startActivity(Intent(this, AdminManageOrdersActivity::class.java))
        }

        // Navigate to Advertisement Management Section
        advertisementManagementSection.setOnClickListener {
            startActivity(Intent(this, AdminManageAdvertisementsActivity::class.java))
        }

        // Navigate to Character Management Section
        characterManagementSection.setOnClickListener {
            startActivity(Intent(this, AdminManageCharactersActivity::class.java))
        }

        // Navigate to View Store Analytics Section
        viewStoreAnalyticsSection.setOnClickListener {
            startActivity(Intent(this, AdminViewStoreAnalyticsActivity::class.java))
        }

        // Log out the admin and navigate to the login screen
        logoutButton.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }

    }

    // Navigate to the Login screen
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear activity stack
        startActivity(intent)
        finish()
    }

    // Check if the user is authenticated when resuming the activity
    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            navigateToLogin()
        }
    }

}
