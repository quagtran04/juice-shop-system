package com.example.juiceshop.model

import android.os.Parcel
import android.os.Parcelable

data class OrderDetail(
    val detailId: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",   // Lưu tên tại thời điểm đặt hàng
    val imageUrl: String = "",      // Lưu ảnh tại thời điểm đặt hàng
    val quantity: Int = 0,
    val unitPrice: Double = 0.0
) : Parcelable {

    constructor() : this("", "", "", "", "", 0, 0.0)

    constructor(parcel: Parcel) : this(
        detailId    = parcel.readString() ?: "",
        orderId     = parcel.readString() ?: "",
        productId   = parcel.readString() ?: "",
        productName = parcel.readString() ?: "",
        imageUrl    = parcel.readString() ?: "",
        quantity    = parcel.readInt(),
        unitPrice   = parcel.readDouble()
    )

    fun getSubTotal(): Double = unitPrice * quantity

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(detailId)
        dest.writeString(orderId)
        dest.writeString(productId)
        dest.writeString(productName)
        dest.writeString(imageUrl)
        dest.writeInt(quantity)
        dest.writeDouble(unitPrice)
    }

    companion object CREATOR : Parcelable.Creator<OrderDetail> {
        override fun createFromParcel(parcel: Parcel): OrderDetail = OrderDetail(parcel)
        override fun newArray(size: Int): Array<OrderDetail?> = arrayOfNulls(size)
    }
}