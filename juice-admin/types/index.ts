// ── Khớp với Firestore collections của Android app ──────────────────────────

export interface Category {
  categoryId: string;
  name:       string;
  imageUrl:   string;
  createdAt:  string;
}

export interface Product {
  productId:   string;
  categoryId:  string;
  name:        string;
  price:       number;
  stock:       number;
  soldCount:   number;
  description: string;
  note:        string;
  imageUrl:    string;
  createdAt:   string;
}

export interface User {
  userId:    string;
  username:  string;
  email:     string;
  phone:     string;
  address:   string;
  role:      "user" | "admin";
  createdAt: string;
}

export interface OrderDetail {
  productId:   string;
  productName: string;
  quantity:    number;
  unitPrice:   number;
}

export interface Order {
  orderId:       string;
  userId:        string;
  customerName:  string;
  phone:         string;
  address:       string;
  totalAmount:   number;
  status:        "pending" | "confirmed" | "delivering" | "completed" | "cancelled";
  orderDate:     string;
  details:       OrderDetail[];
  paymentMethod: "cod" | "momo";   // ✅ thêm
  paymentStatus: "paid" | "unpaid"; // ✅ thêm
}

export interface Voucher {
  voucherId:       string;
  code:            string;
  discountPercent: number;
  maxDiscount:     number;
  expiryDate:      string;
  isActive:        boolean;
  createdAt:       string;
}

export interface DashboardStats {
  totalRevenue:   number;
  totalOrders:    number;
  totalProducts:  number;
  totalUsers:     number;
  pendingOrders:  number;
  revenueByMonth: { month: string; revenue: number }[];
}