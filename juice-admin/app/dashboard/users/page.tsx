"use client";

import { useEffect, useState } from "react";
import {
  collection, getDocs, doc, updateDoc, query, where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { User } from "@/types";
import toast from "react-hot-toast";

// Component

export default function UsersPage() {
  const [users,    setUsers]    = useState<User[]>([]);
  const [loading,  setLoading]  = useState(true);
  const [search,   setSearch]   = useState("");
  const [selected, setSelected] = useState<User | null>(null);
  const [updating, setUpdating] = useState(false);

  useEffect(() => { loadUsers(); }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      // Chỉ load customer, không load admin
      const snap = await getDocs(
        query(collection(db, "users"), where("role", "==", "user"))
      );
      setUsers(snap.docs.map(d => ({ ...d.data(), userId: d.id } as User)));
    } catch {
      toast.error("Không thể tải danh sách tài khoản");
    } finally {
      setLoading(false);
    }
  };

  // Lọc

  const filtered = users.filter(u =>
    search === "" ||
    u.username?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.phone?.includes(search)
  );

  // Khóa / Mở khóa tài khoản

  const toggleBlock = async (user: User) => {
    const isBlocked = (user as any).blocked ?? false;
    setUpdating(true);
    try {
      await updateDoc(doc(db, "users", user.userId), {
        blocked: !isBlocked,
      });
      const updated = { ...user, blocked: !isBlocked } as any;
      setUsers(prev => prev.map(u => u.userId === user.userId ? updated : u));
      if (selected?.userId === user.userId) setSelected(updated);
      toast.success(isBlocked ? "Đã mở khóa tài khoản!" : "Đã khóa tài khoản!");
    } catch {
      toast.error("Cập nhật thất bại, thử lại!");
    } finally {
      setUpdating(false);
    }
  };

  // Avatar placeholder

  const initials = (name: string) =>
    name ? name.trim().split(" ").pop()![0].toUpperCase() : "?";

  const avatarColor = (userId: string) => {
    const colors = [
      "bg-red-400",    "bg-orange-400", "bg-yellow-400",
      "bg-green-400",  "bg-teal-400",   "bg-blue-400",
      "bg-indigo-400", "bg-purple-400", "bg-pink-400",
    ];
    const idx = userId.charCodeAt(0) % colors.length;
    return colors[idx];
  };

  // Render

  return (
    <div className="p-6 space-y-5">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Quản lý Tài khoản</h1>
          <p className="text-gray-500 text-sm mt-1">{users.length} khách hàng</p>
        </div>
        <button
          onClick={loadUsers}
          className="flex items-center gap-2 px-4 py-2 text-sm bg-white border text-gray-800 rounded-lg hover:bg-gray-50 transition-colors"
        >
          🔄 Làm mới
        </button>
      </div>

      {/* Search */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <input
          type="text"
          placeholder="Tìm theo tên, email, số điện thoại..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border text-gray-800 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-green-500 bg-white"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <svg className="animate-spin h-8 w-8 text-green-500" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
            </svg>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase border-b">
                <tr>
                  {["Tài khoản", "Email", "Số điện thoại", "Địa chỉ", "Trạng thái", "Thao tác"].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(user => {
                  const isBlocked = (user as any).blocked ?? false;
                  return (
                    <tr key={user.userId} className="hover:bg-gray-50 transition-colors">

                      {/* Avatar + tên */}
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className={`w-9 h-9 rounded-full ${avatarColor(user.userId)} flex items-center justify-center text-white text-sm font-bold shrink-0`}>
                            {initials(user.username)}
                          </div>
                          <div>
                            <p className="font-medium text-gray-800">{user.username}</p>
                            <p className="text-xs text-gray-400 mt-0.5">
                              {user.createdAt ? `Tham gia ${user.createdAt.substring(0, 10)}` : ""}
                            </p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-3.5 text-gray-600">{user.email}</td>
                      <td className="px-5 py-3.5 text-gray-600">{user.phone || "—"}</td>
                      <td className="px-5 py-3.5 text-gray-600 max-w-[160px]">
                        <p className="truncate">{user.address || "—"}</p>
                      </td>

                      {/* Trạng thái */}
                      <td className="px-5 py-3.5">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                          isBlocked
                            ? "bg-red-100 text-red-700 border-red-200"
                            : "bg-green-100 text-green-700 border-green-200"
                        }`}>
                          {isBlocked ? "🔒 Đã khóa" : "✅ Hoạt động"}
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <button
                            onClick={() => setSelected(user)}
                            className="text-blue-500 hover:text-blue-700 text-xs font-medium hover:underline"
                          >
                            Chi tiết
                          </button>
                          <button
                            onClick={() => toggleBlock(user)}
                            disabled={updating}
                            className={`text-xs font-medium hover:underline disabled:opacity-50 ${
                              isBlocked
                                ? "text-green-600 hover:text-green-800"
                                : "text-red-500 hover:text-red-700"
                            }`}
                          >
                            {isBlocked ? "Mở khóa" : "Khóa"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {filtered.length === 0 && !loading && (
              <div className="text-center py-12 text-gray-400">
                <p className="text-3xl mb-2">👤</p>
                <p>Không có tài khoản nào</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/*Modal chi tiết user */}
      {selected && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">

            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-lg font-bold text-gray-800">Chi tiết tài khoản</h2>
              <button
                onClick={() => setSelected(null)}
                className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
              >
                ×
              </button>
            </div>

            <div className="p-6 space-y-5">

              {/* Avatar + tên */}
              <div className="flex flex-col items-center gap-3">
                <div className={`w-16 h-16 rounded-full ${avatarColor(selected.userId)} flex items-center justify-center text-white text-2xl font-bold`}>
                  {initials(selected.username)}
                </div>
                <div className="text-center">
                  <p className="font-bold text-gray-800 text-lg">{selected.username}</p>
                  <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                    (selected as any).blocked
                      ? "bg-red-100 text-red-700 border-red-200"
                      : "bg-green-100 text-green-700 border-green-200"
                  }`}>
                    {(selected as any).blocked ? "🔒 Đã khóa" : "✅ Hoạt động"}
                  </span>
                </div>
              </div>

              {/* Thông tin */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                {[
                  { icon: "📧", label: "Email",        value: selected.email       },
                  { icon: "📱", label: "Điện thoại",   value: selected.phone || "—"},
                  { icon: "📍", label: "Địa chỉ",      value: selected.address || "—"},
                  { icon: "📅", label: "Ngày tham gia", value: selected.createdAt?.substring(0, 10) || "—"},
                ].map(row => (
                  <div key={row.label} className="flex items-start gap-3 text-sm">
                    <span className="text-base shrink-0">{row.icon}</span>
                    <div>
                      <p className="text-gray-500 text-xs">{row.label}</p>
                      <p className="text-gray-800 font-medium">{row.value}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Nút khóa / mở khóa */}
              <button
                onClick={() => toggleBlock(selected)}
                disabled={updating}
                className={`w-full py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 ${
                  (selected as any).blocked
                    ? "bg-green-500 hover:bg-green-600 text-white"
                    : "bg-red-50 hover:bg-red-100 text-red-600 border border-red-200"
                }`}
              >
                {updating
                  ? "Đang cập nhật..."
                  : (selected as any).blocked
                    ? "🔓 Mở khóa tài khoản"
                    : "🔒 Khóa tài khoản"
                }
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}