import { useState } from 'react'
import { Form, Input, Button, Card, Tabs, message, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'
import SliderCaptcha from '../../components/SliderCaptcha'

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
      await api.auth.register({ ...values, captchaPassToken: registerPassToken })
      message.success(t('auth.registerSuccess'))
      setActiveTab('login')
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

  const sendCode = async (formRef, type) => {
    const email = formRef.getFieldValue('email')
    if (!email) { message.warning('请先输入邮箱'); return }
    try {
      await api.auth.sendCode(email, type)
      message.success('验证码已发送')
      let s = 60
      setCountdown(s)
      const timer = setInterval(() => {
        s -= 1
        setCountdown(s)
        if (s <= 0) clearInterval(timer)
      }, 1000)
    } catch {}
  }

  const CodeBtn = ({ formRef, type }) => (
    <Button disabled={countdown > 0} onClick={() => sendCode(formRef, type)} size="small">
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
                <Form.Item name="emailCode" label={t('auth.emailCode')} rules={[{ required: true }]}>
                  <Space.Compact style={{ width: '100%' }}>
                    <Input style={{ flex: 1 }} />
                    <CodeBtn formRef={registerForm} type="REGISTER" />
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
              <Form form={resetForm} onFinish={handleReset} layout="vertical">
                <Form.Item name="email" label={t('auth.email')} rules={[{ required: true, type: 'email' }]}>
                  <Input />
                </Form.Item>
                <Form.Item name="emailCode" label={t('auth.emailCode')} rules={[{ required: true }]}>
                  <Space.Compact style={{ width: '100%' }}>
                    <Input style={{ flex: 1 }} />
                    <CodeBtn formRef={resetForm} type="RESET_PASSWORD" />
                  </Space.Compact>
                </Form.Item>
                <Form.Item name="newPassword" label={t('auth.newPassword')} rules={[{ required: true, min: 6 }]}>
                  <Input.Password />
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block>重置密码</Button>
              </Form>
            ),
          },
        ]} />
      </Card>
    </div>
  )
}
