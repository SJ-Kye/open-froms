import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 서버가 /api 요청을 백엔드로 넘겨줍니다. 브라우저 입장에서는 5173 한 곳만 호출하는
    // 동일 출처가 되므로 CORS 설정이 아예 필요 없습니다(백엔드에 CORS 설정을 두지 않은 이유).
    // 배포 시에는 리버스 프록시가 같은 역할을 하며, 필요하면 VITE_API_BASE_URL 로 절대 URL 을
    // 지정해 우회할 수도 있습니다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
