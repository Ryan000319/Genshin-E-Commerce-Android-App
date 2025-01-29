package com.example.genshin_shop

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Suppress("DEPRECATION")
class UserOrderDetailActivity : AppCompatActivity() {

    private lateinit var orderItemsRecyclerView: RecyclerView
    private lateinit var orderIdTextView: TextView
    private lateinit var orderTotalTextView: TextView
    private lateinit var database: DatabaseReference

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var orderAdapter: OrderDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_order_detail_activity)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize Views
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView)
        orderIdTextView = findViewById(R.id.order_id)
        orderTotalTextView = findViewById(R.id.order_total)
        val subtotalTextView: TextView = findViewById(R.id.subtotalTextView)
        val coinsDiscountTextView: TextView = findViewById(R.id.coinsDiscountTextView)
        val deliveryFeeTextView: TextView = findViewById(R.id.deliveryFeeTextView)
        val recipientInfoTextView: TextView = findViewById(R.id.recipient_info)

        // Get the order from the intent
        val order: Order? = intent.getParcelableExtra("order")
        if (order == null) {
            Toast.makeText(this, "Order details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Populate UI with order details
        orderIdTextView.text = "Order ID: ${order.id}"
        orderTotalTextView.text = String.format("Total: RM%.2f", order.totalAmount)
        recipientInfoTextView.text = "Address: ${order.address.ifEmpty { "No Address Provided" }}"

        // Calculate and display the price breakdown
        val subtotal = order.items.values.sumOf { it.totalPrice }
        val coinsDiscount = order.coinsApplied
        val deliveryFee = order.deliveryFee
        val total = subtotal + deliveryFee - coinsDiscount

        subtotalTextView.text = String.format("Subtotal: RM%.2f", subtotal)
        coinsDiscountTextView.text = String.format("Coins Applied: - RM%.2f", coinsDiscount)
        deliveryFeeTextView.text = String.format("Delivery Fee: RM%.2f", deliveryFee)
        orderTotalTextView.text = String.format("Total: RM%.2f", total)

        // Set up RecyclerView with OrderDetailAdapter
        orderAdapter = OrderDetailAdapter(
            items = order.items.values.toList(),
            orderStatus = order.status,
            onRateItem = { orderItem ->
                showRatingDialog(order.id, orderItem)
            }
        )
        orderItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        orderItemsRecyclerView.adapter = orderAdapter
    }


    private fun showRatingDialog(orderId: String, orderItem: OrderItem) {
        val ratingDialogView = layoutInflater.inflate(R.layout.user_dialog_rate_product, null)
        val ratingBar = ratingDialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentEditText = ratingDialogView.findViewById<EditText>(R.id.commentEditText)
        val submitButton = ratingDialogView.findViewById<Button>(R.id.submitRatingButton)

        val dialog = AlertDialog.Builder(this)
            .setView(ratingDialogView)
            .setCancelable(true)
            .create()

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = commentEditText.text.toString().trim()

            if (rating > 0) {
                submitRating(orderId, orderItem, rating, comment) {
                    dialog.dismiss() // Close the dialog after successful submission
                }
            } else {
                Toast.makeText(this, "Please select a rating.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun submitRating(orderId: String, orderItem: OrderItem, rating: Int, comment: String, onComplete: () -> Unit) {
        userId?.let { uid ->
            // Update the `rated` flag for this item in the order
            val itemRef = database.child("orders").child(uid).child(orderId).child("items").child(orderItem.itemId)
            itemRef.child("rated").setValue(true)
                .addOnSuccessListener {
                    // Save the rating to the `product_ratings` node
                    val ratingRef = database.child("product_ratings").child(orderItem.itemId).push()
                    val ratingData = mapOf(
                        "userId" to uid,
                        "rating" to rating,
                        "comment" to comment,
                        "timestamp" to System.currentTimeMillis()
                    )
                    ratingRef.setValue(ratingData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                            // Update the item in the adapter and refresh the UI
                            orderItem.rated = true
                            orderAdapter.notifyDataSetChanged()
                            onComplete() // Call the onComplete callback
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this, "Failed to save rating: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to update item: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
