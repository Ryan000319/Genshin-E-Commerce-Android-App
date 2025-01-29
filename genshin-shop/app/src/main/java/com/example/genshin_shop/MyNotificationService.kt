package com.example.genshin_shop

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyNotificationService : Service() {

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val EXTRA_USER_ID = "extra_user_id"

        private const val CHANNEL_ID_SERVICE = "admin_service_channel"
        private const val SERVICE_NOTIFICATION_ID = 9999

        private const val CHANNEL_NEW_ORDER = "new_order_channel"
        private const val CHANNEL_ORDER_STATUS = "order_status_channel"
        private const val CHANNEL_WISHLIST = "wishlist_notifications"
    }

    private lateinit var database: DatabaseReference
    private var adminChildEventListener: ChildEventListener? = null
    private var userOrderChildEventListener: ChildEventListener? = null
    private val productListeners = mutableMapOf<String, ValueEventListener>()

    private var userId: String? = null
    private var role: String? = null

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        role = intent?.getStringExtra(EXTRA_ROLE)
        userId = intent?.getStringExtra(EXTRA_USER_ID)

        // Start the service as a foreground service with a notification
        startForeground(SERVICE_NOTIFICATION_ID, createForegroundNotification("Notification service is running"))

        // Depending on the role, start the appropriate listeners
        if (role == "admin") {
            startAdminOrderListener()
        } else {
            userId?.let { startUserListeners(it) }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllListeners()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    //Start admin listener for new orders.
    private fun startAdminOrderListener() {
        if (adminChildEventListener != null) return

        adminChildEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.children.forEach { orderSnapshot ->
                    val orderId = orderSnapshot.key ?: "Unknown Order"
                    val order = orderSnapshot.getValue(Order::class.java)
                    order?.let {
                        showNotification(
                            CHANNEL_NEW_ORDER,
                            "New Order Received",
                            "Order ID: $orderId has been placed."
                        )
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        database.child("orders").addChildEventListener(adminChildEventListener as ChildEventListener)
        updateForegroundNotification("Admin order listener running...")
    }

    //Start user-related listeners.
    private fun startUserListeners(uid: String) {
        startUserOrderStatusListener(uid)
        startWishlistMonitoring(uid)
        updateForegroundNotification("User listeners running...")
    }

    private fun startUserOrderStatusListener(uid: String) {
        if (userOrderChildEventListener != null) return

        userOrderChildEventListener = object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedOrder = snapshot.getValue(Order::class.java)
                updatedOrder?.let {
                    showNotification(
                        CHANNEL_ORDER_STATUS,
                        "Order Status Update",
                        "Order ID: ${it.id} is now ${it.status}"
                    )
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        database.child("orders").child(uid).addChildEventListener(userOrderChildEventListener as ChildEventListener)
    }

    private fun startWishlistMonitoring(uid: String) {
        database.child("wishlists").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { wishlistItem ->
                    val productId = wishlistItem.key ?: return@forEach
                    monitorProductChanges(productId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun monitorProductChanges(productId: String) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product::class.java)
                product?.let {
                    val currentPrice = it.price.toDoubleOrNull() ?: 0.0
                    val originalPrice = it.originalPrice.toDoubleOrNull() ?: currentPrice
                    if (currentPrice <= 0.0 || originalPrice <= 0.0) return

                    val discountPercentage = ((originalPrice - currentPrice) / originalPrice) * 100
                    if (discountPercentage >= MainActivity.DISCOUNT_THRESHOLD_PERCENTAGE || it.quantity < MainActivity.LOW_STOCK_THRESHOLD) {
                        sendWishlistNotification(it)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        database.child("products").child(productId).addValueEventListener(listener)
        productListeners[productId] = listener
    }

    private fun sendWishlistNotification(product: Product) {
        showNotification(
            CHANNEL_WISHLIST,
            "Wishlist Alert",
            "${product.name} is discounted or low in stock!"
        )
    }

    //Stop all listeners and cleanup.
    private fun stopAllListeners() {
        adminChildEventListener?.let {
            database.child("orders").removeEventListener(it)
            adminChildEventListener = null
        }

        userOrderChildEventListener?.let {
            userId?.let { uid ->
                database.child("orders").child(uid).removeEventListener(it)
            }
            userOrderChildEventListener = null
        }

        productListeners.forEach { (productId, listener) ->
            database.child("products").child(productId).removeEventListener(listener)
        }
        productListeners.clear()
    }

    //Notification and Foreground Service Setup
    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Listener Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val newOrderChannel = NotificationChannel(
            CHANNEL_NEW_ORDER,
            "New Orders",
            NotificationManager.IMPORTANCE_HIGH
        )
        val orderStatusChannel = NotificationChannel(
            CHANNEL_ORDER_STATUS,
            "Order Status Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        val wishlistChannel = NotificationChannel(
            CHANNEL_WISHLIST,
            "Wishlist Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(newOrderChannel)
        notificationManager.createNotificationChannel(orderStatusChannel)
        notificationManager.createNotificationChannel(wishlistChannel)
    }

    private fun createForegroundNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setContentTitle("Notification Service Running")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val notification = createForegroundNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun showNotification(channelId: String, title: String, message: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
