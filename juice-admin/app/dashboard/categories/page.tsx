"use client";

import { useEffect, useState, useRef } from "react";
import {
  collection, getDocs, doc,
  addDoc, updateDoc, deleteDoc, serverTimestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Category } from "@/types";
import Image from "next/image";
import toast from "react-hot-toast";

// Types

type FormData = Omit<Category, "categoryId" | "createdAt">;

const EMPTY_FORM: FormData = { name: "", imageUrl: "" };

// Component

export default function CategoriesPage() {
  const [categories,  setCategories]  = useState<Category[]>([]);
  const [loading,     setLoading]     = useState(true);
  const [showModal,   setShowModal]   = useState(false);
  const [editCat,     setEditCat]     = useState<Category | null>(null);
  const [form,        setForm]        = useState<FormData>(EMPTY_FORM);
  const [saving,      setSaving]      = useState(false);
  const [uploading,   setUploading]   = useState(false);
  const [preview,     setPreview]     = useState("");
  const [deleteId,    setDeleteId]    = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => { loadCategories(); }, []);

  const loadCategories = async () => {
    setLoading(true);
    try {
      const snap = await getDocs(collection(db, "categories"));
      setCategories(snap.docs.map(d => ({ ...d.data(), categoryId: d.id } as Category)));
    } catch {
      toast.error("Không thể tải danh mục");
    } finally {
      setLoading(false);
    }
  };

  // Modal

  const openAdd = () => {
    setEditCat(null);
    setForm(EMPTY_FORM);
    setPreview("");
    setShowModal(true);
  };

  const openEdit = (c: Category) => {
    setEditCat(c);
    setForm({ name: c.name, imageUrl: c.imageUrl });
    setPreview(c.imageUrl);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditCat(null);
    setPreview("");
  };

  // Upload ảnh

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
      setPreview(editCat?.imageUrl ?? "");
    } finally {
      setUploading(false);
    }
  };

  // Lưu 

  const handleSave = async () => {
    if (!form.name.trim())   return toast.error("Vui lòng nhập tên danh mục");
    if (!form.imageUrl)      return toast.error("Vui lòng upload ảnh");
    if (uploading)           return toast.error("Đang upload ảnh, vui lòng chờ...");

    setSaving(true);
    try {
      if (editCat) {
        await updateDoc(doc(db, "categories", editCat.categoryId), { ...form });
        setCategories(prev =>
          prev.map(c => c.categoryId === editCat.categoryId ? { ...c, ...form } : c)
        );
        toast.success("Cập nhật danh mục thành công!");
      } else {
        const ref = await addDoc(collection(db, "categories"), {
          ...form,
          createdAt: serverTimestamp(),
        });
        setCategories(prev => [...prev, {
          ...form,
          categoryId: ref.id,
          createdAt:  new Date().toISOString(),
        }]);
        toast.success("Thêm danh mục thành công!");
      }
      closeModal();
    } catch {
      toast.error("Lưu thất bại, thử lại!");
    } finally {
      setSaving(false);
    }
  };

  // Xóa 

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await deleteDoc(doc(db, "categories", deleteId));
      setCategories(prev => prev.filter(c => c.categoryId !== deleteId));
      toast.success("Xóa danh mục thành công!");
    } catch {
      toast.error("Xóa thất bại, thử lại!");
    } finally {
      setDeleteId(null);
    }
  };

  // Render 

  return (
    <div className="p-6 space-y-5">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Quản lý Danh mục</h1>
          <p className="text-gray-500 text-sm mt-1">{categories.length} danh mục</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 text-white text-sm font-medium rounded-lg transition-colors"
        >
          + Thêm danh mục
        </button>
      </div>

      {/* Grid */}
      {loading ? (
        <div className="flex items-center justify-center py-16">
          <svg className="animate-spin h-8 w-8 text-green-500" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
          </svg>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {categories.map(c => (
            <div
              key={c.categoryId}
              className="bg-white rounded-xl border shadow-sm overflow-hidden hover:shadow-md transition-shadow group"
            >
              {/* Ảnh */}
              <div className="relative h-32 bg-gray-100">
                {c.imageUrl ? (
                  <Image
                    src={c.imageUrl} alt={c.name}
                    fill className="object-cover"
                    sizes="200px"
                  />
                ) : (
                  <div className="flex items-center justify-center h-full text-3xl">📂</div>
                )}
              </div>

              {/* Info + actions */}
              <div className="p-3">
                <p className="font-semibold text-gray-800 text-sm truncate">{c.name}</p>
                <div className="flex gap-2 mt-2">
                  <button
                    onClick={() => openEdit(c)}
                    className="flex-1 text-xs py-1.5 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors font-medium"
                  >
                    Sửa
                  </button>
                  <button
                    onClick={() => setDeleteId(c.categoryId)}
                    className="flex-1 text-xs py-1.5 bg-red-50 text-red-500 rounded-lg hover:bg-red-100 transition-colors font-medium"
                  >
                    Xóa
                  </button>
                </div>
              </div>
            </div>
          ))}

          {categories.length === 0 && (
            <div className="col-span-full text-center py-12 text-gray-400">
              <p className="text-3xl mb-2">📂</p>
              <p>Chưa có danh mục nào</p>
            </div>
          )}
        </div>
      )}

      {/* ── Modal thêm / sửa ── */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl">

            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-lg font-bold text-gray-800">
                {editCat ? "Chỉnh sửa danh mục" : "Thêm danh mục mới"}
              </h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
            </div>

            <div className="p-6 space-y-4">

              {/* Upload ảnh */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh danh mục</label>
                <div
                  onClick={() => fileRef.current?.click()}
                  className="border-2 border-dashed border-gray-300 rounded-xl p-4 cursor-pointer hover:border-green-400 transition-colors flex flex-col items-center justify-center min-h-[120px] relative overflow-hidden"
                >
                  {preview ? (
                    <div className="relative w-full h-28">
                      <Image src={preview} alt="preview" fill className="object-contain rounded-lg" sizes="300px"/>
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
                    </>
                  )}
                </div>
                <input ref={fileRef} type="file" accept="image/*" onChange={handleImageChange} className="hidden"/>
              </div>

              {/* Tên */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tên danh mục <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
                  placeholder="VD: Nước ép trái cây"
                  className="w-full px-4 py-2.5 border rounded-lg text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-1">
                <button
                  onClick={closeModal}
                  className="flex-1 py-2.5 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50"
                >
                  Hủy
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving || uploading}
                  className="flex-1 py-2.5 bg-green-500 hover:bg-green-600 disabled:bg-green-300 text-white rounded-lg text-sm font-medium transition-colors"
                >
                  {saving ? "Đang lưu..." : editCat ? "Cập nhật" : "Thêm mới"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal xác nhận xóa */}
      {deleteId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm shadow-2xl p-6 text-center">
            <p className="text-4xl mb-4">🗑️</p>
            <h3 className="text-lg font-bold text-gray-800 mb-2">Xóa danh mục?</h3>
            <p className="text-gray-500 text-sm mb-6">
              Các sản phẩm trong danh mục này sẽ không còn danh mục. Hành động không thể hoàn tác.
            </p>
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