package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserToShipFragment : Fragment() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrderAdapter
    private lateinit var ordersList: MutableList<Order>
    private lateinit var database: DatabaseReference
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_orders_list, container, false)
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView)

        // Initialize Firebase Database reference and RecyclerView
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        ordersList = mutableListOf()

        ordersAdapter = OrderAdapter(
            orders = ordersList,
            isAdmin = false,
            onUserAction = { order, action ->
                if (action == OrderAdapter.UserAction.VIEW_DETAILS) {
                    openOrderDetail(order)
                }
            }
        )

        ordersRecyclerView.layoutManager = LinearLayoutManager(context)
        ordersRecyclerView.adapter = ordersAdapter

        fetchToShipOrders()

        return view
    }

    private fun fetchToShipOrders() {
        userId?.let { uid ->
            database.child("orders").child(uid)
                .orderByChild("status").equalTo("To Ship")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        ordersList.clear()

                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            if (order != null) {
                                order.id = orderSnapshot.key.orEmpty()

                                // Parse `items` safely
                                val rawItemsSnapshot = orderSnapshot.child("items")
                                order.items = parseItems(rawItemsSnapshot)

                                ordersList.add(order)
                            }
                        }

                        ordersAdapter.notifyDataSetChanged()

                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun parseItems(itemsSnapshot: DataSnapshot): Map<String, OrderItem> {
        val itemsMap = mutableMapOf<String, OrderItem>()

        if (itemsSnapshot.exists()) {
            try {
                if (itemsSnapshot.value is List<*>) {
                    // Handle ArrayList format
                    itemsSnapshot.children.forEachIndexed { index, itemSnapshot ->
                        val itemValue = itemSnapshot.getValue(OrderItem::class.java)
                        if (itemValue != null) {
                            itemsMap[index.toString()] = itemValue
                        }
                    }
                } else if (itemsSnapshot.value is Map<*, *>) {
                    // Handle Map format
                    itemsSnapshot.children.forEach { itemSnapshot ->
                        val itemKey = itemSnapshot.key.orEmpty()
                        val itemValue = itemSnapshot.getValue(OrderItem::class.java)
                        if (itemValue != null) {
                            itemsMap[itemKey] = itemValue
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error parsing items: ${e.message}")
            }
        }

        return itemsMap
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(context, UserOrderDetailActivity::class.java)
        intent.putExtra("order", order)
        startActivity(intent)
    }
}
