import type { SensorPoint } from './api'

export function mergeSensorChartPoints(
  history: SensorPoint[],
  live: SensorPoint[],
  minutes: number,
  now = Date.now()
): SensorPoint[] {
  const cutoff = now - minutes * 60_000
  const points = new Map<string, SensorPoint>()

  for (const point of [...history, ...live]) {
    const time = Date.parse(point.time)
    if (Number.isFinite(time) && time >= cutoff) points.set(point.time, point)
  }

  return [...points.values()].sort((a, b) => Date.parse(a.time) - Date.parse(b.time))
}
