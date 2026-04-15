package com.example.juiceshop.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.R
import com.example.juiceshop.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_BOT    = 0
        private const val TYPE_USER   = 1
        private const val TYPE_TYPING = 2
    }

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun removeTyping() {
        val idx = messages.indexOfLast { it.isTyping }
        if (idx != -1) {
            messages.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    fun clearAll() {
        messages.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isTyping  -> TYPE_TYPING
            msg.isUser    -> TYPE_USER
            else          -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER   -> UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.item_message_typing, parent, false))
            else        -> BotViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BotViewHolder    -> holder.bind(messages[position])
            is UserViewHolder   -> holder.bind(messages[position])
            is TypingViewHolder -> holder.startAnimation()
        }
    }

    // ── Bot ViewHolder ──
    inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView    = view.findViewById(R.id.tvTime)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.text
            tvTime.text    = msg.timestamp
        }
    }

    // ── User ViewHolder ──
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView    = view.findViewById(R.id.tvTime)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.text
            tvTime.text    = msg.timestamp
        }
    }

    // ── Typing ViewHolder (3 dots animation) ──
    inner class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dot1: View = view.findViewById(R.id.dot1)
        private val dot2: View = view.findViewById(R.id.dot2)
        private val dot3: View = view.findViewById(R.id.dot3)

        fun startAnimation() {
            animateDot(dot1, 0)
            animateDot(dot2, 200)
            animateDot(dot3, 400)
        }

        private fun animateDot(dot: View, delay: Long) {
            ObjectAnimator.ofFloat(dot, "translationY", 0f, -12f, 0f).apply {
                duration    = 600
                startDelay  = delay
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
    }
}