export const formatMoney = (value) => {
  if (value === null || value === undefined) return '0.00';
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (!Number.isFinite(num)) return '0.00'; // 杜绝 ¥NaN / ¥undefined
  return num.toFixed(2);
};

// 账单/资产场景空值口径：缺字段显示 --.-- 而非 0.00（子页面规范 §4）
export const formatAmount = (value) => {
  if (value === null || value === undefined || value === '') return '--.--';
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (!Number.isFinite(num)) return '--.--';
  return num.toFixed(2);
};

export const formatDate = (dateStr, fmt = 'YYYY-MM-DD') => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  const minute = String(d.getMinutes()).padStart(2, '0');
  const second = String(d.getSeconds()).padStart(2, '0');
  return fmt
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hour)
    .replace('mm', minute)
    .replace('ss', second);
};

export const maskPhone = (phone) => {
  if (!phone || phone.length !== 11) return phone || '';
  return phone.substring(0, 3) + '****' + phone.substring(7);
};
