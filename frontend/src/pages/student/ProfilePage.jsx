import { useEffect, useState } from 'react'
import { Card, Form, Input, Button, Descriptions, Divider, Tag, message, Spin, Tabs, Table, Badge } from 'antd'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'

const roleLabel = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
const roleColor = { STUDENT: 'blue', TEACHER: 'green', ADMIN: 'red' }

export default function ProfilePage() {
  const { t } = useTranslation()
  const { user, login } = useAuth()
  const [profile, setProfile] = useState(null)
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [loginRecords, setLoginRecords] = useState([])
  const [loginTotal, setLoginTotal] = useState(0)
  const [loginPage, setLoginPage] = useState(0)
  const [loginLoading, setLoginLoading] = useState(false)

  useEffect(() => {
    api.user.profile()
      .then(data => {
        setProfile(data)
        form.setFieldsValue({ nickname: data.nickname })
      })
      .finally(() => setLoading(false))
  }, [])

  const loadLoginRecords = (p = 0) => {
    setLoginLoading(true)
    api.user.loginRecords(p, 10)
      .then(data => {
        setLoginRecords(data?.content || [])
        setLoginTotal(data?.totalElements || 0)
        setLoginPage(p)
      })
      .finally(() => setLoginLoading(false))
  }

  const handleSave = async (values) => {
    const payload = {}
    if (values.nickname) payload.nickname = values.nickname
    if (values.newPassword) {
      payload.currentPassword = values.currentPassword
      payload.newPassword = values.newPassword
    }
    setSaving(true)
    try {
      await api.user.updateProfile(payload)
      message.success(t('common.success'))
      if (values.nickname) {
        login({ ...user, nickname: values.nickname }, localStorage.getItem('token'))
      }
      form.resetFields(['currentPassword', 'newPassword', 'confirmPassword'])
    } finally {
      setSaving(false)
    }
  }

  const loginColumns = [
    { title: 'IP', dataIndex: 'ipAddress', width: 140 },
    { title: '地区', dataIndex: 'location', width: 120 },
    {
      title: '状态',
      dataIndex: 'isAnomaly',
      width: 80,
      render: v => v ? <Badge status="error" text="异常" /> : <Badge status="success" text="正常" />,
    },
    {
      title: '时间',
      dataIndex: 'loginAt',
      render: v => v ? new Date(v).toLocaleString() : '-',
    },
  ]

  if (loading) return <Spin />

  return (
    <div style={{ maxWidth: 700 }}>
      <Tabs
        items={[
          {
            key: 'profile',
            label: '个人信息',
            children: (
              <>
                <Card title={t('nav.profile')} style={{ marginBottom: 24 }}>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="用户名">{profile?.username}</Descriptions.Item>
                    <Descriptions.Item label="邮箱">{profile?.email}</Descriptions.Item>
                    <Descriptions.Item label="角色">
                      <Tag color={roleColor[profile?.role]}>{roleLabel[profile?.role]}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="注册时间">
                      {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>

                <Card title="修改信息">
                  <Form form={form} onFinish={handleSave} layout="vertical">
                    <Form.Item label="昵称" name="nickname">
                      <Input placeholder="修改昵称" />
                    </Form.Item>
                    <Divider>修改密码（不修改请留空）</Divider>
                    <Form.Item label="当前密码" name="currentPassword">
                      <Input.Password placeholder="输入当前密码" />
                    </Form.Item>
                    <Form.Item
                      label="新密码"
                      name="newPassword"
                      rules={[
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || value.length >= 6) return Promise.resolve()
                            return Promise.reject('密码至少6位')
                          },
                        }),
                      ]}
                    >
                      <Input.Password placeholder="输入新密码（至少6位）" />
                    </Form.Item>
                    <Form.Item
                      label="确认新密码"
                      name="confirmPassword"
                      dependencies={['newPassword']}
                      rules={[
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                            return Promise.reject('两次密码不一致')
                          },
                        }),
                      ]}
                    >
                      <Input.Password placeholder="再次输入新密码" />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={saving}>
                      {t('common.save')}
                    </Button>
                  </Form>
                </Card>
              </>
            ),
          },
          {
            key: 'login-records',
            label: '登录记录',
            onTabClick: () => { if (loginRecords.length === 0) loadLoginRecords(0) },
            children: (
              <Table
                dataSource={loginRecords}
                columns={loginColumns}
                rowKey="id"
                size="small"
                loading={loginLoading}
                pagination={{
                  total: loginTotal,
                  pageSize: 10,
                  current: loginPage + 1,
                  onChange: p => loadLoginRecords(p - 1),
                }}
              />
            ),
          },
        ]}
        onChange={key => { if (key === 'login-records' && loginRecords.length === 0) loadLoginRecords(0) }}
      />
    </div>
  )
}
