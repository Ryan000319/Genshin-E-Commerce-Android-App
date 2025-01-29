package com.example.genshin_shop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var userHomeFragment: Fragment
    private lateinit var userCartFragment: Fragment
    private lateinit var userProfileFragment: Fragment
    private var activeFragment: Fragment? = null

    companion object {
        private const val NOTIFICATION_REQUEST_CODE = 1001
        const val DISCOUNT_THRESHOLD_PERCENTAGE = 20.0
        const val LOW_STOCK_THRESHOLD = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        setupFragments()
        setupBottomNavigation()
        requestNotificationPermission()

        checkUserAuthentication()
    }

    private fun setupFragments() {
        userHomeFragment = UserHomeFragment()
        userCartFragment = UserCartFragment()
        userProfileFragment = UserProfileFragment()
        activeFragment = userHomeFragment

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, userProfileFragment, "PROFILE_FRAGMENT").hide(userProfileFragment)
            .add(R.id.fragment_container, userCartFragment, "CART_FRAGMENT").hide(userCartFragment)
            .add(R.id.fragment_container, userHomeFragment, "HOME_FRAGMENT")
            .commit()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(userHomeFragment)
                R.id.nav_cart -> switchFragment(userCartFragment)
                R.id.nav_profile -> switchFragment(userProfileFragment)
            }
            true
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment!!)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRole(currentUser.uid)
        } else {
            navigateToLogin()
        }
    }

    private fun checkUserRole(userId: String) {
        database.child("users").child(userId).child("role").get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java)
                if (role == "admin") {
                    navigateToAdminHome()
                } else {
                    // Start the foreground service for user
                    val serviceIntent = Intent(this, MyNotificationService::class.java).apply {
                        putExtra(MyNotificationService.EXTRA_ROLE, "user")
                        putExtra(MyNotificationService.EXTRA_USER_ID, userId)
                    }
                    startForegroundService(serviceIntent)
                }
            }
            .addOnFailureListener { navigateToLogin() }
    }


    private fun navigateToLogin() {
        startActivity(Intent(this, StartLoginActivity::class.java))
        finish()
    }

    private fun navigateToAdminHome() {
        val intent = Intent(this, AdminHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Verify if user is still logged in, otherwise navigate to login screen
        if (auth.currentUser == null) {
            navigateToLogin()
        }
    }

    override fun onStop() {
        super.onStop()
        // Sign out Firebase listeners or stop services here if needed
        if (auth.currentUser == null) {
            val serviceIntent = Intent(this, MyNotificationService::class.java)
            stopService(serviceIntent)
        }
    }


}
