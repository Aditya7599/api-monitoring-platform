import { useEffect } from 'react'
import { useRouter } from 'next/router'
import { isLoggedIn } from '../lib/api'

export default function Home() {
  const router = useRouter()
  
  useEffect(() => {
    if (isLoggedIn()) {
      router.push('/dashboard')
    } else {
      router.push('/login')
    }
  }, [])
  
  return <div className="min-h-screen flex items-center justify-center">Loading...</div>
}

