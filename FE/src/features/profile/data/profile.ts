export type ProjectLink = {
  type: 'github' | 'figma' | 'postman' | 'report'
  label: string
  description: string
  url: string
}

export type ProfileInfo = {
  fullName: string
  studentCode: string
  className: string
  email: string
  avatarUrl: string
  projectLinks: ProjectLink[]
}

export const profileInfo: ProfileInfo = {
  fullName: '',
  studentCode: '',
  className: '',
  email: '',
  avatarUrl: '',
  projectLinks: [
    { type: 'github', label: 'GitHub', description: 'Mã nguồn dự án', url: '' },
    { type: 'figma', label: 'Figma', description: 'Thiết kế giao diện', url: '' },
    { type: 'postman', label: 'Postman', description: 'Tài liệu API', url: '' },
    { type: 'report', label: 'Báo cáo', description: 'Báo cáo dự án', url: '' },
  ],
}

const storageKey = 'shadcn_admin_profile'

export function getProfile(): ProfileInfo {
  if (typeof window === 'undefined') return profileInfo
  try {
    const value = window.localStorage.getItem(storageKey)
    return value ? { ...profileInfo, ...JSON.parse(value) } : profileInfo
  } catch {
    return profileInfo
  }
}

export function saveProfile(profile: ProfileInfo) {
  window.localStorage.setItem(storageKey, JSON.stringify(profile))
}
