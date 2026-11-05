import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { HomeRedirect, ProtectedRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { CustomerDashboard } from './pages/customer/CustomerDashboard'
import { BeneficiariesPage } from './pages/customer/BeneficiariesPage'
import { NewTransferPage } from './pages/customer/NewTransferPage'
import { CustomerTransferDetailPage } from './pages/customer/CustomerTransferDetailPage'
import { OperationsDashboard } from './pages/operations/OperationsDashboard'
import { OperationsDetailPage } from './pages/operations/OperationsDetailPage'
import { ComplianceDashboard } from './pages/compliance/ComplianceDashboard'
import { ComplianceDetailPage } from './pages/compliance/ComplianceDetailPage'
import { AdminDashboard } from './pages/admin/AdminDashboard'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/" element={<HomeRedirect />} />

      <Route element={<Layout />}>
        <Route element={<ProtectedRoute allow={['CUSTOMER']} />}>
          <Route path="/customer" element={<CustomerDashboard />} />
          <Route
            path="/customer/beneficiaries"
            element={<BeneficiariesPage />}
          />
          <Route
            path="/customer/transfers/new"
            element={<NewTransferPage />}
          />
          <Route
            path="/customer/transfers/:id"
            element={<CustomerTransferDetailPage />}
          />
        </Route>

        <Route element={<ProtectedRoute allow={['OPERATIONS']} />}>
          <Route path="/operations" element={<OperationsDashboard />} />
          <Route
            path="/operations/transfers/:id"
            element={<OperationsDetailPage />}
          />
        </Route>

        <Route element={<ProtectedRoute allow={['COMPLIANCE']} />}>
          <Route path="/compliance" element={<ComplianceDashboard />} />
          <Route
            path="/compliance/holds/:id"
            element={<ComplianceDetailPage />}
          />
        </Route>

        <Route element={<ProtectedRoute allow={['ADMIN']} />}>
          <Route path="/admin" element={<AdminDashboard />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
