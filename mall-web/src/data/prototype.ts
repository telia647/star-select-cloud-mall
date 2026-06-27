import type {
  CartItem,
  CategoryResponse,
  AdminIdResponse,
  LoginResponse,
  MemberBenefit,
  MemberCoupon,
  OrderCreateResponse,
  OrderDetail,
  OrderListItem,
  OrderStatusLogResponse,
  PageResult,
  PaymentResponse,
  PromotionActivityAdmin,
  PromotionOperationLog,
  PromotionSeckillSkuAdmin,
  PromotionSessionAdmin,
  ProductDetail,
  ProductListItem,
  SeckillItem,
  SeckillCreateResponse,
  SeckillSession,
  SeckillTokenResponse,
  StockFlowResponse,
  SkuResponse,
  UserResponse
} from '@/types/api'

export interface PrototypeProductQuery {
  pageNo?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
}

export interface ProductPresentation {
  accent: string
  badge: string
  delivery: string
  highlights: string[]
  imageLabel: string
  marketTag: string
  priceFrom: number
  rating: string
  scene: string
  tone: string
}

const demoUser: UserResponse = {
  id: 10001,
  username: 'demo',
  phone: '13800000000',
  status: 1,
  roleCode: 'MEMBER'
}

const adminUser: UserResponse = {
  id: 10000,
  username: 'admin',
  phone: '13900000000',
  status: 1,
  roleCode: 'ADMIN'
}

let currentUser = demoUser

const categories: CategoryResponse[] = [
  { id: 1, parentId: 0, name: '精选数码', sort: 10 },
  { id: 2, parentId: 0, name: '居家生活', sort: 20 },
  { id: 3, parentId: 0, name: '运动户外', sort: 30 },
  { id: 4, parentId: 0, name: '美妆个护', sort: 40 },
  { id: 5, parentId: 0, name: '服饰箱包', sort: 50 }
]

const products: ProductDetail[] = [
  {
    id: 101,
    categoryId: 1,
    name: '曜声降噪头戴耳机',
    subtitle: '自适应降噪、40 小时续航，适合通勤和专注办公。',
    status: 1,
    skus: [
      buildSku(3001, 101, '耳机-黑色', { 颜色: '曜石黑', 版本: '标准版' }, 799),
      buildSku(3002, 101, '耳机-银色', { 颜色: '月光银', 版本: '旅行套装' }, 899)
    ]
  },
  {
    id: 102,
    categoryId: 2,
    name: '沐刻温控手冲壶',
    subtitle: '精准控温、细口稳定水流，让日常咖啡更接近专业出品。',
    status: 1,
    skus: [
      buildSku(3011, 102, '手冲壶-900ML', { 容量: '900ml', 颜色: '岩灰' }, 469),
      buildSku(3012, 102, '手冲壶-1.2L', { 容量: '1.2L', 颜色: '奶白' }, 529)
    ]
  },
  {
    id: 103,
    categoryId: 4,
    name: '光研玻色因精华',
    subtitle: '轻盈乳感质地，主打保湿、提亮和夜间修护。',
    status: 1,
    skus: [
      buildSku(3021, 103, '精华-30ML', { 规格: '30ml', 套组: '单瓶' }, 329),
      buildSku(3022, 103, '精华-60ML', { 规格: '60ml', 套组: '双瓶装' }, 579)
    ]
  },
  {
    id: 104,
    categoryId: 3,
    name: '跃行缓震跑鞋',
    subtitle: '轻量回弹中底，兼顾日常慢跑和城市通勤。',
    status: 1,
    skus: [
      buildSku(3031, 104, '跑鞋-42码', { 尺码: '42', 颜色: '雾蓝' }, 639),
      buildSku(3032, 104, '跑鞋-43码', { 尺码: '43', 颜色: '炭黑' }, 639)
    ]
  },
  {
    id: 105,
    categoryId: 2,
    name: '巢境香薰空气仪',
    subtitle: '微雾扩香、空气状态提醒，适合卧室和办公桌。',
    status: 1,
    skus: [
      buildSku(3041, 105, '香薰仪-暖白', { 颜色: '暖白', 香氛: '雪松' }, 299),
      buildSku(3042, 105, '香薰仪-薄荷绿', { 颜色: '薄荷绿', 香氛: '橙花' }, 319)
    ]
  },
  {
    id: 106,
    categoryId: 5,
    name: '城行轻量通勤包',
    subtitle: '分层收纳、可放 15 英寸电脑，适合城市移动办公。',
    status: 1,
    skus: [
      buildSku(3051, 106, '通勤包-18L', { 容量: '18L', 颜色: '深海蓝' }, 399),
      buildSku(3052, 106, '通勤包-24L', { 容量: '24L', 颜色: '石墨灰' }, 459)
    ]
  },
  {
    id: 107,
    categoryId: 1,
    name: '脉点迷你智能音箱',
    subtitle: '小体积大声场，支持多房间联动和语音助手。',
    status: 1,
    skus: [
      buildSku(3061, 107, '音箱-单只', { 颜色: '云白', 套餐: '单只' }, 269),
      buildSku(3062, 107, '音箱-双只', { 颜色: '云白', 套餐: '双只立体声' }, 499)
    ]
  },
  {
    id: 108,
    categoryId: 5,
    name: '柔层羊毛混纺围巾',
    subtitle: '柔软亲肤、轻暖不厚重，是冬季搭配的实用单品。',
    status: 1,
    skus: [
      buildSku(3071, 108, '围巾-驼色', { 颜色: '驼色', 尺寸: '180cm' }, 219),
      buildSku(3072, 108, '围巾-烟灰', { 颜色: '烟灰', 尺寸: '180cm' }, 219)
    ]
  }
]

