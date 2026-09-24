// JD 订单行归一化（C-3）：字段取自仓库已提交 MailvorJdOrderDto/BaseOrderDto（非 C-1 草案），
// 映射到订单页通用视图模型；B-2 补贴字段（草案）落档后在此扩展，不在本轮接。
export const normalizeJdOrder = (item) => ({
  ...item,
  title: item.title || item.skuName || '',
  pic: item.pic || item.mainPic || item.goodsInfo?.imageUrl || '',
  shopName: item.shopName || item.goodsInfo?.shopName || '',
  amount: item.amount ?? item.actualCosPrice ?? item.estimateCosPrice ?? item.price,
  payPrice: item.payPrice ?? item.actualCosPrice ?? item.estimateCosPrice ?? item.price,
  // 未结算时 actualFee=0，按契约注释「预估佣金以 estimateFee 为准」取实发优先、预估兜底
  commission: item.commission ?? (Number(item.actualFee) > 0 ? item.actualFee : item.estimateFee),
  orderTime: item.orderTime || item.createTime,
  settleTime: item.settleTime || (Number(item.validCode) === 17 ? item.finishTime : undefined),
  // validCode 16=已付款→待结算(1)；17=已完成→已结算(2)；其余(无效码/待付款)不映射
  status: item.status || (Number(item.validCode) === 16 ? 1 : Number(item.validCode) === 17 ? 2 : item.status),
});
