import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CloudRain } from 'lucide-react'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { endpoints } from '@/lib/api'

export function RainAlertCard() {
  const queryClient = useQueryClient()
  const [threshold, setThreshold] = useState('')
  const alert = useQuery({
    queryKey: ['iot', 'rain-alert'],
    queryFn: endpoints.rainAlert,
    refetchInterval: 15000,
  })
  const save = useMutation({
    mutationFn: () => endpoints.updateRainAlertSettings(Number(threshold)),
    onSuccess: async (status) => {
      setThreshold(String(status.threshold))
      queryClient.setQueryData(['iot', 'rain-alert'], status)
      await queryClient.invalidateQueries({ queryKey: ['iot', 'rain-alert'] })
      toast.success('Đã lưu ngưỡng cảnh báo độ mưa.')
    },
    onError: () => toast.error('Không lưu được ngưỡng cảnh báo.'),
  })

  useEffect(() => {
    if (alert.data?.configured) setThreshold(String(alert.data.threshold))
  }, [alert.data?.configured, alert.data?.threshold])

  const active = alert.data?.active ?? false
  const numericThreshold = Number(threshold)
  const validThreshold = threshold.trim() !== '' && Number.isFinite(numericThreshold)
    && numericThreshold >= 0 && numericThreshold <= 500
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (validThreshold) save.mutate()
  }

  return (
    <Card className={`min-h-24 justify-center gap-2 p-3 ${active ? 'animate-pulse border-destructive bg-destructive/5' : ''}`}>
      <CardHeader className='flex flex-row items-center justify-between gap-2 p-0'>
        <div className='flex min-w-0 items-center gap-2'>
          <CloudRain className={`size-5 shrink-0 ${active ? 'text-destructive' : 'text-sky-600'}`} />
          <CardTitle className='truncate text-sm font-semibold'>Đèn cảnh báo</CardTitle>
        </div>
        <Badge variant={active ? 'destructive' : 'secondary'} className='shrink-0'>
          {alert.isLoading ? 'Đang tải' : alert.isError ? 'Mất kết nối' : active ? 'Đang cảnh báo' : alert.data?.configured ? 'Bình thường' : 'Chưa cài ngưỡng'}
        </Badge>
      </CardHeader>
      <CardContent className='flex flex-col gap-2 p-0'>
        <p className='text-xs text-muted-foreground'>Bật hôm nay: <span className='font-semibold text-foreground'>{alert.data?.activationsToday ?? '—'}</span> lần</p>
        <form onSubmit={submit} className='flex items-end gap-2'>
          <label className='min-w-0 flex-1 text-xs text-muted-foreground' htmlFor='rain-alert-threshold'>
            Ngưỡng (mức)
            <input
              id='rain-alert-threshold'
              type='number'
              min={0}
              max={500}
              step='0.01'
              value={threshold}
              onChange={event => setThreshold(event.target.value)}
              placeholder='0–500'
              className='mt-1 h-8 w-full rounded-md border border-input bg-background px-2 text-sm text-foreground'
            />
          </label>
          <Button type='submit' size='sm' disabled={!validThreshold || save.isPending}>
            {save.isPending ? 'Lưu…' : 'Lưu'}
          </Button>
        </form>
        {alert.isError && <p role='alert' className='text-xs text-destructive'>Không tải được trạng thái cảnh báo.</p>}
      </CardContent>
    </Card>
  )
}