const presentation: Record<number, Omit<ProductPresentation, 'priceFrom'>> = {
  101: {
    accent: '热卖',
    badge: '静享通勤',
    delivery: '次日达',
    highlights: ['混合降噪', '40h 续航', '低延迟模式'],
    imageLabel: '耳机',
    marketTag: '本周精选',
    rating: '4.9',
    scene: '数码装备',
    tone: 'tone-ink'
  },
  102: {
    accent: '新品',
    badge: '手冲友好',
    delivery: '48 小时发货',
    highlights: ['精准控温', '细口出水', '304 不锈钢'],
    imageLabel: '温控壶',
    marketTag: '咖啡角',
    rating: '4.8',
    scene: '居家生活',
    tone: 'tone-copper'
  },
  103: {
    accent: '口碑',
    badge: '夜间修护',
    delivery: '包邮',
    highlights: ['清爽乳感', '保湿提亮', '敏感肌友好'],
    imageLabel: '精华',
    marketTag: '护肤实验室',
    rating: '4.9',
    scene: '美妆个护',
    tone: 'tone-rose'
  },
  104: {
    accent: '限时',
    badge: '城市慢跑',
    delivery: '次日达',
    highlights: ['回弹中底', '轻量鞋面', '稳定支撑'],
    imageLabel: '跑鞋',
    marketTag: '运动热榜',
    rating: '4.7',
    scene: '运动户外',
    tone: 'tone-blue'
  },
  105: {
    accent: '治愈',
    badge: '桌面香氛',
    delivery: '今日发货',
    highlights: ['微雾扩香', '低噪运行', '氛围夜灯'],
    imageLabel: '空气仪',
    marketTag: '空间焕新',
    rating: '4.8',
    scene: '居家生活',
    tone: 'tone-mint'
  },
  106: {
    accent: '通勤',
    badge: '高效收纳',
    delivery: '包邮',
    highlights: ['电脑隔层', '防泼水', '轻量背负'],
    imageLabel: '背包',
    marketTag: '城市通勤',
    rating: '4.8',
    scene: '服饰箱包',
    tone: 'tone-slate'
  },
  107: {
    accent: '智能',
    badge: '小身材声场',
    delivery: '48 小时发货',
    highlights: ['语音助手', '多房间联动', '低音增强'],
    imageLabel: '音箱',
    marketTag: '智能家居',
    rating: '4.7',
    scene: '数码装备',
    tone: 'tone-violet'
  },
  108: {
    accent: '柔软',
    badge: '轻暖搭配',
    delivery: '包邮',
    highlights: ['羊毛混纺', '亲肤柔软', '百搭配色'],
    imageLabel: '围巾',
    marketTag: '冬日衣橱',
    rating: '4.8',
    scene: '服饰箱包',
    tone: 'tone-sand'
  }
}

