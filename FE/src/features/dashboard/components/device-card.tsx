import { Lightbulb, LoaderCircle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Switch } from '@/components/ui/switch'
import { type ClassroomDevice } from '../data/devices'

type DeviceCardProps = {
  device: ClassroomDevice
  onToggle: (id: ClassroomDevice['id'], checked: boolean) => void
}

export function DeviceCard({ device, onToggle }: DeviceCardProps) {
  const isLoading = device.status === 'LOADING'
  const isOn = device.status === 'ON'

  return (
    <Card className='min-h-24 justify-center gap-3 p-3'>
      <CardHeader className='flex flex-row items-center gap-2 p-0'>
        <Lightbulb className='size-5 shrink-0 text-amber-500' />
        <CardTitle className='truncate text-base font-semibold'>
          {device.name}
        </CardTitle>
      </CardHeader>
      <CardContent className='flex flex-col items-center gap-2 p-0 sm:flex-row sm:justify-between'>
        <Badge variant={isOn ? 'default' : isLoading ? 'outline' : 'secondary'}>
          {isLoading ? (
            <>
              <LoaderCircle className='size-4 animate-spin' aria-hidden='true' />
              <span className='sr-only'>Đang xử lý</span>
            </>
          ) : (
            device.status
          )}
        </Badge>
        <Switch
          checked={isOn}
          disabled={isLoading}
          onCheckedChange={(checked) => onToggle(device.id, checked)}
          aria-label={`Điều khiển ${device.name}`}
        />
      </CardContent>
    </Card>
  )
}
