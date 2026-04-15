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
import com.example.juiceshop.model.Category

class CategoryAdapter(
    private val onClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCategory: ImageView = view.findViewById(R.id.ivCategory)
        private val tvName: TextView      = view.findViewById(R.id.tvCategoryName)

        fun bind(category: Category) {
            tvName.text = category.name
            Glide.with(itemView.context)
                .load(category.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivCategory)
            itemView.setOnClickListener { onClick(category) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(a: Category, b: Category) = a.categoryId == b.categoryId
        override fun areContentsTheSame(a: Category, b: Category) = a == b
    }
}