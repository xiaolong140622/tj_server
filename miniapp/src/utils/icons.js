// 内联 SVG 线性图标（避免新增位图；emoji 仅作降级）
export const lineIcon = (stroke, inner) =>
  `url("data:image/svg+xml,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='${stroke}' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>${inner}</svg>`
  )}")`;

export const camIcon = lineIcon('#FF6B35',
  "<rect x='3' y='7.5' width='18' height='12.5' rx='2'/><circle cx='12' cy='13.8' r='3.2'/><path d='M8.6 7.5 10.1 5h3.8l1.5 2.5'/>");

export const copyIcon = lineIcon('rgba(255,255,255,0.95)',
  "<rect x='8' y='8' width='12' height='12' rx='2'/><path d='M5 16H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v1'/>");

export const ICONS = {
  order: (c) => lineIcon(c, "<path d='M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z'/><path d='M14 3v5h5'/><path d='M9 13h6M9 17h4'/>"),
  bill: (c) => lineIcon(c, "<path d='M4 6h2M4 12h2M4 18h2M10 6h10M10 12h10M10 18h7'/>"),
  spread: (c) => lineIcon(c, "<circle cx='6' cy='12' r='2.5'/><circle cx='18' cy='6' r='2.5'/><circle cx='18' cy='18' r='2.5'/><path d='M8.2 10.9l7.6-3.8M8.2 13.1l7.6 3.8'/>"),
  settings: (c) => lineIcon(c, "<path d='M4 6h4M14 6h6M4 12h8M18 12h2M4 18h2M12 18h8'/><circle cx='11' cy='6' r='2.3'/><circle cx='15.5' cy='12' r='2.3'/><circle cx='9' cy='18' r='2.3'/>"),
  user: (c) => lineIcon(c, "<circle cx='12' cy='8' r='4'/><path d='M4 21c.8-4.2 4-6.5 8-6.5s7.2 2.3 8 6.5'/>"),
  phone: (c) => lineIcon(c, "<rect x='7' y='2.5' width='10' height='19' rx='2.5'/><path d='M11 18.5h2'/>"),
  agreement: (c) => lineIcon(c, "<path d='M6 2h9l4 4v16H6z'/><path d='M15 2v4h4'/><path d='M9 12h6M9 16h4'/>"),
  privacy: (c) => lineIcon(c, "<path d='M12 2.5 4.5 5.5v6c0 4.6 3.1 8.4 7.5 9.9 4.4-1.5 7.5-5.3 7.5-9.9v-6z'/><path d='M9 12l2 2 4-4'/>"),
  refresh: (c) => lineIcon(c, "<path d='M20 11a8 8 0 1 0-2.3 6.3'/><path d='M20 4v7h-7'/>"),
  cache: (c) => lineIcon(c, "<ellipse cx='12' cy='5.5' rx='8' ry='3'/><path d='M4 5.5v13c0 1.7 3.6 3 8 3s8-1.3 8-3v-13'/><path d='M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3'/>"),
  about: (c) => lineIcon(c, "<circle cx='12' cy='12' r='9.5'/><path d='M12 11v5.5'/><circle cx='12' cy='7.8' r='0.4'/>"),
  logout: (c) => lineIcon(c, "<path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'/><path d='M16 17l5-5-5-5'/><path d='M21 12H9'/>"),
  friends: (c) => lineIcon(c, "<circle cx='9' cy='8' r='3.4'/><path d='M2.8 19c.7-3.4 3.2-5.3 6.2-5.3s5.5 1.9 6.2 5.3'/><circle cx='17.2' cy='9.2' r='2.6'/><path d='M16.2 14.1c2.6.2 4.5 1.9 5.1 4.4'/>"),
  invite: (c) => lineIcon(c, "<circle cx='6' cy='12' r='2.6'/><circle cx='18' cy='5.5' r='2.6'/><circle cx='18' cy='18.5' r='2.6'/><path d='M8.3 10.8l7.4-4M8.3 13.2l7.4 4'/>"),
};
