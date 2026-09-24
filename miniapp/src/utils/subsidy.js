// B-2 补贴展示判定（冻结契约 shared/jd-channel-contract-v1.md §C-1，管理 seq-362 放行）：
// subsidyRate / subSideRate / commissionRate 均 Double 可 null；任一 null 即隐藏对应项，禁止以 0 填充。
// 比例单位为 %（上游数值原样展示）；订单 DTO 历史键 subsideRate 做兼容读取，契约口径以 subSideRate 为准。
const rateTag = (label, v) =>
  (v === null || v === undefined || v === '' ? null : { key: label, label: `${label} ${v}%` });

export const subsidyTags = (row) => {
  if (!row) return [];
  return [
    rateTag('联盟补贴', row.subsidyRate),
    rateTag('平台补贴', row.subSideRate ?? row.subsideRate),
    rateTag('佣金比例', row.commissionRate),
  ].filter(Boolean);
};
