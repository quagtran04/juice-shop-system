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
import com.example.juiceshop.model.Product

class ProductSearchAdapter(
    private val onClick: (Product) -> Unit
) : ListAdapter<Product, ProductSearchAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivProduct: ImageView    = view.findViewById(R.id.ivProduct)
        private val tvName: TextView        = view.findViewById(R.id.tvProductName)
        private val tvPrice: TextView       = view.findViewById(R.id.tvProductPrice)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)

        fun bind(product: Product) {
            tvName.text        = product.name
            tvPrice.text       = "${String.format("%,.0f", product.price)} VND"
            tvDescription.text = product.description.ifBlank { "No description" }
            Glide.with(itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivProduct)
            itemView.setOnClickListener { onClick(product) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(a: Product, b: Product) = a.productId == b.productId
        override fun areContentsTheSame(a: Product, b: Product) = a == b
    }
}