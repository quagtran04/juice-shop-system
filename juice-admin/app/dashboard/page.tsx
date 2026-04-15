"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Order, Product, User } from "@/types";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, LineChart, Line,
  PieChart, Pie, Cell, Legend, ComposedChart, Area,
} from "recharts";

interface Stats {
  totalRevenue:  number;
  totalOrders:   number;
  totalProducts: number;
  totalUsers:    number;
  pendingOrders: number;
}

const STATUS_LABEL: Record<string, string> = {
  pending:    "Chờ xác nhận",
  confirmed:  "Đã xác nhận",
  delivering: "Đang giao",
  completed:  "Hoàn thành",
  cancelled:  "Đã hủy",
};

const STATUS_COLOR: Record<string, string> = {
  pending:    "bg-orange-100 text-orange-700",
  confirmed:  "bg-blue-100 text-blue-700",
  delivering: "bg-purple-100 text-purple-700",
  completed:  "bg-green-100 text-green-700",
  cancelled:  "bg-red-100 text-red-700",
};

const PIE_COLORS = ["#22c55e", "#3b82f6", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4", "#ec4899"];

const fmt = (n: number) => n.toLocaleString("vi-VN") + " đ";
const fmtM = (v: number) => (v / 1000).toFixed(0) + "K";

export default function DashboardPage() {
  const [stats,        setStats]        = useState<Stats | null>(null);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [chartData,    setChartData]    = useState<any[]>([]);
  const [pieData,      setPieData]      = useState<any[]>([]);
  const [allOrders,    setAllOrders]    = useState<Order[]>([]);
  const [loading,      setLoading]      = useState(true);
  const [fromDate,     setFromDate]     = useState("");
  const [toDate,       setToDate]       = useState("");

  useEffect(() => { loadDashboard(); }, []);

  const loadDashboard = async () => {
    try {
      const [ordersSnap, productsSnap, usersSnap, categoriesSnap] = await Promise.all([
        getDocs(collection(db, "orders")),
        getDocs(collection(db, "products")),
        getDocs(query(collection(db, "users"), where("role", "==", "user"))),
        getDocs(collection(db, "categories")),
      ]);

      const orders    = ordersSnap.docs.map(d  => ({ ...d.data(), orderId:    d.id } as Order));
      const products  = productsSnap.docs.map(d => ({ ...d.data(), productId: d.id } as Product));
      const categories = categoriesSnap.docs.map(d => ({ id: d.id, ...d.data() } as any));

      setAllOrders(orders);

      const completed    = orders.filter(o => o.status === "completed");
      const totalRevenue = completed.reduce((sum, o) => sum + o.totalAmount, 0);

      setStats({
        totalRevenue,
        totalOrders:   orders.length,
        totalProducts: products.length,
        totalUsers:    usersSnap.size,
        pendingOrders: orders.filter(o => o.status === "pending").length,
      });

      const sorted = [...orders].sort((a, b) =>
        new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime()
      );
      setRecentOrders(sorted.slice(0, 5));

      // Chart doanh thu + số đơn theo tháng
      buildChartData(orders);

      // Pie chart: số sản phẩm theo danh mục
      const catMap: Record<string, number> = {};
      products.forEach(p => {
        const cat = categories.find((c: any) => c.id === p.categoryId);
        const name = cat?.name ?? "Khác";
        catMap[name] = (catMap[name] ?? 0) + 1;
      });
      setPieData(Object.entries(catMap).map(([name, value]) => ({ name, value })));

    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const buildChartData = (orders: Order[], from?: string, to?: string) => {
    // group theo ngày, group theo tháng
    const hasFilter = !!(from || to);

    const completed = orders.filter(o => {
      if (o.status !== "completed") return false;
      if (from && o.orderDate < from) return false;
      if (to   && o.orderDate > to + "z") return false;
      return true;
    });

    const allFiltered = orders.filter(o => {
      if (from && o.orderDate < from) return false;
      if (to   && o.orderDate > to + "z") return false;
      return true;
    });

    const dataMap: Record<string, { revenue: number; orders: number }> = {};

    completed.forEach(o => {
      const key = hasFilter
        ? (o.orderDate?.substring(0, 10) ?? "")
        : (o.orderDate?.substring(0, 7) ?? "");
      if (!key) return;
      if (!dataMap[key]) dataMap[key] = { revenue: 0, orders: 0 };
      dataMap[key].revenue += o.totalAmount;
    });

    allFiltered.forEach(o => {
      const key = hasFilter
        ? (o.orderDate?.substring(0, 10) ?? "")
        : (o.orderDate?.substring(0, 7) ?? "");
      if (!key) return;
      if (!dataMap[key]) dataMap[key] = { revenue: 0, orders: 0 };
      dataMap[key].orders += 1;
    });

    const chart = Object.entries(dataMap)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-30)
      .map(([key, data]) => ({
        month: hasFilter ? key.substring(5) : key.replace("-", "/"),
        revenue: data.revenue,
        orders:  data.orders,
      }));
    setChartData(chart);
  };

  const handleFilter = () => {
    buildChartData(allOrders, fromDate, toDate);
  };

  const handleReset = () => {
    setFromDate("");
    setToDate("");
    buildChartData(allOrders);
  };

  const completedOrders = allOrders
    .filter(o => o.status === "completed")
    .sort((a, b) => new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime())
    .slice(0, 8);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full py-32 bg-gray-100">
        <svg className="animate-spin h-8 w-8 text-green-500" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
        </svg>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 bg-gray-100 min-h-screen">

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Tổng quan</h1>
        <p className="text-gray-500 text-sm mt-1">Chào mừng trở lại, Admin!</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: "Doanh thu",  value: fmt(stats?.totalRevenue ?? 0), icon: "💰", color: "text-green-600",  bg: "bg-green-50"  },
          { label: "Đơn hàng",  value: stats?.totalOrders ?? 0,        icon: "📦", color: "text-blue-600",   bg: "bg-blue-50"   },
          { label: "Sản phẩm",  value: stats?.totalProducts ?? 0,      icon: "🧃", color: "text-purple-600", bg: "bg-purple-50" },
          { label: "Khách hàng",value: stats?.totalUsers ?? 0,         icon: "👤", color: "text-orange-600", bg: "bg-orange-50" },
        ].map((card) => (
          <div key={card.label} className="bg-white rounded-xl p-5 border border-gray-200 hover:shadow-md transition-all">
            <div className="flex items-center justify-between mb-3">
              <div className={`w-10 h-10 ${card.bg} rounded-xl flex items-center justify-center text-xl`}>
                {card.icon}
              </div>
              {card.label === "Đơn hàng" && (stats?.pendingOrders ?? 0) > 0 && (
                <span className="bg-orange-100 text-orange-700 text-xs font-semibold px-2.5 py-1 rounded-full">
                  {stats?.pendingOrders} chờ
                </span>
              )}
            </div>
            <p className={`text-2xl font-bold ${card.color}`}>{card.value}</p>
            <p className="text-gray-500 text-sm mt-0.5">{card.label}</p>
          </div>
        ))}
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

        {/* Combo chart: Doanh thu (bar) + Đơn hàng (line) */}
        <div className="lg:col-span-2 bg-white rounded-xl p-5 border border-gray-200">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-700">Doanh thu & Tổng đơn hàng theo tháng</h2>
          </div>

          {/* Bộ lọc ngày */}
          <div className="flex items-center gap-2 mb-4 flex-wrap">
            <span className="text-sm text-gray-500">Từ ngày:</span>
            <input
              type="date"
              value={fromDate}
              onChange={e => setFromDate(e.target.value)}
              className="border border-gray-300 bg-white text-gray-700 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
            <span className="text-sm text-gray-500">Đến ngày:</span>
            <input
              type="date"
              value={toDate}
              onChange={e => setToDate(e.target.value)}
              className="border border-gray-300 bg-white text-gray-700 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
            <button
              onClick={handleFilter}
              className="px-4 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              Lọc
            </button>
            <button
              onClick={handleReset}
              className="px-4 py-1.5 bg-white text-gray-600 text-sm rounded-lg hover:bg-gray-100 transition-colors border border-gray-300"
            >
              Đặt lại
            </button>
          </div>

          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <ComposedChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                <XAxis dataKey="month" tick={{ fontSize: 12, fill: "#6b7280" }}/>
                <YAxis yAxisId="left"  tick={{ fontSize: 11, fill: "#6b7280" }} tickFormatter={fmtM}/>
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11, fill: "#6b7280" }}/>
                <Tooltip
                  contentStyle={{ backgroundColor: "#ffffff", border: "1px solid #e5e7eb", borderRadius: "8px", color: "#111827" }}
                  formatter={(value, name) =>
                    name === "revenue" ? [fmt(Number(value)), "Doanh thu"] : [value, "Số đơn"]
                  }
                />
                <Legend formatter={(v) => v === "revenue" ? "Doanh thu" : "Tổng đơn hàng"} wrapperStyle={{ color: "#6b7280" }}/>
                <Bar     yAxisId="left"  dataKey="revenue" fill="#93c5fd" radius={[4,4,0,0]}/>
                <Line    yAxisId="right" dataKey="orders"  stroke="#ef4444" strokeWidth={2} dot={{ r: 4 }} type="monotone"/>
              </ComposedChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
              Không có dữ liệu trong khoảng thời gian này
            </div>
          )}
        </div>

        {/* Pie chart: sản phẩm theo danh mục */}
        <div className="bg-white rounded-xl p-5 border border-gray-200">
          <h2 className="font-semibold text-gray-700 mb-4">Số lượng sản phẩm theo danh mục</h2>
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="45%"
                  outerRadius={80}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {pieData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]}/>
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: "#ffffff", border: "1px solid #e5e7eb", borderRadius: "8px", color: "#111827" }} formatter={(v, n) => [v, n]}/>
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-48 text-gray-400 text-sm">
              Chưa có dữ liệu
            </div>
          )}
        </div>
      </div>

      {/* Đơn hàng đã hoàn thành */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="p-5 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-semibold text-gray-700">Đơn hàng đã hoàn thành</h2>
          <a href="/dashboard/orders" className="text-blue-600 text-sm hover:underline font-medium">
            Xem tất cả →
          </a>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
              <tr>
                {["Mã đơn", "Khách hàng", "Tổng tiền", "Trạng thái", "Ngày mua"].map(h => (
                  <th key={h} className="px-5 py-3 text-left font-medium">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {recentOrders.map((order) => (
                <tr key={order.orderId} className="hover:bg-gray-50 transition-colors">
                  <td className="px-5 py-3 font-mono text-xs text-gray-500 font-medium">
                    #{order.orderId.slice(-6).toUpperCase()}
                  </td>
                  <td className="px-5 py-3 font-medium text-gray-800">{order.customerName}</td>
                  <td className="px-5 py-3 text-green-600 font-semibold">{fmt(order.totalAmount)}</td>
                  <td className="px-5 py-3">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${STATUS_COLOR[order.status] ?? ""}`}>
                      {STATUS_LABEL[order.status] ?? order.status}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-gray-500 text-xs">{order.orderDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {recentOrders.length === 0 && (
            <p className="text-center text-gray-400 py-8">Chưa có đơn hàng nào</p>
          )}
        </div>
      </div>

    </div>
  );
}