import { useEffect, useState } from 'react'
import { List, Button, Tag, Typography, Empty } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

const typeColor = { GRADE_DONE: 'green', SECURITY_ALERT: 'red', SYSTEM: 'blue' }

const getNavTarget = (item) => {
  if (item.type === 'GRADE_DONE') return '/history'
  if (item.type === 'SECURITY_ALERT') return '/profile'
  return null
}

export default function NotificationPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)

  const load = (p = 0) => {
    api.notification.list(p, 20).then(data => {
      setItems(data?.content || [])
      setTotal(data?.totalElements || 0)
      setPage(p)
    })
  }

  useEffect(() => { load() }, [])

  const markAll = () => {
    api.notification.markAllRead().then(() => load())
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('notification.title')}</Typography.Title>
        <Button onClick={markAll}>{t('notification.markAllRead')}</Button>
      </div>
      <List
        dataSource={items}
        locale={{ emptyText: <Empty description={t('notification.empty')} /> }}
        pagination={{ total, pageSize: 20, current: page + 1, onChange: p => load(p - 1) }}
        renderItem={item => {
          const target = getNavTarget(item)
          return (
            <List.Item
              key={item.id}
              style={{
                background: item.isRead ? 'transparent' : '#e6f4ff',
                padding: '12px 16px',
                borderRadius: 8,
                marginBottom: 8,
                cursor: target ? 'pointer' : 'default',
              }}
              onClick={target ? () => navigate(target) : undefined}
            >
              <List.Item.Meta
                avatar={<BellOutlined style={{ fontSize: 20, color: typeColor[item.type] || '#666' }} />}
                title={
                  <span>
                    <Tag color={typeColor[item.type]}>{t(`notification.${item.type}`)}</Tag>
                    {item.title}
                    {!item.isRead && <Tag color="blue" style={{ marginLeft: 8 }}>新</Tag>}
                  </span>
                }
                description={
                  <span>
                    {item.content}
                    <span style={{ marginLeft: 12, color: '#aaa', fontSize: 12 }}>
                      {new Date(item.createdAt).toLocaleString()}
                    </span>
                  </span>
                }
              />
            </List.Item>
          )
        }}
      />
    </div>
  )
}
