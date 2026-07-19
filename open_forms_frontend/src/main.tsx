import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { queryClient } from './lib/queryClient'
import AppRouter from './routes/AppRouter'
import './index.css'

/*
 * 조립 순서에 이유가 있습니다. AuthProvider 가 로그아웃 시 서버 상태 캐시를 비우므로
 * QueryClientProvider 가 바깥에 있어야 하고, 라우터는 인증 상태를 읽어 리다이렉트를 결정하므로
 * AuthProvider 안쪽에 있어야 합니다.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <AppRouter />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
