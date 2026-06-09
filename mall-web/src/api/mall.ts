import { http } from './http'
import type {
  CartItem,
  CategoryResponse,
  AdminIdResponse,
  LoginResponse,
  OrderCreateResponse,
  OrderDetail,
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
  UserResponse
} from '@/types/api'
import {
  prototypeAddCartItem,
  prototypeListAdminActivities,
  prototypeListAdminItems,
  prototypeListAdminOperationLogs,
  prototypeListAdminSessions,
  prototypeCancelOrder,
  prototypeCheckoutCart,
  prototypeClearCartItems,
  prototypeCreateOrder,
  prototypeGetMe,
  prototypeGetOrder,
  prototypeListOrderStatusLogs,
  prototypeListStockFlows,
  prototypeGetPayment,
  prototypeGetProduct,
  prototypeGetSeckillResult,
  prototypeInitSeckillStock,
  prototypeIssueSeckillToken,
  prototypeListSeckillItems,
  prototypeListSeckillSessions,
  prototypeListCartItems,
  prototypeListCategories,
  prototypeLogin,
  prototypePageProducts,
  prototypePayOrder,
  prototypeRegister,
  prototypeRemoveCartItem,
  prototypeSaveAdminActivity,
  prototypeSaveAdminItem,
  prototypeSaveAdminSession,
  prototypeSubmitSeckill,
  prototypeUpdateCartItem
} from '@/data/prototype'

export interface ProductQuery {
  pageNo?: number
  pageSize?: number
  categoryId?: number
  keyword?: string
}

function requestWithOptionalMock<T>(request: () => Promise<T>, mock: () => Promise<T>) {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    return mock()
  }
  return request()
}

export function login(payload: { username: string; password: string }) {
  return requestWithOptionalMock(() => http.post<LoginResponse, LoginResponse>('/auth/login', payload), () =>
    prototypeLogin(payload)
  )
}

export function register(payload: { username: string; password: string; phone?: string }) {
  return requestWithOptionalMock(() => http.post<UserResponse, UserResponse>('/users/register', payload), () =>
    prototypeRegister()
  )
}

export function getMe() {
  return requestWithOptionalMock(() => http.get<UserResponse, UserResponse>('/users/me'), () => prototypeGetMe())
}

export function listCategories() {
  return requestWithOptionalMock(() => http.get<CategoryResponse[], CategoryResponse[]>('/categories'), () =>
    prototypeListCategories()
  )
}

export function pageProducts(params: ProductQuery) {
  return requestWithOptionalMock(
    () => http.get<PageResult<ProductListItem>, PageResult<ProductListItem>>('/products', { params }),
    () => prototypePageProducts(params)
  )
}

export function getProduct(id: number) {
  return requestWithOptionalMock(() => http.get<ProductDetail, ProductDetail>(`/products/${id}`), () =>
    prototypeGetProduct(id)
  )
}

export function addCartItem(payload: { skuId: number; quantity: number }) {
  return requestWithOptionalMock(() => http.post<CartItem, CartItem>('/cart/items', payload), () =>
    prototypeAddCartItem(payload)
  )
}

export function listCartItems() {
  return requestWithOptionalMock(() => http.get<CartItem[], CartItem[]>('/cart/items'), () => prototypeListCartItems())
}

export function updateCartItem(skuId: number, payload: { quantity: number }) {
  return requestWithOptionalMock(
    () => http.put<CartItem, CartItem>(`/cart/items/${skuId}`, payload),
    () => prototypeUpdateCartItem(skuId, payload)
  )
}

export function removeCartItem(skuId: number) {
  return requestWithOptionalMock(() => http.delete<void, void>(`/cart/items/${skuId}`), () =>
    prototypeRemoveCartItem(skuId)
  )
}

export function clearCartItems() {
  return requestWithOptionalMock(() => http.delete<void, void>('/cart/items'), () => prototypeClearCartItems())
}

export function checkoutCart(payload: { remark?: string; requestId?: string }) {
  return requestWithOptionalMock(
    () => http.post<OrderCreateResponse, OrderCreateResponse>('/cart/checkout', payload),
    () => prototypeCheckoutCart(payload)
  )
}

export function createOrder(payload: { items: Array<{ skuId: number; quantity: number }>; remark?: string; requestId?: string }) {
  return requestWithOptionalMock(
    () => http.post<OrderCreateResponse, OrderCreateResponse>('/orders', payload),
    () => prototypeCreateOrder(payload)
  )
}

export function getOrder(orderNo: string) {
  return requestWithOptionalMock(() => http.get<OrderDetail, OrderDetail>(`/orders/${orderNo}`), () =>
    prototypeGetOrder(orderNo)
  )
}

export function listOrderStatusLogs(orderNo: string) {
  return requestWithOptionalMock(
    () => http.get<OrderStatusLogResponse[], OrderStatusLogResponse[]>(`/orders/admin/${orderNo}/status-logs`),
    () => prototypeListOrderStatusLogs(orderNo)
  )
}

