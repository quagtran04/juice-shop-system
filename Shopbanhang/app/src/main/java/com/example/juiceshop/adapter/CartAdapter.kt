package com.example.juiceshop.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.juiceshop.R
import com.example.juiceshop.model.CartItem

class CartAdapter(
    private val onRemove: (position: Int) -> Unit,
    private val onQuantityChange: (position: Int, quantity: Int) -> Unit,
    private val onSelectionChanged: () -> Unit  // ← callback khi tick thay đổi
) : ListAdapter<CartItem, CartAdapter.ViewHolder>(DiffCallback) {

    // Lưu trạng thái tick theo productId
    private val selectedIds = mutableSetOf<String>()

    override fun onCurrentListChanged(
        previousList: MutableList<CartItem>,
        currentList: MutableList<CartItem>
    ) {
        // Tự động tick những sản phẩm mới thêm vào
        currentList.forEach { selectedIds.add(it.product.productId) }
        super.onCurrentListChanged(previousList, currentList)
    }

    /** Lấy danh sách sản phẩm đang được chọn */
    fun getSelectedItems(): List<CartItem> =
        currentList.filter { selectedIds.contains(it.product.productId) }

    /** Tổng tiền chỉ tính sản phẩm được chọn */
    fun getSelectedTotal(): Double =
        getSelectedItems().sumOf { it.getTotalPrice() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cbSelect:    CheckBox    = view.findViewById(R.id.cbSelect)
        private val ivProduct:   ImageView   = view.findViewById(R.id.ivProduct)
        private val tvName:      TextView    = view.findViewById(R.id.tvProductName)
        private val tvPrice:     TextView    = view.findViewById(R.id.tvUnitPrice)
        private val tvSubTotal:  TextView    = view.findViewById(R.id.tvSubTotal)
        private val tvQuantity:  TextView    = view.findViewById(R.id.tvQuantity)
        private val btnIncrease: TextView    = view.findViewById(R.id.btnIncrease)
        private val btnDecrease: TextView    = view.findViewById(R.id.btnDecrease)
        private val btnRemove:   ImageButton = view.findViewById(R.id.btnRemove)

        fun bind(item: CartItem, position: Int) {
            val productId = item.product.productId

            tvName.text     = item.product.name
            tvPrice.text    = "${String.format("%,.0f", item.product.price)} đ"
            tvQuantity.text = item.quantity.toString()
            tvSubTotal.text = "${String.format("%,.0f", item.getTotalPrice())} đ"

            Glide.with(itemView.context)
                .load(item.product.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivProduct)

            // Set trạng thái checkbox, tắt listener trước để tránh vòng lặp
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selectedIds.contains(productId)

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(productId)
                else selectedIds.remove(productId)
                onSelectionChanged()  // thông báo CartActivity cập nhật tổng tiền
            }

            // Làm mờ item nếu không được chọn
            itemView.alpha = if (selectedIds.contains(productId)) 1.0f else 0.5f

            btnIncrease.setOnClickListener {
                onQuantityChange(position, item.quantity + 1)
            }
            btnDecrease.setOnClickListener {
                if (item.quantity > 1) onQuantityChange(position, item.quantity - 1)
                else onRemove(position)
            }
            btnRemove.setOnClickListener {
                selectedIds.remove(productId)
                onRemove(position)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(a: CartItem, b: CartItem) =
            a.product.productId == b.product.productId
        override fun areContentsTheSame(a: CartItem, b: CartItem) = a == b
    }
}