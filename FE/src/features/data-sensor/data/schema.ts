export type SensorType = 'temperature' | 'humidity' | 'light'
export type SensorSortField = 'id' | 'type' | 'value' | 'unit' | 'time'

export type SensorRecord = {
  id: number
  type: SensorType
  value: number
  unit: '°C' | '%' | 'lux'
  time: string
}