const productImages: Record<number, { mainImage: string; galleryImages: string[] }> = {
  101: {
    mainImage: '/demo-products/headset.svg',
    galleryImages: ['/demo-products/headset.svg', '/demo-products/speaker.svg', '/demo-products/phone.svg']
  },
  102: {
    mainImage: '/demo-products/kettle.svg',
    galleryImages: ['/demo-products/kettle.svg', '/demo-products/aroma.svg', '/demo-products/cleanser.svg']
  },
  103: {
    mainImage: '/demo-products/serum.svg',
    galleryImages: ['/demo-products/serum.svg', '/demo-products/cleanser.svg', '/demo-products/aroma.svg']
  },
  104: {
    mainImage: '/demo-products/running-shoes.svg',
    galleryImages: ['/demo-products/running-shoes.svg', '/demo-products/backpack.svg', '/demo-products/scarf.svg']
  },
  105: {
    mainImage: '/demo-products/aroma.svg',
    galleryImages: ['/demo-products/aroma.svg', '/demo-products/kettle.svg', '/demo-products/serum.svg']
  },
  106: {
    mainImage: '/demo-products/backpack.svg',
    galleryImages: ['/demo-products/backpack.svg', '/demo-products/notebook.svg', '/demo-products/running-shoes.svg']
  },
  107: {
    mainImage: '/demo-products/speaker.svg',
    galleryImages: ['/demo-products/speaker.svg', '/demo-products/headset.svg', '/demo-products/phone.svg']
  },
  108: {
    mainImage: '/demo-products/scarf.svg',
    galleryImages: ['/demo-products/scarf.svg', '/demo-products/backpack.svg', '/demo-products/running-shoes.svg']
  }
}

export const showcaseStats = [
  { label: '精选 SKU', value: '120+' },
  { label: '平均发货', value: '18h' },
  { label: '用户好评', value: '98%' }
]

export const servicePromises = [
  { title: '正品保障', text: '商品来自自营和严选供应链' },
  { title: '极速履约', text: '核心城市最快次日送达' },
  { title: '无忧售后', text: '支持七天无理由和订单追踪' }
]

const cartItems: CartItem[] = []
const orders = new Map<string, OrderDetail>()
const payments = new Map<string, PaymentResponse>()
const seckillResults = new Map<string, SeckillCreateResponse>()
const adminActivities: PromotionActivityAdmin[] = [
  {
    id: 7001,
    name: '星选商城限时秒杀',
    title: '正在进行的秒杀',
    description: '用于本地秒杀验收的初始化活动。',
    status: 1
  }
]
const adminSessions: PromotionSessionAdmin[] = [
  {
    id: 7101,
    activityId: 7001,
    name: '当前场次',
    startTime: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    status: 1,
    sort: 1
  },
  {
    id: 7102,
    activityId: 7001,
    name: '下一场次',
    startTime: new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 5 * 60 * 60 * 1000).toISOString(),
    status: 1,
    sort: 2
  }
]
const adminItems: PromotionSeckillSkuAdmin[] = [
  {
    id: 7201,
    activityId: 7001,
    sessionId: 7101,
    skuId: 3001,
    productId: 2001,
    productName: '商城演示手机',
    skuCode: '手机-黑色-128G',
    subtitle: '当前场次限量库存，用于冒烟验收。',
    originalPrice: 1999,
    seckillPrice: 1599,
    totalStock: 120,
    availableStock: 36,
    limitPerUser: 1,
    badge: '进行中',
    sort: 1,
    status: 1
  },
  {
    id: 7202,
    activityId: 7001,
    sessionId: 7101,
    skuId: 3002,
    productId: 2001,
    productName: '商城演示手机',
    skuCode: '手机-白色-256G',
    subtitle: '大容量版本，享秒杀优惠价。',
    originalPrice: 2399,
    seckillPrice: 1899,
    totalStock: 80,
    availableStock: 18,
    limitPerUser: 1,
    badge: '限量',
    sort: 2,
    status: 1
  }
]
const adminOperationLogs: PromotionOperationLog[] = []
let orderSequence = 202606090001
let paymentSequence = 880001
let adminActivitySequence = 8000
let adminSessionSequence = 8100
let adminItemSequence = 8200
let adminOperationLogSequence = 8300

