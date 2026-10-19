export type DeviceStatus = 'ON' | 'OFF' | 'LOADING'

export type ClassroomDevice = {
  id: string
  name: string
  status: DeviceStatus
  online?: boolean
}

export const devices: ClassroomDevice[] = [
  {
    id: 'led-1',
    name: 'LED1',
    status: 'OFF',
  },
  {
    id: 'led-2',
    name: 'LED2',
    status: 'ON',
  },
  {
    id: 'led-3',
    name: 'LED3',
    status: 'ON',
  },
]
