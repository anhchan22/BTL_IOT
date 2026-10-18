import { useEffect, useState, type ElementType, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  ExternalLink,
  FileText,
  GraduationCap,
  IdCard,
  Mail,
  Pencil,
  RotateCcw,
  NotebookTabs,
} from 'lucide-react'
import { IconFigma, IconGithub } from '@/assets/brand-icons'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Header } from '@/components/layout/header'
import { Main } from '@/components/layout/main'
import { ThemeSwitch } from '@/components/theme-switch'
import { endpoints, getStoredUser, saveUser, type UserData } from '@/lib/api'

type Form = Omit<UserData, 'id' | 'username'>
type ResourceKey = 'githubUrl' | 'figmaUrl' | 'postmanUrl' | 'reportUrl'

const empty: Form = {
  fullName: '', studentCode: '', className: '', email: '', avatarUrl: '',
  githubUrl: '', figmaUrl: '', postmanUrl: '', reportUrl: '',
}

const fields: [keyof Form, string][] = [
  ['fullName', 'Họ và tên'],
  ['studentCode', 'Mã sinh viên'],
  ['className', 'Lớp'],
  ['email', 'Email liên hệ'],
  ['avatarUrl', 'URL ảnh đại diện'],
  ['githubUrl', 'GitHub'],
  ['figmaUrl', 'Figma'],
  ['postmanUrl', 'Postman'],
  ['reportUrl', 'Báo cáo'],
]

const resources: {
  key: ResourceKey
  title: string
  description: string
  Icon: ElementType<{ className?: string }>
}[] = [
  { key: 'githubUrl', title: 'GitHub', description: 'Repository mã nguồn dự án', Icon: IconGithub },
  { key: 'figmaUrl', title: 'Figma', description: 'Thiết kế giao diện hệ thống', Icon: IconFigma },
  { key: 'postmanUrl', title: 'Postman', description: 'Tài liệu API và collection test', Icon: NotebookTabs },
  { key: 'reportUrl', title: 'Báo cáo', description: 'Tài liệu báo cáo bài tập lớn', Icon: FileText },
]

const toForm = (user: UserData): Form =>
  Object.fromEntries(fields.map(([key]) => [key, user[key] ?? ''])) as Form

const safeExternalUrl = (value: string) => {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}

const initials = (name: string) => name.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase() || 'IoT'

