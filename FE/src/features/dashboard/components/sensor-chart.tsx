import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { type SensorPoint } from '@/lib/api'

type SensorChartProps = {
  data: SensorPoint[]
  height?: number | string
}

const formatTime = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))

export function SensorChart({ data, height = 260 }: SensorChartProps) {
  return (
    <ResponsiveContainer width='100%' height={height}>
      <LineChart data={data} margin={{ top: 4, left: 4, right: 12, bottom: 0 }}>
        <CartesianGrid strokeDasharray='3 3' className='stroke-muted' />
        <XAxis
          dataKey='time'
          tickFormatter={formatTime}
          stroke='#888888'
          fontSize={12}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          yAxisId='temperature'
          stroke='#888888'
          fontSize={12}
          tickLine={false}
          axisLine={false}
          width={36}
        />
        <YAxis
          yAxisId='light'
          orientation='right'
          stroke='#888888'
          fontSize={12}
          tickLine={false}
          axisLine={false}
          width={44}
        />
        <YAxis yAxisId='rain' domain={[0, 500]} hide />
        <Tooltip
          labelFormatter={(value) => formatTime(String(value))}
          formatter={(value, name) => [
            name === 'temperature'
              ? `${value} °C`
              : name === 'humidity'
                ? `${value}%`
                : name === 'rain'
                  ? `${value} mức`
                : `${value} lux`,
            name === 'temperature'
              ? 'Nhiệt độ'
              : name === 'humidity'
                ? 'Độ ẩm'
                : name === 'rain'
                  ? 'Độ mưa'
                  : 'Ánh sáng',
          ]}
        />
        <Line
          yAxisId='rain'
          type='monotone'
          dataKey='humidity'
          stroke='currentColor'
          strokeWidth={2}
          dot={false}
          className='text-emerald-600'
        />
        <Line
          yAxisId='temperature'
          type='monotone'
          dataKey='temperature'
          stroke='currentColor'
          strokeWidth={2}
          dot={false}
          className='text-red-500'
        />
        <Line
          yAxisId='temperature'
          type='monotone'
          dataKey='rain'
          stroke='currentColor'
          strokeWidth={2}
          dot={false}
          className='text-sky-500'
        />
        <Line
          yAxisId='light'
          type='monotone'
          dataKey='light'
          stroke='currentColor'
          strokeWidth={2}
          dot={false}
          className='text-muted-foreground'
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
