"use client";

import { useEffect, useState, useRef } from "react";
import {
  collection, getDocs, doc,
  addDoc, updateDoc, deleteDoc, serverTimestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Product, Category } from "@/types";
import Image from "next/image";
import toast from "react-hot-toast";

// Types

type FormData = Omit<Product, "productId" | "createdAt" | "soldCount">;

const EMPTY_FORM: FormData = {
  categoryId:  "",
  name:        "",
  price:       0,
  stock:       0,
  description: "",
  note:        "",
  imageUrl:    "",
};

const fmt = (n: number) => n.toLocaleString("vi-VN") + " đ";

// Component

export default function ProductsPage() {
  const [products,    setProducts]    = useState<Product[]>([]);
  const [categories,  setCategories]  = useState<Category[]>([]);
  const [loading,     setLoading]     = useState(true);
  const [search,      setSearch]      = useState("");
  const [filterCat,   setFilterCat]   = useState("all");
  const [showModal,   setShowModal]   = useState(false);
  const [editProduct, setEditProduct] = useState<Product | null>(null);
  const [form,        setForm]        = useState<FormData>(EMPTY_FORM);
  const [saving,      setSaving]      = useState(false);
  const [uploading,   setUploading]   = useState(false);
  const [preview,     setPreview]     = useState("");
  const [deleteId,    setDeleteId]    = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [prodSnap, catSnap] = await Promise.all([
        getDocs(collection(db, "products")),
        getDocs(collection(db, "categories")),
      ]);
      setProducts(prodSnap.docs.map(d => ({ ...d.data(), productId: d.id } as Product)));
      setCategories(catSnap.docs.map(d => ({ ...d.data(), categoryId: d.id } as Category)));
    } catch {
      toast.error("Không thể tải dữ liệu");
    } finally {
      setLoading(false);
    }
  };

  // Lọc sản phẩm

  const filtered = products.filter(p => {
    const matchCat    = filterCat === "all" || p.categoryId === filterCat;
    const matchSearch = search === "" || p.name.toLowerCase().includes(search.toLowerCase());
    return matchCat && matchSearch;
  });

  // Mở modal thêm / sửa

  const openAdd = () => {
    setEditProduct(null);
    setForm(EMPTY_FORM);
    setPreview("");
    setShowModal(true);
  };

  const openEdit = (p: Product) => {
    setEditProduct(p);
    setForm({
      categoryId:  p.categoryId,
      name:        p.name,
      price:       p.price,
      stock:       p.stock,
      description: p.description,
      note:        p.note,
      imageUrl:    p.imageUrl,
    });
    setPreview(p.imageUrl);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditProduct(null);
    setPreview("");
  };

  // Upload ảnh lên Cloudinary

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setPreview(URL.createObjectURL(file));
    setUploading(true);

    try {
      const fd = new FormData();
      fd.append("file", file);
      const res  = await fetch("/api/upload", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      setForm(prev => ({ ...prev, imageUrl: data.url }));
      toast.success("Upload ảnh thành công!");
    } catch (err: any) {
      toast.error("Upload ảnh thất bại: " + err.message);
      setPreview(editProduct?.imageUrl ?? "");
    } finally {
      setUploading(false);
    }
  };

  // Lưu sản phẩm

  const handleSave = async () => {
    if (!form.name.trim())                   return toast.error("Vui lòng nhập tên sản phẩm");
    if (!form.categoryId)                    return toast.error("Vui lòng chọn danh mục");
    if (form.price <= 0)                     return toast.error("Giá phải lớn hơn 0");
    if (!form.imageUrl && !editProduct)      return toast.error("Vui lòng upload ảnh");
    if (uploading)                           return toast.error("Đang upload ảnh, vui lòng chờ...");

    setSaving(true);
    try {
      if (editProduct) {
        await updateDoc(doc(db, "products", editProduct.productId), { ...form });
        setProducts(prev =>
          prev.map(p => p.productId === editProduct.productId ? { ...p, ...form } : p)
        );
        toast.success("Cập nhật sản phẩm thành công!");
      } else {
        const ref = await addDoc(collection(db, "products"), {
          ...form,
          soldCount: 0,                  
          createdAt: serverTimestamp(),
        });
        setProducts(prev => [...prev, {
          ...form,
          productId: ref.id,
          soldCount: 0,
          createdAt: new Date().toISOString(),
        }]);
        toast.success("Thêm sản phẩm thành công!");
      }
      closeModal();
    } catch {
      toast.error("Lưu thất bại, thử lại!");
    } finally {
      setSaving(false);
    }
  };

  // Xóa sản phẩm

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await deleteDoc(doc(db, "products", deleteId));
      setProducts(prev => prev.filter(p => p.productId !== deleteId));
      toast.success("Xóa sản phẩm thành công!");
    } catch {
      toast.error("Xóa thất bại, thử lại!");
    } finally {
      setDeleteId(null);
    }
  };

  const getCatName = (id: string) =>
    categories.find(c => c.categoryId === id)?.name ?? "—";

  // Render

  return (
    <div className="p-6 space-y-5">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Quản lý Sản phẩm</h1>
          <p className="text-gray-500 text-sm mt-1">{products.length} sản phẩm</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 text-white text-sm font-medium rounded-lg transition-colors"
        >
          + Thêm sản phẩm
        </button>
      </div>

      {/* Filter + Search */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
          <input
            type="text"
            placeholder="Tìm sản phẩm..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 bg-white"
          />
        </div>
        <select
          value={filterCat}
          onChange={e => setFilterCat(e.target.value)}
          className="px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 bg-white"
        >
          <option value="all">Tất cả danh mục</option>
          {categories.map(c => (
            <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
          ))}
        </select>
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
                  {["Ảnh", "Tên sản phẩm", "Danh mục", "Giá", "Tồn kho", "Đã bán", "Thao tác"].map(h => (
                    <th key={h} className="px-5 py-3.5 text-left font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.map(p => (
                  <tr key={p.productId} className="hover:bg-gray-50 transition-colors">

                    {/* Ảnh */}
                    <td className="px-5 py-3">
                      <div className="w-12 h-12 rounded-lg overflow-hidden bg-gray-100 relative">
                        {p.imageUrl ? (
                          <Image src={p.imageUrl} alt={p.name} fill className="object-cover" sizes="48px"/>
                        ) : (
                          <span className="flex items-center justify-center h-full text-xl">🧃</span>
                        )}
                      </div>
                    </td>

                    {/* Tên */}
                    <td className="px-5 py-3 font-medium text-gray-800 max-w-[200px]">
                      <p className="truncate">{p.name}</p>
                      {p.description && (
                        <p className="text-xs text-gray-400 truncate mt-0.5">{p.description}</p>
                      )}
                    </td>

                    <td className="px-5 py-3 text-gray-500">{getCatName(p.categoryId)}</td>
                    <td className="px-5 py-3 text-green-600 font-semibold">{fmt(p.price)}</td>

                    {/* Tồn kho */}
                    <td className="px-5 py-3">
                      <span className={`font-medium ${p.stock <= 5 ? "text-red-500" : "text-gray-700"}`}>
                        {p.stock}
                        {p.stock <= 5 && <span className="text-xs ml-1 text-red-400">(sắp hết)</span>}
                      </span>
                    </td>

                    {/* Đã bán */}
                    <td className="px-5 py-3">
                      <span className={`font-medium ${(p.soldCount ?? 0) > 0 ? "text-orange-500" : "text-gray-400"}`}>
                        {p.soldCount ?? 0}
                        {(p.soldCount ?? 0) >= 10 && (
                          <span className="ml-1 px-1.5 py-0.5 bg-orange-100 text-orange-600 text-xs rounded-full">🔥</span>
                        )}
                      </span>
                    </td>

                    {/* Thao tác */}
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <button
                          onClick={() => openEdit(p)}
                          className="text-blue-500 hover:text-blue-700 text-xs font-medium hover:underline"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => setDeleteId(p.productId)}
                          className="text-red-500 hover:text-red-700 text-xs font-medium hover:underline"
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filtered.length === 0 && !loading && (
              <div className="text-center py-12 text-gray-400">
                <p className="text-3xl mb-2">🧃</p>
                <p>Không có sản phẩm nào</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Modal thêm / sửa sản phẩm */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-xl shadow-2xl max-h-[90vh] overflow-y-auto">

            <div className="flex items-center justify-between p-6 border-b sticky top-0 bg-white z-10">
              <h2 className="text-lg font-bold text-gray-800">
                {editProduct ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm mới"}
              </h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
            </div>

            <div className="p-6 space-y-4">

              {/* Upload ảnh */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh sản phẩm</label>
                <div
                  onClick={() => fileRef.current?.click()}
                  className="border-2 border-dashed border-gray-300 rounded-xl p-4 cursor-pointer hover:border-green-400 transition-colors flex flex-col items-center justify-center min-h-[140px] relative overflow-hidden"
                >
                  {preview ? (
                    <div className="relative w-full h-32">
                      <Image src={preview} alt="preview" fill className="object-contain rounded-lg" sizes="400px"/>
                      {uploading && (
                        <div className="absolute inset-0 bg-white/80 flex items-center justify-center rounded-lg">
                          <svg className="animate-spin h-6 w-6 text-green-500" viewBox="0 0 24 24" fill="none">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                          </svg>
                        </div>
                      )}
                    </div>
                  ) : (
                    <>
                      <span className="text-3xl mb-2">📷</span>
                      <p className="text-sm text-gray-500">Click để chọn ảnh</p>
                      <p className="text-xs text-gray-400 mt-1">JPG, PNG — tối đa 5MB</p>
                    </>
                  )}
                </div>
                <input ref={fileRef} type="file" accept="image/*" onChange={handleImageChange} className="hidden"/>
              </div>

              {/* Tên sản phẩm */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tên sản phẩm <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
                  placeholder="Nhập tên sản phẩm"
                  className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Danh mục */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Danh mục <span className="text-red-500">*</span>
                </label>
                <select
                  value={form.categoryId}
                  onChange={e => setForm(p => ({ ...p, categoryId: e.target.value }))}
                  className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 bg-white"
                >
                  <option value="">-- Chọn danh mục --</option>
                  {categories.map(c => (
                    <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
                  ))}
                </select>
              </div>

              {/* Giá + Tồn kho */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Giá (VNĐ) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    value={form.price}
                    onChange={e => setForm(p => ({ ...p, price: Number(e.target.value) }))}
                    min={0}
                    className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Tồn kho</label>
                  <input
                    type="number"
                    value={form.stock}
                    onChange={e => setForm(p => ({ ...p, stock: Number(e.target.value) }))}
                    min={0}
                    className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
              </div>

              {/* Mô tả */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Mô tả</label>
                <textarea
                  value={form.description}
                  onChange={e => setForm(p => ({ ...p, description: e.target.value }))}
                  rows={3}
                  placeholder="Mô tả sản phẩm..."
                  className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500 resize-none"
                />
              </div>

              {/* Ghi chú */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ghi chú</label>
                <input
                  type="text"
                  value={form.note}
                  onChange={e => setForm(p => ({ ...p, note: e.target.value }))}
                  placeholder="Ghi chú thêm..."
                  className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-2">
                <button
                  onClick={closeModal}
                  className="flex-1 py-2.5 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
                >
                  Hủy
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving || uploading}
                  className="flex-1 py-2.5 bg-green-500 hover:bg-green-600 disabled:bg-green-300 text-white rounded-lg text-sm font-medium transition-colors"
                >
                  {saving ? "Đang lưu..." : editProduct ? "Cập nhật" : "Thêm mới"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/*Modal xác nhận xóa */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6 text-center">
            <p className="text-4xl mb-4">🗑️</p>
            <h3 className="text-lg font-bold text-gray-800 mb-2">Xóa sản phẩm?</h3>
            <p className="text-gray-500 text-sm mb-6">Hành động này không thể hoàn tác.</p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteId(null)}
                className="flex-1 py-2.5 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50"
              >
                Hủy
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-medium"
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