import { type HistoryRecord } from './schema'

const baseTime = new Date('2026-08-17T09:00:00+07:00')
const devices: HistoryRecord['device'][] = ['LED1', 'LED2', 'LED3']

export const historyRecords: HistoryRecord[] = Array.from({ length: 24 }, (_, i) => {
  const action: HistoryRecord['action'] = i % 2 === 0 ? 'ON' : 'OFF'
  const device = devices[i % devices.length]
  const responseTimeSeconds =
    i % 5 === 0 ? null : i % 7 === 0 ? 10.8 : 1.2 + (i % 8) * 0.8
  const status: HistoryRecord['status'] =
    responseTimeSeconds === null
      ? 'LOADING'
      : responseTimeSeconds > 10
        ? 'FAILED'
        : 'SUCCESS'
  return {
    id: i + 1,
    device,
    action,
    performedBy: 'Admin',
    responseTimeSeconds,
    status,
    time: new Date(baseTime.getTime() + i * 12 * 60_000).toISOString(),
  }
})
