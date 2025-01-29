package com.example.genshin_shop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class StartRegisterActivity : AppCompatActivity(){

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var usernameInput: EditText
    private lateinit var emailInput:EditText
    private lateinit var passwordInput: EditText
    private lateinit var phoneNumberInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginRedirectText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://genshinshop-92fff-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        //Initialize Views
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        loginRedirectText = findViewById(R.id.loginRedirectText)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)

        //Register Button Click Listener
        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val phoneNumber = phoneNumberInput.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() &&
                confirmPassword.isNotEmpty() && phoneNumber.isNotEmpty()
            ) {
                if (password == confirmPassword) {
                    registerUser(username, email, password, phoneNumber)
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        //Redirect to Login if already registered
        loginRedirectText.setOnClickListener{
            startActivity(Intent(this, StartLoginActivity::class.java))
        }

    }

    private fun registerUser(username: String, email: String, password: String, phoneNumber: String) {
        if (!phoneNumber.startsWith("+") || phoneNumber.length < 10) {
            Toast.makeText(this, "Phone number must be in E.164 format (+1234567890).", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToDatabase(userId, username, phoneNumber)
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(userId: String, username: String, phoneNumber: String) {
        val userRef = database.child("users").child(userId)
        val user = mapOf(
            "username" to username,
            "phoneNumber" to phoneNumber,
            "role" to "user"
        )

        userRef.setValue(user).addOnSuccessListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to save user: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }


}