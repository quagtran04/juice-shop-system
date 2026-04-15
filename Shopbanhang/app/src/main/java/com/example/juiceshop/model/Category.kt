package com.example.juiceshop.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Category(
    val categoryId: String = "",
    val name: String = "",
    val imageUrl: String = ""
) {
    constructor() : this("", "", "")

    override fun toString(): String = name
}