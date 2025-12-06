import { useState, useEffect } from 'react'
import { useRouter } from 'next/router'
import api, { isLoggedIn, logout, getUsername } from '../lib/api'

export default function Dashboard() {
  const router = useRouter()
  const [stats, setStats] = useState(null)
  const [logs, setLogs] = useState([])
  const [alerts, setAlerts] = useState([])
  const [issues, setIssues] = useState([])
  const [loading, setLoading] = useState(true)
  const [filters, setFilters] = useState({ service: '', endpoint: '', status: '', dateFrom: '', dateTo: '', slow: false, broken: false, rateLimited: false })
  const [activeTab, setActiveTab] = useState('logs')

  useEffect(() => {
    if (!isLoggedIn()) {
      router.push('/login')
      return
    }
    fetchAll()
  }, [])

  const fetchAll = async () => {
    setLoading(true)
    try {
      const [statsRes, logsRes, alertsRes, issuesRes] = await Promise.all([
        api.get('/stats/dashboard'),
        api.get('/logs', { params: { limit: 500 } }),
        api.get('/alerts', { params: { limit: 100 } }),
        api.get('/issues')
      ])
      setStats(statsRes.data)
      setLogs(logsRes.data)
      setAlerts(alertsRes.data)
      setIssues(issuesRes.data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    router.push('/login')
  }

  const resolveIssue = async (issueId) => {
    try {
      await api.post('/issues/resolve', { issueId, resolvedBy: getUsername() })
      alert('Issue resolved successfully!')
      fetchAll()
    } catch (err) {
      console.error('Resolve error:', err.response?.data)
      alert(err.response?.data?.message || err.response?.data?.error || 'Failed to resolve issue')
    }
  }

  const sendTestLog = async (statusCode = 200, latencyMs = 50) => {
    try {
      await api.post('/collect/log', {
        endpoint: '/api/test',
        method: 'GET',
        timestamp: new Date().toISOString(),
        requestSize: 128,
        responseSize: 256,
        statusCode,
        latencyMs,
        serviceName: 'client-service',
        rateLimitHit: false
      })
      fetchAll()
    } catch (err) {
      console.error(err)
    }
  }

  const filteredLogs = logs.filter(log => {
    if (filters.service && log.serviceName !== filters.service) return false
    if (filters.endpoint && !log.endpoint.toLowerCase().includes(filters.endpoint.toLowerCase())) return false
    if (filters.status && log.statusCode !== parseInt(filters.status)) return false
    if (filters.dateFrom && new Date(log.timestamp) < new Date(filters.dateFrom)) return false
    if (filters.dateTo && new Date(log.timestamp) > new Date(filters.dateTo + 'T23:59:59')) return false
    if (filters.slow && log.latencyMs <= 500) return false
    if (filters.broken && log.statusCode < 500) return false
    if (filters.rateLimited && !log.rateLimitHit) return false
    return true
  })

  if (loading) {
    return (
      <div className="min-h-screen bg-animated flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-16 h-16 border-4 border-violet-500/30 border-t-violet-500 rounded-full animate-spin"></div>
          <p className="text-zinc-500">Loading dashboard...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-white">
      {/* Ambient background */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-violet-600/10 rounded-full blur-3xl"></div>
        <div className="absolute top-1/2 -left-40 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl"></div>
      </div>

      {/* Header */}
      <header className="glass sticky top-0 z-50 border-b border-white/5">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div>
                <h1 className="text-xl font-bold bg-gradient-to-r from-white to-zinc-400 bg-clip-text text-transparent">API Monitor</h1>
                <p className="text-xs text-zinc-500">Observability Dashboard</p>
              </div>
            </div>
            <div className="flex items-center gap-6">
              <button onClick={fetchAll} className="text-zinc-400 hover:text-white transition p-2 hover:bg-white/5 rounded-lg" title="Refresh">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-emerald-400 to-cyan-400 flex items-center justify-center text-sm font-bold text-black">
                  {getUsername()?.charAt(0).toUpperCase()}
                </div>
                <span className="text-zinc-400 text-sm">{getUsername()}</span>
              </div>
              <button onClick={handleLogout} className="text-zinc-400 hover:text-red-400 transition text-sm flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 relative z-10">
        {/* Stats Grid */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
          <StatCard icon="📊" title="Total Logs" value={stats?.totalLogs || 0} gradient="from-blue-500 to-cyan-500" />
          <StatCard icon="🐢" title="Slow APIs" value={stats?.slowApiCount || 0} gradient="from-amber-500 to-orange-500" subtitle=">500ms" />
          <StatCard icon="💥" title="Broken APIs" value={stats?.brokenApiCount || 0} gradient="from-red-500 to-pink-500" subtitle="5xx errors" />
          <StatCard icon="🚦" title="Rate Limits" value={stats?.rateLimitHits || 0} gradient="from-violet-500 to-purple-500" />
          <StatCard icon="⚡" title="Avg Latency" value={`${(stats?.avgLatencyMs || 0).toFixed(0)}ms`} gradient="from-emerald-500 to-teal-500" />
          <StatCard icon="🎫" title="Open Issues" value={stats?.openIssues || 0} gradient="from-fuchsia-500 to-pink-500" />
        </div>

        {/* Quick Actions */}
        <div className="glass rounded-2xl p-6 mb-8">
          <h3 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider mb-4">Test Controls</h3>
          <div className="flex flex-wrap gap-3">
            <button onClick={() => sendTestLog(200, 50)} className="px-4 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 transition flex items-center gap-2 text-sm font-medium">
              <span className="w-2 h-2 rounded-full bg-emerald-400"></span> Send OK (200)
            </button>
            <button onClick={() => sendTestLog(500, 100)} className="px-4 py-2 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500/20 transition flex items-center gap-2 text-sm font-medium">
              <span className="w-2 h-2 rounded-full bg-red-400"></span> Send Error (500)
            </button>
            <button onClick={() => sendTestLog(200, 750)} className="px-4 py-2 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 hover:bg-amber-500/20 transition flex items-center gap-2 text-sm font-medium">
              <span className="w-2 h-2 rounded-full bg-amber-400"></span> Send Slow (750ms)
            </button>
          </div>
        </div>

        {/* Top Slow Endpoints */}
        {stats?.topSlowEndpoints?.length > 0 && (
          <div className="glass rounded-2xl p-6 mb-8">
            <h3 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider mb-4">🐢 Slowest Endpoints</h3>
            <div className="grid gap-2">
              {stats.topSlowEndpoints.map((ep, i) => (
                <div key={i} className="flex items-center justify-between bg-white/5 rounded-xl px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span className="w-6 h-6 rounded-lg bg-amber-500/20 text-amber-400 flex items-center justify-center text-xs font-bold">
                      {i + 1}
                    </span>
                    <code className="text-amber-300 text-sm">{ep.endpoint}</code>
                  </div>
                  <span className="text-zinc-400 text-sm font-mono">{ep.avgLatencyMs.toFixed(0)}ms</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Error Rate Graph */}
        {stats?.errorRateByEndpoint?.length > 0 && (
          <div className="glass rounded-2xl p-6 mb-8">
            <h3 className="text-sm font-semibold text-zinc-400 uppercase tracking-wider mb-4">📊 Error Rate by Endpoint</h3>
            <div className="space-y-3">
              {stats.errorRateByEndpoint.map((ep, i) => (
                <div key={i} className="space-y-1">
                  <div className="flex justify-between text-sm">
                    <code className="text-zinc-300">{ep.endpoint}</code>
                    <span className={`font-mono ${ep.errorRate > 10 ? 'text-red-400' : ep.errorRate > 5 ? 'text-amber-400' : 'text-emerald-400'}`}>
                      {ep.errorRate.toFixed(1)}%
                    </span>
                  </div>
                  <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className={`h-full rounded-full transition-all ${
                        ep.errorRate > 10 ? 'bg-gradient-to-r from-red-500 to-red-600' : 
                        ep.errorRate > 5 ? 'bg-gradient-to-r from-amber-500 to-orange-500' : 
                        'bg-gradient-to-r from-emerald-500 to-teal-500'
                      }`}
                      style={{ width: `${Math.min(ep.errorRate, 100)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className="flex gap-2 mb-6">
          {[
            { id: 'logs', label: 'Logs', icon: '📋' },
            { id: 'alerts', label: 'Alerts', icon: '🔔' },
            { id: 'issues', label: 'Issues', icon: '🎫' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-5 py-3 rounded-xl font-medium text-sm transition flex items-center gap-2 ${
                activeTab === tab.id 
                  ? 'bg-violet-500/20 text-violet-300 border border-violet-500/30' 
                  : 'text-zinc-500 hover:text-zinc-300 hover:bg-white/5'
              }`}
            >
              <span>{tab.icon}</span>
              {tab.label}
              {tab.id === 'alerts' && alerts.length > 0 && (
                <span className="w-5 h-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center">{alerts.length}</span>
              )}
            </button>
          ))}
        </div>

        {/* Logs Tab */}
        {activeTab === 'logs' && (
          <div className="glass rounded-2xl overflow-hidden">
            {/* Filters */}
            <div className="p-4 border-b border-white/5 flex flex-wrap gap-3 items-center">
              <select
                value={filters.service}
                onChange={(e) => setFilters({...filters, service: e.target.value})}
                className="input-modern rounded-lg px-3 py-2 text-sm"
              >
                <option value="">All Services</option>
                {[...new Set(logs.map(l => l.serviceName))].map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <input
                type="text"
                placeholder="Endpoint"
                value={filters.endpoint}
                onChange={(e) => setFilters({...filters, endpoint: e.target.value})}
                className="input-modern rounded-lg px-3 py-2 text-sm w-32"
              />
              <input
                type="number"
                placeholder="Status code"
                value={filters.status}
                onChange={(e) => setFilters({...filters, status: e.target.value})}
                className="input-modern rounded-lg px-3 py-2 text-sm w-28"
              />
              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <span>From:</span>
                <input
                  type="date"
                  value={filters.dateFrom}
                  onChange={(e) => setFilters({...filters, dateFrom: e.target.value})}
                  className="input-modern rounded-lg px-2 py-2 text-sm"
                />
              </div>
              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <span>To:</span>
                <input
                  type="date"
                  value={filters.dateTo}
                  onChange={(e) => setFilters({...filters, dateTo: e.target.value})}
                  className="input-modern rounded-lg px-2 py-2 text-sm"
                />
              </div>
              <label className="flex items-center gap-2 text-sm text-zinc-400 cursor-pointer hover:text-zinc-300">
                <input type="checkbox" checked={filters.slow} onChange={(e) => setFilters({...filters, slow: e.target.checked})} className="accent-amber-500" />
                Slow (&gt;500ms)
              </label>
              <label className="flex items-center gap-2 text-sm text-zinc-400 cursor-pointer hover:text-zinc-300">
                <input type="checkbox" checked={filters.broken} onChange={(e) => setFilters({...filters, broken: e.target.checked})} className="accent-red-500" />
                Broken (5xx)
              </label>
              <label className="flex items-center gap-2 text-sm text-zinc-400 cursor-pointer hover:text-zinc-300">
                <input type="checkbox" checked={filters.rateLimited} onChange={(e) => setFilters({...filters, rateLimited: e.target.checked})} className="accent-violet-500" />
                Rate Limited
              </label>
            </div>
            
            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full table-modern">
                <thead>
                  <tr>
                    <th className="px-4 py-4 text-left">Endpoint</th>
                    <th className="px-4 py-4 text-left">Method</th>
                    <th className="px-4 py-4 text-left">Status</th>
                    <th className="px-4 py-4 text-left">Latency</th>
                    <th className="px-4 py-4 text-left">Service</th>
                    <th className="px-4 py-4 text-left">Rate Limited</th>
                    <th className="px-4 py-4 text-left">Time</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLogs.slice(0, 50).map((log, i) => (
                    <tr key={i} className="border-t border-white/5">
                      <td className="px-4 py-3"><code className="text-cyan-400 text-sm">{log.endpoint}</code></td>
                      <td className="px-4 py-3">
                        <span className={`badge px-2 py-1 rounded ${
                          log.method === 'GET' ? 'bg-emerald-500/20 text-emerald-400' :
                          log.method === 'POST' ? 'bg-blue-500/20 text-blue-400' :
                          log.method === 'PUT' ? 'bg-amber-500/20 text-amber-400' :
                          'bg-red-500/20 text-red-400'
                        }`}>{log.method}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`badge px-2 py-1 rounded ${
                          log.statusCode >= 500 ? 'bg-red-500/20 text-red-400' : 
                          log.statusCode >= 400 ? 'bg-amber-500/20 text-amber-400' : 
                          'bg-emerald-500/20 text-emerald-400'
                        }`}>{log.statusCode}</span>
                      </td>
                      <td className={`px-4 py-3 font-mono text-sm ${log.latencyMs > 500 ? 'text-amber-400' : 'text-zinc-400'}`}>
                        {log.latencyMs}ms
                      </td>
                      <td className="px-4 py-3 text-zinc-500 text-sm">{log.serviceName}</td>
                      <td className="px-4 py-3">
                        {log.rateLimitHit ? (
                          <span className="badge bg-red-500/20 text-red-400 px-2 py-1 rounded">YES</span>
                        ) : (
                          <span className="text-zinc-600">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-zinc-600 text-xs">{new Date(log.timestamp).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="p-4 text-zinc-600 text-sm border-t border-white/5">
              Showing {Math.min(50, filteredLogs.length)} of {filteredLogs.length} logs
            </div>
          </div>
        )}

        {/* Alerts Tab */}
        {activeTab === 'alerts' && (
          <div className="space-y-3">
            {alerts.length === 0 ? (
              <div className="glass rounded-2xl p-12 text-center">
                <div className="text-4xl mb-4">✨</div>
                <p className="text-zinc-500">No alerts — everything looks good!</p>
              </div>
            ) : (
              alerts.map((alert, i) => (
                <div key={i} className={`glass rounded-xl p-5 border-l-4 ${
                  alert.type === 'SERVER_ERROR' ? 'border-red-500' :
                  alert.type === 'HIGH_LATENCY' ? 'border-amber-500' :
                  'border-violet-500'
                }`} style={{ animationDelay: `${i * 0.05}s` }}>
                  <div className="flex justify-between items-start">
                    <div className="flex items-start gap-4">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg ${
                        alert.type === 'SERVER_ERROR' ? 'bg-red-500/20' :
                        alert.type === 'HIGH_LATENCY' ? 'bg-amber-500/20' :
                        'bg-violet-500/20'
                      }`}>
                        {alert.type === 'SERVER_ERROR' && '💥'}
                        {alert.type === 'HIGH_LATENCY' && '🐢'}
                        {alert.type === 'RATE_LIMIT' && '🚦'}
                      </div>
                      <div>
                        <span className={`text-sm font-semibold ${
                          alert.type === 'SERVER_ERROR' ? 'text-red-400' :
                          alert.type === 'HIGH_LATENCY' ? 'text-amber-400' :
                          'text-violet-400'
                        }`}>{alert.type.replace('_', ' ')}</span>
                        <p className="text-zinc-300 text-sm mt-1">{alert.message}</p>
                        <p className="text-zinc-600 text-xs mt-2 flex items-center gap-2">
                          <code className="text-cyan-500">{alert.serviceName}</code>
                          <span>→</span>
                          <code className="text-zinc-400">{alert.endpoint}</code>
                        </p>
                      </div>
                    </div>
                    <span className="text-zinc-600 text-xs">{new Date(alert.timestamp).toLocaleString()}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* Issues Tab */}
        {activeTab === 'issues' && (
          <div className="space-y-3">
            {issues.length === 0 ? (
              <div className="glass rounded-2xl p-12 text-center">
                <div className="text-4xl mb-4">🎉</div>
                <p className="text-zinc-500">No issues reported</p>
              </div>
            ) : (
              issues.map((issue, i) => (
                <div key={i} className={`glass rounded-xl p-5 ${issue.resolved ? 'opacity-60' : ''}`}>
                  <div className="flex justify-between items-start">
                    <div className="flex items-start gap-4">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        issue.resolved ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'
                      }`}>
                        {issue.resolved ? '✓' : '!'}
                      </div>
                      <div>
                        <div className="flex items-center gap-3">
                          <span className="font-semibold">{issue.title}</span>
                          <span className={`badge px-2 py-1 rounded ${
                            issue.resolved ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'
                          }`}>{issue.status || (issue.resolved ? 'RESOLVED' : 'OPEN')}</span>
                        </div>
                        <p className="text-zinc-500 text-sm mt-1">{issue.description}</p>
                      </div>
                    </div>
                    {!issue.resolved && (
                      <button
                        onClick={() => resolveIssue(issue.id)}
                        className="px-4 py-2 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 transition text-sm font-medium"
                      >
                        Resolve
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </main>
    </div>
  )
}

function StatCard({ icon, title, value, gradient, subtitle }) {
  return (
    <div className="stat-card glass rounded-2xl p-5 group cursor-default">
      <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center text-lg mb-3 group-hover:scale-110 transition`}>
        {icon}
      </div>
      <div className="text-2xl font-bold text-white">{value}</div>
      <div className="text-xs text-zinc-500 mt-1">{title}</div>
      {subtitle && <div className="text-[10px] text-zinc-600 mt-0.5">{subtitle}</div>}
    </div>
  )
}
