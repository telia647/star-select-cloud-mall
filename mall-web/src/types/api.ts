export interface ApiResult<T> {
  code: number
  message: string
  data: T
  success: boolean
  traceId?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface UserResponse {
  id: number
  username: string
  phone: string
  status: number
  roleCode?: string
}

export interface CategoryResponse {
  id: number
  parentId: number
  name: string
  sort: number
}

export interface ProductListItem {
  id: number
  categoryId: number
  shopId?: number | null
  shopName?: string | null
  name: string
  subtitle: string
  mainImage?: string | null
  status: number
}

export interface SkuResponse {
  id: number
  productId: number
  skuCode: string
  specJson: string
  price: number
  status: number
}

export interface ProductDetail {
  id: number
  categoryId: number
  shopId?: number | null
  shopName?: string | null
  name: string
  subtitle: string
  mainImage?: string | null
  galleryImages?: string | null
  status: number
  skus: SkuResponse[]
}

export interface CartItem {
  skuId: number
  productId: number
  productName: string
  skuCode: string
  specJson: string
  quantity: number
  price: number
  totalAmount: number
}

export interface OrderCreateResponse {
  orderNo: string
  totalAmount: number
  status: number
}

export interface OrderItem {
  skuId: number
  productId: number
  productName: string
  skuCode: string
  specJson: string
  quantity: number
  price: number
  totalAmount: number
}

export interface OrderDetail {
  orderNo: string
  userId: number
  totalAmount: number
  status: number
  payNo: string | null
  payTime: string | null
  cancelTime: string | null
  expireTime?: string | null
  remark: string | null
  items: OrderItem[]
}

export interface OrderListItem {
  orderNo: string
  totalAmount: number
  status: number
  payNo: string | null
  payTime: string | null
  cancelTime: string | null
  expireTime?: string | null
  remark: string | null
  itemCount: number
  firstProductName: string | null
  createdAt?: string
}

export interface MemberBenefit {
  code: string
  title: string
  description: string
  sort: number
}

export interface MemberCoupon {
  id: number
  couponName: string
  couponType: string
  discountAmount: number
  thresholdAmount: number
  status: number
  validFrom: string
  validTo: string
}

export interface OrderStatusLogResponse {
  orderNo: string
  userId: number
  fromStatus: number | null
  toStatus: number
  eventType: string
  bizNo: string | null
  remark: string | null
  createdAt?: string
}

export interface StockFlowResponse {
  orderNo: string
  skuId: number
  operation: string
  quantity: number
  beforeAvailableStock: number
  afterAvailableStock: number
  beforeLockedStock: number
  afterLockedStock: number
  createdAt?: string
}

export interface PaymentResponse {
  payNo: string
  orderNo: string
  userId: number
  amount: number
  status: number
  payChannel: string
  paidAt: string
}

export interface SeckillSession {
  id: number
  activityId: number
  name: string
  startTime: string
  endTime: string
  status: number
  state: 'UPCOMING' | 'RUNNING' | 'ENDED'
}

export interface SeckillItem {
  id: number
  activityId: number
  sessionId: number
  skuId: number
  productId: number
  productName: string
  skuCode: string
  subtitle: string
  originalPrice: number
  seckillPrice: number
  totalStock: number
  availableStock: number
  soldPercent: number
  limitPerUser: number
  badge: string
  status: number
  state: 'UPCOMING' | 'RUNNING' | 'ENDED'
}

export interface SeckillCreateResponse {
  requestId: string
  orderNo: string | null
  status: 'ACCEPTED' | 'CREATED' | 'FAILED'
  message: string
}

export interface SeckillTokenResponse {
  token: string
  expiresIn: number
}

export interface PromotionActivityAdmin {
  id: number
  name: string
  title: string
  description: string | null
  status: number
}

export interface PromotionSessionAdmin {
  id: number
  activityId: number
  name: string
  startTime: string
  endTime: string
  status: number
  sort: number
}

export interface PromotionSeckillSkuAdmin {
  id: number
  activityId: number
  sessionId: number
  skuId: number
  productId: number
  productName: string
  skuCode: string
  subtitle: string | null
  originalPrice: number
  seckillPrice: number
  totalStock: number
  availableStock: number
  limitPerUser: number
  badge: string | null
  sort: number
  status: number
}

export interface AdminIdResponse {
  id: number
}

export interface PromotionOperationLog {
  id: number
  operatorId: number
  operatorName: string
  roleCode: string
  action: string
  resourceType: string
  resourceId: number | null
  detail: string | null
  createdAt?: string
}

export interface AiReference {
  docId: number
  docTitle: string
  category: string
  content: string
  score?: number | null
}

export interface AiChatResponse {
  conversationId: string
  answer: string
  references: AiReference[]
  toolResult?: unknown
}

export interface AiConversation {
  id: string
  title: string
  updatedAt?: string
}

export interface AiMessage {
  id: string | number
  roleCode: 'USER' | 'ASSISTANT'
  content: string
  referencesJson?: string | null
  toolResultJson?: string | null
  createdAt?: string
}

export interface KnowledgeDoc {
  id: string
  title: string
  category: string
  content: string
  status: number
  embeddingStatus: number
  lastEmbeddingError?: string | null
  updatedAt?: string
}

export interface AiLog {
  id: number
  conversationId?: number | null
  userId?: number | null
  name: string
  status: number
  elapsedMs: number
  errorMessage?: string | null
  createdAt?: string
}