function buildSku(id: number, productId: number, skuCode: string, spec: Record<string, string>, price: number): SkuResponse {
  return {
    id,
    productId,
    skuCode,
    specJson: JSON.stringify(spec),
    price,
    status: 1
  }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function resolve<T>(value: T): Promise<T> {
  return Promise.resolve(clone(value))
}

function summaries(): ProductListItem[] {
  return products.map(({ id, categoryId, name, subtitle, status }) => ({
    id,
    categoryId,
    name,
    subtitle,
    mainImage: productImages[id]?.mainImage,
    status
  }))
}

function withProductImages(product: ProductDetail): ProductDetail {
  const images = productImages[product.id]
  if (!images) {
    return product
  }
  return {
    ...product,
    mainImage: images.mainImage,
    galleryImages: JSON.stringify(images.galleryImages)
  }
}

function findProductBySku(skuId: number) {
  for (const product of products) {
    const sku = product.skus.find((item) => item.id === skuId)
    if (sku) {
      return { product, sku }
    }
  }
  return null
}

function buildCartItem(product: ProductDetail, sku: SkuResponse, quantity: number): CartItem {
  return {
    skuId: sku.id,
    productId: product.id,
    productName: product.name,
    skuCode: sku.skuCode,
    specJson: sku.specJson,
    quantity,
    price: sku.price,
    totalAmount: Number((sku.price * quantity).toFixed(2))
  }
}

function nextOrderNo() {
  orderSequence += 1
  return `D${orderSequence}`
}

function nextPayNo() {
  paymentSequence += 1
  return `P${Date.now()}${paymentSequence}`
}

function createOrderFromItems(items: CartItem[], remark?: string | null): OrderDetail {
  const orderNo = nextOrderNo()
  const totalAmount = Number(items.reduce((sum, item) => sum + item.totalAmount, 0).toFixed(2))
  const order: OrderDetail = {
    orderNo,
    userId: demoUser.id,
    totalAmount,
    status: 10,
    payNo: null,
    payTime: null,
    cancelTime: null,
    remark: remark || null,
    items: clone(items)
  }
  orders.set(orderNo, order)
  return order
}

export function getProductPresentation(product: Pick<ProductListItem, 'id' | 'name'>, index = 0): ProductPresentation {
  const detail = products.find((item) => item.id === product.id)
  const minPrice = detail ? Math.min(...detail.skus.map((sku) => Number(sku.price))) : 199 + (index % 5) * 80
  const fallbackTones = ['tone-ink', 'tone-copper', 'tone-rose', 'tone-blue', 'tone-mint', 'tone-slate']
  const fallback: Omit<ProductPresentation, 'priceFrom'> = {
    accent: '精选',
    badge: '品质好物',
    delivery: '快速发货',
    highlights: ['严选品质', '灵活规格', '安心售后'],
    imageLabel: product.name.slice(0, 2),
    marketTag: '商城优选',
    rating: '4.8',
    scene: '品质生活',
    tone: fallbackTones[index % fallbackTones.length]
  }

  return {
    ...(presentation[product.id] || fallback),
    priceFrom: minPrice
  }
}

export function prototypeLogin(payload?: { username: string }): Promise<LoginResponse> {
  currentUser = payload?.username === 'admin' ? adminUser : demoUser
  return resolve({
    accessToken: 'prototype-access-token',
    refreshToken: 'prototype-refresh-token',
    expiresIn: 7200,
    tokenType: 'Bearer'
  })
}

export function prototypeRegister(): Promise<UserResponse> {
  return resolve(demoUser)
}

export function prototypeGetMe(): Promise<UserResponse> {
  return resolve(currentUser)
}

export function prototypeListMemberBenefits(): Promise<MemberBenefit[]> {
  return resolve([
    { code: 'COUPON', title: '会员专享券', description: '领取满减券并在下单时抵扣', sort: 1 },
    { code: 'SECKILL_PRIORITY', title: '秒杀提醒', description: '关注秒杀场次，提前进入抢购链路', sort: 2 },
    { code: 'SELF_OPERATED', title: '正品保障', description: '星选自营商品提供正品和售后保障', sort: 3 },
    { code: 'FREE_SHIPPING', title: '基础包邮', description: '自营商品默认免基础运费', sort: 4 }
  ])
}

export function prototypeListMemberCoupons(): Promise<MemberCoupon[]> {
  const now = new Date()
  const validTo = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
  return resolve([
    {
      id: 9101,
      couponName: '新人满 199 减 20',
      couponType: 'FULL_REDUCTION',
      discountAmount: 20,
      thresholdAmount: 199,
      status: 1,
      validFrom: now.toISOString(),
      validTo
    },
    {
      id: 9102,
      couponName: '会员满 399 减 50',
      couponType: 'FULL_REDUCTION',
      discountAmount: 50,
      thresholdAmount: 399,
      status: 1,
      validFrom: now.toISOString(),
      validTo
    },
    {
      id: 9103,
      couponName: '秒杀免运券',
      couponType: 'SHIPPING',
      discountAmount: 8,
      thresholdAmount: 0,
      status: 1,
      validFrom: now.toISOString(),
      validTo
    }
  ])
}

export function prototypeListCategories(): Promise<CategoryResponse[]> {
  return resolve(categories)
}

export function prototypePageProducts(params: PrototypeProductQuery): Promise<PageResult<ProductListItem>> {
  const pageNo = params.pageNo || 1
  const pageSize = params.pageSize || 8
  const keyword = params.keyword?.trim().toLowerCase()
  const records = summaries().filter((product) => {
    const matchesCategory = params.categoryId ? product.categoryId === params.categoryId : true
    const matchesKeyword = keyword
      ? `${product.name} ${product.subtitle}`.toLowerCase().includes(keyword)
      : true
    return matchesCategory && matchesKeyword
  })
  const start = (pageNo - 1) * pageSize

  return resolve({
    records: records.slice(start, start + pageSize),
    total: records.length,
    pageNo,
    pageSize
  })
}

export function prototypeGetProduct(id: number): Promise<ProductDetail> {
  const product = products.find((item) => item.id === id) || products[0]
  return resolve(withProductImages(product))
}

export function prototypeAddCartItem(payload: { skuId: number; quantity: number }): Promise<CartItem> {
  const resolved = findProductBySku(payload.skuId)
  if (!resolved) {
    return Promise.reject(new Error('演示商品不存在'))
  }

  const existing = cartItems.find((item) => item.skuId === payload.skuId)
  if (existing) {
    existing.quantity += payload.quantity
    existing.totalAmount = Number((existing.price * existing.quantity).toFixed(2))
    return resolve(existing)
  }

  const item = buildCartItem(resolved.product, resolved.sku, payload.quantity)
  cartItems.push(item)
  return resolve(item)
}

export function prototypeListCartItems(): Promise<CartItem[]> {
  return resolve(cartItems)
}

export function prototypeUpdateCartItem(skuId: number, payload: { quantity: number }): Promise<CartItem> {
  const item = cartItems.find((cartItem) => cartItem.skuId === skuId)
  if (!item) {
    return Promise.reject(new Error('购物车商品不存在'))
  }
  item.quantity = payload.quantity
  item.totalAmount = Number((item.price * item.quantity).toFixed(2))
  return resolve(item)
}

export function prototypeRemoveCartItem(skuId: number): Promise<void> {
  const index = cartItems.findIndex((item) => item.skuId === skuId)
  if (index >= 0) {
    cartItems.splice(index, 1)
  }
  return Promise.resolve()
}

export function prototypeClearCartItems(): Promise<void> {
  cartItems.splice(0, cartItems.length)
  return Promise.resolve()
}

export function prototypeCheckoutCart(payload: { remark?: string }): Promise<OrderCreateResponse> {
  if (cartItems.length === 0) {
    return Promise.reject(new Error('购物车为空'))
  }
  const order = createOrderFromItems(cartItems, payload.remark)
  cartItems.splice(0, cartItems.length)
  return resolve({
    orderNo: order.orderNo,
    totalAmount: order.totalAmount,
    status: order.status
  })
}

export function prototypeCreateOrder(payload: { items: Array<{ skuId: number; quantity: number }>; remark?: string }): Promise<OrderCreateResponse> {
  const orderItems = payload.items.map((item) => {
    const resolved = findProductBySku(item.skuId)
    if (!resolved) {
      throw new Error('演示商品不存在')
    }
    return buildCartItem(resolved.product, resolved.sku, item.quantity)
  })
  const order = createOrderFromItems(orderItems, payload.remark)
  return resolve({
    orderNo: order.orderNo,
    totalAmount: order.totalAmount,
    status: order.status
  })
}

export function prototypeGetOrder(orderNo: string): Promise<OrderDetail> {
  const order = orders.get(orderNo)
  if (!order) {
    return Promise.reject(new Error('演示订单不存在'))
  }
  return resolve(order)
}

export function prototypeListMyOrders(params: { pageNo?: number; pageSize?: number }): Promise<PageResult<OrderListItem>> {
  const pageNo = params.pageNo || 1
  const pageSize = params.pageSize || 10
  const records = Array.from(orders.values())
    .sort((a, b) => b.orderNo.localeCompare(a.orderNo))
    .map((order) => ({
      orderNo: order.orderNo,
      totalAmount: order.totalAmount,
      status: order.status,
      payNo: order.payNo,
      payTime: order.payTime,
      cancelTime: order.cancelTime,
      expireTime: order.expireTime,
      remark: order.remark,
      itemCount: order.items.reduce((sum, item) => sum + item.quantity, 0),
      firstProductName: order.items[0]?.productName || null,
      createdAt: new Date().toISOString()
    }))
  const start = (pageNo - 1) * pageSize
  return resolve({
    records: records.slice(start, start + pageSize),
    total: records.length,
    pageNo,
    pageSize
  })
}

export function prototypeListOrderStatusLogs(orderNo: string): Promise<OrderStatusLogResponse[]> {
  const order = orders.get(orderNo)
  if (!order) {
    return Promise.reject(new Error('演示订单不存在'))
  }
  const logs: OrderStatusLogResponse[] = [
    {
      orderNo,
      userId: order.userId,
      fromStatus: null,
      toStatus: 10,
      eventType: order.remark === '秒杀订单' ? '秒杀建单' : '创建订单',
      bizNo: orderNo,
      remark: order.remark || '订单已创建',
      createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString()
    }
  ]
  if (order.status === 20) {
    logs.push({
      orderNo,
      userId: order.userId,
      fromStatus: 10,
      toStatus: 20,
      eventType: '支付成功',
      bizNo: order.payNo,
      remark: '支付成功',
      createdAt: order.payTime || new Date().toISOString()
    })
  }
  if (order.status === 30) {
    logs.push({
      orderNo,
      userId: order.userId,
      fromStatus: 10,
      toStatus: 30,
      eventType: '用户取消',
      bizNo: null,
      remark: '用户取消订单',
      createdAt: order.cancelTime || new Date().toISOString()
    })
  }
  return resolve(logs)
}

export function prototypeListStockFlows(orderNo: string): Promise<StockFlowResponse[]> {
  const order = orders.get(orderNo)
  if (!order) {
    return Promise.reject(new Error('演示订单不存在'))
  }
  const skuId = order.items[0]?.skuId || 3001
  const quantity = order.items.reduce((sum, item) => sum + item.quantity, 0) || 1
  const flows: StockFlowResponse[] = [
    {
      orderNo,
      skuId,
      operation: 'LOCK',
      quantity,
      beforeAvailableStock: 100,
      afterAvailableStock: 100 - quantity,
      beforeLockedStock: 0,
      afterLockedStock: quantity,
      createdAt: new Date(Date.now() - 4 * 60 * 1000).toISOString()
    }
  ]
  if (order.status === 20) {
    flows.push({
      orderNo,
      skuId,
      operation: 'DEDUCT',
      quantity,
      beforeAvailableStock: 100 - quantity,
      afterAvailableStock: 100 - quantity,
      beforeLockedStock: quantity,
      afterLockedStock: 0,
      createdAt: order.payTime || new Date().toISOString()
    })
  }
  if (order.status === 30) {
    flows.push({
      orderNo,
      skuId,
      operation: 'RELEASE',
      quantity,
      beforeAvailableStock: 100 - quantity,
      afterAvailableStock: 100,
      beforeLockedStock: quantity,
      afterLockedStock: 0,
      createdAt: order.cancelTime || new Date().toISOString()
    })
  }
  return resolve(flows)
}

export function prototypeCancelOrder(orderNo: string): Promise<{ orderNo: string; status: number }> {
  const order = orders.get(orderNo)
  if (!order) {
    return Promise.reject(new Error('演示订单不存在'))
  }
  order.status = 30
  order.cancelTime = new Date().toISOString()
  return resolve({ orderNo, status: order.status })
}

export function prototypePayOrder(payload: { orderNo: string; payChannel: string }): Promise<PaymentResponse> {
  const order = orders.get(payload.orderNo)
  if (!order) {
    return Promise.reject(new Error('演示订单不存在'))
  }
  const payment: PaymentResponse = {
    payNo: nextPayNo(),
    orderNo: order.orderNo,
    userId: demoUser.id,
    amount: order.totalAmount,
    status: 1,
    payChannel: payload.payChannel,
    paidAt: new Date().toISOString()
  }
  order.status = 20
  order.payNo = payment.payNo
  order.payTime = payment.paidAt
  payments.set(payment.payNo, payment)
  return resolve(payment)
}

export function prototypeGetPayment(payNo: string): Promise<PaymentResponse> {
  const payment = payments.get(payNo)
  if (!payment) {
    return Promise.reject(new Error('演示支付单不存在'))
  }
  return resolve(payment)
}

export function prototypeListAdminActivities(): Promise<PromotionActivityAdmin[]> {
  return resolve(adminActivities)
}

export function prototypeSaveAdminActivity(payload: Partial<PromotionActivityAdmin>): Promise<AdminIdResponse> {
  const id = payload.id || ++adminActivitySequence
  const next: PromotionActivityAdmin = {
    id,
    name: payload.name || '新建秒杀活动',
    title: payload.title || payload.name || '新建秒杀活动',
    description: payload.description || null,
    status: payload.status ?? 1
  }
  upsertById(adminActivities, next)
  recordAdminOperation(payload.id ? 'UPDATE_ACTIVITY' : 'CREATE_ACTIVITY', 'PROMO_ACTIVITY', id, payload)
  return resolve({ id })
}

export function prototypeListAdminSessions(activityId: number): Promise<PromotionSessionAdmin[]> {
  return resolve(adminSessions.filter((item) => item.activityId === activityId))
}

export function prototypeSaveAdminSession(payload: Partial<PromotionSessionAdmin>): Promise<AdminIdResponse> {
  const id = payload.id || ++adminSessionSequence
  const next: PromotionSessionAdmin = {
    id,
    activityId: payload.activityId || 7001,
    name: payload.name || '新建场次',
    startTime: payload.startTime || new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    endTime: payload.endTime || new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString(),
    status: payload.status ?? 1,
    sort: payload.sort ?? adminSessions.length + 1
  }
  upsertById(adminSessions, next)
  recordAdminOperation(payload.id ? 'UPDATE_SESSION' : 'CREATE_SESSION', 'PROMO_SESSION', id, payload)
  return resolve({ id })
}

export function prototypeListAdminItems(sessionId: number): Promise<PromotionSeckillSkuAdmin[]> {
  return resolve(adminItems.filter((item) => item.sessionId === sessionId))
}

export function prototypeSaveAdminItem(payload: Partial<PromotionSeckillSkuAdmin>): Promise<AdminIdResponse> {
  const id = payload.id || ++adminItemSequence
  const next: PromotionSeckillSkuAdmin = {
    id,
    activityId: payload.activityId || 7001,
    sessionId: payload.sessionId || 7101,
    skuId: payload.skuId || 3001,
    productId: payload.productId || 2001,
    productName: payload.productName || '商城演示手机',
    skuCode: payload.skuCode || `SKU-${payload.skuId || 3001}`,
    subtitle: payload.subtitle || null,
    originalPrice: payload.originalPrice || 1999,
    seckillPrice: payload.seckillPrice || 1599,
    totalStock: payload.totalStock ?? 100,
    availableStock: payload.availableStock ?? 100,
    limitPerUser: payload.limitPerUser ?? 1,
    badge: payload.badge || null,
    sort: payload.sort ?? adminItems.length + 1,
    status: payload.status ?? 1
  }
  upsertById(adminItems, next)
  recordAdminOperation(payload.id ? 'UPDATE_SECKILL_SKU' : 'CREATE_SECKILL_SKU', 'PROMO_SECKILL_SKU', id, payload)
  return resolve({ id })
}

export function prototypeListAdminOperationLogs(): Promise<PromotionOperationLog[]> {
  return resolve(adminOperationLogs)
}

export function prototypeListSeckillSessions(): Promise<SeckillSession[]> {
  const now = new Date()
  const base = new Date(now)
  base.setMinutes(0, 0, 0)
  return resolve([
    buildSession(7101, '10:00 早场', addHours(base, -3), addHours(base, -1), now),
    buildSession(7102, '14:00 午场', addHours(base, -1), addHours(base, 1), now),
    buildSession(7103, '20:00 晚场', addHours(base, 3), addHours(base, 5), now)
  ])
}

export function prototypeListSeckillItems(sessionId: number): Promise<SeckillItem[]> {
  const items: Record<number, SeckillItem[]> = {
    7101: [
      buildSeckillItem(7201, 7101, 3001, '曜声降噪头戴耳机', '自适应降噪，早场限量开抢', 899, 699, 120, 0, '已抢光', 'ENDED'),
      buildSeckillItem(7202, 7101, 3041, '巢境香薰空气仪', '桌面香氛设备，限时直降', 329, 249, 80, 0, '售罄', 'ENDED')
    ],
    7102: [
      buildSeckillItem(7203, 7102, 3001, '曜声降噪头戴耳机', '午场爆款，抢完即止', 899, 649, 150, 46, '正在抢', 'RUNNING'),
      buildSeckillItem(7204, 7102, 3031, '跃行缓震跑鞋', '城市慢跑鞋，限量秒杀', 639, 489, 90, 22, '运动热卖', 'RUNNING')
    ],
    7103: [
      buildSeckillItem(7205, 7103, 3061, '脉点迷你智能音箱', '晚场预告，小身材大声场', 299, 219, 100, 100, '即将开始', 'UPCOMING'),
      buildSeckillItem(7206, 7103, 3051, '城行轻量通勤包', '通勤背包晚场补货', 459, 359, 70, 70, '预约提醒', 'UPCOMING')
    ]
  }
  return resolve(items[sessionId] || [])
}

export function prototypeInitSeckillStock(): Promise<void> {
  return Promise.resolve()
}

export function prototypeIssueSeckillToken(): Promise<SeckillTokenResponse> {
  return resolve({
    token: `mock-token-${Date.now()}`,
    expiresIn: 300
  })
}

export function prototypeSubmitSeckill(payload: {
  activityId: number
  sessionId: number
  skuId: number
  quantity: number
  token: string
  requestId?: string
}): Promise<SeckillCreateResponse> {
  const resolved = findProductBySku(payload.skuId)
  if (!resolved) {
    return Promise.reject(new Error('演示秒杀商品不存在'))
  }
  const order = createOrderFromItems([buildCartItem(resolved.product, resolved.sku, payload.quantity)], '秒杀订单')
  const requestId = payload.requestId || `REQ-${Date.now()}`
  const result: SeckillCreateResponse = {
    requestId,
    orderNo: order.orderNo,
    status: 'CREATED',
    message: '秒杀成功，系统已为你锁定库存并创建订单。'
  }
  seckillResults.set(requestId, result)
  return resolve(result)
}

export function prototypeGetSeckillResult(requestId: string): Promise<SeckillCreateResponse> {
  const result = seckillResults.get(requestId)
  if (!result) {
    return resolve({
      requestId,
      orderNo: null,
      status: 'ACCEPTED',
      message: '请求已接收，订单正在异步创建中。'
    })
  }
  return resolve(result)
}

function buildSession(id: number, name: string, startTime: Date, endTime: Date, now: Date): SeckillSession {
  return {
    id,
    activityId: 7001,
    name,
    startTime: startTime.toISOString(),
    endTime: endTime.toISOString(),
    status: 1,
    state: now < startTime ? 'UPCOMING' : now > endTime ? 'ENDED' : 'RUNNING'
  }
}

function addHours(date: Date, hours: number) {
  const value = new Date(date)
  value.setHours(value.getHours() + hours)
  return value
}

function upsertById<T extends { id: number }>(items: T[], next: T) {
  const index = items.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    items[index] = next
    return
  }
  items.push(next)
}

function recordAdminOperation(action: string, resourceType: string, resourceId: number, detail: unknown) {
  adminOperationLogSequence += 1
  adminOperationLogs.unshift({
    id: adminOperationLogSequence,
    operatorId: currentUser.id,
    operatorName: currentUser.username,
    roleCode: currentUser.roleCode || 'ADMIN',
    action,
    resourceType,
    resourceId,
    detail: JSON.stringify(detail),
    createdAt: new Date().toISOString()
  })
  adminOperationLogs.splice(30)
}

function buildSeckillItem(
  id: number,
  sessionId: number,
  skuId: number,
  productName: string,
  subtitle: string,
  originalPrice: number,
  seckillPrice: number,
  totalStock: number,
  availableStock: number,
  badge: string,
  state: SeckillItem['state']
): SeckillItem {
  return {
    id,
    activityId: 7001,
    sessionId,
    skuId,
    productId: 2001,
    productName,
    skuCode: `SKU-${skuId}`,
    subtitle,
    originalPrice,
    seckillPrice,
    totalStock,
    availableStock,
    soldPercent: totalStock <= 0 ? 100 : Math.floor(((totalStock - availableStock) * 100) / totalStock),
    limitPerUser: 1,
    badge,
    status: 1,
    state
  }
}
