import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useQueryClient } from '@tanstack/react-query'
import { apiBaseUrl, getToken, type LatestSensor, type SensorPoint } from './api'
import { mergeSensorChartPoints } from './sensor-chart-points'

type SensorSnapshot = SensorPoint & {
  statuses: { temperature: string; humidity: string; light: string }
}

export function useRealtime() {
  const queryClient = useQueryClient()
  useEffect(() => {
    const token = getToken()
    if (!token) return
    let hasConnected = false
    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        if (hasConnected) {
          void queryClient.invalidateQueries({ queryKey: ['iot', 'latest'] })
          void queryClient.invalidateQueries({ queryKey: ['iot', 'chart'] })
        }
        hasConnected = true
        client.subscribe('/topic/sensor', (message) => {
          let snapshot: SensorSnapshot
          try { snapshot = JSON.parse(message.body) as SensorSnapshot } catch { return }
          if (!snapshot || !Number.isFinite(snapshot.id) || !Number.isFinite(Date.parse(snapshot.time)) ||
              ![snapshot.temperature, snapshot.humidity, snapshot.light].every(Number.isFinite) ||
              !snapshot.statuses) return

          queryClient.setQueryData<LatestSensor>(['iot', 'latest'], previous => {
            if (previous?.lastUpdatedAt && Date.parse(previous.lastUpdatedAt) > Date.parse(snapshot.time)) return previous
            return {
              temperature: { value: snapshot.temperature, unit: previous?.temperature?.unit ?? '°C', status: snapshot.statuses.temperature, recordedAt: snapshot.time },
              humidity: { value: snapshot.humidity, unit: previous?.humidity?.unit ?? '%', status: snapshot.statuses.humidity, recordedAt: snapshot.time },
              light: { value: snapshot.light, unit: previous?.light?.unit ?? 'lux', status: snapshot.statuses.light, recordedAt: snapshot.time },
              lastUpdatedAt: snapshot.time,
            }
          })

          for (const [key] of queryClient.getQueriesData<SensorPoint[]>({ queryKey: ['iot', 'chart'] })) {
            const minutes = key[2]
            if (typeof minutes !== 'number') continue
            queryClient.setQueryData<SensorPoint[]>(key, previous =>
              mergeSensorChartPoints(previous ?? [], [snapshot], minutes))
          }
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
