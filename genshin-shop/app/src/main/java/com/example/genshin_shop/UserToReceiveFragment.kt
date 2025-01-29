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

class UserToReceiveFragment : Fragment() {

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

        fetchToReceiveOrders()

        return view
    }

    private fun fetchToReceiveOrders() {
        userId?.let { uid ->
            database.child("orders").child(uid)
                .orderByChild("status").equalTo("Shipped")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        ordersList.clear()

                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            if (order != null) {
                                order.id = orderSnapshot.key.orEmpty()

                                // Parse items safely
                                val rawItemsSnapshot = orderSnapshot.child("items")
                                order.items = order.parseItems(rawItemsSnapshot)

                                ordersList.add(order)
                            }
                        }
                        ordersAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }



    private fun openOrderDetail(order: Order) {
        val intent = Intent(context, UserOrderDetailActivity::class.java)
        intent.putExtra("order", order)
        startActivity(intent)
    }
}
