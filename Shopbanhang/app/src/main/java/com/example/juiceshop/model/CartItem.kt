package com.example.juiceshop.model

import android.os.Parcel
import android.os.Parcelable

data class CartItem(
    val product: Product,
    var quantity: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        product = Product(
            productId  = parcel.readString() ?: "",
            name       = parcel.readString() ?: "",
            price      = parcel.readDouble(),
            description= parcel.readString() ?: "",
            note       = parcel.readString() ?: "",
            stock      = parcel.readInt(),
            categoryId = parcel.readString() ?: "",
            imageUrl   = parcel.readString() ?: ""
        ),
        quantity = parcel.readInt()
    )

    fun getTotalPrice(): Double = product.price * quantity

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(product.productId)
        parcel.writeString(product.name)
        parcel.writeDouble(product.price)
        parcel.writeString(product.description)
        parcel.writeString(product.note)
        parcel.writeInt(product.stock)
        parcel.writeString(product.categoryId)
        parcel.writeString(product.imageUrl)
        parcel.writeInt(quantity)
    }

    companion object CREATOR : Parcelable.Creator<CartItem> {
        override fun createFromParcel(parcel: Parcel): CartItem = CartItem(parcel)
        override fun newArray(size: Int): Array<CartItem?> = arrayOfNulls(size)
    }
}