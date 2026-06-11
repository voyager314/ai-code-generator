import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserLoginVO } from '@/types';

interface UserStore {
  user: UserLoginVO | null;
  setUser: (user: UserLoginVO | null) => void;
  isAdmin: () => boolean;
}

export const useUserStore = create<UserStore>()(
  persist(
    (set, get) => ({
      user: null,
      setUser: (user) => set({ user }),
      isAdmin: () => get().user?.userRole === 'admin',
    }),
    { name: 'user-storage' }
  )
);
