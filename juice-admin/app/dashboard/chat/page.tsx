"use client";

import { useEffect, useState, useRef } from "react";
import { db } from "@/lib/firebase";
import {
  collection, onSnapshot, query,
  addDoc, updateDoc, doc, orderBy, serverTimestamp
} from "firebase/firestore";

interface ChatSession {
  id: string;
  userId: string;
  userName: string;
  lastMessage: string;
  lastUpdated: string;
  status: string;
  unread?: number;
}

interface Message {
  id: string;
  text: string;
  senderId: string;
  timestamp: string;
  isRead: boolean;
}

const ADMIN_ID = "admin";

export default function ChatPage() {
  const [sessions,        setSessions]        = useState<ChatSession[]>([]);
  const [selectedSession, setSelectedSession] = useState<ChatSession | null>(null);
  const [messages,        setMessages]        = useState<Message[]>([]);
  const [inputText,       setInputText]       = useState("");
  const [sending,         setSending]         = useState(false);
  const [searchText,      setSearchText]      = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Load danh sách sessions
  useEffect(() => {
    const q = query(collection(db, "chats"), orderBy("lastUpdated", "desc"));
    const unsub = onSnapshot(q,
      (snap) => {
        setSessions(snap.docs.map(d => ({ id: d.id, ...d.data() } as ChatSession)));
      },
      (error) => { console.error("Lỗi load chats:", error); }
    );
    return () => unsub();
  }, []);

  // Load messages khi chọn session
  useEffect(() => {
  if (!selectedSession) return;
  const msgQuery = query(
    collection(db, "chats", selectedSession.id, "messages")
  );
  const unsub = onSnapshot(msgQuery, (snap) => {
    const msgs = snap.docs
      .map(d => ({ id: d.id, ...d.data() } as any))
      .filter(m => !m.createdAt || m.createdAt.seconds)
      .sort((a: any, b: any) => {
        const aSeconds = a.createdAt?.seconds ?? 0;
        const bSeconds = b.createdAt?.seconds ?? 0;
        if (aSeconds !== bSeconds) return aSeconds - bSeconds;
      
        return a.id < b.id ? -1 : 1;
      });
    setMessages(msgs);
    scrollToBottom();
  });
  return () => unsub();
}, [selectedSession]);

  const scrollToBottom = () => {
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, 100);
  };

  const sendMessage = async () => {
    const text = inputText.trim();
    if (!text || !selectedSession || sending) return;
    setSending(true);
    setInputText("");
    try {
      const now = new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
      await addDoc(
        collection(db, "chats", selectedSession.id, "messages"),
        { text, senderId: ADMIN_ID, timestamp: now, createdAt: serverTimestamp(), isRead: false }
      );
      await updateDoc(doc(db, "chats", selectedSession.id), {
        lastMessage: text,
        lastUpdated: now,
        updatedAt: serverTimestamp(),
      });
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const filteredSessions = sessions.filter(s => {
    const q = searchText.toLowerCase().trim();
    if (!q) return true;
    return (
      s.userName?.toLowerCase().includes(q) ||
      s.userId?.toLowerCase().includes(q) ||
      s.lastMessage?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="flex h-screen bg-gray-50">

      {/* Danh sách cuộc hội thoại */}
      <div className="w-80 bg-white border-r flex flex-col">
        <div className="p-4 border-b bg-white space-y-3">
          <div>
            <h1 className="text-lg font-bold text-gray-800">💬 Hỗ trợ khách hàng</h1>
            <p className="text-sm text-gray-500 mt-0.5">{sessions.length} cuộc hội thoại</p>
          </div>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm pointer-events-none">🔍</span>
            <input
              type="text"
              value={searchText}
              onChange={e => setSearchText(e.target.value)}
              placeholder="Tìm theo tên, email..."
              className="w-full pl-8 pr-8 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 text-gray-800 placeholder-gray-400"
            />
            {searchText && (
              <button
                onClick={() => setSearchText("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs font-bold"
              >✕</button>
            )}
          </div>
          {searchText && (
            <p className="text-xs text-gray-500">
              Tìm thấy <span className="font-semibold text-green-600">{filteredSessions.length}</span> kết quả
            </p>
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          {filteredSessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 px-4">
              <p className="text-3xl mb-2">{searchText ? "🔍" : "💬"}</p>
              <p className="text-sm text-center">
                {searchText ? `Không tìm thấy "${searchText}"` : "Chưa có tin nhắn nào"}
              </p>
            </div>
          ) : (
            filteredSessions.map((session) => (
              <div
                key={session.id}
                onClick={() => setSelectedSession(session)}
                className={`flex items-center gap-3 p-4 cursor-pointer border-b transition-colors ${
                  selectedSession?.id === session.id
                    ? "bg-green-50 border-l-4 border-l-green-500"
                    : "hover:bg-gray-50"
                }`}
              >
                <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center text-green-700 font-bold text-sm shrink-0">
                  {(session.userName || session.userId || "U").charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-gray-800 truncate">
                      {session.userName || session.userId}
                    </p>
                    <span className="text-xs text-gray-400 shrink-0 ml-1">
                      {session.lastUpdated || ""}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 truncate mt-0.5">
                    {session.lastMessage || "Chưa có tin nhắn"}
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Khu vực chat */}
      {selectedSession ? (
        <div className="flex-1 flex flex-col">
          <div className="bg-white border-b px-6 py-4 flex items-center gap-3 shadow-sm">
            <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center text-green-700 font-bold">
              {(selectedSession.userName || selectedSession.userId || "U").charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="font-semibold text-gray-800">
                {selectedSession.userName || selectedSession.userId}
              </p>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-500 inline-block"></span>
                <span className="text-xs text-gray-500">Đang hoạt động</span>
              </div>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-6 space-y-3">
            {messages.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-gray-400">
                <p className="text-4xl mb-2">👋</p>
                <p className="text-sm">Chưa có tin nhắn trong cuộc hội thoại này</p>
              </div>
            ) : (
              messages.map((msg) => {
                const isAdmin = msg.senderId === ADMIN_ID;
                return (
                  <div key={msg.id} className={`flex ${isAdmin ? "justify-end" : "justify-start"}`}>
                    {!isAdmin && (
                      <div className="w-7 h-7 rounded-full bg-green-100 flex items-center justify-center text-green-700 text-xs font-bold mr-2 shrink-0 self-end">
                        {(selectedSession.userName || "U").charAt(0).toUpperCase()}
                      </div>
                    )}
                    <div className={`max-w-xs lg:max-w-md flex flex-col ${isAdmin ? "items-end" : "items-start"}`}>
                      <div className={`px-4 py-2.5 rounded-2xl text-sm ${
                        isAdmin
                          ? "bg-green-500 text-white rounded-br-sm"
                          : "bg-white text-gray-800 border rounded-bl-sm shadow-sm"
                      }`}>
                        {msg.text}
                      </div>
                      <span className="text-xs text-gray-400 mt-1 px-1">{msg.timestamp}</span>
                    </div>
                    {isAdmin && (
                      <div className="w-7 h-7 rounded-full bg-blue-500 flex items-center justify-center text-white text-xs font-bold ml-2 shrink-0 self-end">
                        A
                      </div>
                    )}
                  </div>
                );
              })
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="bg-white border-t p-4">
            <div className="flex items-end gap-3">
              <textarea
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Nhập tin nhắn... (Enter để gửi)"
                rows={1}
                className="flex-1 resize-none border rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 max-h-32"
                style={{ minHeight: "42px" }}
              />
              <button
                onClick={sendMessage}
                disabled={!inputText.trim() || sending}
                className="bg-green-500 hover:bg-green-600 disabled:opacity-50 text-white rounded-xl px-4 py-2.5 text-sm font-medium transition-colors shrink-0"
              >
                {sending ? "⏳" : "Gửi ➤"}
              </button>
            </div>
            <p className="text-xs text-gray-400 mt-1.5">Shift+Enter để xuống dòng</p>
          </div>
        </div>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
          <p className="text-6xl mb-4">💬</p>
          <p className="text-lg font-medium text-gray-500">Chọn một cuộc hội thoại</p>
          <p className="text-sm mt-1">để bắt đầu hỗ trợ khách hàng</p>
        </div>
      )}
    </div>
  );
}