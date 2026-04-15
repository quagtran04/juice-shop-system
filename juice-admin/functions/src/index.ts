import * as admin from "firebase-admin";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

admin.initializeApp();
const db = admin.firestore();

export const onOrderCreated = onDocumentCreated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;

    const detailsSnap = await db
      .collection("orders")
      .doc(orderId)
      .collection("orderDetails")
      .get();

    if (detailsSnap.empty) return;

    const batch = db.batch();

    detailsSnap.docs.forEach((doc) => {
      const detail = doc.data();
      const productRef = db.collection("products").doc(detail.productId);

      batch.update(productRef, {
        stock:     admin.firestore.FieldValue.increment(-detail.quantity),
        soldCount: admin.firestore.FieldValue.increment(detail.quantity),
      });
    });

    await batch.commit();
    console.log(`✅ Đã cập nhật stock cho đơn hàng ${orderId}`);
  }
);