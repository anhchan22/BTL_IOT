import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { SidebarMenu, SidebarMenuItem } from '@/components/ui/sidebar'
import { Button } from '@/components/ui/button'
import { LogOut } from 'lucide-react'
import { clearSession } from '@/lib/api'
import { useNavigate } from '@tanstack/react-router'

type NavUserProps = {
  user: { name: string; email: string; avatar: string }
}

export function NavUser({ user }: NavUserProps) {
  const navigate = useNavigate()
  return (
    <SidebarMenu>
      <SidebarMenuItem>
          <div className='flex items-center gap-2 px-2 py-2 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0'>
          <Avatar className='h-8 w-8 shrink-0 rounded-lg group-data-[collapsible=icon]:hidden'>
            <AvatarImage src={user.avatar} alt={user.name} />
            <AvatarFallback className='rounded-lg'>IoT</AvatarFallback>
          </Avatar>
          <div className='grid min-w-0 text-start text-sm leading-tight group-data-[collapsible=icon]:hidden'>
            <span className='truncate font-semibold'>{user.name}</span>
            <span className='truncate text-xs text-muted-foreground'>
              {user.email}
            </span>
          </div>
          <Button variant='ghost' size='icon' className='size-8 shrink-0 group-data-[collapsible=icon]:mx-auto' title='Đăng xuất' aria-label='Đăng xuất' onClick={() => { clearSession(); void navigate({ to: '/login', search: { redirect: '/' } }) }}><LogOut className='size-4' /></Button>
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
