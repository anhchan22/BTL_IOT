import { Thermometer } from 'lucide-react'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { sidebarData } from './data/sidebar-data'
import { NavGroup } from './nav-group'
import { NavUser } from './nav-user'
import { getStoredUser } from '@/lib/api'

export function AppSidebar() {
  const storedUser = getStoredUser()
  return (
    <Sidebar collapsible='icon' variant='sidebar'>
      <SidebarHeader>
        <div className='flex items-center gap-2 px-2 py-1 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0'>
          <div className='flex size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground group-data-[collapsible=icon]:hidden'>
            <Thermometer className='size-4' />
          </div>
          <span className='truncate font-semibold group-data-[collapsible=icon]:hidden'>
            Classroom IoT
          </span>
          <SidebarTrigger variant='outline' className='ms-auto size-8 shrink-0 group-data-[collapsible=icon]:mx-auto' />
        </div>
      </SidebarHeader>
      <SidebarContent>
        {sidebarData.navGroups.map((props) => (
          <NavGroup key={props.title} {...props} />
        ))}
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={{ name: storedUser?.fullName || sidebarData.user.name, email: storedUser?.email || storedUser?.username || sidebarData.user.email, avatar: storedUser?.avatarUrl || sidebarData.user.avatar }} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
