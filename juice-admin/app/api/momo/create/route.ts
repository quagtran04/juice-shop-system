import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";

export async function POST(req: NextRequest) {
  try {
    const { orderId, amount, orderInfo } = await req.json();

    if (!orderId || !amount) {
      return NextResponse.json({ error: "Thiếu orderId hoặc amount" }, { status: 400 });
    }

    const partnerCode = process.env.MOMO_PARTNER_CODE!;
    const accessKey   = process.env.MOMO_ACCESS_KEY!;
    const secretKey   = process.env.MOMO_SECRET_KEY!;
    const appUrl      = process.env.NEXT_PUBLIC_APP_URL!;

    const requestId   = `${partnerCode}${Date.now()}`;
    const redirectUrl = `${appUrl}/api/momo/callback`;
    const ipnUrl      = `${appUrl}/api/momo/callback`;
    const requestType = "payWithMethod";
    const extraData   = "";
    const autoCapture = true;
    const lang        = "vi";

    // Tạo chữ ký HMAC SHA256
    const rawSignature =
      `accessKey=${accessKey}` +
      `&amount=${amount}` +
      `&extraData=${extraData}` +
      `&ipnUrl=${ipnUrl}` +
      `&orderId=${orderId}` +
      `&orderInfo=${orderInfo ?? "Thanh toan don hang JuiceShop"}` +
      `&partnerCode=${partnerCode}` +
      `&redirectUrl=${redirectUrl}` +
      `&requestId=${requestId}` +
      `&requestType=${requestType}`;

    const signature = crypto
      .createHmac("sha256", secretKey)
      .update(rawSignature)
      .digest("hex");

    const body = {
      partnerCode,
      accessKey,
      requestId,
      amount,
      orderId,
      orderInfo: orderInfo ?? "Thanh toan don hang JuiceShop",
      redirectUrl,
      ipnUrl,
      extraData,
      requestType,
      signature,
      lang,
      autoCapture,
    };

    const response = await fetch(process.env.MOMO_API_URL!, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await response.json();

    if (data.resultCode !== 0) {
      return NextResponse.json(
        { error: data.message ?? "MoMo từ chối tạo đơn" },
        { status: 400 }
      );
    }

    return NextResponse.json({ payUrl: data.payUrl, deeplink: data.deeplink });

  } catch (err: any) {
    console.error("[MoMo Create]", err);
    return NextResponse.json({ error: "Lỗi server" }, { status: 500 });
  }
}