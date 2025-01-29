package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UserOrderConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_order_confirmation)

        // Get order details from intent
        val orderId = intent.getStringExtra("orderId")
        val totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
        val awardedCoins = intent.getIntExtra("awardedCoins", 0)

        // Initialize UI elements
        val orderIdTextView = findViewById<TextView>(R.id.orderIdTextView)
        val totalAmountTextView = findViewById<TextView>(R.id.totalAmountTextView)
        val awardedCoinsTextView = findViewById<TextView>(R.id.awardedCoinsTextView)
        val backToHomeButton = findViewById<Button>(R.id.backToHomeButton)

        // Set order details
        orderIdTextView.text = "$orderId"
        totalAmountTextView.text = "RM%.2f".format(totalAmount)
        awardedCoinsTextView.text = "$awardedCoins"

        // Back to Home Button Action
        backToHomeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("redirectTo", "home") // Ensure it redirects to HomeFragment
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }
}
