import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api'
const USERNAME = __ENV.USERNAME || 'demo'
const PASSWORD = __ENV.PASSWORD || '123456'
const ACTIVITY_ID = Number(__ENV.ACTIVITY_ID || 7001)
const SESSION_ID = Number(__ENV.SESSION_ID || 7101)
const SKU_ID = Number(__ENV.SKU_ID || 3001)
const QUANTITY = Number(__ENV.QUANTITY || 1)
const POLL_ATTEMPTS = Number(__ENV.POLL_ATTEMPTS || 5)

export const options = {
  scenarios: {
    seckill_smoke: {
      executor: 'ramping-vus',
      stages: [
        { duration: '20s', target: Number(__ENV.VUS || 20) },
        { duration: __ENV.HOLD || '40s', target: Number(__ENV.VUS || 20) },
        { duration: '20s', target: 0 }
      ]
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
    seckill_submit_success_rate: ['rate>0.80']
  }
}

const submitLatency = new Trend('seckill_submit_latency')
const submitSuccessRate = new Rate('seckill_submit_success_rate')

export default function () {
  const accessToken = login()
  if (!accessToken) {
    submitSuccessRate.add(false)
    sleep(1)
    return
  }

  const token = issueSeckillToken(accessToken)
  if (!token) {
    submitSuccessRate.add(false)
    sleep(1)
    return
  }

  const requestId = `k6-${__VU}-${__ITER}-${Date.now()}`
  const started = Date.now()
  const submitResponse = http.post(
    `${BASE_URL}/orders/seckill`,
    JSON.stringify({
      activityId: ACTIVITY_ID,
      sessionId: SESSION_ID,
      skuId: SKU_ID,
      quantity: QUANTITY,
      token,
      requestId
    }),
    jsonHeaders(accessToken)
  )
  submitLatency.add(Date.now() - started)

  const accepted = check(submitResponse, {
    'submit accepted': (res) => res.status === 200 && resultSuccess(res)
  })
  submitSuccessRate.add(accepted)
  if (!accepted) {
    sleep(1)
    return
  }

  const body = submitResponse.json()
  const data = body.data || {}
  if (data.status === 'ACCEPTED') {
    pollResult(accessToken, data.requestId)
  }
  sleep(1)
}

function login() {
  const response = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    jsonHeaders()
  )
  const ok = check(response, {
    'login ok': (res) => res.status === 200 && resultSuccess(res) && Boolean(res.json().data?.accessToken)
  })
  return ok ? response.json().data.accessToken : null
}

function issueSeckillToken(accessToken) {
  const response = http.post(
    `${BASE_URL}/orders/seckill/tokens`,
    JSON.stringify({
      activityId: ACTIVITY_ID,
      sessionId: SESSION_ID,
      skuId: SKU_ID,
      quantity: QUANTITY
    }),
    jsonHeaders(accessToken)
  )
  const ok = check(response, {
    'token issued': (res) => res.status === 200 && resultSuccess(res) && Boolean(res.json().data?.token)
  })
  return ok ? response.json().data.token : null
}

function pollResult(accessToken, requestId) {
  for (let index = 0; index < POLL_ATTEMPTS; index += 1) {
    const response = http.get(`${BASE_URL}/orders/seckill/${requestId}`, jsonHeaders(accessToken))
    check(response, {
      'result readable': (res) => res.status === 200 && resultSuccess(res)
    })
    const status = response.json().data?.status
    if (status && status !== 'ACCEPTED') {
      return status
    }
    sleep(0.5)
  }
  return 'ACCEPTED'
}

function jsonHeaders(accessToken) {
  const headers = {
    'Content-Type': 'application/json'
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  return { headers }
}

function resultSuccess(response) {
  try {
    const body = response.json()
    return body && body.code === 0
  } catch (error) {
    return false
  }
}
