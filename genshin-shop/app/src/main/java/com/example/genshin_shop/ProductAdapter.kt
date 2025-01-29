package com.example.genshin_shop

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProductAdapter(
    private val products: MutableList<Product>,
    private val isAdmin: Boolean, // True for admin layout, false for user layout
    private val onProductEdit: (Product) -> Unit = {}, // Admin-only actions
    private val onProductDelete: (Product) -> Unit = {},
    var onProductClick: ((Product) -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private var userId = auth.currentUser?.uid
    private var wishlistRef: DatabaseReference? = null
    private var wishlistListener: ChildEventListener? = null

    init {
        setupUserWishlistRef()
    }

    private fun setupUserWishlistRef() {
        userId = auth.currentUser?.uid
        wishlistRef = userId?.let {
            FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("wishlists").child(it)
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)

        // Admin-specific fields
        val productStock: TextView? = itemView.findViewById(R.id.product_stock)
        val productWishlistCount: TextView? = itemView.findViewById(R.id.product_wishlist_count)
        val editIcon: ImageView? = itemView.findViewById(R.id.edit_product_icon)
        val deleteIcon: ImageView? = itemView.findViewById(R.id.delete_product_icon)

        // User-specific fields
        val wishlistIcon: ImageView? = itemView.findViewById(R.id.wishlist_icon)

        init {
            itemView.setOnClickListener {
                val product = products[bindingAdapterPosition]
                onProductClick?.invoke(product)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdmin) 1 else 0 // 1 for admin layout, 0 for user layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layoutId = if (viewType == 1) R.layout.item_product else R.layout.item_product_user
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Set product name and formatted price
        holder.productName.text = product.name
        holder.productPrice.text = "Price: RM%.2f".format(product.price.toDouble())

        // Load product image using Glide
        Glide.with(holder.productImage.context)
            .load(product.mainImage)
            .placeholder(R.drawable.merch_1) // Placeholder while loading
            .error(R.drawable.merch_1) // Fallback image on error
            .into(holder.productImage)

        // Admin View: Display Stock Quantity and Wishlist Count
        if (isAdmin) {
            holder.productStock?.text = "Stock: ${product.quantity}"
            holder.productWishlistCount?.text = "Wishlisted: ${product.wishlistCount} times"
            holder.editIcon?.setOnClickListener { onProductEdit(product) }
            holder.deleteIcon?.setOnClickListener { onProductDelete(product) }
        } else {
            // User View: Handle Wishlist Icon
            setupWishlistIcon(holder, product)
        }

        // Log to verify binding
        Log.d("ProductAdapter", "Binding product: ${product.name}")
    }

    override fun getItemCount(): Int = products.size

    private fun setupWishlistIcon(holder: ProductViewHolder, product: Product) {
        wishlistRef?.child(product.id)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isWishlisted = snapshot.exists()
                product.isWishlisted = isWishlisted
                updateWishlistIcon(holder, isWishlisted)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WishlistSync", "Error loading wishlist status", error.toException())
                Toast.makeText(holder.itemView.context, "Failed to load wishlist", Toast.LENGTH_SHORT).show()
            }
        })

        holder.wishlistIcon?.setOnClickListener {
            if (wishlistRef != null) {
                toggleWishlistStatus(product, holder)
            } else {
                Toast.makeText(holder.itemView.context, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleWishlistStatus(product: Product, holder: ProductViewHolder) {
        wishlistRef?.let { ref ->
            val newWishlistStatus = !product.isWishlisted
            product.isWishlisted = newWishlistStatus
            updateWishlistIcon(holder, newWishlistStatus)

            if (newWishlistStatus) {
                ref.child(product.id).setValue(true).addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Failed to add to wishlist", Toast.LENGTH_SHORT).show()
                }
            } else {
                ref.child(product.id).removeValue().addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateWishlistIcon(holder: ProductViewHolder, isWishlisted: Boolean) {
        holder.wishlistIcon?.setImageResource(
            if (isWishlisted) R.drawable.wishlist_filled else R.drawable.wishlist_outline
        )
    }

    fun setupWishlistRealtimeListener() {
        if (userId == null) {
            Log.e("WishlistSync", "User not authenticated, unable to set up wishlist listener.")
            return
        }

        wishlistListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val productId = snapshot.key
                productId?.let { updateProductWishlistStatus(it, true) }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val productId = snapshot.key
                productId?.let { updateProductWishlistStatus(it, false) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WishlistSync", "Error syncing wishlist changes", error.toException())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }
        wishlistRef?.addChildEventListener(wishlistListener!!)
    }

    fun removeWishlistListener() {
        wishlistListener?.let { wishlistRef?.removeEventListener(it) }
    }

    private fun updateProductWishlistStatus(productId: String, isWishlisted: Boolean) {
        val index = products.indexOfFirst { it.id == productId }
        if (index != -1) {
            products[index].isWishlisted = isWishlisted
            notifyItemChanged(index)
        }
    }

    fun updateProducts(newProducts: List<Product>) {
        Log.d("ProductAdapter", "Updating adapter with ${newProducts.size} products")

        // Filter products with stock > 0
        val filteredProducts = newProducts.filter { it.quantity > 0 }

        products.clear()
        products.addAll(filteredProducts)
        notifyDataSetChanged()

        Log.d("ProductAdapter", "Filtered products: ${filteredProducts.size} products with stock > 0")
    }


}
