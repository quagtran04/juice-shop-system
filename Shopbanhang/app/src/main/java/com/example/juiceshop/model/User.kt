package com.example.juiceshop.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val role: String = "user"  // "user" or "admin"
) {
    // Firebase requires a no-arg constructor
    constructor() : this("", "", "", "", "", "user")
}