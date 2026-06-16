import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''

export const apiClient = axios.create({
  baseURL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json, application/problem+json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data, headers } = error.response
      const problem = {
        status,
        title: data?.title ?? 'Error',
        detail: data?.detail ?? error.message,
        path: data?.path,
        timestamp: data?.timestamp,
        type: data?.type,
        errors: data?.errors,
        retryAfter: headers?.['retry-after'],
        raw: data,
      }
      return Promise.reject(problem)
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject({
        status: 0,
        title: 'Tiempo de espera agotado',
        detail: 'El servidor no respondió a tiempo.',
      })
    }
    return Promise.reject({
      status: 0,
      title: 'Error de red',
      detail: 'No se pudo conectar con el servidor.',
    })
  },
)

export const API_BASE_URL = baseURL
