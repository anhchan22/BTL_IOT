import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  ArrowDown,
  ArrowDownUp,
  ArrowUp,
  Check,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Copy,
  RotateCcw,
  Search,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ThemeSwitch } from '@/components/theme-switch'
import { endpoints, type HistoryRecord } from '@/lib/api'

type SortField = 'id' | 'device' | 'action' | 'performed_by' | 'status' | 'time'
type SortState = { field: SortField; direction: 'asc' | 'desc' }
type HistoryFilters = { device: string; action: string; status: string; user: string; from: string; to: string }

const initialFilters: HistoryFilters = { device: '', action: '', status: '', user: '', from: '', to: '' }

const timeParts = (value: string) => {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(value))
  return Object.fromEntries(parts.map(part => [part.type, part.value])) as Record<string, string>
}

const displayTime = (value: string) => {
  const p = timeParts(value)
  return `${p.hour}:${p.minute}:${p.second} ${p.day}/${p.month}/${p.year}`
}

const copiedTime = (value: string) => {
  const p = timeParts(value)
  return `${p.year}-${p.month}-${p.day} ${p.hour}:${p.minute}:${p.second}`
}

const exactTimeRange = (input: string) => {
  const match = input.trim().match(/^(\d{4})(?:-(\d{2})(?:-(\d{2})(?:[ T](\d{2})(?::(\d{2})(?::(\d{2}))?)?)?)?)?$/)
  if (!match) return null

  const [year, month, day, hour, minute, second] = match.slice(1).map(part => part === undefined ? undefined : Number(part))
  const precision = second !== undefined ? 'second'
    : minute !== undefined ? 'minute'
      : hour !== undefined ? 'hour'
        : day !== undefined ? 'day'
          : month !== undefined ? 'month' : 'year'
  const start = new Date(0)
  start.setUTCFullYear(year!, (month ?? 1) - 1, day ?? 1)
  start.setUTCHours(hour ?? 0, minute ?? 0, second ?? 0, 0)
  if (
    start.getUTCFullYear() !== year || start.getUTCMonth() !== (month ?? 1) - 1 ||
    start.getUTCDate() !== (day ?? 1) || start.getUTCHours() !== (hour ?? 0) ||
    start.getUTCMinutes() !== (minute ?? 0) || start.getUTCSeconds() !== (second ?? 0)
  ) return null

  const format = (date: Date) => {
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}T${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}+07:00`
  }
  const end = new Date(start)
  if (precision === 'year') end.setUTCFullYear(end.getUTCFullYear() + 1)
  else if (precision === 'month') end.setUTCMonth(end.getUTCMonth() + 1)
  else if (precision === 'day') end.setUTCDate(end.getUTCDate() + 1)
  else if (precision === 'hour') end.setUTCHours(end.getUTCHours() + 1)
  else if (precision === 'minute') end.setUTCMinutes(end.getUTCMinutes() + 1)
  else end.setUTCSeconds(end.getUTCSeconds() + 1)
  return { from: format(start), to: format(end) }
}

const statusLabels: Record<string, string> = {
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  LOADING: 'Đang tải',
}

export function History() {
  const [device, setDevice] = useState('')
  const [action, setAction] = useState('')
  const [status, setStatus] = useState('')
  const [timeInput, setTimeInput] = useState('')
  const [filters, setFilters] = useState<HistoryFilters>(initialFilters)
  const [searchError, setSearchError] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(15)
  const [sort, setSort] = useState<SortState>({ field: 'time', direction: 'desc' })
  const [copiedId, setCopiedId] = useState<number | null>(null)

  const query = useQuery({
    queryKey: ['iot', 'history', filters, page, pageSize, sort],
    queryFn: () => endpoints.history({
      page,
      page_size: pageSize,
      device: filters.device || undefined,
      action: filters.action || undefined,
      status: filters.status || undefined,
      performed_by: filters.user || undefined,
      from: filters.from || undefined,
      to: filters.to || undefined,
      sort_by: sort.field,
      sort_dir: sort.direction,
    }),
  })

  const apply = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSearchError('')
    const timestamp = timeInput.trim()
    const range = timestamp ? exactTimeRange(timestamp) : null
    if (timestamp && !range) {
      setSearchError('Định dạng hỗ trợ: YYYY, YYYY-MM, YYYY-MM-DD, YYYY-MM-DD HH, YYYY-MM-DD HH:mm hoặc YYYY-MM-DD HH:mm:ss.')
      return
    }

    setFilters({
      device,
      action,
      status,
      user: '',
      from: range?.from ?? '',
      to: range?.to ?? '',
    })
    setPage(1)
  }

  const reset = () => {
    setDevice('')
    setAction('')
    setStatus('')
    setTimeInput('')
    setFilters(initialFilters)
    setSearchError('')
    setSort({ field: 'time', direction: 'desc' })
    setPage(1)
  }

  const changeSort = (field: SortField) => {
    setSort(current => ({
      field,
      direction: current.field === field
        ? current.direction === 'asc' ? 'desc' : 'asc'
        : field === 'time' || field === 'id' ? 'desc' : 'asc',
    }))
    setPage(1)
  }

  const copyTime = async (row: HistoryRecord) => {
    try {
      await navigator.clipboard.writeText(copiedTime(row.time))
      setCopiedId(row.id)
      window.setTimeout(() => setCopiedId(current => current === row.id ? null : current), 1500)
    } catch {
      setSearchError('Trình duyệt không cho phép sao chép. Hãy mở trang bằng localhost hoặc HTTPS.')
    }
  }

  const result = query.data
  const totalPages = Math.max(result?.totalPages ?? 1, 1)
  const sortIcon = (field: SortField) => {
    if (sort.field !== field) return <ArrowDownUp className='size-4 text-muted-foreground' />
    return sort.direction === 'asc' ? <ArrowUp className='size-4' /> : <ArrowDown className='size-4' />
  }
  const sortButton = (field: SortField, label: string) => (
    <button className='inline-flex items-center gap-1.5 whitespace-nowrap' onClick={() => changeSort(field)} aria-label={`Sắp xếp theo ${label}`}>
      {label} {sortIcon(field)}
    </button>
  )

  return <>
    <Header fixed><div className='ms-auto'><ThemeSwitch /></div></Header>
    <Main className='flex h-[calc(100svh-2.5rem)] flex-1 flex-col gap-3 overflow-hidden'>
      <div><h2 className='text-2xl font-bold tracking-tight'>Lịch sử</h2></div>

      <Card className='shrink-0'>
        <form onSubmit={apply}>
          <CardContent className='grid gap-3 p-4 sm:grid-cols-2 xl:grid-cols-[minmax(16rem,1.2fr)_repeat(3,minmax(10rem,1fr))_auto_auto] xl:items-end'>
            <div className='grid gap-2'>
              <Label htmlFor='history-time'>Tìm theo thời gian</Label>
              <Input
                id='history-time'
                value={timeInput}
                onChange={event => { setTimeInput(event.target.value); setSearchError('') }}
                placeholder='YYYY-MM-DD HH:mm:ss'
                aria-invalid={Boolean(searchError)}
              />
            </div>
            <div className='grid gap-2'>
              <Label htmlFor='history-device'>Thiết bị</Label>
              <select id='history-device' className='h-10 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs' value={device} onChange={event => setDevice(event.target.value)}>
                <option value=''>Tất cả</option>
                {['LED1', 'LED2', 'LED3'].map(item => <option key={item} value={item}>{item}</option>)}
                <option value='LED4'>LED cảnh báo</option>
              </select>
            </div>
            <div className='grid gap-2'>
              <Label htmlFor='history-action'>Thao tác</Label>
              <select id='history-action' className='h-10 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs' value={action} onChange={event => setAction(event.target.value)}>
                <option value=''>Tất cả</option><option value='ON'>Bật</option><option value='OFF'>Tắt</option>
              </select>
            </div>
            <div className='grid gap-2'>
              <Label htmlFor='history-status'>Trạng thái</Label>
              <select id='history-status' className='h-10 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs' value={status} onChange={event => setStatus(event.target.value)}>
                <option value=''>Tất cả</option>
                <option value='SUCCESS'>Thành công</option><option value='FAILED'>Thất bại</option><option value='LOADING'>Đang tải</option>
              </select>
            </div>
            <Button type='submit' className='h-10 px-5'><Search /> Tìm kiếm</Button>
            <Button type='button' variant='outline' size='icon' className='h-10 w-10' onClick={reset} aria-label='Đặt lại bộ lọc'><RotateCcw /></Button>
          </CardContent>
        </form>
      </Card>
      {searchError && <p role='alert' className='-mt-2 text-sm text-destructive'>{searchError}</p>}

      <div className='min-h-0 flex-1 overflow-auto rounded-lg border'>
        <table className='w-full min-w-[1050px] text-sm'>
          <thead className='sticky top-0 z-10 bg-muted'>
            <tr className='text-left'>
              <th className='px-3 py-3 font-medium'>{sortButton('id', 'ID')}</th>
              <th className='px-3 py-3 font-medium'>{sortButton('device', 'Thiết bị')}</th>
              <th className='px-3 py-3 font-medium'>{sortButton('action', 'Thao tác')}</th>
              <th className='px-3 py-3 font-medium'>{sortButton('performed_by', 'Người thực hiện')}</th>
              <th className='px-3 py-3 font-medium'>{sortButton('status', 'Trạng thái')}</th>
              <th className='px-3 py-3 font-medium'>Thời gian phản hồi</th>
              <th className='px-3 py-3 font-medium'>{sortButton('time', 'Thời gian')}</th>
            </tr>
          </thead>
          <tbody>
            {query.isLoading ? <tr><td colSpan={7} className='p-8 text-center text-muted-foreground'>Đang tải lịch sử…</td></tr>
              : query.isError ? <tr><td colSpan={7} className='p-8 text-center text-destructive'>Không thể tải lịch sử.</td></tr>
                : !result?.items.length ? <tr><td colSpan={7} className='p-8 text-center text-muted-foreground'>Chưa có thao tác phù hợp.</td></tr>
                  : result.items.map((row: HistoryRecord) => <tr key={row.id} className='border-t hover:bg-muted/40'>
                    <td className='px-3 py-2.5 tabular-nums'>{row.id}</td>
                    <td className='px-3 py-2.5'>{row.device === 'LED4' ? 'LED cảnh báo' : row.device}</td>
                    <td className='px-3 py-2.5'>
                      <span className={`inline-flex min-w-12 justify-center rounded-full border px-2 py-0.5 text-xs font-medium ${row.action === 'ON' ? 'border-primary bg-primary text-primary-foreground' : 'bg-background'}`}>
                        {row.action}
                      </span>
                    </td>
                    <td className='px-3 py-2.5'>{row.performedBy ?? '—'}</td>
                    <td className='px-3 py-2.5'>
                      <span title={row.errorMessage ?? undefined} className={`inline-flex whitespace-nowrap rounded-full border px-2.5 py-1 text-xs font-medium ${row.status === 'SUCCESS' ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950 dark:text-emerald-300' : row.status === 'FAILED' ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300' : 'border-border bg-muted text-muted-foreground'}`}>
                        {statusLabels[row.status] ?? row.status}
                      </span>
                    </td>
                    <td className='px-3 py-2.5 tabular-nums'>
                      {row.responseTimeSeconds == null ? row.status === 'LOADING' ? 'Đang xử lý' : '—' : `${row.responseTimeSeconds} s`}
                    </td>
                    <td className='px-3 py-2.5'>
                      <div className='flex items-center gap-2'>
                        <time className='whitespace-nowrap tabular-nums'>{displayTime(row.time)}</time>
                        <Button
                          type='button'
                          variant='ghost'
                          size='icon'
                          className='size-7 text-muted-foreground hover:text-foreground'
                          onClick={() => void copyTime(row)}
                          aria-label={`Sao chép thời gian ${copiedTime(row.time)}`}
                          title={`Sao chép ${copiedTime(row.time)}`}
                        >
                          {copiedId === row.id ? <Check className='size-4 text-emerald-600' /> : <Copy className='size-4' />}
                        </Button>
                      </div>
                    </td>
                  </tr>)}
          </tbody>
        </table>
      </div>

      <div className='flex shrink-0 flex-wrap items-center justify-between gap-3 text-sm'>
        <span className='text-muted-foreground'>Trang <span className='font-medium text-foreground'>{result?.page ?? page}</span> trên {totalPages} · Tổng {result?.totalItems ?? 0} thao tác</span>
        <div className='flex items-center gap-2'>
          <span className='text-muted-foreground'>Số dòng</span>
          <select aria-label='Số dòng mỗi trang' className='h-9 rounded-md border border-input bg-background px-2' value={pageSize} onChange={event => { setPageSize(Number(event.target.value)); setPage(1) }}>
            <option value={15}>15</option><option value={20}>20</option><option value={50}>50</option>
          </select>
          <Button variant='outline' size='icon' disabled={page <= 1} onClick={() => setPage(1)} aria-label='Trang đầu'><ChevronsLeft /></Button>
          <Button variant='outline' size='icon' disabled={page <= 1} onClick={() => setPage(current => current - 1)} aria-label='Trang trước'><ChevronLeft /></Button>
          <Button variant='outline' size='icon' disabled={page >= totalPages} onClick={() => setPage(current => current + 1)} aria-label='Trang sau'><ChevronRight /></Button>
          <Button variant='outline' size='icon' disabled={page >= totalPages} onClick={() => setPage(totalPages)} aria-label='Trang cuối'><ChevronsRight /></Button>
        </div>
      </div>
    </Main>
  </>
}
