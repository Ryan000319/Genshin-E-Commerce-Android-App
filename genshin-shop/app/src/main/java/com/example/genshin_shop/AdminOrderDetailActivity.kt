package com.example.genshin_shop

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AdminOrderDetailActivity : AppCompatActivity() {

    private lateinit var orderItemsRecyclerView: RecyclerView
    private lateinit var orderIdTextView: TextView
    private lateinit var orderAddressTextView: TextView
    private lateinit var orderStatusTextView: TextView
    private lateinit var orderSubtotalTextView: TextView
    private lateinit var orderShippingCostTextView: TextView
    private lateinit var orderCoinsAppliedTextView: TextView
    private lateinit var orderFinalTotalTextView: TextView
    private lateinit var database: DatabaseReference

    private var userId: String? = null
    private var orderKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_order_detail)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize Views
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView)
        orderIdTextView = findViewById(R.id.order_id)
        orderAddressTextView = findViewById(R.id.order_address)
        orderStatusTextView = findViewById(R.id.order_status)
        orderSubtotalTextView = findViewById(R.id.order_subtotal)
        orderShippingCostTextView = findViewById(R.id.order_shipping_cost)
        orderCoinsAppliedTextView = findViewById(R.id.order_coins_applied)
        orderFinalTotalTextView = findViewById(R.id.order_final_total)

        // Get userId and orderKey from the intent
        userId = intent.getStringExtra("userId")
        orderKey = intent.getStringExtra("orderKey")

        if (userId == null || orderKey == null) {
            Toast.makeText(this, "Invalid order data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        val orderRef = database.child("orders").child(userId!!).child(orderKey!!)
        orderRef.get().addOnSuccessListener { snapshot ->
            val order = snapshot.getValue(Order::class.java)
            order?.let {
                // Populate UI with order details
                orderIdTextView.text = "Order ID: ${it.id}"
                orderAddressTextView.text = "Address: ${it.address}"
                orderStatusTextView.text = "Status: ${it.status}"

                // Display total breakdown
                displayTotalBreakdown(it)

                // Pass items to the adapter
                val itemList = it.items.values.toList()
                val orderAdapter = OrderDetailAdapter(
                    items = itemList,
                    orderStatus = it.status,
                    onRateItem = { _ -> }
                )
                orderItemsRecyclerView.layoutManager = LinearLayoutManager(this)
                orderItemsRecyclerView.adapter = orderAdapter
            } ?: run {
                Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Failed to load order: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayTotalBreakdown(order: Order) {
        // Calculate and display the price breakdown
        val subtotal = order.items.values.sumOf { it.totalPrice }
        val coinsDiscount = order.coinsApplied
        val deliveryFee = order.deliveryFee
        val total = subtotal + deliveryFee - coinsDiscount

        orderSubtotalTextView.text = String.format("Subtotal: RM%.2f", subtotal)
        orderCoinsAppliedTextView.text = String.format("Coins Applied: - RM%.2f", coinsDiscount)
        orderShippingCostTextView.text = String.format("Delivery Fee: RM%.2f", deliveryFee)
        orderFinalTotalTextView.text = String.format("Total: RM%.2f", total)
    }

}