export function Profile() {
  const user = getStoredUser()
  const qc = useQueryClient()
  const profile = useQuery({
    queryKey: ['iot', 'profile', user?.username],
    queryFn: () => endpoints.profile(user!.username),
    enabled: !!user?.username,
  })
  const [form, setForm] = useState<Form>(empty)
  const [editOpen, setEditOpen] = useState(false)

  useEffect(() => {
    if (profile.data) setForm(toForm(profile.data))
  }, [profile.data])

  const mutation = useMutation({
    mutationFn: (body: Form) => endpoints.updateProfile(profile.data!.id, body),
    onSuccess: async data => {
      setForm(toForm(data))
      saveUser(data)
      setEditOpen(false)
      await qc.invalidateQueries({ queryKey: ['iot', 'profile'] })
      toast.success('Đã lưu hồ sơ')
    },
    onError: () => toast.error('Không lưu được hồ sơ. Kiểm tra lại thông tin.'),
  })

  const set = (key: keyof Form, value: string) => setForm(current => ({ ...current, [key]: value }))
  const restoreSavedProfile = () => {
    if (profile.data) setForm(toForm(profile.data))
    setEditOpen(false)
    toast.success('Đã khôi phục thông tin hồ sơ đã lưu')
  }
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    mutation.mutate(form)
  }

  const currentName = form.fullName || profile.data?.username || 'Người dùng'

  return <>
    <Header fixed><div className='ms-auto'><ThemeSwitch /></div></Header>
    <Main className='flex h-[calc(100svh-2.5rem)] flex-1 flex-col gap-5 overflow-auto'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <h2 className='text-2xl font-bold tracking-tight'>Profile</h2>
        <Button variant='outline' onClick={restoreSavedProfile} disabled={profile.isLoading || !profile.data}>
          <RotateCcw /> Khôi phục mặc định
        </Button>
      </div>

      {profile.isLoading ? <p className='text-muted-foreground'>Đang tải hồ sơ…</p>
        : profile.isError || !profile.data ? <p className='text-destructive'>Không thể tải hồ sơ người dùng.</p>
          : <div className='grid items-stretch gap-5 lg:grid-cols-[0.85fr_1.15fr]'>
            <Card className='relative overflow-hidden gap-0 py-0'>
              <div className='relative h-44 bg-gradient-to-br from-zinc-900 via-zinc-800 to-zinc-950'>
                <Button variant='secondary' className='absolute right-4 top-4 bg-white/10 text-white hover:bg-white/20' onClick={() => setEditOpen(true)}>
                  <Pencil /> Sửa hồ sơ
                </Button>
              </div>
              <div className='px-6 pb-6'>
                <div className='-mt-20 flex justify-center'>
                  <div className='relative flex size-36 items-center justify-center overflow-hidden rounded-full border-4 border-background bg-primary text-3xl font-semibold text-primary-foreground shadow-md'>
                    <span>{initials(currentName)}</span>
                    {form.avatarUrl && <img src={form.avatarUrl} alt={`Ảnh đại diện ${currentName}`} className='absolute inset-0 size-full object-cover' onError={event => { event.currentTarget.style.display = 'none' }} />}
                  </div>
                </div>
                <div className='mt-4 text-center'>
                  <h3 className='text-2xl font-semibold'>{currentName}</h3>
                  <p className='mt-1 text-muted-foreground'>{form.studentCode || 'Chưa cập nhật mã sinh viên'}</p>
                </div>
                <div className='mt-6 border-t pt-2'>
                  <ProfileRow icon={IdCard} label='Mã sinh viên' value={form.studentCode} />
                  <ProfileRow icon={GraduationCap} label='Lớp' value={form.className} />
                  <ProfileRow icon={Mail} label='Email liên hệ' value={form.email} />
                </div>
              </div>
            </Card>

            <Card className='gap-5 py-6'>
              <CardContent className='flex h-full flex-col gap-5'>
                <CardTitle className='text-xl'>Tài nguyên dự án</CardTitle>
                <div className='grid flex-1 gap-4 sm:grid-cols-2'>
                  {resources.map(resource => {
                    const href = safeExternalUrl(form[resource.key] ?? '')
                    const linked = Boolean(href)
                    const Icon = resource.Icon
                    return <div key={resource.key} className='flex min-h-44 flex-col rounded-xl border p-4 shadow-sm'>
                      <div className='flex items-start gap-3'>
                        <div className='flex size-12 shrink-0 items-center justify-center rounded-xl border bg-background text-muted-foreground'>
                          <Icon className='size-6' />
                        </div>
                        <div className='min-w-0 flex-1'>
                          <div className='flex flex-wrap items-center gap-2'>
                            <h3 className='font-semibold'>{resource.title}</h3>
                            <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${linked ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300' : 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'}`}>
                              {linked ? 'Đã liên kết' : 'Chưa liên kết'}
                            </span>
                          </div>
                          <p className='mt-1 text-sm text-muted-foreground'>{resource.description}</p>
                        </div>
                      </div>
                      <div className='mt-auto pt-5'>
                        {linked
                          ? <Button variant='outline' className='w-full' onClick={() => window.open(href, '_blank', 'noopener,noreferrer')}><ExternalLink /> Mở tài nguyên</Button>
                          : <Button variant='secondary' className='w-full' onClick={() => setEditOpen(true)}><ExternalLink /> Cấu hình ngay</Button>}
                      </div>
                    </div>
                  })}
                </div>
              </CardContent>
            </Card>
          </div>}
    </Main>

    <Dialog open={editOpen} onOpenChange={setEditOpen}>
      <DialogContent className='max-h-[90svh] overflow-y-auto sm:max-w-2xl'>
        <DialogHeader>
          <DialogTitle>Sửa hồ sơ</DialogTitle>
          <DialogDescription>Cập nhật thông tin cá nhân và liên kết tài nguyên dự án.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className='grid gap-5'>
          <div className='grid gap-3 sm:grid-cols-2'>
            {fields.slice(0, 5).map(([key, label]) => <div key={key} className='grid gap-2'>
              <Label htmlFor={`profile-${key}`}>{label}</Label>
              <Input
                id={`profile-${key}`}
                type={key === 'email' ? 'email' : key === 'avatarUrl' ? 'url' : 'text'}
                required={key === 'fullName'}
                value={form[key] ?? ''}
                onChange={event => set(key, event.target.value)}
              />
            </div>)}
          </div>
          <div className='grid gap-3 sm:grid-cols-2'>
            {fields.slice(5).map(([key, label]) => <div key={key} className='grid gap-2'>
              <Label htmlFor={`profile-${key}`}>{label}</Label>
              <Input id={`profile-${key}`} type='url' value={form[key] ?? ''} onChange={event => set(key, event.target.value)} placeholder='https://' />
            </div>)}
          </div>
          <DialogFooter>
            <Button type='button' variant='outline' onClick={() => { if (profile.data) setForm(toForm(profile.data)); setEditOpen(false) }}>Hủy</Button>
            <Button type='submit' disabled={mutation.isPending}>{mutation.isPending ? 'Đang lưu…' : 'Lưu hồ sơ'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </>
}

function ProfileRow({ icon: Icon, label, value }: { icon: ElementType<{ className?: string }>; label: string; value: string | null }) {
  return <div className='flex min-h-12 items-center gap-3 border-b last:border-b-0'>
    <Icon className='size-5 shrink-0 text-muted-foreground' />
    <span className='min-w-32 text-xs font-semibold uppercase tracking-wide text-muted-foreground'>{label}</span>
    <span className='ml-auto break-all text-right text-sm font-medium'>{value || '—'}</span>
  </div>
}
