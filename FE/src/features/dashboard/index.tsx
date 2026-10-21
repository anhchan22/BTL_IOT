import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CloudRain, Droplets, Lightbulb, Thermometer } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ThemeSwitch } from '@/components/theme-switch'
import { endpoints, type Device } from '@/lib/api'
import { DeviceCard } from './components/device-card'
import { SensorChart } from './components/sensor-chart'

export function Dashboard() {
  const [chartMinutes, setChartMinutes] = useState(60)
  const [rainThreshold, setRainThreshold] = useState(() => {
    const savedThreshold = window.localStorage.getItem('iot_rain_threshold')
    if (savedThreshold == null) return 70
    const stored = Number(savedThreshold)
    return Number.isFinite(stored) && stored >= 0 && stored <= 100 ? stored : 70
  })
  const wasRainAboveThreshold = useRef(false)
  const qc = useQueryClient()
  const latest = useQuery({ queryKey: ['iot', 'latest'], queryFn: endpoints.latest, refetchInterval: 15000 })
  const chart = useQuery({ queryKey: ['iot', 'chart', chartMinutes], queryFn: () => endpoints.chart(chartMinutes), refetchInterval: 3000 })
  const devicesQuery = useQuery({ queryKey: ['iot', 'devices'], queryFn: endpoints.devices, refetchInterval: 10000 })
  const mutation = useMutation({ mutationFn: endpoints.control, onSuccess: async ({ command }) => { toast.success(`Đã gửi lệnh ${command}`); await qc.invalidateQueries({ queryKey: ['iot', 'devices'] }) }, onError: () => toast.error('Không gửi được lệnh. Kiểm tra kết nối MQTT.') })
  const devices = devicesQuery.data ?? []
  const toggle = (device: Device, checked: boolean) => mutation.mutate(`${device.deviceCode}_${checked ? 'ON' : 'OFF'}`)
  const toggleAll = (checked: boolean) => mutation.mutate(checked ? 'ALL_ON' : 'ALL_OFF')
  const t = latest.data?.temperature?.value
  const h = latest.data?.humidity?.value
  const l = latest.data?.light?.value
  const rain = latest.data?.rain?.value
  const rainAlert = rain != null && rain >= rainThreshold
  useEffect(() => {
    window.localStorage.setItem('iot_rain_threshold', String(rainThreshold))
  }, [rainThreshold])
  useEffect(() => {
    if (rainAlert && !wasRainAboveThreshold.current) {
      toast.warning(`Độ mưa đã đạt ${rain}% (ngưỡng cảnh báo ${rainThreshold}%).`)
    }
    wasRainAboveThreshold.current = rainAlert
  }, [rainAlert, rain, rainThreshold])
  const unavailable = (query: { isLoading: boolean; isError: boolean }) => query.isLoading ? 'Đang tải…' : query.isError ? 'Lỗi kết nối' : '--'
  return <><Header className='shrink-0'><div className='ms-auto'><ThemeSwitch /></div></Header><Main className='grid min-h-0 flex-1 grid-rows-[auto_auto_minmax(0,1fr)_minmax(9rem,auto)] gap-3 overflow-auto'><div><h1 className='text-2xl font-bold tracking-tight'>Dashboard</h1></div><div className='grid shrink-0 grid-cols-2 gap-2 xl:grid-cols-4'><MetricCard title='Nhiệt độ' value={t == null ? unavailable(latest) : `${t} °C`} description={latest.data?.temperature?.status ?? 'Cập nhật từ cảm biến'} icon={Thermometer} /><MetricCard title='Độ ẩm' value={h == null ? unavailable(latest) : `${h}%`} description={latest.data?.humidity?.status ?? 'Cập nhật từ cảm biến'} icon={Droplets} /><MetricCard title='Ánh sáng' value={l == null ? unavailable(latest) : `${l} lux`} description={latest.data?.light?.status ?? 'Cập nhật từ cảm biến'} icon={Lightbulb} /><Card className={rainAlert ? 'animate-pulse border-destructive bg-destructive/5' : ''}><CardHeader className='flex flex-row items-center justify-between space-y-0 px-4 pb-0'><CardTitle className='text-sm font-medium'>Độ mưa</CardTitle><CloudRain className={`size-4 ${rainAlert ? 'text-destructive' : 'text-muted-foreground'}`} /></CardHeader><CardContent className='px-4'><div className={`text-2xl font-bold leading-none ${rainAlert ? 'text-destructive' : ''}`}>{rain == null ? unavailable(latest) : `${rain}%`}</div><div className='mt-2 flex items-center gap-2 text-xs'><label htmlFor='rain-threshold' className='text-muted-foreground'>Ngưỡng cảnh báo (%)</label><input id='rain-threshold' type='number' min={0} max={100} step={1} value={rainThreshold} onChange={event => { const value = Number(event.target.value); if (event.target.value !== '' && value >= 0 && value <= 100) setRainThreshold(value) }} className='h-7 w-16 rounded-md border border-input bg-background px-2 text-sm' /></div>{rainAlert && <p role='status' className='mt-1 text-xs font-semibold text-destructive'>Cảnh báo: độ mưa đã vượt ngưỡng</p>}</CardContent></Card></div><Card className='flex min-h-56 flex-col gap-2 overflow-hidden py-2'><CardHeader className='flex shrink-0 flex-row flex-wrap items-center justify-between gap-x-3 gap-y-1 px-4 pb-0'><div className='flex flex-wrap items-center gap-3'><CardTitle>Biến động cảm biến</CardTitle><select aria-label='Khoảng thời gian biểu đồ' className='h-8 rounded-md border border-input bg-background px-2 text-xs' value={chartMinutes} onChange={event => setChartMinutes(Number(event.target.value))}><option value={30}>30 phút gần đây</option><option value={60}>1 giờ gần đây</option></select></div><div className='flex gap-3 text-xs text-muted-foreground'><ChartKey color='bg-red-500' label='Nhiệt độ' /><ChartKey color='bg-emerald-600' label='Độ ẩm' /><ChartKey color='bg-muted-foreground' label='Ánh sáng' /><ChartKey color='bg-sky-500' label='Độ mưa' /></div></CardHeader><CardContent className='min-h-48 flex-1 px-2 pb-1'>{chart.data && <SensorChart data={chart.data} height='100%' />}{chart.isError && <p className='p-6 text-sm text-destructive'>Không tải được dữ liệu biểu đồ.</p>}{chart.isLoading && <p className='p-6 text-sm text-muted-foreground'>Đang tải dữ liệu…</p>}</CardContent></Card><Card className='shrink-0 gap-2 py-2'><CardHeader className='flex flex-row flex-wrap items-center justify-between gap-2 px-4 pb-0'><CardTitle className='text-base'>Điều khiển thiết bị</CardTitle><div className='flex gap-2'><Button size='sm' disabled={mutation.isPending || devices.some(d => d.status === 'LOADING')} onClick={() => toggleAll(true)}>Bật tất cả</Button><Button size='sm' variant='outline' disabled={mutation.isPending || devices.some(d => d.status === 'LOADING')} onClick={() => toggleAll(false)}>Tắt tất cả</Button></div></CardHeader><CardContent className='px-4 pb-1'>{devicesQuery.isError ? <p className='text-sm text-destructive'>Không tải được danh sách thiết bị.</p> : <div className='grid grid-cols-3 gap-2'>{devices.map(device => <DeviceCard key={device.id} device={{ id: String(device.id), name: device.deviceCode, status: device.status }} onToggle={(_id, checked) => toggle(device, checked)} />)}</div>}</CardContent></Card></Main></>
}
function ChartKey({ color, label }: { color: string; label: string }) { return <span className='flex items-center gap-1.5 whitespace-nowrap'><span className={`size-2 rounded-full ${color}`} />{label}</span> }
function MetricCard({ title, value, description, icon: Icon }: { title: string; value: string; description: string; icon: React.ComponentType<{ className?: string }> }) { return <Card className='gap-2 py-3'><CardHeader className='flex flex-row items-center justify-between space-y-0 px-4 pb-0'><CardTitle className='text-sm font-medium'>{title}</CardTitle><Icon className='size-4 text-muted-foreground' /></CardHeader><CardContent className='px-4'><div className='flex items-center gap-2'><div className='text-2xl font-bold leading-none'>{value}</div></div><p className='text-xs text-muted-foreground'>{description}</p></CardContent></Card> }
