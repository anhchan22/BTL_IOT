import { createFileRoute } from '@tanstack/react-router'
import { DataSensor } from '@/features/data-sensor'

export const Route = createFileRoute('/_authenticated/data-sensor/')({
  component: DataSensor,
})
