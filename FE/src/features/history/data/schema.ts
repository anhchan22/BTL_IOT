export type DeviceName = 'LED1' | 'LED2' | 'LED3'
export type DeviceAction = 'ON' | 'OFF'
export type DeviceStatus = 'SUCCESS' | 'FAILED' | 'LOADING'
export type DeviceControlStatus = 'ON' | 'OFF' | 'LOADING'

export type HistoryRecord = {
  id: number
  device: DeviceName
  action: DeviceAction
  performedBy: string
  responseTimeSeconds: number | null
  status: DeviceStatus
  time: string
}

export type HistorySortField = keyof HistoryRecord
