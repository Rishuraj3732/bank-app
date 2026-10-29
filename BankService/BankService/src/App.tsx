import { useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AdminLayout from './components/AdminLayout'
import AuthPage from './pages/AuthPage'
import CustomersPage, { NewCustomerPage } from './pages/CustomersPage'
import OverviewPage from './pages/OverviewPage'
import SavingsPage, { NewSavingsPage } from './pages/SavingsPage'
import type { Admin } from './types/banking'

function App() {
  const [admin, setAdmin] = useState<Admin | null>(null)

  return (
    <Routes>
      <Route
        path="/login"
        element={admin ? <Navigate to="/" replace /> : <AuthPage mode="login" onSignIn={setAdmin} />}
      />
      <Route
        path="/register"
        element={admin ? <Navigate to="/" replace /> : <AuthPage mode="register" onSignIn={setAdmin} />}
      />
      <Route
        path="/"
        element={
          admin ? (
            <AdminLayout
              admin={admin}
              onSignOut={() => setAdmin(null)}
            />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      >
        <Route index element={<OverviewPage />} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="customers/new" element={<NewCustomerPage />} />
        <Route path="savings" element={<SavingsPage />} />
        <Route path="savings/new" element={<NewSavingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to={admin ? '/' : '/login'} replace />} />
    </Routes>
  )
}

export default App

export type { Admin }
