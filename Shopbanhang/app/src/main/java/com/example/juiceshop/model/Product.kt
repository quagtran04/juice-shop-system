package com.example.juiceshop.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Product(
    val productId:   String = "",
    val name:        String = "",
    val price:       Double = 0.0,
    val description: String = "",
    val note:        String = "",
    val stock:       Int    = 0,
    val soldCount:   Int    = 0,
    val categoryId:  String = "",
    val imageUrl:    String = "",
    val createdAt:   Date?  = null
) {
    constructor() : this("", "", 0.0, "", "", 0, 0, "", "", null)
}