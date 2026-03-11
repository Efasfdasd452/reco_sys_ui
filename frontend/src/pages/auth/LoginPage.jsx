import { useState } from 'react'
import { Form, Input, Button, Card, Tabs, message, Space, Modal, Typography, QRCode, Segmented, Descriptions } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'
import SliderCaptcha from '../../components/SliderCaptcha'

const { Text, Paragraph } = Typography

export default function LoginPage() {
  const { t } = useTranslation()
  const { login } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [activeTab, setActiveTab] = useState('login')

  // 各表单的滑块验证通行证
  const [loginPassToken, setLoginPassToken] = useState('')
  const [registerPassToken, setRegisterPassToken] = useState('')
  // 用于强制重置 SliderCaptcha 组件（key 变化时重新挂载）
  const [loginCaptchaKey, setLoginCaptchaKey] = useState(0)
  const [registerCaptchaKey, setRegisterCaptchaKey] = useState(0)

  // 发送邮箱验证码专用滑块（注册和重置密码各一个）
  const [registerEmailPassToken, setRegisterEmailPassToken] = useState('')
  const [registerEmailCaptchaKey, setRegisterEmailCaptchaKey] = useState(0)
  const [resetEmailPassToken, setResetEmailPassToken] = useState('')
  const [resetEmailCaptchaKey, setResetEmailCaptchaKey] = useState(0)

  // 注册成功后弹出 TOTP 绑定 Modal
  const [totpModal, setTotpModal] = useState({ open: false, secret: '', qrUri: '', username: '' })

  // 重置密码方式：'email' | 'totp'
  const [resetMode, setResetMode] = useState('email')
  const [totpResetForm] = Form.useForm()

  const [loginForm] = Form.useForm()
  const [registerForm] = Form.useForm()
  const [resetForm] = Form.useForm()

  const handleLogin = async (values) => {
    if (!loginPassToken) {
      message.warning('请先完成滑块验证')
      return
    }
    setLoading(true)
    try {
      const data = await api.auth.login({ ...values, captchaPassToken: loginPassToken })
      login(
        { userId: data.userId, username: data.username, role: data.role, nickname: data.nickname },
        data.token
      )
      message.success(t('auth.loginSuccess'))
      navigate('/')
    } catch {
      // 登录失败后重置验证码
      setLoginPassToken('')
      setLoginCaptchaKey(k => k + 1)
    } finally {
      setLoading(false)
    }
  }

  const handleRegister = async (values) => {
    if (!registerPassToken) {
      message.warning('请先完成滑块验证')
      return
    }
    setLoading(true)
    try {
      const data = await api.auth.register({ ...values, captchaPassToken: registerPassToken })
      // 注册成功：弹出 TOTP 绑定向导
      setTotpModal({ open: true, secret: data.totpSecret, qrUri: data.totpQrUri, username: values.username })
      registerForm.resetFields()
      setRegisterPassToken('')
      setRegisterCaptchaKey(k => k + 1)
    } catch {
      setRegisterPassToken('')
      setRegisterCaptchaKey(k => k + 1)
    } finally {
      setLoading(false)
    }
  }

  const handleReset = async (values) => {
    setLoading(true)
    try {
      await api.auth.resetPassword(values)
      message.success('密码重置成功，请登录')
      setActiveTab('login')
    } finally {
      setLoading(false)
    }
  }

  const handleResetByTotp = async (values) => {
    setLoading(true)
    try {
      await api.auth.resetPasswordByTotp(values)
      message.success('密码重置成功，请登录')
      totpResetForm.resetFields()
      setActiveTab('login')
    } finally {
      setLoading(false)
    }
  }

  const sendCode = async (formRef, type, passToken) => {
    const email = formRef.getFieldValue('email')
    if (!email) { message.warning('请先输入邮箱'); return }
    try {
      await api.auth.sendCode(email, type, passToken)
      message.success('验证码已发送')
      // 发送成功后重置该次验证码专用滑块
      if (type === 'REGISTER') {
        setRegisterEmailPassToken('')
        setRegisterEmailCaptchaKey(k => k + 1)
      } else {
        setResetEmailPassToken('')
        setResetEmailCaptchaKey(k => k + 1)
      }
      let s = 60
      setCountdown(s)
      const timer = setInterval(() => {
        s -= 1
        setCountdown(s)
        if (s <= 0) clearInterval(timer)
      }, 1000)
    } catch {}
  }

  const CodeBtn = ({ formRef, type, passToken }) => (
    <Button disabled={countdown > 0 || !passToken} onClick={() => sendCode(formRef, type, passToken)} size="small">
      {countdown > 0 ? `${countdown}s` : t('auth.sendCode')}
    </Button>
  )

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 420, boxShadow: '0 4px 24px rgba(0,0,0,0.08)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <h2 style={{ margin: 0 }}>{t('app.name')}</h2>
        </div>
        <Tabs activeKey={activeTab} onChange={setActiveTab} centered items={[
          {
            key: 'login',
            label: t('auth.login'),
            children: (
              <Form form={loginForm} onFinish={handleLogin} layout="vertical">
                <Form.Item name="username" label={t('auth.username')} rules={[{ required: true }]}>
                  <Input autoComplete="username" />
                </Form.Item>
                <Form.Item name="password" label={t('auth.password')} rules={[{ required: true }]}>
                  <Input.Password autoComplete="current-password" />
                </Form.Item>

                {/* 滑块验证码 */}
                <Form.Item label="安全验证" required style={{ marginBottom: 8 }}>
                  <SliderCaptcha
                    key={loginCaptchaKey}
                    onSuccess={(passToken) => setLoginPassToken(passToken)}
                    onReset={() => setLoginPassToken('')}
                  />
                </Form.Item>

                <Button type="link" onClick={() => setActiveTab('reset')} style={{ padding: 0, marginBottom: 12 }}>
                  {t('auth.forgotPassword')}
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  disabled={!loginPassToken}
                  block
                >
                  {t('auth.login')}
                </Button>
                <div style={{ marginTop: 12, textAlign: 'center' }}>
                  <Button type="link" onClick={() => setActiveTab('register')}>
                    {t('auth.noAccount')} {t('auth.register')}
                  </Button>
                </div>
              </Form>
            ),
          },
          {
            key: 'register',
            label: t('auth.register'),
            children: (
              <Form form={registerForm} onFinish={handleRegister} layout="vertical">
                <Form.Item name="username" label={t('auth.username')} rules={[{ required: true, min: 3 }]}>
                  <Input autoComplete="username" />
                </Form.Item>
                <Form.Item name="password" label={t('auth.password')} rules={[{ required: true, min: 6 }]}>
                  <Input.Password autoComplete="new-password" />
                </Form.Item>
                <Form.Item name="email" label={t('auth.email')} rules={[{ required: true, type: 'email' }]}>
                  <Input />
                </Form.Item>
                <Form.Item label="发送前请完成验证" required style={{ marginBottom: 4 }}>
                  <SliderCaptcha
                    key={registerEmailCaptchaKey}
                    onSuccess={(token) => setRegisterEmailPassToken(token)}
                    onReset={() => setRegisterEmailPassToken('')}
                  />
                </Form.Item>
                <Form.Item name="emailCode" label={t('auth.emailCode')} rules={[{ required: true }]}>
                  <Space.Compact style={{ width: '100%' }}>
                    <Input style={{ flex: 1 }} />
                    <CodeBtn formRef={registerForm} type="REGISTER" passToken={registerEmailPassToken} />
                  </Space.Compact>
                </Form.Item>
                <Form.Item name="nickname" label={t('auth.nickname')}>
                  <Input />
                </Form.Item>

                {/* 滑块验证码 */}
                <Form.Item label="安全验证" required style={{ marginBottom: 8 }}>
                  <SliderCaptcha
                    key={registerCaptchaKey}
                    onSuccess={(passToken) => setRegisterPassToken(passToken)}
                    onReset={() => setRegisterPassToken('')}
                  />
                </Form.Item>

                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  disabled={!registerPassToken}
                  block
                >
                  {t('auth.register')}
                </Button>
                <div style={{ marginTop: 12, textAlign: 'center' }}>
                  <Button type="link" onClick={() => setActiveTab('login')}>
                    {t('auth.hasAccount')} {t('auth.login')}
                  </Button>
                </div>
              </Form>
            ),
          },
          {
            key: 'reset',
            label: t('auth.resetPassword'),
            children: (
              <div>
                <Segmented
                  block
                  style={{ marginBottom: 16 }}
                  value={resetMode}
                  onChange={setResetMode}
                  options={[
                    { label: '邮箱验证码找回', value: 'email' },
                    { label: '验证器App找回', value: 'totp' },
                  ]}
                />

                {resetMode === 'email' ? (
                  <Form form={resetForm} onFinish={handleReset} layout="vertical">
                    <Form.Item name="email" label={t('auth.email')} rules={[{ required: true, type: 'email' }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item label="发送前请完成验证" required style={{ marginBottom: 4 }}>
                      <SliderCaptcha
                        key={resetEmailCaptchaKey}
                        onSuccess={(token) => setResetEmailPassToken(token)}
                        onReset={() => setResetEmailPassToken('')}
                      />
                    </Form.Item>
                    <Form.Item name="emailCode" label={t('auth.emailCode')} rules={[{ required: true }]}>
                      <Space.Compact style={{ width: '100%' }}>
                        <Input style={{ flex: 1 }} />
                        <CodeBtn formRef={resetForm} type="RESET_PASSWORD" passToken={resetEmailPassToken} />
                      </Space.Compact>
                    </Form.Item>
                    <Form.Item name="newPassword" label={t('auth.newPassword')} rules={[{ required: true, min: 6 }]}>
                      <Input.Password />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={loading} block>重置密码</Button>
                  </Form>
                ) : (
                  <Form form={totpResetForm} onFinish={handleResetByTotp} layout="vertical">
                    <Form.Item name="username" label={t('auth.username')} rules={[{ required: true }]}>
                      <Input autoComplete="username" />
                    </Form.Item>
                    <Form.Item
                      name="totpCode"
                      label="验证器App 验证码"
                      rules={[{ required: true, len: 6, message: '请输入6位验证码' }]}
                    >
                      <Input maxLength={6} placeholder="打开验证器App查看6位数字" style={{ letterSpacing: 4, fontSize: 18 }} />
                    </Form.Item>
                    <Form.Item name="newPassword" label={t('auth.newPassword')} rules={[{ required: true, min: 6 }]}>
                      <Input.Password />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={loading} block>重置密码</Button>
                  </Form>
                )}
              </div>
            ),
          },
        ]} />
      </Card>

      {/* 注册成功 - TOTP 绑定向导 */}
      <Modal
        open={totpModal.open}
        title="绑定验证器App（强烈建议）"
        footer={null}
        closable={false}
        width={480}
      >
        <Paragraph type="secondary" style={{ marginBottom: 12 }}>
          打开 Google Authenticator / 微软 Authenticator 等 App，选择"添加账户"后按以下任意方式绑定。
          绑定后可在忘记密码时通过 App 验证码找回账号。
        </Paragraph>

        <Tabs
          centered
          items={[
            {
              key: 'qr',
              label: '扫描二维码',
              children: (
                <div style={{ textAlign: 'center', padding: '8px 0' }}>
                  <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                    在 App 中选择"扫描二维码"，对准下方二维码即可自动填入所有信息。
                  </Paragraph>
                  {totpModal.qrUri && (
                    <div style={{ display: 'inline-block', padding: 12, background: '#fff', border: '1px solid #f0f0f0', borderRadius: 8 }}>
                      <QRCode value={totpModal.qrUri} size={200} />
                    </div>
                  )}
                  <Paragraph type="secondary" style={{ marginTop: 12, fontSize: 12 }}>
                    二维码内容为标准 <Text code>otpauth://totp/</Text> 格式，兼容所有主流验证器 App
                  </Paragraph>
                </div>
              ),
            },
            {
              key: 'manual',
              label: '手动输入',
              children: (
                <div style={{ padding: '8px 0' }}>
                  <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                    在 App 中选择"手动输入"或"输入设置密钥"，按下表填写各字段。
                  </Paragraph>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="账户名称">
                      <Text copyable>{totpModal.username ? `reco_sys:${totpModal.username}` : 'reco_sys:你的用户名'}</Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="密钥（Key）">
                      <Text code copyable={{ text: totpModal.secret }} style={{ letterSpacing: 1 }}>
                        {totpModal.secret}
                      </Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="类型">基于时间 (TOTP)</Descriptions.Item>
                    <Descriptions.Item label="位数">6 位</Descriptions.Item>
                    <Descriptions.Item label="时间间隔">30 秒</Descriptions.Item>
                    <Descriptions.Item label="算法">SHA1</Descriptions.Item>
                    <Descriptions.Item label="发行方（Issuer）">
                      <Text copyable>reco_sys</Text>
                    </Descriptions.Item>
                  </Descriptions>
                </div>
              ),
            },
          ]}
        />

        <div style={{ marginTop: 20, display: 'flex', gap: 8, justifyContent: 'center' }}>
          <Button
            type="primary"
            onClick={() => {
              setTotpModal({ open: false, secret: '', qrUri: '', username: '' })
              message.success('注册成功，请登录')
              setActiveTab('login')
            }}
          >
            已绑定，完成注册
          </Button>
          <Button
            onClick={() => {
              setTotpModal({ open: false, secret: '', qrUri: '', username: '' })
              message.warning('已跳过，之后可在个人中心重新绑定')
              setActiveTab('login')
            }}
          >
            暂时跳过
          </Button>
        </div>
      </Modal>
    </div>
  )
}
