import { historyRecords as initialHistoryRecords } from './history'
import {
  type DeviceAction,
  type DeviceControlStatus,
  type DeviceName,
  type HistoryRecord,
} from './schema'

const storageKey = 'classroom-iot-history'
const historyUpdatedEvent = 'classroom-iot-history-updated'
const deviceStorageKey = 'classroom-iot-device-statuses'
const deviceUpdatedEvent = 'classroom-iot-device-statuses-updated'
const defaultDeviceStatuses: Record<DeviceName, DeviceControlStatus> = {
  LED1: 'OFF',
  LED2: 'ON',
  LED3: 'ON',
}
let inMemoryDeviceStatuses = { ...defaultDeviceStatuses }

export function getHistoryRecords(): HistoryRecord[] {
  if (typeof window === 'undefined') return initialHistoryRecords

  try {
    const storedRecords = window.localStorage.getItem(storageKey)
    if (!storedRecords) return initialHistoryRecords

    const parsedRecords: unknown = JSON.parse(storedRecords)
    return Array.isArray(parsedRecords)
      ? (parsedRecords as HistoryRecord[])
      : initialHistoryRecords
  } catch {
    return initialHistoryRecords
  }
}

export function subscribeToHistory(listener: () => void) {
  if (typeof window === 'undefined') return () => undefined

  window.addEventListener(historyUpdatedEvent, listener)
  const handleStorage = (event: StorageEvent) => {
    if (event.key === storageKey) listener()
  }
  window.addEventListener('storage', handleStorage)

  return () => {
    window.removeEventListener(historyUpdatedEvent, listener)
    window.removeEventListener('storage', handleStorage)
  }
}

export function getDeviceStatuses(): Record<DeviceName, DeviceControlStatus> {
  if (typeof window === 'undefined') return inMemoryDeviceStatuses

  try {
    const storedStatuses = window.localStorage.getItem(deviceStorageKey)
    if (!storedStatuses) return inMemoryDeviceStatuses

    const parsedStatuses: unknown = JSON.parse(storedStatuses)
    if (!parsedStatuses || typeof parsedStatuses !== 'object') {
      return inMemoryDeviceStatuses
    }

    const statuses = parsedStatuses as Record<string, unknown>
    inMemoryDeviceStatuses = {
      LED1: isDeviceControlStatus(statuses.LED1)
        ? statuses.LED1
        : defaultDeviceStatuses.LED1,
      LED2: isDeviceControlStatus(statuses.LED2)
        ? statuses.LED2
        : defaultDeviceStatuses.LED2,
      LED3: isDeviceControlStatus(statuses.LED3)
        ? statuses.LED3
        : defaultDeviceStatuses.LED3,
    }
    return inMemoryDeviceStatuses
  } catch {
    return inMemoryDeviceStatuses
  }
}

export function subscribeToDeviceStatuses(listener: () => void) {
  if (typeof window === 'undefined') return () => undefined

  window.addEventListener(deviceUpdatedEvent, listener)
  const handleStorage = (event: StorageEvent) => {
    if (event.key === deviceStorageKey) listener()
  }
  window.addEventListener('storage', handleStorage)

  return () => {
    window.removeEventListener(deviceUpdatedEvent, listener)
    window.removeEventListener('storage', handleStorage)
  }
}

export function setDeviceStatus(
  device: DeviceName,
  status: DeviceControlStatus
) {
  inMemoryDeviceStatuses = { ...getDeviceStatuses(), [device]: status }
  try {
    window.localStorage.setItem(
      deviceStorageKey,
      JSON.stringify(inMemoryDeviceStatuses)
    )
  } catch {
    // Keep the current session responsive when storage is unavailable.
  }
  window.dispatchEvent(new Event(deviceUpdatedEvent))
}

export function startDeviceAction(device: DeviceName, action: DeviceAction) {
  const records = getHistoryRecords()
  const id =
    records.reduce((maxId, record) => Math.max(maxId, record.id), 0) + 1
  const record: HistoryRecord = {
    id,
    device,
    action,
    performedBy: 'Admin',
    responseTimeSeconds: null,
    status: 'LOADING',
    time: new Date().toISOString(),
  }

  saveHistoryRecords([record, ...records])
  return id
}

export function completeDeviceAction(id: number, responseTimeSeconds: number) {
  const records = getHistoryRecords().map((record) => {
    if (record.id !== id) return record

    return {
      ...record,
      responseTimeSeconds,
      status: responseTimeSeconds > 10 ? 'FAILED' : 'SUCCESS',
    } satisfies HistoryRecord
  })

  saveHistoryRecords(records)
}

function saveHistoryRecords(records: HistoryRecord[]) {
  if (typeof window === 'undefined') return

  try {
    window.localStorage.setItem(storageKey, JSON.stringify(records))
  } catch {
    // Keep the current session responsive when storage is unavailable.
  }

  window.dispatchEvent(new Event(historyUpdatedEvent))
}

function isDeviceControlStatus(value: unknown): value is DeviceControlStatus {
  return value === 'ON' || value === 'OFF' || value === 'LOADING'
}
