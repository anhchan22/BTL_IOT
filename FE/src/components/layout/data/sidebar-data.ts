import {
  Clock3,
  LayoutDashboard,
  TableProperties,
  UserRound,
  AudioWaveform,
} from 'lucide-react'
import { type SidebarData } from '../types'

export const sidebarData: SidebarData = {
  user: {
    name: 'Admin',
    email: 'admin@classroom-iot.local',
    avatar: '/avatars/shadcn.jpg',
  },
  teams: [
    {
      name: 'IoT Device',
      logo: AudioWaveform,
      plan: 'Realtime',
    }
  ],
  navGroups: [
    {
      title: 'Hệ thống',
      items: [
        {
          title: 'Dashboard',
          url: '/',
          icon: LayoutDashboard,
        },
        {
          title: 'Data Sensor',
          url: '/data-sensor',
          icon: TableProperties,
        },
        {
          title: 'History',
          url: '/history',
          icon: Clock3,
        },
        {
          title: 'Profile',
          url: '/profile',
          icon: UserRound,
        },
      ],
    },
  ],
}
