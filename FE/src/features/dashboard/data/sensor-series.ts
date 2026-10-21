export type SensorPoint = {
  id: number
  temperature: number
  humidity: number
  light: number
  rain: number
  time: string
}

const baseTime = new Date('2026-08-17T09:00:00+07:00')

export const sensorSeries: SensorPoint[] = Array.from({ length: 24 }).map(
  (_, index) => {
    const time = new Date(baseTime.getTime() + index * 5 * 60 * 1000)
    return {
      id: index + 1,
      temperature: Number((26.4 + Math.sin(index / 3) * 2.1 + index * 0.04).toFixed(1)),
      humidity: Math.round(58 + Math.cos(index / 5) * 8 + (index % 4)),
      light: Math.round(480 + Math.cos(index / 4) * 110 + index * 8),
      rain: Math.round(20 + Math.sin(index / 3) * 18 + (index % 5) * 7),
      time: time.toISOString(),
    }
  }
)
