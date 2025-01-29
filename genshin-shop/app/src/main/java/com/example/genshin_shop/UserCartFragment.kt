package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class UserCartFragment : Fragment() {

    private lateinit var cartItemsRecyclerView: RecyclerView
    private lateinit var totalAmountTextView: TextView
    private lateinit var proceedToCheckoutButton: Button
    private lateinit var userCartAdapter: UserCartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_fragment_cart, container, false)

        // Initialize views
        cartItemsRecyclerView = view.findViewById(R.id.cartRecyclerView)
        totalAmountTextView = view.findViewById(R.id.totalAmountTextView)
        proceedToCheckoutButton = view.findViewById(R.id.checkoutButton)

        setupRecyclerView()
        updateTotalAmount(userCartAdapter.getSelectedItems()) // Initially calculate total based on selected items

        // Set up the button to proceed to checkout
        proceedToCheckoutButton.setOnClickListener {
            proceedToCheckout()
        }

        return view
    }

    private fun setupRecyclerView() {
        userCartAdapter = UserCartAdapter(
            UserCartManager.getCartItems().toMutableList(),
            onQuantityChanged = { itemId, newQuantity ->
                UserCartManager.updateQuantity(itemId, newQuantity)
                userCartAdapter.updateItems(UserCartManager.getCartItems())
                updateTotalAmount(userCartAdapter.getSelectedItems())
            },
            onSelectionChanged = { selectedItems ->
                updateTotalAmount(selectedItems) // Update total dynamically as selection changes
            }
        )
        cartItemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cartItemsRecyclerView.adapter = userCartAdapter
    }

    private fun updateTotalAmount(selectedItems: List<OrderItem>) {
        val totalAmount = selectedItems.sumOf { it.totalPrice }
        totalAmountTextView.text = String.format("Total: RM%.2f", totalAmount)
    }

    private fun proceedToCheckout() {
        val selectedItems = userCartAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please select items to checkout.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireActivity(), UserCheckoutActivity::class.java).apply {
            putParcelableArrayListExtra("selected_items", ArrayList(selectedItems))
        }
        startActivity(intent)

    }

    override fun onResume() {
        super.onResume()
        // Reload the cart items from UserCartManager when the fragment is resumed
        userCartAdapter.updateItems(UserCartManager.getCartItems())
        updateTotalAmount(userCartAdapter.getSelectedItems())
    }

}
