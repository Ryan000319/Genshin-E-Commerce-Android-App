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


class UserCompletedFragment : Fragment() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrderAdapter
    private lateinit var completedOrdersList: MutableList<Order>
    private lateinit var database: DatabaseReference
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_orders_list, container, false)
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView)

        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        completedOrdersList = mutableListOf()

        ordersAdapter = OrderAdapter(
            orders = completedOrdersList,
            isAdmin = false,
            onUserAction = { order, action ->
                when (action) {
                    OrderAdapter.UserAction.VIEW_DETAILS -> openOrderDetail(order)
                    else -> {} // Remove or ignore the RATE case
                }
            }
        )

        ordersRecyclerView.layoutManager = LinearLayoutManager(context)
        ordersRecyclerView.adapter = ordersAdapter

        fetchCompletedOrders()

        return view
    }

    private fun fetchCompletedOrders() {
        userId?.let { uid ->
            database.child("orders").child(uid)
                .orderByChild("status").equalTo("Delivered")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        completedOrdersList.clear()

                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            if (order != null) {
                                order.id = orderSnapshot.key.orEmpty()

                                // Parse `items` safely
                                val rawItemsSnapshot = orderSnapshot.child("items")
                                order.items = parseItems(rawItemsSnapshot)

                                completedOrdersList.add(order)
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
