/**
 * 内联 SVG 图标（避免额外图片资源与网络依赖）
 */
const svg = (content) => 'data:image/svg+xml,' + encodeURIComponent(content)

const wrap = (body, color) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`

const icons = {
  location: svg(wrap('<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>', '#333333')),
  search: svg(wrap('<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>', '#999999')),
  cart: svg(wrap('<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>', '#333333')),
  cartWhite: svg(wrap('<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>', '#ffffff')),
  chevron: svg(wrap('<path d="m9 18 6-6-6-6"/>', '#bbbbbb')),
  clock: svg(wrap('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>', '#999999')),
  star: svg('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#ff8a00"><path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z"/></svg>'),
  order: svg(wrap('<path d="M6 2h9l5 5v15H6z"/><path d="M15 2v5h5"/><path d="M9 12h7M9 17h5"/>', '#555555')),
  address: svg(wrap('<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>', '#555555')),
  service: svg(wrap('<path d="M4 12a8 8 0 0 1 16 0"/><rect x="2" y="12" width="4" height="7" rx="2"/><rect x="18" y="12" width="4" height="7" rx="2"/><path d="M20 19a4 4 0 0 1-4 3h-2"/>', '#555555')),
  coupon: svg(wrap('<path d="M3 8a2 2 0 0 0 0 4v4a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-4a2 2 0 0 1 0-4V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2z"/><path d="M13 6v2M13 11v2M13 16v2"/>', '#555555')),
  gift: svg(wrap('<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7"/><path d="M12 8S9 8 7.5 6.5A2.1 2.1 0 0 1 12 4a2.1 2.1 0 0 1 4.5 2.5C15 8 12 8 12 8z"/>', '#555555'))
}

module.exports = icons