export function listStockFlows(orderNo: string) {
  return requestWithOptionalMock(
    () => http.get<StockFlowResponse[], StockFlowResponse[]>('/inventory/admin/stock-flows', { params: { orderNo } }),
    () => prototypeListStockFlows(orderNo)
  )
}

export function cancelOrder(orderNo: string) {
  return requestWithOptionalMock(
    () =>
      http.post<{ orderNo: string; status: number }, { orderNo: string; status: number }>(
        `/orders/${orderNo}/cancel`
      ),
    () => prototypeCancelOrder(orderNo)
  )
}

export function payOrder(payload: { orderNo: string; payChannel: string }) {
  return requestWithOptionalMock(() => http.post<PaymentResponse, PaymentResponse>('/payments/pay', payload), () =>
    prototypePayOrder(payload)
  )
}

export function getPayment(payNo: string) {
  return requestWithOptionalMock(() => http.get<PaymentResponse, PaymentResponse>(`/payments/${payNo}`), () =>
    prototypeGetPayment(payNo)
  )
}

export function listSeckillSessions() {
  return requestWithOptionalMock(
    () => http.get<SeckillSession[], SeckillSession[]>('/promotions/seckill/sessions'),
    () => prototypeListSeckillSessions()
  )
}

export function listSeckillItems(sessionId: number) {
  return requestWithOptionalMock(
    () => http.get<SeckillItem[], SeckillItem[]>(`/promotions/seckill/sessions/${sessionId}/items`),
    () => prototypeListSeckillItems(sessionId)
  )
}

export function initSeckillStock(payload: { activityId: number; sessionId: number; skuId: number; quantity: number }) {
  return requestWithOptionalMock(() => http.post<void, void>('/orders/seckill/stocks', payload), () =>
    prototypeInitSeckillStock()
  )
}

export function issueSeckillToken(payload: { activityId: number; sessionId: number; skuId: number; quantity: number }) {
  return requestWithOptionalMock(
    () => http.post<SeckillTokenResponse, SeckillTokenResponse>('/orders/seckill/tokens', payload),
    () => prototypeIssueSeckillToken()
  )
}

export function submitSeckill(payload: {
  activityId: number
  sessionId: number
  skuId: number
  quantity: number
  token: string
  requestId?: string
}) {
  return requestWithOptionalMock(
    () => http.post<SeckillCreateResponse, SeckillCreateResponse>('/orders/seckill', payload),
    () => prototypeSubmitSeckill(payload)
  )
}

export function getSeckillResult(requestId: string) {
  return requestWithOptionalMock(
    () => http.get<SeckillCreateResponse, SeckillCreateResponse>(`/orders/seckill/${requestId}`),
    () => prototypeGetSeckillResult(requestId)
  )
}

export function listAdminActivities() {
  return requestWithOptionalMock(
    () => http.get<PromotionActivityAdmin[], PromotionActivityAdmin[]>('/promotions/admin/seckill/activities'),
    () => prototypeListAdminActivities()
  )
}

export function saveAdminActivity(payload: Partial<PromotionActivityAdmin>) {
  return requestWithOptionalMock(
    () => http.post<AdminIdResponse, AdminIdResponse>('/promotions/admin/seckill/activities', payload),
    () => prototypeSaveAdminActivity(payload)
  )
}

export function listAdminSessions(activityId: number) {
  return requestWithOptionalMock(
    () =>
      http.get<PromotionSessionAdmin[], PromotionSessionAdmin[]>(
        `/promotions/admin/seckill/activities/${activityId}/sessions`
      ),
    () => prototypeListAdminSessions(activityId)
  )
}

export function saveAdminSession(payload: Partial<PromotionSessionAdmin>) {
  return requestWithOptionalMock(
    () => http.post<AdminIdResponse, AdminIdResponse>('/promotions/admin/seckill/sessions', payload),
    () => prototypeSaveAdminSession(payload)
  )
}

export function listAdminItems(sessionId: number) {
  return requestWithOptionalMock(
    () =>
      http.get<PromotionSeckillSkuAdmin[], PromotionSeckillSkuAdmin[]>(
        `/promotions/admin/seckill/sessions/${sessionId}/items`
      ),
    () => prototypeListAdminItems(sessionId)
  )
}

export function saveAdminItem(payload: Partial<PromotionSeckillSkuAdmin>) {
  return requestWithOptionalMock(
    () => http.post<AdminIdResponse, AdminIdResponse>('/promotions/admin/seckill/items', payload),
    () => prototypeSaveAdminItem(payload)
  )
}

export function listAdminOperationLogs() {
  return requestWithOptionalMock(
    () => http.get<PromotionOperationLog[], PromotionOperationLog[]>('/promotions/admin/seckill/operation-logs'),
    () => prototypeListAdminOperationLogs()
  )
}
