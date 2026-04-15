import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";
import { doc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const {
      partnerCode, orderId, requestId, amount,
      orderInfo, orderType, transId, resultCode,
      message, payType, responseTime, extraData, signature,
    } = body;

    // Xác minh chữ ký từ MoMo 
    const secretKey = process.env.MOMO_SECRET_KEY!;
    const rawSignature =
      `accessKey=${process.env.MOMO_ACCESS_KEY}` +
      `&amount=${amount}` +
      `&extraData=${extraData}` +
      `&message=${message}` +
      `&orderId=${orderId}` +
      `&orderInfo=${orderInfo}` +
      `&orderType=${orderType}` +
      `&partnerCode=${partnerCode}` +
      `&payType=${payType}` +
      `&requestId=${requestId}` +
      `&responseTime=${responseTime}` +
      `&resultCode=${resultCode}` +
      `&transId=${transId}`;

    const expectedSignature = crypto
      .createHmac("sha256", secretKey)
      .update(rawSignature)
      .digest("hex");

    if (signature !== expectedSignature) {
      console.error("[MoMo Callback] Chữ ký không hợp lệ!");
      return NextResponse.json({ message: "Invalid signature" }, { status: 400 });
    }

    // Cập nhật Firebase
    if (resultCode === 0) {
      // Thanh toán thành công
      await updateDoc(doc(db, "orders", orderId), {
        status:        "confirmed",
        paymentMethod: "momo",
        paymentStatus: "paid",     
        momoTransId:   transId,
        paidAt:        new Date().toISOString(),
      });
      console.log(`[MoMo Callback] Đơn ${orderId} thanh toán thành công!`);
    } else {
      // Thanh toán thất bại / bị hủy
      await updateDoc(doc(db, "orders", orderId), {
        status:        "cancelled",
        paymentMethod: "momo",
        paymentStatus: "unpaid",     
        cancelReason:  message,
      });
      console.log(`[MoMo Callback] Đơn ${orderId} thất bại: ${message}`);
    }

    return NextResponse.json({ message: "OK" }, { status: 200 });

  } catch (err: any) {
    console.error("[MoMo Callback Error]", err);
    return NextResponse.json({ error: "Lỗi server" }, { status: 500 });
  }
}

// GET: MoMo redirect user về sau khi thanh toán
export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const resultCode = searchParams.get("resultCode");
  const orderId    = searchParams.get("orderId");

  if (resultCode === "0") {
    return NextResponse.redirect(
      new URL(`/payment/success?orderId=${orderId}`, req.url)
    );
  } else {
    return NextResponse.redirect(
      new URL(`/payment/failed?orderId=${orderId}`, req.url)
    );
  }
}