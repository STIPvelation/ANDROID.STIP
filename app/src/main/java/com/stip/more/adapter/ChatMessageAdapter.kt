package com.stip.stip.more.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.more.model.ChatMessage

class ChatMessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        val tvMessageContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val cardMessage: CardView = view.findViewById(R.id.cardMessage)
        val messageContainer: LinearLayout = view.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        
        // 메시지 콘텐츠 설정
        holder.tvMessageContent.text = message.content
        holder.tvTime.text = message.time
        
        if (message.isCurrentUser) {
            // 내가 보낸 메시지
            holder.tvSenderName.visibility = View.GONE
            holder.messageContainer.gravity = Gravity.END
            holder.cardMessage.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
            holder.tvMessageContent.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            // 시간을 왼쪽에 배치
            holder.messageContainer.removeView(holder.tvTime)
            holder.messageContainer.addView(holder.tvTime, 0)
        } else {
            // 상대방이 보낸 메시지
            holder.tvSenderName.visibility = View.VISIBLE
            holder.tvSenderName.text = message.sender
            holder.messageContainer.gravity = Gravity.START
            holder.cardMessage.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            holder.tvMessageContent.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.color_text_default))
            // 시간을 오른쪽에 배치
            holder.messageContainer.removeView(holder.tvTime)
            holder.messageContainer.addView(holder.tvTime)
        }
    }

    override fun getItemCount() = messages.size
}
