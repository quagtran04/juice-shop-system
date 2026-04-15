package com.example.juiceshop.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.model.Order

class OrderAdapter(
    private val onClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvOrderId       : TextView    = view.findViewById(R.id.tvOrderId)
        private val tvDate          : TextView    = view.findViewById(R.id.tvOrderDate)
        private val tvTotal         : TextView    = view.findViewById(R.id.tvTotalAmount)
        private val tvStatus        : TextView    = view.findViewById(R.id.tvStatus)
        private val tvCustomerName  : TextView    = view.findViewById(R.id.tvCustomerName)
        private val tvPhone         : TextView    = view.findViewById(R.id.tvPhone)
        private val tvItemCount     : TextView    = view.findViewById(R.id.tvItemCount)
        private val tvPaymentStatus : TextView    = view.findViewById(R.id.tvPaymentStatus)
        private val btnDetail       : ImageButton = view.findViewById(R.id.btnDetail)

        fun bind(order: Order) {
            tvOrderId.text      = "#${order.orderId.takeLast(6).uppercase()}"
            tvDate.text         = order.orderDate
            tvTotal.text        = String.format("%,.0f", order.totalAmount)
            tvCustomerName.text = order.customerName
            tvPhone.text        = order.phone
            tvItemCount.text    = "${order.details.size} sản phẩm"
            tvStatus.text       = when (order.status) {
                "pending"    -> "Chờ xác nhận"
                "confirmed"  -> "Confirmed"
                "delivering" -> "Đang giao"
                "completed"  -> "Hoàn thành"
                "cancelled"  -> "Đã hủy"
                else         -> order.status
            }

            // Màu trạng thái đơn hàng
            val colorRes = when (order.status) {
                "pending"    -> R.color.status_pending
                "confirmed"  -> R.color.status_confirmed
                "delivering" -> R.color.status_delivering
                "completed"  -> R.color.status_completed
                "cancelled"  -> R.color.status_cancelled
                else         -> R.color.status_pending
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            // ✅ Badge trạng thái thanh toán
            if (order.paymentStatus == "paid") {
                val label = if (order.paymentMethod == "momo") "✅ Đã TT · MoMo" else "✅ Đã thanh toán"
                tvPaymentStatus.text = label
                tvPaymentStatus.setBackgroundColor(Color.parseColor("#4CAF50")) // xanh lá
            } else {
                tvPaymentStatus.text = "💵 Chưa thanh toán"
                tvPaymentStatus.setBackgroundColor(Color.parseColor("#FF9800")) // cam
            }

            btnDetail.setOnClickListener { onClick(order) }
            itemView.setOnClickListener  { onClick(order) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}