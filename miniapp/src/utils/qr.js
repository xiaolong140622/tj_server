// /spread/code 的 qrBase64 实为 JPEG 而非 PNG（JAVA seq-202 冒烟结论）：
// 按 base64 魔数嗅探格式，生成 data URI 或落盘后缀，禁止写死 image/png。
export const sniffImageFormat = (b64) => {
  const s = String(b64 || '');
  if (s.startsWith('/9j/')) return { mime: 'image/jpeg', ext: 'jpg' };
  return { mime: 'image/png', ext: 'png' };
};

export const toImageUri = (b64) => {
  const s = String(b64 || '');
  if (s.startsWith('data:')) return s;
  return `data:${sniffImageFormat(s).mime};base64,${s}`;
};
