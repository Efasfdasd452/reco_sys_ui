import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Button, Select, message, Popconfirm, Typography, Tabs } from 'antd'
import { UserOutlined, ReloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

export default function AdminPage() {
  const { t } = useTranslation()
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])

  useEffect(() => {
    api.admin.statistics().then(setStats).catch(() => {})
    api.admin.users().then(setUsers).catch(() => {})
  }, [])

  const changeRole = async (userId, role) => {
    try {
      await api.admin.setRole(userId, role)
      message.success(t('common.success'))
      api.admin.users().then(setUsers)
    } catch {}
  }

  const retrain = async () => {
    try {
      await api.admin.retrain()
      message.success('重训练任务已触发')
    } catch {}
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: t('auth.username'), dataIndex: 'username' },
    { title: t('auth.nickname'), dataIndex: 'nickname' },
    { title: t('auth.email'), dataIndex: 'email' },
    {
      title: '角色',
      dataIndex: 'role',
      render: (role, record) => (
        <Select
          value={role}
          size="small"
          onChange={val => changeRole(record.id, val)}
          options={[
            { value: 'STUDENT', label: '学生' },
            { value: 'TEACHER', label: '教师' },
            { value: 'ADMIN', label: '管理员' },
          ]}
        />
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={4}>{t('nav.admin')}</Typography.Title>
      <Tabs items={[
        {
          key: 'stats',
          label: t('nav.statistics'),
          children: (
            <>
              <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                {stats && [
                  ['总用户', stats.totalUsers],
                  ['学生', stats.totalStudents],
                  ['教师', stats.totalTeachers],
                  ['课程', stats.totalCourses],
                  ['习题', stats.totalExercises],
                  ['答题记录', stats.totalAnswerRecords],
                  ['推荐记录', stats.totalRecommendations],
                  ['待批改', stats.pendingGrading],
                ].map(([label, val]) => (
                  <Col span={6} key={label}>
                    <Card><Statistic title={label} value={val} /></Card>
                  </Col>
                ))}
              </Row>
              <Button icon={<ReloadOutlined />} onClick={retrain} type="primary" danger>
                触发推荐模型重训练
              </Button>
            </>
          ),
        },
        {
          key: 'users',
          label: '用户管理',
          children: (
            <Table
              dataSource={users}
              columns={columns}
              rowKey="id"
              size="small"
            />
          ),
        },
      ]} />
    </div>
  )
}
