import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useUserStore } from '@/store/user';
import Login from '@/pages/Login';
import Home from '@/pages/Home';
import AppList from '@/pages/AppList';
import AppChat from '@/pages/AppChat';
import Admin from '@/pages/Admin';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const user = useUserStore((s) => s.user);
  return user ? <>{children}</> : <Navigate to="/login" />;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const isAdmin = useUserStore((s) => s.isAdmin());
  return isAdmin ? <>{children}</> : <Navigate to="/apps" />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
        <Route path="/apps" element={<ProtectedRoute><AppList /></ProtectedRoute>} />
        <Route path="/app/:id" element={<ProtectedRoute><AppChat /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute><AdminRoute><Admin /></AdminRoute></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  );
}
