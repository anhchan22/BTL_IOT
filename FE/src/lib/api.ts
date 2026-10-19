import axios from 'axios'

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
export const resolveAvatarUrl = (value: string | null | undefined) =>
  value?.startsWith('/') ? `${apiBaseUrl}${value}` : value || undefined
export const api = axios.create({ baseURL: apiBaseUrl, timeout: 15000 })
const TOKEN_KEY = 'iot_access_token'
const USER_KEY = 'iot_user_data'

export type UserData = {
  id: number; username: string; fullName: string; studentCode: string | null
  className: string | null; email: string | null; avatarUrl: string | null
  githubUrl: string | null; figmaUrl: string | null; postmanUrl: string | null; reportUrl: string | null
}
export type LoginResponse = { accessToken: string; tokenType: string; expiresIn: number; userData: UserData }
export type SensorValue = { value: number; unit: string; status: string; recordedAt: string }
export type LatestSensor = { temperature: SensorValue | null; humidity: SensorValue | null; light: SensorValue | null; lastUpdatedAt: string | null }
export type SensorPoint = { id: number; temperature: number; humidity: number; light: number; time: string }
export type SensorRecord = { id: number; sensorCode: string; sensorName: string; value: number; unit: string; status: string; recordedAt: string }
export type Device = { id: number; deviceCode: string; name: string; location: string | null; description: string | null; status: 'ON' | 'OFF' | 'LOADING'; lastConfirmedStatus: 'ON' | 'OFF'; lastChangedAt: string | null; online: boolean }
export type HistoryRecord = { id: number; device: string; action: 'ON' | 'OFF'; performedBy: string | null; responseTimeSeconds: number | null; status: 'SUCCESS' | 'FAILED' | 'LOADING'; time: string; errorMessage?: string | null }
export type Page<T> = { items: T[]; page: number; pageSize: number; totalItems: number; totalPages: number }

export const getToken = () => sessionStorage.getItem(TOKEN_KEY)
export const getStoredUser = (): UserData | null => {
  try { const user = sessionStorage.getItem(USER_KEY); return user ? JSON.parse(user) as UserData : null } catch { return null }
}
export function saveSession(login: LoginResponse) {
  sessionStorage.setItem(TOKEN_KEY, login.accessToken)
  sessionStorage.setItem(USER_KEY, JSON.stringify(login.userData))
}
export function saveUser(user: UserData) {
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new Event('iot-user-updated'))
}
export function clearSession() { sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(USER_KEY) }

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
api.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && getToken()) {
    clearSession()
    if (window.location.pathname !== '/login') window.location.assign('/login')
  }
  return Promise.reject(error)
})

type ApiResponse<T> = { success: boolean; code: string; message: string; data: T }
const unwrap = <T>(response: { data: ApiResponse<T> }) => response.data.data

export const endpoints = {
  login: async (username: string, password: string) => unwrap(await api.post<ApiResponse<LoginResponse>>('/api/auth/login', { username, password })),
  profile: async (username: string) => unwrap(await api.get<ApiResponse<UserData>>(`/api/auth/profile/${encodeURIComponent(username)}`)),
  updateProfile: async (id: number, body: Omit<UserData, 'id' | 'username'>) => unwrap(await api.put<ApiResponse<UserData>>(`/api/auth/profile/${id}`, body)),
  uploadAvatar: async (id: number, file: File) => {
    const body = new FormData()
    body.append('file', file)
    return unwrap(await api.post<ApiResponse<UserData>>(`/api/auth/profile/${id}/avatar`, body))
  },
  latest: async () => unwrap(await api.get<ApiResponse<LatestSensor>>('/api/sensor/latest')),
  chart: async (minutes = 60) => unwrap(await api.get<ApiResponse<SensorPoint[]>>('/api/sensor/chart', { params: { minutes } })),
  sensors: async (params: Record<string, string | number | undefined>) => unwrap(await api.get<ApiResponse<Page<SensorRecord>>>('/api/sensor', { params })),
  devices: async () => unwrap(await api.get<ApiResponse<Device[]>>('/api/devices')),
  mqttStatus: async () => unwrap(await api.get<ApiResponse<{ connected: boolean; checkedAt: string }>>('/api/devices/mqtt-status')),
  control: async (command: string) => unwrap(await api.post<ApiResponse<{ command: string; actions: HistoryRecord[] }>>('/api/devices/control', { command })),
  history: async (params: Record<string, string | number | undefined>) => unwrap(await api.get<ApiResponse<Page<HistoryRecord>>>('/api/history', { params })),
}
