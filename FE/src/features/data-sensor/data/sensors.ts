import { sensorSeries } from '@/features/dashboard/data/sensor-series'
import { type SensorRecord } from './schema'

export const sensorRecords: SensorRecord[] = sensorSeries.flatMap((sample) => [
  {
    id: (sample.id - 1) * 4 + 1,
    type: 'temperature',
    value: sample.temperature,
    unit: '°C',
    time: sample.time,
  },
  {
    id: (sample.id - 1) * 4 + 2,
    type: 'humidity',
    value: sample.humidity,
    unit: '%',
    time: sample.time,
  },
  {
    id: (sample.id - 1) * 4 + 3,
    type: 'light',
    value: sample.light,
    unit: 'lux',
    time: sample.time,
  },
  {
    id: (sample.id - 1) * 4 + 4,
    type: 'rain',
    value: sample.rain,
    unit: '%',
    time: sample.time,
  },
])
