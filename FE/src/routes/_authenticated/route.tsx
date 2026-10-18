import { createFileRoute } from '@tanstack/react-router'
import { AuthenticatedLayout } from '@/components/layout/authenticated-layout'
import { redirect } from '@tanstack/react-router'
import { getToken } from '@/lib/api'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: ({ location }) => {
    if (!getToken()) {
      const redirectTo = `${location.pathname}${location.search}${location.hash}`
      throw redirect({ to: '/login', search: { redirect: redirectTo } })
    }
  },
  component: AuthenticatedLayout,
})
