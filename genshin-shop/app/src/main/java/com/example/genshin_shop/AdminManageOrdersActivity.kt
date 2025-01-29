package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminManageOrdersActivity : AppCompatActivity() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var completedOrderAdapter: OrderAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var database: DatabaseReference
    private lateinit var toggleOrdersButton: Button

    private val orderList = mutableListOf<Order>()
    private val completedOrderList = mutableListOf<Order>()
    private var viewingCompletedOrders = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_manage_orders)
        sortSpinner = findViewById(R.id.sortSpinner)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        setupAdapters()
        setupSort()

        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        toggleOrdersButton = findViewById(R.id.toggleOrdersButton)
        ordersRecyclerView.adapter = orderAdapter
        ordersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Listen for any real-time changes in "orders" node
        listenForOrderChanges()

        // Start the foreground service for admin
        val userId = intent.getStringExtra("USER_ID") ?: ""
        val serviceIntent = Intent(this, MyNotificationService::class.java).apply {
            putExtra(MyNotificationService.EXTRA_ROLE, "admin")
            putExtra(MyNotificationService.EXTRA_USER_ID, userId)
        }
        startForegroundService(serviceIntent)

        toggleOrdersButton.setOnClickListener { toggleOrderView() }
    }

    override fun onResume() {
        super.onResume()
        val userId = intent.getStringExtra("USER_ID") ?: ""
        val serviceIntent = Intent(this, MyNotificationService::class.java).apply {
            putExtra(MyNotificationService.EXTRA_ROLE, "admin")
            putExtra(MyNotificationService.EXTRA_USER_ID, userId)
        }
        startForegroundService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the notification service upon logout
        val serviceIntent = Intent(this, MyNotificationService::class.java)
        stopService(serviceIntent)
    }


    private fun setupAdapters() {
        orderAdapter = OrderAdapter(orderList, isAdmin = true) { order, action ->
            when (action) {
                OrderAdapter.UserAction.VIEW_DETAILS -> {
                    val intent = Intent(this, AdminOrderDetailActivity::class.java).apply {
                        putExtra("userId", order.userId)
                        putExtra("orderKey", order.id)
                    }
                    startActivity(intent)
                }
                OrderAdapter.UserAction.MODIFY_STATUS -> {
                    showModifyStatusDialog(order) // Handle status modification
                }
            }
        }

        completedOrderAdapter = OrderAdapter(completedOrderList, isAdmin = true) { order, action ->
            when (action) {
                OrderAdapter.UserAction.VIEW_DETAILS -> {
                    val intent = Intent(this, AdminOrderDetailActivity::class.java).apply {
                        putExtra("userId", order.userId)
                        putExtra("orderKey", order.id)
                    }
                    startActivity(intent)
                }
                else -> Unit
            }
        }
    }


    private fun listenForOrderChanges() {
        // Listen for changes in the "orders" node
        orderList.clear()
        orderAdapter.notifyDataSetChanged()


        // Listen for active orders under "orders" node
        database.child("orders").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderSnapshot(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderSnapshot(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminManageOrdersActivity, "Failed to listen for changes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showModifyStatusDialog(order: Order) {
        val statuses = listOf("Pending", "Shipped", "Delivered", "Completed")
        val currentStatusIndex = statuses.indexOf(order.status)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modify Order Status")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statuses)
        builder.setSingleChoiceItems(adapter, currentStatusIndex) { dialog, which ->
            val newStatus = statuses[which]

            if (newStatus == "Completed") {
                // Move the order to "completed_orders" node in Firebase
                database.child("orders").child(order.userId).child(order.id).get().addOnSuccessListener { snapshot ->
                    val completedOrder = snapshot.getValue(Order::class.java)
                    completedOrder?.status = "Completed"

                    // Add to "completed_orders" and remove from "orders"
                    database.child("completed_orders").child(order.userId).child(order.id).setValue(completedOrder)
                        .addOnSuccessListener {
                            database.child("orders").child(order.userId).child(order.id).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Order moved to Completed Orders", Toast.LENGTH_SHORT).show()

                                    // Update UI
                                    orderList.remove(order)
                                    if (completedOrder != null) {
                                        completedOrderList.add(completedOrder)
                                    }
                                    orderAdapter.notifyDataSetChanged()
                                    completedOrderAdapter.notifyDataSetChanged()
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Failed to remove order: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this, "Failed to move order to completed: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                // Regular status update
                database.child("orders").child(order.userId).child(order.id).child("status").setValue(newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Order status updated to $newStatus", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        // Update UI
                        order.status = newStatus
                        orderAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Failed to update status: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


    private fun handleOrderSnapshot(snapshot: DataSnapshot) {
        val userId = snapshot.key.orEmpty()
        snapshot.children.forEach { orderSnapshot ->
            val orderKey = orderSnapshot.key.orEmpty()
            val order = orderSnapshot.getValue(Order::class.java)

            order?.let {
                it.id = orderKey
                it.userId = userId

                // Check if the order is completed
                if (it.status == "Completed") {
                    // Remove from active orders and add to completed orders
                    orderList.removeAll { o -> o.id == it.id && o.userId == it.userId }
                    completedOrderList.add(it)
                    completedOrderAdapter.notifyDataSetChanged()
                } else {
                    // Update or add to active orders list
                    val existingIndex = orderList.indexOfFirst { o -> o.id == it.id && o.userId == it.userId }
                    if (existingIndex == -1) {
                        orderList.add(it)
                    } else {
                        orderList[existingIndex] = it
                    }
                    filterAndSortOrders() // Apply sorting after adding or updating orders
                }
            }
        }
    }


    private fun loadCompletedOrders() {
        // Clear list to avoid duplication
        completedOrderList.clear()

        // Load completed orders only once using a single value event listener
        database.child("completed_orders").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key.orEmpty()
                    userSnapshot.children.forEach { orderSnapshot ->
                        val order = orderSnapshot.getValue(Order::class.java)
                        order?.let {
                            it.id = orderSnapshot.key.orEmpty()
                            it.userId = userId
                            completedOrderList.add(it)
                        }
                    }
                }
                completedOrderAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminManageOrdersActivity, "Failed to load completed orders: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleOrderView() {
        if (viewingCompletedOrders) {
            ordersRecyclerView.adapter = orderAdapter
            toggleOrdersButton.text = "View Completed Orders"
            viewingCompletedOrders = false
        } else {
            ordersRecyclerView.adapter = completedOrderAdapter
            toggleOrdersButton.text = "View Active Orders"
            viewingCompletedOrders = true
            loadCompletedOrders()
        }
    }

    private fun setupSort() {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.sort_order, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        // Default to "Sort by Date (Newest)"
        sortSpinner.setSelection(0)

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterAndSortOrders()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun filterAndSortOrders() {
        val sortedOrders = when (sortSpinner.selectedItemPosition) {
            0 -> orderList.sortedByDescending { it.createdAt } // Sort by Date (Newest)
            1 -> orderList.sortedBy { it.createdAt }           // Sort by Date (Oldest)
            else -> orderList
        }

        orderAdapter.updateOrders(sortedOrders)
    }
}
