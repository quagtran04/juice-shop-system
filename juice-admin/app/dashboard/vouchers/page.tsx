"use client";

import { useEffect, useState } from "react";
import {
  collection, getDocs, addDoc, updateDoc, deleteDoc,
  doc, query, orderBy,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Voucher } from "@/types";
import toast from "react-hot-toast";

const EMPTY_FORM = {
  code:            "",
  discountPercent: 10,
  maxDiscount:     50000,
  expiryDate:      "",
  isActive:        true,
};

export default function VouchersPage() {
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [loading,  setLoading]  = useState(true);
  const [saving,   setSaving]   = useState(false);
  const [search,   setSearch]   = useState("");

  // Modal
  const [showModal, setShowModal]   = useState(false);
  const [editTarget, setEditTarget] = useState<Voucher | null>(null);
  const [form, setForm]             = useState(EMPTY_FORM);

  // Confirm xóa
  const [deleteTarget, setDeleteTarget] = useState<Voucher | null>(null);

  useEffect(() => { loadVouchers(); }, []);

  // Load
  const loadVouchers = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(
        query(collection(db, "vouchers"), orderBy("createdAt", "desc"))
      );
      setVouchers(snap.docs.map(d => ({ ...d.data(), voucherId: d.id } as Voucher)));
    } catch {
      toast.error("Không thể tải danh sách voucher");
    } finally {
      setLoading(false);
    }
  };

  // Lọc
  const filtered = vouchers.filter(v =>
    search === "" || v.code.toLowerCase().includes(search.toLowerCase())
  );

  // Mở modal thêm / sửa
  const openAdd = () => {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEdit = (v: Voucher) => {
    setEditTarget(v);
    setForm({
      code:            v.code,
      discountPercent: v.discountPercent,
      maxDiscount:     v.maxDiscount,
      expiryDate:      v.expiryDate,
      isActive:        v.isActive,
    });
    setShowModal(true);
  };

  // Validate 
  const validate = () => {
    if (!form.code.trim())          return "Vui lòng nhập mã voucher";
    if (form.discountPercent <= 0 || form.discountPercent > 100)
                                    return "% giảm phải từ 1 - 100";
    if (form.maxDiscount <= 0)      return "Mức giảm tối đa phải > 0";
    if (!form.expiryDate)           return "Vui lòng chọn ngày hết hạn";
    return null;
  };

  // Lưu (thêm hoặc sửa)
  const handleSave = async () => {
    const err = validate();
    if (err) { toast.error(err); return; }

    setSaving(true);
    try {
      const data = {
        code:            form.code.trim().toUpperCase(),
        discountPercent: Number(form.discountPercent),
        maxDiscount:     Number(form.maxDiscount),
        expiryDate:      form.expiryDate,
        isActive:        form.isActive,
      };

      if (editTarget) {
        // Cập nhật
        await updateDoc(doc(db, "vouchers", editTarget.voucherId), data);
        setVouchers(prev => prev.map(v =>
          v.voucherId === editTarget.voucherId
            ? { ...v, ...data }
            : v
        ));
        toast.success("Đã cập nhật voucher!");
      } else {
        // Thêm mới
        const now = new Date().toISOString().substring(0, 10);
        const ref = await addDoc(collection(db, "vouchers"), { ...data, createdAt: now });
        setVouchers(prev => [{ ...data, voucherId: ref.id, createdAt: now }, ...prev]);
        toast.success("Đã thêm voucher mới!");
      }
      setShowModal(false);
    } catch {
      toast.error("Lưu thất bại, thử lại!");
    } finally {
      setSaving(false);
    }
  };

  // Bật / tắt nhanh
  const toggleActive = async (v: Voucher) => {
    try {
      await updateDoc(doc(db, "vouchers", v.voucherId), { isActive: !v.isActive });
      setVouchers(prev => prev.map(x =>
        x.voucherId === v.voucherId ? { ...x, isActive: !x.isActive } : x
      ));
      toast.success(v.isActive ? "Đã tắt voucher" : "Đã bật voucher");
    } catch {
      toast.error("Cập nhật thất bại!");
    }
  };

  // Xóa
  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteDoc(doc(db, "vouchers", deleteTarget.voucherId));
      setVouchers(prev => prev.filter(v => v.voucherId !== deleteTarget.voucherId));
      toast.success("Đã xóa voucher!");
    } catch {
      toast.error("Xóa thất bại!");
    } finally {
      setDeleteTarget(null);
    }
  };

  // Helper
  const isExpired = (expiryDate: string) =>
    new Date(expiryDate) < new Date(new Date().toDateString());

  const statusBadge = (v: Voucher) => {
    if (!v.isActive)          return { label: "Tắt",       cls: "bg-gray-100 text-gray-500 border-gray-200" };
    if (isExpired(v.expiryDate)) return { label: "Hết hạn", cls: "bg-red-100 text-red-600 border-red-200" };
    return                           { label: "Hoạt động", cls: "bg-green-100 text-green-700 border-green-200" };
  };

  // Render
  return (
    <div className="p-6 space-y-5">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Quản lý Voucher</h1>
          <p className="text-gray-500 text-sm mt-1">{vouchers.length} voucher</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={loadVouchers}
            className="flex items-center gap-2 px-4 py-2 text-sm bg-white text-gray-800 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            🔄 Làm mới
          </button>
          <button
            onClick={openAdd}
            className="flex items-center gap-2 px-4 py-2 text-sm bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium"
          >
            ＋ Thêm voucher
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <input
          type="text"
          placeholder="Tìm theo mã voucher..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 bg-white"
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
                  {["Mã voucher", "Giảm", "Giảm tối đa", "Hết hạn", "Trạng thái", "Thao tác"].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(v => {
                  const badge = statusBadge(v);
                  return (
                    <tr key={v.voucherId} className="hover:bg-gray-50 transition-colors">

                      {/* Mã */}
                      <td className="px-5 py-3.5">
                        <span className="font-mono font-bold text-green-700 bg-green-50 px-2.5 py-1 rounded-lg text-sm">
                          {v.code}
                        </span>
                      </td>

                      {/* % giảm */}
                      <td className="px-5 py-3.5 font-medium text-gray-800">
                        {v.discountPercent}%
                      </td>

                      {/* Tối đa */}
                      <td className="px-5 py-3.5 text-gray-600">
                        {v.maxDiscount.toLocaleString("vi-VN")} đ
                      </td>

                      {/* Hết hạn */}
                      <td className="px-5 py-3.5 text-gray-600">
                        {v.expiryDate}
                      </td>

                      {/* Trạng thái */}
                      <td className="px-5 py-3.5">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${badge.cls}`}>
                          {badge.label}
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <button
                            onClick={() => openEdit(v)}
                            className="text-blue-500 hover:text-blue-700 text-xs font-medium hover:underline"
                          >
                            Sửa
                          </button>
                          <button
                            onClick={() => toggleActive(v)}
                            className={`text-xs font-medium hover:underline ${
                              v.isActive
                                ? "text-yellow-600 hover:text-yellow-800"
                                : "text-green-600 hover:text-green-800"
                            }`}
                          >
                            {v.isActive ? "Tắt" : "Bật"}
                          </button>
                          <button
                            onClick={() => setDeleteTarget(v)}
                            className="text-red-500 hover:text-red-700 text-xs font-medium hover:underline"
                          >
                            Xóa
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
                <p className="text-3xl mb-2">🎟️</p>
                <p>Không có voucher nào</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Modal thêm / sửa */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl">

            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-lg font-bold text-gray-800">
                {editTarget ? "Chỉnh sửa Voucher" : "Thêm Voucher mới"}
              </h2>
              <button
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
              >×</button>
            </div>

            <div className="p-6 space-y-4">

              {/* Mã voucher */}
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1.5">Mã voucher</label>
                <input
                  type="text"
                  value={form.code}
                  onChange={e => setForm(f => ({ ...f, code: e.target.value.toUpperCase() }))}
                  placeholder="VD: GIAM10"
                  className="w-full border rounded-lg px-3 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 font-mono uppercase"
                />
              </div>

              {/* % giảm + tối đa */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1.5">% Giảm giá</label>
                  <div className="relative">
                    <input
                      type="number"
                      min={1} max={100}
                      value={form.discountPercent}
                      onChange={e => setForm(f => ({ ...f, discountPercent: Number(e.target.value) }))}
                      className="w-full border rounded-lg px-3 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 pr-8"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">%</span>
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-500 mb-1.5">Giảm tối đa (đ)</label>
                  <input
                    type="number"
                    min={0}
                    value={form.maxDiscount}
                    onChange={e => setForm(f => ({ ...f, maxDiscount: Number(e.target.value) }))}
                    className="w-full border rounded-lg px-3 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
              </div>

              {/* Preview */}
              <div className="bg-green-50 rounded-lg px-4 py-3 text-sm text-green-700">
                🎟️ Giảm <strong>{form.discountPercent}%</strong>, tối đa <strong>{Number(form.maxDiscount).toLocaleString("vi-VN")} đ</strong>
              </div>

              {/* Ngày hết hạn */}
              <div>
                <label className="block text-xs font-medium text-gray-500 mb-1.5">Ngày hết hạn</label>
                <input
                  type="date"
                  value={form.expiryDate}
                  min={new Date().toISOString().substring(0, 10)}
                  onChange={e => setForm(f => ({ ...f, expiryDate: e.target.value }))}
                  className="w-full border rounded-lg px-3 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Trạng thái */}
              <div className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3">
                <span className="text-sm font-medium text-gray-700">Kích hoạt ngay</span>
                <button
                  onClick={() => setForm(f => ({ ...f, isActive: !f.isActive }))}
                  className={`relative w-11 h-6 rounded-full transition-colors ${
                    form.isActive ? "bg-green-500" : "bg-gray-300"
                  }`}
                >
                  <span className={`absolute top-1 left-0 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                    form.isActive ? "translate-x-6" : "translate-x-1"
                  }`}/>
                </button>
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-2">
                <button
                  onClick={() => setShowModal(false)}
                  className="flex-1 py-2.5 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
                >
                  Hủy
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving}
                  className="flex-1 py-2.5 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {saving ? "Đang lưu..." : editTarget ? "Cập nhật" : "Thêm mới"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal xác nhận xóa */}
      {deleteTarget && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6 space-y-4">
            <div className="text-center">
              <p className="text-3xl mb-2">🗑️</p>
              <h3 className="text-lg font-bold text-gray-800">Xóa voucher?</h3>
              <p className="text-sm text-gray-500 mt-1">
                Bạn chắc chắn muốn xóa voucher{" "}
                <span className="font-mono font-bold text-red-600">{deleteTarget.code}</span>?
                Hành động này không thể hoàn tác.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="flex-1 py-2.5 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50"
              >
                Hủy
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 py-2.5 bg-red-500 text-white rounded-lg text-sm font-medium hover:bg-red-600"
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}