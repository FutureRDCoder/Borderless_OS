import type { ApiErrorBody } from './types'

export class ApiError extends Error {
  status: number
  code?: string
  requestId?: string
  validationErrors: Record<string, string>

  constructor(body: ApiErrorBody) {
    super(body.message || 'Request failed')
    this.status = body.status
    this.code = body.code
    this.requestId = body.requestId
    this.validationErrors = body.validationErrors ?? {}
  }
}

let csrfToken: string | null = null

async function fetchCsrfToken(): Promise<string> {
  const response = await fetch('/api/v1/auth/csrf', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Unable to initialise a secure session.')
  }

  const body: { token: string } = await response.json()
  csrfToken = body.token
  return body.token
}

export async function refreshCsrfToken(): Promise<string> {
  return fetchCsrfToken()
}

interface RequestOptions {
  method?: string
  body?: unknown
  headers?: Record<string, string>
}

export async function api<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const method = options.method ?? 'GET'
  const headers = new Headers()

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  Object.entries(options.headers ?? {}).forEach(([name, value]) => {
    headers.set(name, value)
  })

  if (method !== 'GET' && method !== 'HEAD') {
    const token = csrfToken ?? (await fetchCsrfToken())
    headers.set('X-XSRF-TOKEN', token)
  }

  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers,
    body: options.body === undefined
      ? undefined
      : JSON.stringify(options.body),
  })

  if (response.status === 204) {
    return undefined as T
  }

  if (!response.ok) {
    let body: ApiErrorBody = {
      status: response.status,
      message: response.statusText || 'Request failed',
    }

    try {
      body = await response.json()
    } catch {
      // Keep the response status as the fallback error.
    }

    if ((response.status === 401 || response.status === 403)
        && body.code === undefined) {
      csrfToken = null
    }

    throw new ApiError(body)
  }

  return response.json() as Promise<T>
}

export async function login(
  email: string,
  password: string,
): Promise<void> {
  await fetchCsrfToken()
  await api('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  await fetchCsrfToken()
}

export interface SignupInput {
  displayName: string
  email: string
  password: string
}

export async function signup(input: SignupInput): Promise<void> {
  await fetchCsrfToken()
  await api('/api/v1/auth/signup', {
    method: 'POST',
    body: input,
  })
  await fetchCsrfToken()
}

export interface EmployeeAccessRequestInput {
  fullName: string
  workEmail: string
  requestedRole: 'ADMIN' | 'COMPLIANCE' | 'OPERATIONS'
  note: string
}

export async function requestEmployeeAccess(
  input: EmployeeAccessRequestInput,
): Promise<void> {
  await fetchCsrfToken()
  await api('/api/v1/auth/employee-access-request', {
    method: 'POST',
    body: input,
  })
  await fetchCsrfToken()
}

export async function logout(): Promise<void> {
  await api('/api/v1/auth/logout', { method: 'POST' })
  csrfToken = null
}

export function currentUser(): Promise<{
  email: string
  roles: string[]
}> {
  return api('/api/v1/auth/me')
}
