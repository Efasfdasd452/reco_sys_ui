import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card, Button, Tag, Typography, message, Modal, Spin, Space, Alert,
  Radio, Checkbox, Input, Tabs,
} from 'antd'
import {
  MobileOutlined, PictureOutlined, SendOutlined, ArrowLeftOutlined,
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import MDEditor from '@uiw/react-md-editor'
import { QRCodeCanvas } from 'qrcode.react'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }

export default function ExerciseDetailPage() {
  const { id } = useParams()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const [exercise, setExercise] = useState(null)
  const [loading, setLoading] = useState(true)
  const [answer, setAnswer] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)
  const [timeSpent, setTimeSpent] = useState(0)
  const timerRef = useRef(null)

  // 手机二维码上传
  const [qrVisible, setQrVisible] = useState(false)
  const [qrUrl, setQrUrl] = useState('')
  const [wsConnected, setWsConnected] = useState(false)
  const wsRef = useRef(null)

  useEffect(() => {
    api.exercise.get(id)
      .then(setExercise)
      .finally(() => setLoading(false))
    // 开始计时
    timerRef.current = setInterval(() => setTimeSpent(s => s + 1), 1000)
    return () => {
      clearInterval(timerRef.current)
      if (wsRef.current) wsRef.current.close()
    }
  }, [id])

  const handleSubmit = async () => {
    if (!answer.trim()) { message.warning('请填写答案'); return }
    setSubmitting(true)
    clearInterval(timerRef.current)
    try {
      const result = await api.learning.submit({ exerciseId: Number(id), answer, timeSpent })
      setSubmitResult(result)
      setSubmitted(true)
    } finally {
      setSubmitting(false)
    }
  }

  const openMobileUpload = async () => {
    try {
      const url = await api.file.getMobileSession()
      setQrUrl(url)
      setQrVisible(true)
      // 从 URL 中提取 token 建立 WebSocket
      const token = new URL(url).searchParams.get('token')
      if (token) {
        const ws = new WebSocket(`ws://${window.location.host}/ws/upload/${token}`)
        wsRef.current = ws
        ws.onopen = () => setWsConnected(true)
        ws.onmessage = (e) => {
          const data = JSON.parse(e.data)
          if (data.type === 'UPLOAD_DONE') {
            const imgMd = `\n![手写答案](${data.url})\n`
            setAnswer(prev => prev + imgMd)
            setQrVisible(false)
            setWsConnected(false)
            message.success('图片已上传，已插入答题框')
          }
        }
        ws.onclose = () => setWsConnected(false)
      }
    } catch (e) {
      message.error('获取上传链接失败')
    }
  }

  const handleImageUpload = async (file) => {
    try {
      const url = await api.file.uploadImage(file)
      setAnswer(prev => prev + `\n![图片](${url})\n`)
      return false // 阻止 antd 默认上传行为
    } catch {
      return false
    }
  }

  if (loading) return <div style={{ padding: 40, textAlign: 'center' }}><Spin size="large" /></div>
  if (!exercise) return <Alert message="习题不存在" type="error" />

  const isObjective = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'FILL_BLANK'].includes(exercise.type)

  // 解析选项（约定选择题 content 格式：题干\nA. ...\nB. ...\n）
  const parseOptions = (content) => {
    const lines = content.split('\n')
    const options = lines.filter(l => /^[A-D]\./.test(l.trim()))
    return options.map(o => ({ label: o, value: o[0] }))
  }

  const options = exercise.type === 'SINGLE_CHOICE' || exercise.type === 'MULTIPLE_CHOICE'
    ? parseOptions(exercise.content)
    : []

  return (
    <div>
      <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} style={{ marginBottom: 16 }}>
        返回列表
      </Button>

      <Card
        title={
          <Space>
            <Tag>{t(`exercise.type.${exercise.type}`)}</Tag>
            <Tag color={difficultyColor[exercise.difficulty]}>{t(`exercise.difficulty.${exercise.difficulty}`)}</Tag>
            <span style={{ color: '#999', fontSize: 13 }}>用时：{Math.floor(timeSpent / 60)}:{String(timeSpent % 60).padStart(2, '0')}</span>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <div style={{ fontSize: 16, lineHeight: 1.8 }}>
          <ReactMarkdown>{exercise.content}</ReactMarkdown>
        </div>
      </Card>

      {submitted ? (
        <Alert
          type={submitResult?.status === 'AUTO_GRADED' ? (submitResult?.score === 100 ? 'success' : 'error') : 'info'}
          message={
            submitResult?.status === 'AUTO_GRADED'
              ? `自动批改完成，得分：${submitResult.score} 分`
              : '提交成功，等待教师批改'
          }
          description={submitResult?.teacherComment}
          showIcon
          action={
            <Button onClick={() => navigate(-1)}>返回列表</Button>
          }
        />
      ) : (
        <Card
          title={t('exercise.yourAnswer')}
          extra={
            !isObjective && (
              <Space>
                <Button
                  icon={<PictureOutlined />}
                  size="small"
                  onClick={() => {
                    const input = document.createElement('input')
                    input.type = 'file'
                    input.accept = 'image/*'
                    input.onchange = e => handleImageUpload(e.target.files[0])
                    input.click()
                  }}
                >
                  上传图片
                </Button>
                <Button
                  icon={<MobileOutlined />}
                  size="small"
                  onClick={openMobileUpload}
                >
                  {t('exercise.takePhoto')}
                </Button>
              </Space>
            )
          }
        >
          {/* 选择题 */}
          {exercise.type === 'SINGLE_CHOICE' && (
            <Radio.Group value={answer} onChange={e => setAnswer(e.target.value)}>
              <Space direction="vertical">
                {options.map(o => <Radio key={o.value} value={o.value}>{o.label}</Radio>)}
              </Space>
            </Radio.Group>
          )}

          {exercise.type === 'MULTIPLE_CHOICE' && (
            <Checkbox.Group
              value={answer ? answer.split(',') : []}
              onChange={vals => setAnswer(vals.sort().join(','))}
            >
              <Space direction="vertical">
                {options.map(o => <Checkbox key={o.value} value={o.value}>{o.label}</Checkbox>)}
              </Space>
            </Checkbox.Group>
          )}

          {exercise.type === 'FILL_BLANK' && (
            <Input
              value={answer}
              onChange={e => setAnswer(e.target.value)}
              placeholder="请填写答案"
              style={{ maxWidth: 400 }}
            />
          )}

          {exercise.type === 'SHORT_ANSWER' && (
            <Tabs
              defaultActiveKey="edit"
              items={[
                {
                  key: 'edit',
                  label: '编辑',
                  children: (
                    <MDEditor
                      value={answer}
                      onChange={setAnswer}
                      height={300}
                      preview="edit"
                    />
                  ),
                },
                {
                  key: 'preview',
                  label: '预览',
                  children: (
                    <div style={{ minHeight: 200, padding: 16, border: '1px solid #d9d9d9', borderRadius: 6 }}>
                      <ReactMarkdown>{answer || '（暂无内容）'}</ReactMarkdown>
                    </div>
                  ),
                },
              ]}
            />
          )}

          <div style={{ marginTop: 16 }}>
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSubmit}
              loading={submitting}
              disabled={!answer}
              size="large"
            >
              {t('exercise.submit')}
            </Button>
          </div>
        </Card>
      )}

      {/* 手机二维码上传弹窗 */}
      <Modal
        open={qrVisible}
        title="手机扫码拍照上传"
        footer={null}
        onCancel={() => { setQrVisible(false); if (wsRef.current) wsRef.current.close() }}
      >
        <div style={{ textAlign: 'center', padding: 24 }}>
          {wsConnected && (
            <Alert message="已连接，等待手机上传..." type="info" showIcon style={{ marginBottom: 16 }} />
          )}
          {qrUrl && (
            <>
              <QRCodeCanvas value={qrUrl} size={200} style={{ margin: '0 auto 16px' }} />
              <p style={{ color: '#666', fontSize: 13 }}>
                请确保手机与电脑在<strong>同一WiFi</strong>下，用手机扫描上方二维码，拍照后上传。
              </p>
              <p style={{ fontSize: 12, color: '#aaa', wordBreak: 'break-all' }}>{qrUrl}</p>
            </>
          )}
        </div>
      </Modal>
    </div>
  )
}
