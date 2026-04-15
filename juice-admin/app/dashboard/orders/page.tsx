"use client";

import { useEffect, useState } from "react";
import {
  collection, getDocs, doc, updateDoc, orderBy, query
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Order, OrderDetail } from "@/types";
import toast from "react-hot-toast";

const STATUS_STEPS = ["pending", "confirmed", "delivering", "completed"] as const;

const STATUS_LABEL: Record<string, string> = {
  pending:    "Chờ xác nhận",
  confirmed:  "Đã xác nhận",
  delivering: "Đang giao",
  completed:  "Hoàn thành",
  cancelled:  "Đã hủy",
};

const STATUS_COLOR: Record<string, string> = {
  pending:    "bg-orange-100 text-orange-700 border-orange-200",
  confirmed:  "bg-blue-100 text-blue-700 border-blue-200",
  delivering: "bg-purple-100 text-purple-700 border-purple-200",
  completed:  "bg-green-100 text-green-700 border-green-200",
  cancelled:  "bg-red-100 text-red-700 border-red-200",
};

const FILTER_TABS = [
  { key: "all",        label: "Tất cả"       },
  { key: "pending",    label: "Chờ xác nhận" },
  { key: "confirmed",  label: "Đã xác nhận"  },
  { key: "delivering", label: "Đang giao"    },
  { key: "completed",  label: "Hoàn thành"   },
  { key: "cancelled",  label: "Đã hủy"       },
];

const fmt = (n: number) => n.toLocaleString("vi-VN") + " đ";

// Badge trạng thái thanh toán
const PaymentBadge = ({ status, method }: { status?: string; method?: string }) => {
  if (status === "paid") {
    return (
      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 border border-green-200">
        ✅ Đã TT {method === "momo" ? "· MoMo" : ""}
      </span>
    );
  }
  return (
    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-700 border border-orange-200">
      💵 COD
    </span>
  );
};

