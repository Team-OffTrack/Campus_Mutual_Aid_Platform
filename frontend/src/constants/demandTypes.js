/**
 * Shared demand type definitions — single source of truth for labels, styles, and per-type config.
 * Used by DemandPublish, DemandDetail, DemandList, MyOrders, and Home.
 */
export const DEMAND_TYPES = {
  errand:     { value: 'errand', label: '跑腿代取', icon: 'logistics',   color: '#E65100', bg: '#FFF0E5' },
  trade:      { value: 'trade', label: '二手交易', icon: 'shop',        color: '#2E7D32', bg: '#E8F5E9' },
  team:       { value: 'team', label: '组队匹配', icon: 'friends',      color: '#1565C0', bg: '#E3F2FD' },
  lost_found: { value: 'lost_found', label: '失物招领', icon: 'search', color: '#7B1FA2', bg: '#F3E5F5' },
  study:      { value: 'study', label: '学习互助', icon: 'bookmark-o',  color: '#C62828', bg: '#FCE4EC' },
  other:      { value: 'other', label: '其他',     icon: 'ellipsis',    color: '#616161', bg: '#F5F5F5' }
}

export const TYPE_LABELS = Object.fromEntries(
  Object.entries(DEMAND_TYPES).map(([k, v]) => [k, v.label])
)

export const TYPE_STYLES = Object.fromEntries(
  Object.entries(DEMAND_TYPES).map(([k, v]) => [k, { background: v.bg, color: v.color }])
)

/**
 * Per-type UI configuration for forms and detail pages.
 * team is included for forward compatibility but not fully implemented yet.
 */
export const TYPE_CONFIG = {
  errand: {
    showReward: true, showLocation: true, locationRequired: true,
    rewardTypes: ['point', 'cash', 'donation'],
    rewardTypeDefault: 'point',
    rewardLabel: '报酬', locationLabel: '送达地点'
  },
  trade: {
    showReward: true, showLocation: true, locationRequired: false,
    rewardTypes: ['cash'],
    rewardTypeDefault: 'cash',
    rewardLabel: '价格', locationLabel: '交易地点'
  },
  team: {
    showReward: false, showLocation: false, locationRequired: false,
    rewardTypes: [],
    rewardTypeDefault: 'point',
    rewardLabel: '', locationLabel: ''
  },
  lost_found: {
    showReward: false, showLocation: true, locationRequired: true,
    rewardTypes: ['donation'],
    rewardTypeDefault: 'donation',
    rewardLabel: '感谢金（选填）', locationLabel: '地点'
  },
  study: {
    showReward: true, showLocation: true, locationRequired: false,
    rewardTypes: ['point', 'donation'],
    rewardTypeDefault: 'point',
    rewardLabel: '报酬', locationLabel: '地点（线下时填写）'
  },
  other: {
    showReward: true, showLocation: true, locationRequired: false,
    rewardTypes: ['point', 'cash', 'donation'],
    rewardTypeDefault: 'point',
    rewardLabel: '报酬', locationLabel: '地点'
  }
}
