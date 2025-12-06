import axios from 'axios'

const API_URL = 'http://localhost:8081'

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' }
})

// Attach JWT token to requests
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

// Handle 401 errors
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api

// Auth helpers
export const getToken = () => typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null
export const setToken = (token) => localStorage.setItem('accessToken', token)
export const getUsername = () => typeof window !== 'undefined' ? localStorage.getItem('username') : null
export const setUsername = (u) => localStorage.setItem('username', u)
export const logout = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('username')
}
export const isLoggedIn = () => !!getToken()

