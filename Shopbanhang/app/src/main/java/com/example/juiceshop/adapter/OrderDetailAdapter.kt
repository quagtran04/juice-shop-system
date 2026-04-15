package com.example.juiceshop.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.juiceshop.R
import com.example.juiceshop.model.OrderDetail

class OrderDetailAdapter : ListAdapter<OrderDetail, OrderDetailAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivProduct: ImageView  = view.findViewById(R.id.ivProduct)
        private val tvName: TextView      = view.findViewById(R.id.tvProductName)
        private val tvUnitPrice: TextView = view.findViewById(R.id.tvUnitPrice)
        private val tvQuantity: TextView  = view.findViewById(R.id.tvQuantity)
        private val tvSubTotal: TextView  = view.findViewById(R.id.tvSubTotal)

        fun bind(detail: OrderDetail) {
            tvName.text      = detail.productName
            tvUnitPrice.text = "${String.format("%,.0f", detail.unitPrice)} VND"
            tvQuantity.text  = "x${detail.quantity}"
            tvSubTotal.text  = "${String.format("%,.0f", detail.getSubTotal())} VND"

            Glide.with(itemView.context)
                .load(detail.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivProduct)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<OrderDetail>() {
        override fun areItemsTheSame(a: OrderDetail, b: OrderDetail) = a.detailId == b.detailId
        override fun areContentsTheSame(a: OrderDetail, b: OrderDetail) = a == b
    }
}