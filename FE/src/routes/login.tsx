import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { LockKeyhole } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import axios from 'axios'
import { endpoints, saveSession } from '@/lib/api'

export const Route = createFileRoute('/login')({ validateSearch: (search: Record<string, unknown>) => ({ redirect: typeof search.redirect === 'string' ? search.redirect : '/' }), component: LoginPage })

function getInternalRedirect(value: string) {
  try {
    const target = new URL(value, window.location.origin)
    if (target.origin !== window.location.origin || target.pathname === '/login') return '/'
    return `${target.pathname}${target.search}${target.hash}`
  } catch {
    return '/'
  }
}

function LoginPage() {
  const { redirect: redirectTo } = Route.useSearch()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(''); setBusy(true)
    try { const login = await endpoints.login(username, password); saveSession(login); await navigate({ to: getInternalRedirect(redirectTo) as '/' }) }
    catch (e) { setError(axios.isAxiosError(e) ? (e.response?.data?.message || 'Sai tên đăng nhập hoặc mật khẩu.') : 'Không thể kết nối máy chủ.') }
    finally { setBusy(false) }
  }
  return <main className='grid min-h-svh place-items-center bg-muted/30 p-4'><form onSubmit={submit} className='w-full max-w-sm space-y-5 rounded-xl border bg-card p-7 shadow-sm'><div><div className='mb-3 grid size-11 place-items-center rounded-lg bg-primary text-primary-foreground'><LockKeyhole /></div><h1 className='text-2xl font-semibold'>Đăng nhập IoT</h1><p className='mt-1 text-sm text-muted-foreground'>Đăng nhập để xem dữ liệu và điều khiển thiết bị.</p></div><div className='space-y-2'><Label htmlFor='username'>Tên đăng nhập</Label><Input id='username' autoComplete='username' value={username} onChange={e => setUsername(e.target.value)} required /></div><div className='space-y-2'><Label htmlFor='password'>Mật khẩu</Label><Input id='password' type='password' autoComplete='current-password' value={password} onChange={e => setPassword(e.target.value)} required /></div>{error && <p role='alert' className='text-sm text-destructive'>{error}</p>}<Button className='w-full' disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</Button></form></main>
}
