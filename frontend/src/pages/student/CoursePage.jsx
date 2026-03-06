import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Tag, Typography, Modal, Form, Input, message, Spin } from 'antd'
import { PlusOutlined, CheckOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'

export default function CoursePage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN'
  const [all, setAll] = useState([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [form] = Form.useForm()

  const load = () => {
    setLoading(true)
    api.course.list().then(setAll).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleEnroll = async (e, course) => {
    e.stopPropagation()
    try {
      if (course.isEnrolled) {
        await api.course.unenroll(course.id)
      } else {
        await api.course.enroll(course.id)
      }
      message.success(t('common.success'))
      load()
    } catch {}
  }

  const handleCreate = async (values) => {
    try {
      await api.course.create(values)
      message.success(t('common.success'))
      setCreateOpen(false)
      form.resetFields()
      load()
    } catch {}
  }

  if (loading) return <Spin />

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('nav.courses')}</Typography.Title>
        {isTeacher && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            新建课程
          </Button>
        )}
      </div>
      <Row gutter={[16, 16]}>
        {all.map(course => (
          <Col key={course.id} xs={24} sm={12} lg={8}>
            <Card
              title={course.name}
              extra={
                !isTeacher && (
                  <Button
                    type={course.isEnrolled ? 'default' : 'primary'}
                    size="small"
                    icon={course.isEnrolled ? <CheckOutlined /> : <PlusOutlined />}
                    onClick={(e) => handleEnroll(e, course)}
                  >
                    {course.isEnrolled ? t('course.enrolled') : t('course.enroll')}
                  </Button>
                )
              }
            >
              <p style={{ color: '#666', fontSize: 13, minHeight: 40 }}>{course.description || '暂无描述'}</p>
              <div style={{ marginTop: 8 }}>
                <Tag>{t('course.teacher')}: {course.teacherName || '-'}</Tag>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Modal
        open={createOpen}
        title="新建课程"
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        footer={null}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item label="课程名称" name="name" rules={[{ required: true, message: '请输入课程名称' }]}>
            <Input placeholder="请输入课程名称" />
          </Form.Item>
          <Form.Item label="课程描述" name="description">
            <Input.TextArea rows={3} placeholder="请输入课程描述" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>创建</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
