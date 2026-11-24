import { AuthLayout } from '../components/AuthLayout'
import { AuthLoginForm } from '../components/AuthLoginForm'

const demoAccounts = [
  {
    role: 'CUSTOMER',
    label: 'Customer',
    email: 'customer@borderlessos.local',
  },
]

export function LoginPage() {
  return (
    <AuthLayout>
      <AuthLoginForm
        icon="users"
        eyebrow="Customer access"
        heading="Welcome back"
        description="Sign in to send transfers and track their progress."
        demoAccounts={demoAccounts}
        switchPrompt="New to BorderlessOS?"
        switchLabel="Create a customer account"
        switchTo="/signup/customer"
      />
    </AuthLayout>
  )
}
