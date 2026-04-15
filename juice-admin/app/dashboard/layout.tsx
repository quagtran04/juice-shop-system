"use client";

import { useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";

const navItems = [
  { href: "/dashboard",            label: "Tổng quan",  icon: "📊" },
  { href: "/dashboard/orders",     label: "Đơn hàng",   icon: "📦" },
  { href: "/dashboard/products",   label: "Sản phẩm",   icon: "🧃" },
  { href: "/dashboard/categories", label: "Danh mục",   icon: "📂" },
  { href: "/dashboard/users",      label: "Tài khoản",  icon: "👤" },
  { href: "/dashboard/vouchers",   label: "Voucher",    icon: "🎟️" },
  { href: "/dashboard/chat",       label: "Hỗ trợ",     icon: "💬" },
];

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, loading, logout } = useAuth();
  const router   = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="flex flex-col items-center gap-3">
          <svg className="animate-spin h-10 w-10 text-green-500" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
          </svg>
          <p className="text-gray-500">Đang tải...</p>
        </div>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="flex h-screen bg-gray-50">

      {/* Sidebar - Navy dark như TTStore */}
      <aside className="w-60 flex flex-col" style={{ backgroundColor: "#1a2942" }}>

        {/* Logo */}
        <div className="p-6" style={{ borderBottom: "1px solid rgba(255,255,255,0.1)" }}>
          <div className="flex items-center gap-3">
            <span className="text-2xl">🍋</span>
            <div>
              <p className="font-bold text-white text-sm">Juice Shop</p>
              <p className="text-xs" style={{ color: "rgba(255,255,255,0.5)" }}>Admin Panel</p>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 p-4 space-y-1">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className="flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all"
                style={{
                  backgroundColor: isActive ? "#2563eb" : "transparent",
                  color: isActive ? "#ffffff" : "rgba(255,255,255,0.65)",
                }}
                onMouseEnter={e => {
                  if (!isActive) (e.currentTarget as HTMLElement).style.backgroundColor = "rgba(255,255,255,0.08)";
                }}
                onMouseLeave={e => {
                  if (!isActive) (e.currentTarget as HTMLElement).style.backgroundColor = "transparent";
                }}
              >
                <span className="text-lg">{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* User info + logout */}
        <div className="p-4" style={{ borderTop: "1px solid rgba(255,255,255,0.1)" }}>
          <div className="flex items-center gap-3 mb-3">
            <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white text-xs font-bold">
              A
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">Admin</p>
              <p className="text-xs truncate" style={{ color: "rgba(255,255,255,0.45)" }}>
                {user.email}
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full text-sm text-white py-2 px-3 rounded-lg transition-colors text-left font-medium"
            style={{ backgroundColor: "#ef4444" }}
            onMouseEnter={e => (e.currentTarget.style.backgroundColor = "#dc2626")}
            onMouseLeave={e => (e.currentTarget.style.backgroundColor = "#ef4444")}
          >
            🚪 Đăng xuất
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto">
        {children}
      </main>
    </div>
  );
}