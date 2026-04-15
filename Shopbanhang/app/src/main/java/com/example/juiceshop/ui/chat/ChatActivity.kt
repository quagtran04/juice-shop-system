package com.example.juiceshop.ui.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.juiceshop.BuildConfig
import com.example.juiceshop.R
import com.example.juiceshop.adapter.ChatAdapter
import com.example.juiceshop.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages : RecyclerView
    private lateinit var etMessage  : EditText
    private lateinit var btnSend    : ImageButton
    private lateinit var btnBack    : ImageButton
    private lateinit var tvChatName : TextView
    private lateinit var tvStatus   : TextView
    private lateinit var ivAvatar   : ImageView
    private lateinit var tabAI      : TextView
    private lateinit var tabAdmin   : TextView
    private lateinit var chip1      : TextView
    private lateinit var chip2      : TextView
    private lateinit var chip3      : TextView
    private lateinit var adapter    : ChatAdapter

    // Firebase (Admin chat)
    private val db  = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var chatId: String = ""
    private var messageListener: ListenerRegistration? = null

    // Gemini (AI chat)
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private val geminiUrl    = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiApiKey"
    private val SYSTEM_PROMPT = """
        Bạn là JuiceBot, trợ lý AI của JuiceShop - cửa hàng nước ép trái cây.
    Trả lời ngắn gọn, thân thiện, bằng tiếng Việt. Không trả lời quá 3 câu.

    == THÔNG TIN SHOP ==
    - Tên: JuiceShop
    - Giờ mở cửa: 7:00 - 21:00 (tất cả các ngày)
    - Địa chỉ: [Tòa số 12 ngõ 193/64/35 Phường Phú Diễn, Hà Nội]
    - SĐT: [0973385141]

    == SẢN PHẨM & GIÁ ==
    - Nước cam tươi: 25.000đ
    - Nước dâu tây: 30.000đ
    - Nước xoài: 28.000đ
    - [thêm sản phẩm thật từ Firestore của bạn]

    == GIAO HÀNG ==
    - Phạm vi: [xung quanh khu vực Bắc Từ Liêm - Hà Nội]
    - Thời gian: 30-45 phút
    - Phí giao hàng: miễn phí đơn từ 50.000đ

    == THANH TOÁN ==
    - Tiền mặt khi nhận hàng (COD)
    - Ví MoMo

    == VOUCHER ==
    - [nếu có chương trình khuyến mãi thì ghi vào đây]

    == QUY TẮC ==
    - Chỉ tư vấn về sản phẩm, đơn hàng, thanh toán, giao hàng của JuiceShop
    - Câu hỏi ngoài chủ đề → hướng dẫn chuyển sang tab Admin
    - Không bịa đặt thông tin không có trong prompt này
    """.trimIndent()

    private var isAIMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        initViews()
        setupRecyclerView()
        setupListeners()
        setupChips()
        showWelcomeMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage  = findViewById(R.id.etMessage)
        btnSend    = findViewById(R.id.btnSend)
        btnBack    = findViewById(R.id.btnBack)
        tvChatName = findViewById(R.id.tvChatName)
        tvStatus   = findViewById(R.id.tvStatus)
        ivAvatar   = findViewById(R.id.ivAvatar)
        tabAI      = findViewById(R.id.tabAI)
        tabAdmin   = findViewById(R.id.tabAdmin)
        chip1      = findViewById(R.id.chip1)
        chip2      = findViewById(R.id.chip2)
        chip3      = findViewById(R.id.chip3)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener { sendMessage() }
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
        tabAI.setOnClickListener    { switchTab(true) }
        tabAdmin.setOnClickListener { switchTab(false) }
    }

    private fun setupChips() {
        chip1.setOnClickListener { sendUserMessage("🍊 Xem sản phẩm") }
        chip2.setOnClickListener { sendUserMessage("📦 Đơn của tôi") }
        chip3.setOnClickListener { sendUserMessage("💳 Thanh toán") }
    }

    // Switch tab
    private fun switchTab(aiMode: Boolean) {
        isAIMode = aiMode
        messageListener?.remove()
        messageListener = null

        if (aiMode) {
            tabAI.setBackgroundResource(R.drawable.bg_tab_active)
            tabAI.setTextColor(getColor(R.color.status_confirmed))
            tabAdmin.setBackgroundResource(android.R.color.transparent)
            tabAdmin.setTextColor(0xCCFFFFFF.toInt())
            tvChatName.text = "JuiceBot AI"
            tvStatus.text   = "Luôn sẵn sàng hỗ trợ"
            ivAvatar.setImageResource(R.drawable.chat)
        } else {
            tabAdmin.setBackgroundResource(R.drawable.bg_tab_active)
            tabAdmin.setTextColor(getColor(R.color.status_confirmed))
            tabAI.setBackgroundResource(android.R.color.transparent)
            tabAI.setTextColor(0xCCFFFFFF.toInt())
            tvChatName.text = "Admin JuiceShop"
            tvStatus.text   = "Đang hoạt động"
            ivAvatar.setImageResource(R.drawable.taikhoan)
        }

        adapter.clearAll()
        showWelcomeMessage()
        if (!aiMode) initAdminChat()
    }

    private fun showWelcomeMessage() {
        val welcome = if (isAIMode)
            "Xin chào! 👋 Mình là JuiceBot, trợ lý AI của JuiceShop. Mình có thể giúp bạn tìm sản phẩm, kiểm tra đơn hàng và giải đáp thắc mắc!"
        else
            "Xin chào! Đây là hỗ trợ trực tiếp từ Admin JuiceShop. Vui lòng nhắn tin, admin sẽ phản hồi sớm nhất! 😊"
        adapter.addMessage(ChatMessage(text = welcome, isUser = false, timestamp = now()))
        scrollToBottom()
    }

    private fun initAdminChat() {
        if (uid.isEmpty()) return
        chatId = uid
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                chatRef.set(mapOf(
                    "userId"      to uid,
                    "userName"    to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
                    "lastMessage" to "",
                    "lastUpdated" to now(),
                    "status"      to "open"
                ))
            }
            listenToMessages()
        }
    }

    private fun listenToMessages() {
        messageListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val firestoreMessages = snap.documents.mapNotNull { doc ->
                    val text      = doc.getString("text")     ?: return@mapNotNull null
                    val senderId  = doc.getString("senderId") ?: ""
                    val timestamp = doc.getString("timestamp") ?: ""
                    ChatMessage(text = text, isUser = senderId == uid, timestamp = timestamp)
                }
                if (firestoreMessages.isNotEmpty()) {
                    adapter.clearAll()
                    firestoreMessages.forEach { adapter.addMessage(it) }
                    scrollToBottom()
                }
            }
    }

    private fun sendMessageToFirestore(text: String) {
        if (chatId.isEmpty()) return
        val timestamp = now()
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(mapOf(
                "text"      to text,
                "senderId"  to uid,
                "timestamp" to timestamp,
                "createdAt" to FieldValue.serverTimestamp(), // ← thêm dòng này
                "isRead"    to false
            ))
        db.collection("chats").document(chatId).update(mapOf(
            "lastMessage" to text,
            "lastUpdated" to timestamp
        ))
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        etMessage.setText("")
        sendUserMessage(text)
    }

    private fun sendUserMessage(text: String) {
        if (isAIMode) {
            adapter.addMessage(ChatMessage(text = text, isUser = true, timestamp = now()))
            scrollToBottom()
            callGeminiAPI(text)
        } else {
            sendMessageToFirestore(text)
        }
    }

    private fun callGeminiAPI(userText: String) {
        // Hiện typing indicator
        adapter.addMessage(ChatMessage(isTyping = true))
        scrollToBottom()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url  = URL(geminiUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput       = true
                    connectTimeout = 15000
                    readTimeout    = 15000
                }

                // Build request body
                val requestBody = JSONObject().apply {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", SYSTEM_PROMPT) })
                        })
                    })
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", userText) })
                            })
                        })
                    })
                }

                OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

                val responseText = if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }

                val reply = parseGeminiResponse(responseText)

                withContext(Dispatchers.Main) {
                    adapter.removeTyping()
                    adapter.addMessage(ChatMessage(text = reply, isUser = false, timestamp = now()))
                    scrollToBottom()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    adapter.removeTyping()
                    adapter.addMessage(ChatMessage(
                        text = "Xin lỗi, mình đang gặp sự cố kết nối 😔 Bạn thử lại sau hoặc chuyển sang tab Admin nhé!",
                        isUser = false,
                        timestamp = now()
                    ))
                    scrollToBottom()
                }
            }
        }
    }

    private fun parseGeminiResponse(json: String): String {
        return try {
            JSONObject(json)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            "Mình chưa hiểu câu hỏi đó 🤔 Bạn thử hỏi lại hoặc chuyển sang tab Admin nhé!"
        }
    }

    private fun scrollToBottom() {
        rvMessages.post {
            val count = adapter.itemCount
            if (count > 0) rvMessages.smoothScrollToPosition(count - 1)
        }
    }

    private fun now(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}