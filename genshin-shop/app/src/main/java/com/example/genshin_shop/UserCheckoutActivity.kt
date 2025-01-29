package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random

class UserCheckoutActivity : AppCompatActivity() {

    private lateinit var cartItemsRecyclerView: RecyclerView
    private lateinit var addressTextView: TextView
    private lateinit var editAddressButton: Button
    private lateinit var subtotalTextView: TextView
    private lateinit var shippingTextView: TextView
    private lateinit var totalTextView: TextView
    private lateinit var coinsDiscountTextView: TextView
    private lateinit var applyCoinsSwitch: Switch
    private lateinit var confirmPurchaseButton: Button
    private lateinit var availableCoinsTextView: TextView
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var paymentIntentClientSecret: String

    private val shippingCost = 10.00
    private var userAddress: String = "No Address Provided"
    private var availableCoins: Int = 0
    private var coinsToApply: Int = 0
    private val selectedItems = mutableListOf<OrderItem>()
    private val database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_checkout)

        // Initialize Stripe Payment Configuration
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51QL94WG6KjjwwgNKmG1c09YsOJtJlL0x0BYBv3fnW7v5QkOJcsxuBvZER4bbrg3PcI2uHo8l6iX1vwMA488s7wyh00gqVD6EwY"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        // Initialize UI elements
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView)
        addressTextView = findViewById(R.id.addressTextView)
        editAddressButton = findViewById(R.id.editAddressButton)
        subtotalTextView = findViewById(R.id.subtotalTextView)
        shippingTextView = findViewById(R.id.shippingTextView)
        totalTextView = findViewById(R.id.totalTextView)
        coinsDiscountTextView = findViewById(R.id.coinsDiscountTextView)
        applyCoinsSwitch = findViewById(R.id.applyCoinsSwitch)
        confirmPurchaseButton = findViewById(R.id.confirmPurchaseButton)
        availableCoinsTextView = findViewById(R.id.pavailableCoinsTextView)

        // Load cart items and set up UI
        val orderItem = intent.getParcelableExtra<OrderItem>("orderItem")
        val selectedItemsFromIntent = intent.getParcelableArrayListExtra<OrderItem>("selected_items")

        if (orderItem != null) {
            selectedItems.add(orderItem) // Handle the "Buy Now" scenario with a single item
        } else if (selectedItemsFromIntent != null) {
            selectedItems.addAll(selectedItemsFromIntent) // Handle the "Checkout" from cart scenario
        } else {
            Toast.makeText(this, "No items selected for checkout.", Toast.LENGTH_SHORT).show()
            finish() // Exit activity if no items are passed
        }


        loadUserAddress()
        loadUserCoins()
        setupRecyclerView()
        updatePriceBreakdown()

        editAddressButton.setOnClickListener { showEditAddressDialog() }

        applyCoinsSwitch.setOnCheckedChangeListener { _, isChecked ->
            coinsToApply = if (isChecked) availableCoins else 0
            updatePriceBreakdown()
        }

        confirmPurchaseButton.setOnClickListener {
            confirmPurchase()
        }
    }

    private fun loadUserCoins() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.child("users").child(userId).child("coins").get()
            .addOnSuccessListener { snapshot ->
                availableCoins = snapshot.getValue(Int::class.java) ?: 0
                coinsToApply = if (applyCoinsSwitch.isChecked) availableCoins else 0
                availableCoinsTextView.text = "Available Coins: $availableCoins"
                updatePriceBreakdown()
            }
    }

    private fun setupRecyclerView() {
        cartItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        cartItemsRecyclerView.adapter = UserCartAdapter(selectedItems, readOnly = true)
    }

    private fun loadUserAddress() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("address").get()
                .addOnSuccessListener { snapshot ->
                    userAddress = snapshot.getValue(String::class.java) ?: "No Address Provided"
                    addressTextView.text = "Address: $userAddress"
                }
                .addOnFailureListener {
                    addressTextView.text = "Address: $userAddress"
                    Toast.makeText(this, "Failed to load address.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditAddressDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.user_checkout_edit_address, null)
        val addressEditText = dialogView.findViewById<EditText>(R.id.addressEditText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        // Pre-fill the address
        addressEditText.setText(userAddress)

        // Build and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissal on outside touch
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss() // Close the dialog
        }

        saveButton.setOnClickListener {
            val newAddress = addressEditText.text.toString().trim()
            if (newAddress.isNotEmpty()) {
                userAddress = newAddress
                addressTextView.text = "Address: $userAddress"
                saveAddressToFirebase(newAddress)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Address cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }


    private fun saveAddressToFirebase(newAddress: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.child("users").child(userId).child("address").setValue(newAddress)
            .addOnSuccessListener {
                Toast.makeText(this, "Address updated successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update address.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePriceBreakdown() {
        val subtotal = selectedItems.sumOf { it.totalPrice }
        val coinsDiscount = coinsToApply * 0.10
        val total = subtotal + shippingCost - coinsDiscount

        subtotalTextView.text = getString(R.string.subtotal_text, subtotal)
        shippingTextView.text = getString(R.string.shipping_text, shippingCost)
        coinsDiscountTextView.text = getString(R.string.coins_discount_text, coinsDiscount)
        totalTextView.text = getString(R.string.total_text, total.coerceAtLeast(0.0))
    }

    private fun confirmPurchase() {
        if (userAddress.isEmpty()) {
            Toast.makeText(this, "Please provide a delivery address", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch payment intent from the backend before proceeding with payment
        val subtotal = selectedItems.sumOf { it.totalPrice }
        val totalAmount = subtotal + shippingCost - (coinsToApply * 0.10)
        fetchPaymentIntent((totalAmount * 100).toInt(), "usd") // Stripe expects the amount in cents
    }

    private fun fetchPaymentIntent(amount: Int, currency: String) {
        val client = OkHttpClient()
        val url = "https://stripe-server-77he.onrender.com/create-payment-intent"

        val jsonObject = JSONObject()
        jsonObject.put("amount", amount)
        jsonObject.put("currency", currency)
        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonObject.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckoutActivity", "Failed to initiate payment", e)
                runOnUiThread {
                    Toast.makeText(this@UserCheckoutActivity, "Failed to initiate payment", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            val json = JSONObject(responseBody)
                            paymentIntentClientSecret = json.getString("clientSecret")
                            runOnUiThread {
                                presentPaymentSheet()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@UserCheckoutActivity, "Empty response from server", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.e("CheckoutActivity", "Error fetching payment details: ${response.code}")
                        runOnUiThread {
                            Toast.makeText(this@UserCheckoutActivity, "Error fetching payment details", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun presentPaymentSheet() {
        val paymentSheetConfiguration = PaymentSheet.Configuration(
            merchantDisplayName = "Genshin Shop"
        )
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, paymentSheetConfiguration)
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Log.d("CheckoutActivity", "Payment completed successfully")
                // Proceed with order confirmation
                saveOrderToDatabase()
            }
            is PaymentSheetResult.Canceled -> {
                Log.d("CheckoutActivity", "Payment canceled")
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Log.e("CheckoutActivity", "Payment failed", paymentSheetResult.error)
                Toast.makeText(this, "Payment failed: ${paymentSheetResult.error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveOrderToDatabase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val subtotal = selectedItems.sumOf { it.totalPrice }
        val totalAmount = subtotal + shippingCost - (coinsToApply * 0.10)

        // Deduct applied coins from user's account
        val userRef = database.child("users").child(userId)
        userRef.child("coins").setValue(availableCoins - coinsToApply)
            .addOnSuccessListener {
                // Create and save the order in the database
                val orderRef = database.child("orders").child(userId).push()
                val orderId = orderRef.key ?: "Unknown"

                // Convert selectedItems to a Map<String, OrderItem>
                val itemsMap = selectedItems.associateBy { it.itemId }

                val order = Order(
                    id = orderId,
                    userId = userId,
                    address = userAddress,
                    totalAmount = totalAmount.coerceAtLeast(0.0),
                    coinsApplied = coinsToApply * 0.10,
                    deliveryFee = shippingCost,
                    items = itemsMap,
                    status = "To Ship"
                )

                orderRef.setValue(order)
                    .addOnSuccessListener {
                        // Clear the cart after saving the order
                        UserCartManager.clearCart()

                        // Award Coins and redirect to the confirmation page after getting the coins
                        awardPurchaseCoins(subtotal) { awardedCoins ->
                            val intent = Intent(this, UserOrderConfirmationActivity::class.java).apply {
                                putExtra("orderId", orderId)
                                putExtra("totalAmount", totalAmount)
                                putExtra("awardedCoins", awardedCoins)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CheckoutActivity", "Failed to save order", e)
                        Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CheckoutActivity", "Failed to deduct coins", e)
                Toast.makeText(this, "Failed to apply coins. Try again later.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun awardPurchaseCoins(subtotal: Double, callback: (Int) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val randomPercentage = Random.nextDouble(0.50, 1.00)
        val awardedCoins = (subtotal * randomPercentage).toInt()

        if (awardedCoins > 0) {
            val userRef = database.child("users").child(userId)
            userRef.child("coins").get().addOnSuccessListener { snapshot ->
                val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                userRef.child("coins").setValue(currentCoins + awardedCoins)
                    .addOnSuccessListener {
                        // Optional: log coin award in coinHistory
                        val historyRef = database.child("coinHistory").child(userId).push()
                        historyRef.setValue(
                            mapOf(
                                "type" to "purchase_award",
                                "amount" to awardedCoins,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        // Return the awarded coins via callback
                        callback(awardedCoins)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CheckoutActivity", "Failed to update coins balance.", e)
                        callback(0)
                    }
            }.addOnFailureListener { e ->
                Log.e("CheckoutActivity", "Failed to fetch current coins.", e)
                callback(0)
            }
        } else {
            callback(0)
        }
    }
}
