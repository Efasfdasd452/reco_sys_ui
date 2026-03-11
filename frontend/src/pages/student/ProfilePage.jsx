import { useEffect, useState } from 'react'
import { Card, Form, Input, Button, Descriptions, Divider, Tag, message, Spin, Tabs, Table, Badge, QRCode, Typography, Modal, Space } from 'antd'
import { SafetyCertificateOutlined } from '@ant-design/icons'
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
  const [totpInfo, setTotpInfo] = useState(null)
  const [totpLoading, setTotpLoading] = useState(false)
  const [totpResetLoading, setTotpResetLoading] = useState(false)

  useEffect(() => {
    api.user.profile()
      .then(data => {
        setProfile(data)
        form.setFieldsValue({ nickname: data.nickname })
      })
      .finally(() => setLoading(false))
  }, [])

  const loadTotpSetup = () => {
    setTotpLoading(true)
    api.user.getTotpSetup()
      .then(setTotpInfo)
      .finally(() => setTotpLoading(false))
  }

  const handleResetTotp = () => {
    Modal.confirm({
      title: '重新生成 TOTP 密钥',
      content: '重新生成后，旧密钥立即失效，需在验证器 App 中重新添加账号。确定继续吗？',
      okText: '确定重新生成',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        setTotpResetLoading(true)
        try {
          const data = await api.user.resetTotp()
          setTotpInfo(data)
          message.success('已生成新密钥，请重新绑定验证器 App')
        } finally {
          setTotpResetLoading(false)
        }
      },
    })
  }

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
          {
            key: 'totp',
            label: <span><SafetyCertificateOutlined /> 两步验证</span>,
            children: (
              <Card>
                <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
                  两步验证（TOTP）用于在忘记密码时通过验证器 App（如 Google Authenticator、Microsoft Authenticator）验证身份并重置密码。
                </Typography.Paragraph>
                {!totpInfo ? (
                  <Button type="primary" loading={totpLoading} onClick={loadTotpSetup}>
                    查看绑定信息 / 二维码
                  </Button>
                ) : (
                  <Tabs
                    items={[
                      {
                        key: 'qr',
                        label: '扫描二维码',
                        children: (
                          <Space direction="vertical" align="center" style={{ width: '100%', paddingTop: 8 }}>
                            <QRCode value={totpInfo.totpQrUri} size={200} />
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              用验证器 App 扫描上方二维码完成绑定
                            </Typography.Text>
                          </Space>
                        ),
                      },
                      {
                        key: 'manual',
                        label: '手动输入',
                        children: (
                          <Descriptions column={1} bordered size="small" style={{ maxWidth: 480 }}>
                            <Descriptions.Item label="账号">{profile?.username}</Descriptions.Item>
                            <Descriptions.Item label="密钥（Key）">
                              <Typography.Text copyable code style={{ wordBreak: 'break-all' }}>
                                {totpInfo.totpSecret}
                              </Typography.Text>
                            </Descriptions.Item>
                            <Descriptions.Item label="类型">基于时间（TOTP）</Descriptions.Item>
                            <Descriptions.Item label="位数">6</Descriptions.Item>
                            <Descriptions.Item label="时间步长">30 秒</Descriptions.Item>
                            <Descriptions.Item label="算法">SHA1</Descriptions.Item>
                            <Descriptions.Item label="颁发者">reco_sys</Descriptions.Item>
                          </Descriptions>
                        ),
                      },
                    ]}
                  />
                )}
                <Divider />
                <Typography.Paragraph type="warning" style={{ marginBottom: 8 }}>
                  <strong>重新生成密钥</strong>：若验证器 App 丢失或需要换设备，可重新生成密钥。旧密钥立即失效。
                </Typography.Paragraph>
                <Button danger loading={totpResetLoading} onClick={handleResetTotp}>
                  重新生成密钥
                </Button>
              </Card>
            ),
          },
        ]}
        onChange={key => {
          if (key === 'login-records' && loginRecords.length === 0) loadLoginRecords(0)
        }}
      />
    </div>
  )
}