export default function OrdersPage() {
  const [orders,        setOrders]        = useState<Order[]>([]);
  const [loading,       setLoading]       = useState(true);
  const [filterStatus,  setFilterStatus]  = useState("all");
  const [search,        setSearch]        = useState("");
  const [selected,      setSelected]      = useState<Order | null>(null);
  const [details,       setDetails]       = useState<OrderDetail[]>([]);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [updating,      setUpdating]      = useState(false);

  useEffect(() => { loadOrders(); }, []);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(query(collection(db, "orders"), orderBy("orderDate", "desc")));
      setOrders(snap.docs.map(d => ({ ...d.data(), orderId: d.id } as Order)));
    } catch {
      toast.error("Không thể tải danh sách đơn hàng");
    } finally {
      setLoading(false);
    }
  };

  const openDetail = async (order: Order) => {
    setSelected(order);
    setDetails([]);
    setLoadingDetail(true);
    try {
      const snap = await getDocs(collection(db, "orders", order.orderId, "orderDetails"));
      setDetails(snap.docs.map(d => d.data() as OrderDetail));
    } catch {
      toast.error("Không thể tải chi tiết đơn hàng");
    } finally {
      setLoadingDetail(false);
    }
  };

  const closeDetail = () => { setSelected(null); setDetails([]); };

  const filtered = orders.filter(o => {
    const matchStatus = filterStatus === "all" || o.status === filterStatus;
    const matchSearch = search === "" ||
      o.orderId.toLowerCase().includes(search.toLowerCase()) ||
      o.customerName?.toLowerCase().includes(search.toLowerCase()) ||
      o.phone?.includes(search);
    return matchStatus && matchSearch;
  });

  const updateStatus = async (orderId: string, newStatus: string) => {
    setUpdating(true);
    try {
      // Nếu completed + COD → tự động đổi paymentStatus thành paid
      const order = orders.find(o => o.orderId === orderId);
      const updateData: Record<string, string> = { status: newStatus };
      if (newStatus === "completed" && order?.paymentMethod !== "momo") {
        updateData.paymentStatus = "paid";
      }
      await updateDoc(doc(db, "orders", orderId), updateData);
      setOrders(prev => prev.map(o => o.orderId === orderId ? { ...o, status: newStatus as Order["status"] } : o));
      if (selected?.orderId === orderId) {
        setSelected(prev => prev ? { ...prev, status: newStatus as Order["status"] } : null);
      }
      toast.success("Cập nhật trạng thái thành công!");
    } catch {
      toast.error("Cập nhật thất bại, thử lại!");
    } finally {
      setUpdating(false);
    }
  };

  const nextStatus = (current: string) => {
    const idx = STATUS_STEPS.indexOf(current as any);
    if (idx === -1 || idx === STATUS_STEPS.length - 1) return null;
    return STATUS_STEPS[idx + 1];
  };

  const countByStatus = (status: string) =>
    status === "all" ? orders.length : orders.filter(o => o.status === status).length;

  return (
    <div className="p-6 space-y-5">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Quản lý Đơn hàng</h1>
          <p className="text-gray-500 text-sm mt-1">{orders.length} đơn hàng tổng cộng</p>
        </div>
        <button onClick={loadOrders} className="flex items-center gap-2 px-4 py-2 text-sm text-gray-800 bg-white border rounded-lg hover:bg-gray-50 transition-colors">
          🔄 Làm mới
        </button>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {FILTER_TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => setFilterStatus(tab.key)}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors ${
              filterStatus === tab.key ? "bg-green-500 text-white" : "bg-white text-gray-600 border hover:bg-gray-50"
            }`}
          >
            {tab.label}
            <span className={`text-xs px-1.5 py-0.5 rounded-full ${
              filterStatus === tab.key ? "bg-green-400 text-white" : "bg-gray-100 text-gray-500"
            }`}>
              {countByStatus(tab.key)}
            </span>
          </button>
        ))}
      </div>

      {/* Search */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <input
          type="text"
          placeholder="Tìm theo mã đơn, tên khách, số điện thoại..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 text-gray-800 focus:ring-green-500 bg-white"
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
                  {["Mã đơn", "Khách hàng", "SĐT", "Tổng tiền", "Trạng thái", "Thanh toán", "Ngày đặt", "Thao tác"].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(order => (
                  <tr key={order.orderId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-5 py-3.5 font-mono text-xs text-gray-600 font-medium">
                      #{order.orderId.slice(-6).toUpperCase()}
                    </td>
                    <td className="px-5 py-3.5 font-medium text-gray-800">{order.customerName}</td>
                    <td className="px-5 py-3.5 text-gray-500">{order.phone}</td>
                    <td className="px-5 py-3.5 text-green-600 font-semibold">{fmt(order.totalAmount)}</td>
                    <td className="px-5 py-3.5">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${STATUS_COLOR[order.status] ?? ""}`}>
                        {STATUS_LABEL[order.status] ?? order.status}
                      </span>
                    </td>
                    {/*Cột thanh toán mới */}
                    <td className="px-5 py-3.5">
                      <PaymentBadge status={order.paymentStatus} method={order.paymentMethod} />
                    </td>
                    <td className="px-5 py-3.5 text-gray-500 text-xs">{order.orderDate}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <button onClick={() => openDetail(order)} className="text-blue-500 hover:text-blue-700 text-xs font-medium hover:underline">
                          Chi tiết
                        </button>
                        {nextStatus(order.status) && (
                          <button
                            onClick={() => updateStatus(order.orderId, nextStatus(order.status)!)}
                            disabled={updating}
                            className="text-xs bg-green-50 text-green-700 border border-green-200 px-2.5 py-1 rounded-lg hover:bg-green-100 transition-colors disabled:opacity-50"
                          >
                            → {STATUS_LABEL[nextStatus(order.status)!]}
                          </button>
                        )}
                        {(order.status === "pending" || order.status === "confirmed") && (
                          <button
                            onClick={() => updateStatus(order.orderId, "cancelled")}
                            disabled={updating}
                            className="text-xs text-red-500 hover:text-red-700 hover:underline disabled:opacity-50"
                          >
                            Hủy
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filtered.length === 0 && (
              <div className="text-center py-12 text-gray-400">
                <p className="text-3xl mb-2">📭</p>
                <p>Không có đơn hàng nào</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Modal chi tiết */}
      {selected && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-6 border-b">
              <div>
                <h2 className="text-lg font-bold text-gray-800">
                  Chi tiết đơn #{selected.orderId.slice(-6).toUpperCase()}
                </h2>
                <p className="text-sm text-gray-500 mt-0.5">{selected.orderDate}</p>
              </div>
              <button onClick={closeDetail} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
            </div>

            <div className="p-6 space-y-5">
              {/* Thông tin khách */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <h3 className="font-semibold text-gray-700 text-sm mb-3">👤 Thông tin khách hàng</h3>
                {[
                  { label: "Tên",     value: selected.customerName },
                  { label: "SĐT",     value: selected.phone        },
                  { label: "Địa chỉ", value: selected.address      },
                ].map(row => (
                  <div key={row.label} className="flex text-sm">
                    <span className="text-gray-500 w-20 shrink-0">{row.label}:</span>
                    <span className="text-gray-800 font-medium">{row.value}</span>
                  </div>
                ))}
              </div>

              {/* Trạng thái + Thanh toán */}
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600 font-medium">Trạng thái đơn:</span>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${STATUS_COLOR[selected.status]}`}>
                  {STATUS_LABEL[selected.status]}
                </span>
              </div>
              {/*Trạng thái thanh toán trong modal */}
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600 font-medium">Thanh toán:</span>
                <PaymentBadge status={selected.paymentStatus} method={selected.paymentMethod} />
              </div>

              {/* Danh sách sản phẩm */}
              <div>
                <h3 className="font-semibold text-gray-700 text-sm mb-3">🧃 Sản phẩm</h3>
                {loadingDetail ? (
                  <div className="flex justify-center py-6">
                    <svg className="animate-spin h-6 w-6 text-green-500" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                    </svg>
                  </div>
                ) : details.length === 0 ? (
                  <p className="text-sm text-gray-400 text-center py-4">Không có sản phẩm</p>
                ) : (
                  <div className="space-y-2">
                    {details.map((item, i) => (
                      <div key={i} className="flex items-center justify-between text-sm bg-gray-50 rounded-lg px-4 py-3">
                        <div>
                          <p className="font-medium text-gray-800">{item.productName}</p>
                          <p className="text-gray-500 text-xs mt-0.5">{fmt(item.unitPrice)} × {item.quantity}</p>
                        </div>
                        <p className="font-semibold text-green-600">{fmt(item.unitPrice * item.quantity)}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Tổng tiền */}
              <div className="flex items-center justify-between border-t pt-4">
                <span className="font-semibold text-gray-700">Tổng thanh toán:</span>
                <span className="text-xl font-bold text-green-600">{fmt(selected.totalAmount)}</span>
              </div>

              {/* Nút đổi trạng thái */}
              <div className="flex gap-3 pt-2">
                {nextStatus(selected.status) && (
                  <button
                    onClick={() => updateStatus(selected.orderId, nextStatus(selected.status)!)}
                    disabled={updating}
                    className="flex-1 bg-green-500 hover:bg-green-600 text-white py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    {updating ? "Đang cập nhật..." : `→ ${STATUS_LABEL[nextStatus(selected.status)!]}`}
                  </button>
                )}
                {(selected.status === "pending" || selected.status === "confirmed") && (
                  <button
                    onClick={() => updateStatus(selected.orderId, "cancelled")}
                    disabled={updating}
                    className="flex-1 bg-red-50 hover:bg-red-100 text-red-600 py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 border border-red-200"
                  >
                    Hủy đơn
                  </button>
                )}
                {selected.status === "completed" && (
                  <div className="flex-1 text-center text-green-600 text-sm font-medium py-2.5">✅ Đơn hàng đã hoàn thành</div>
                )}
                {selected.status === "cancelled" && (
                  <div className="flex-1 text-center text-red-500 text-sm font-medium py-2.5">❌ Đơn hàng đã bị hủy</div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}