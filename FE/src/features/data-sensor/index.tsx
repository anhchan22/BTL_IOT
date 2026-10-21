import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Check,
  ChevronsLeft,
  ChevronsRight,
  ChevronLeft,
  ChevronRight,
  Copy,
  RotateCcw,
  Search,
  ArrowDown,
  ArrowDownUp,
  ArrowUp,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ThemeSwitch } from '@/components/theme-switch'
import { endpoints, type SensorRecord } from '@/lib/api'

type SearchField = 'all' | 'humidity' | 'light' | 'temperature' | 'rain' | 'time'
type SortField = 'value' | 'unit' | 'time'
type SortState = { field: SortField; direction: 'asc' | 'desc' }
type SensorFilters = { sensor: string; value: string; from: string; to: string }

const labels: Record<string, string> = {
  temperature: 'Nhiệt độ',
  humidity: 'Độ ẩm',
  light: 'Ánh sáng',
  rain: 'Độ mưa',
}

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

const initialFilters: SensorFilters = { sensor: '', value: '', from: '', to: '' }

export function DataSensor() {
  const [searchField, setSearchField] = useState<SearchField>('all')
  const [searchValue, setSearchValue] = useState('')
  const [filters, setFilters] = useState<SensorFilters>(initialFilters)
  const [searchError, setSearchError] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(15)
  const [sort, setSort] = useState<SortState>({ field: 'time', direction: 'desc' })
  const [copiedId, setCopiedId] = useState<number | null>(null)

  const query = useQuery({
    queryKey: ['iot', 'sensors', filters, page, pageSize, sort],
    staleTime: 0,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    queryFn: () => endpoints.sensors({
      page,
      page_size: pageSize,
      sensor_code: filters.sensor || undefined,
      value_min: filters.value || undefined,
      value_max: filters.value || undefined,
      from: filters.from || undefined,
      to: filters.to || undefined,
      sort_by: sort.field,
      sort_dir: sort.direction,
    }),
  })

  const apply = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSearchError('')

    if (searchField === 'time' && searchValue.trim()) {
      const range = exactTimeRange(searchValue)
      if (!range) {
        setSearchError('Định dạng hỗ trợ: YYYY, YYYY-MM, YYYY-MM-DD, YYYY-MM-DD HH, YYYY-MM-DD HH:mm hoặc YYYY-MM-DD HH:mm:ss.')
        return
      }
      setFilters({ ...initialFilters, from: range.from, to: range.to })
    } else {
      const value = searchValue.trim()
      if (value && !Number.isFinite(Number(value))) {
        setSearchError('Giá trị tìm kiếm phải là số; nếu tìm theo thời gian, chọn “Thời gian”.')
        return
      }
      setFilters({
        sensor: searchField === 'all' || searchField === 'time' ? '' : searchField,
        value,
        from: '',
        to: '',
      })
    }

    setPage(1)
  }

  const reset = () => {
    setSearchField('all')
    setSearchValue('')
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
        : field === 'time' ? 'desc' : 'asc',
    }))
    setPage(1)
  }

  const copyTime = async (row: SensorRecord) => {
    try {
      await navigator.clipboard.writeText(copiedTime(row.recordedAt))
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
    return sort.direction === 'asc'
      ? <ArrowUp className='size-4' />
      : <ArrowDown className='size-4' />
  }

  return <>
    <Header fixed><div className='ms-auto'><ThemeSwitch /></div></Header>
    <Main className='flex h-[calc(100svh-2.5rem)] flex-1 flex-col gap-3 overflow-hidden'>
      <div><h2 className='text-2xl font-bold tracking-tight'>Data Sensor</h2></div>

      <Card className='shrink-0'>
        <form id='data-sensor-search' onSubmit={apply}>
        <CardContent className='grid gap-3 p-4 sm:grid-cols-[minmax(12rem,0.7fr)_minmax(18rem,1.5fr)_auto_auto] sm:items-end'>
          <div className='grid gap-2'>
            <Label htmlFor='search-field'>Tìm theo</Label>
            <select
              id='search-field'
              className='h-10 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs'
              value={searchField}
              onChange={event => { setSearchField(event.target.value as SearchField); setSearchError('') }}
            >
              <option value='all'>Tất cả</option>
              <option value='humidity'>Độ ẩm</option>
              <option value='light'>Ánh sáng</option>
              <option value='rain'>Độ mưa</option>
              <option value='temperature'>Nhiệt độ</option>
              <option value='time'>Thời gian</option>
            </select>
          </div>
          <div className='grid gap-2'>
            <Label htmlFor='search-value'>Giá trị tìm kiếm</Label>
            <Input
              id='search-value'
              value={searchValue}
              onChange={event => { setSearchValue(event.target.value); setSearchError('') }}
              placeholder={searchField === 'time' ? 'YYYY | YYYY-MM | YYYY-MM-DD HH:mm:ss' : 'Nhập giá trị cần tìm'}
              aria-invalid={Boolean(searchError)}
            />
          </div>
          <Button type='submit' className='h-10 px-5'>
            <Search /> Tìm
          </Button>
          <Button type='button' variant='outline' size='icon' className='h-10 w-10' onClick={reset} aria-label='Đặt lại bộ lọc'>
            <RotateCcw />
          </Button>
        </CardContent>
        </form>
      </Card>
      {searchError && <p role='alert' className='-mt-2 text-sm text-destructive'>{searchError}</p>}

      <div className='min-h-0 flex-1 overflow-auto rounded-lg border'>
        <table className='w-full min-w-[760px] text-sm'>
          <thead className='sticky top-0 z-10 bg-muted'>
            <tr className='text-left'>
              <th className='w-20 px-3 py-3 font-medium'>ID</th>
              <th className='px-3 py-3 font-medium'>Loại cảm biến</th>
              <th className='px-3 py-3 font-medium'>
                <button className='inline-flex items-center gap-1.5' onClick={() => changeSort('value')} aria-label='Sắp xếp theo giá trị'>Giá trị {sortIcon('value')}</button>
              </th>
              <th className='px-3 py-3 font-medium'>
                <button className='inline-flex items-center gap-1.5' onClick={() => changeSort('unit')} aria-label='Sắp xếp theo đơn vị'>Đơn vị {sortIcon('unit')}</button>
              </th>
              <th className='px-3 py-3 font-medium'>
                <button className='inline-flex items-center gap-1.5' onClick={() => changeSort('time')} aria-label='Sắp xếp theo thời gian'>Thời gian {sortIcon('time')}</button>
              </th>
            </tr>
          </thead>
          <tbody>
            {query.isLoading ? <tr><td colSpan={5} className='p-8 text-center text-muted-foreground'>Đang tải dữ liệu…</td></tr>
              : query.isError ? <tr><td colSpan={5} className='p-8 text-center text-destructive'>Không tải được dữ liệu cảm biến.</td></tr>
                : !result?.items.length ? <tr><td colSpan={5} className='p-8 text-center text-muted-foreground'>Không có dữ liệu phù hợp.</td></tr>
                  : result.items.map((row: SensorRecord) => <tr key={row.id} className='border-t hover:bg-muted/40'>
                    <td className='px-3 py-2.5'>{row.id}</td>
                    <td className='px-3 py-2.5'>{labels[row.sensorCode] ?? row.sensorName}</td>
                    <td className='px-3 py-2.5 tabular-nums'>{row.value}</td>
                    <td className='px-3 py-2.5'>{row.unit}</td>
                    <td className='px-3 py-2.5'>
                      <div className='flex items-center gap-2'>
                        <time className='tabular-nums'>{displayTime(row.recordedAt)}</time>
                        <Button
                          type='button'
                          variant='ghost'
                          size='icon'
                          className='size-7 text-muted-foreground hover:text-foreground'
                          onClick={() => void copyTime(row)}
                          aria-label={`Sao chép thời gian ${copiedTime(row.recordedAt)}`}
                          title={`Sao chép ${copiedTime(row.recordedAt)}`}
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
        <span className='text-muted-foreground'>Trang <span className='font-medium text-foreground'>{result?.page ?? page}</span> trên {totalPages} · {result?.totalItems ?? 0} bản ghi phù hợp</span>
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
