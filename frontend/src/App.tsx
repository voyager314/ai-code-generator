import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { userApi } from '@/api';
import { useUserStore } from '@/store/user';
import Login from '@/pages/Login';
import Home from '@/pages/Home';
import AppList from '@/pages/AppList';
import AppChat from '@/pages/AppChat';
import Admin from '@/pages/Admin';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const user = useUserStore((s) => s.user);
  const setUser = useUserStore((s) => s.setUser);
  const [checking, setChecking] = useState(Boolean(user));

  useEffect(() => {
    let cancelled = false;

    if (!user) {
      setChecking(false);
      return;
    }

    setChecking(true);
    userApi.getLoginUser()
      .then((res) => {
        if (!cancelled) {
          setUser(res.data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setChecking(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [setUser, user?.id]);

  if (checking) {
    return null;
  }

  return user ? <>{children}</> : <Navigate to="/login" replace />;
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
