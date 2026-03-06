import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Button, Select, message, Typography, Tabs, Alert } from 'antd'
import { UserOutlined, CloudDownloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

export default function AdminPage() {
  const { t } = useTranslation()
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [initLoading, setInitLoading] = useState(false)
  const [initResult, setInitResult] = useState(null)

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

  const initPythonData = async () => {
    setInitLoading(true)
    setInitResult(null)
    try {
      const result = await api.admin.initPythonData()
      setInitResult(result)
      if (result.status === 'success') {
        message.success(`导入完成：${result.kcs} 个知识点，${result.exercises} 道习题`)
        api.admin.statistics().then(setStats)
      } else {
        message.info(result.message || '已初始化，无需重复操作')
      }
    } catch {
      message.error('初始化失败，请检查Python推荐服务是否运行')
    } finally {
      setInitLoading(false)
    }
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

              <Card title="KG4Ex 数据初始化" style={{ marginTop: 16 }}>
                <p style={{ color: '#666', marginBottom: 12 }}>
                  从 Python 推荐服务导入 Algebra 2005 数据集（112个知识点 + 1084道习题），
                  导入后推荐系统才能正常工作。幂等操作，重复点击安全。
                </p>
                <Button
                  icon={<CloudDownloadOutlined />}
                  type="primary"
                  loading={initLoading}
                  onClick={initPythonData}
                >
                  导入推荐数据（KG4Ex）
                </Button>
                {initResult && (
                  <Alert
                    style={{ marginTop: 12 }}
                    type={initResult.status === 'success' ? 'success' : 'info'}
                    message={
                      initResult.status === 'success'
                        ? `导入成功：${initResult.kcs} 个知识点，${initResult.exercises} 道习题，课程ID=${initResult.courseId}`
                        : initResult.message
                    }
                    showIcon
                  />
                )}
              </Card>
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
