import { useState, useEffect } from 'react'
import { Layout, Menu, Badge, Avatar, Dropdown, Button, Select } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  BookOutlined, BulbOutlined, StarOutlined, NodeIndexOutlined,
  BellOutlined, UserOutlined, LogoutOutlined, BarChartOutlined,
  EditOutlined, HomeOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../store/authStore'
import { api } from '../api'

const { Header, Content, Sider } = Layout

export default function AppLayout() {
  const { t, i18n } = useTranslation()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    api.notification.unreadCount().then(setUnread).catch(() => {})
    const timer = setInterval(() => {
      api.notification.unreadCount().then(setUnread).catch(() => {})
    }, 30000)
    return () => clearInterval(timer)
  }, [])

  const isAdmin = user?.role === 'ADMIN'
  const isTeacher = user?.role === 'TEACHER' || isAdmin

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: t('nav.home') },
    { key: '/courses', icon: <BookOutlined />, label: t('nav.courses') },
    { key: '/recommend', icon: <StarOutlined />, label: t('nav.recommend') },
    { key: '/knowledge', icon: <NodeIndexOutlined />, label: t('nav.knowledge') },
    ...(isTeacher ? [
      { key: '/grade', icon: <EditOutlined />, label: t('nav.grade') },
    ] : []),
    ...(isAdmin ? [
      { key: '/admin', icon: <BarChartOutlined />, label: t('nav.admin') },
    ] : []),
  ]

  const userMenu = [
    { key: 'profile', icon: <UserOutlined />, label: t('nav.profile') },
    { key: 'logout', icon: <LogoutOutlined />, label: t('nav.logout'), danger: true },
  ]

  const handleUserMenu = ({ key }) => {
    if (key === 'logout') { logout(); navigate('/login') }
    else if (key === 'profile') navigate('/profile')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px' }}>
        <div style={{ color: '#fff', fontSize: 18, fontWeight: 700 }}>{t('app.name')}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Select
            value={i18n.language.startsWith('zh') ? 'zh' : 'en'}
            onChange={lang => i18n.changeLanguage(lang)}
            options={[{ value: 'zh', label: '中文' }, { value: 'en', label: 'EN' }]}
            size="small"
            style={{ width: 70 }}
          />
          <Badge count={unread} size="small">
            <BellOutlined
              style={{ color: '#fff', fontSize: 18, cursor: 'pointer' }}
              onClick={() => navigate('/notifications')}
            />
          </Badge>
          <Dropdown menu={{ items: userMenu, onClick: handleUserMenu }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', color: '#fff' }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <span>{user?.nickname || user?.username}</span>
            </div>
          </Dropdown>
        </div>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        <Layout style={{ padding: '24px' }}>
          <Content style={{ background: '#fff', padding: 24, borderRadius: 8, minHeight: 280 }}>
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  )
}
