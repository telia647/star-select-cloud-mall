export function money(value: number | string | null | undefined) {
  const num = Number(value ?? 0)
  return num.toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  })
}

export function orderStatus(status: number) {
  const map: Record<number, string> = {
    10: '待支付',
    20: '已支付',
    30: '已取消'
  }
  return map[status] ?? `状态 ${status}`
}

export function paymentStatus(status: number) {
  return status === 1 ? '支付成功' : `状态 ${status}`
}

export function parseSpec(specJson: string | null | undefined) {
  if (!specJson) {
    return ''
  }
  try {
    const value = JSON.parse(specJson) as Record<string, unknown>
    return Object.entries(value)
      .map(([key, item]) => `${key}: ${String(item)}`)
      .join(' / ')
  } catch {
    return specJson
  }
}
