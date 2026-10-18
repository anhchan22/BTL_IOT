import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useQueryClient } from '@tanstack/react-query'
import { apiBaseUrl, getToken } from './api'

export function useRealtime() {
  const queryClient = useQueryClient()
  useEffect(() => {
    const token = getToken()
    if (!token) return
    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/sensor', () => {
          void queryClient.invalidateQueries({ queryKey: ['iot', 'latest'] })
          void queryClient.invalidateQueries({ queryKey: ['iot', 'chart'] })
        })
        client.subscribe('/topic/device-status', () => {
          void queryClient.invalidateQueries({ queryKey: ['iot', 'devices'] })
        })
        client.subscribe('/topic/action-log', () => {
          void queryClient.invalidateQueries({ queryKey: ['iot', 'history'] })
        })
      },
      onStompError: (frame) => console.error('STOMP error', frame.headers.message),
    })
    client.activate()
    return () => { void client.deactivate() }
  }, [queryClient])
}
