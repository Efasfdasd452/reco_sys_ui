import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Tag, Tabs, Typography, message, Spin } from 'antd'
import { PlusOutlined, CheckOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

export default function CoursePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [all, setAll] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    api.course.list().then(setAll).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleEnroll = async (course) => {
    try {
      if (course.isEnrolled) {
        await api.course.unenroll(course.id)
        message.success(t('common.success'))
      } else {
        await api.course.enroll(course.id)
        message.success(t('common.success'))
      }
      load()
    } catch {}
  }

  if (loading) return <Spin />

  return (
    <div>
      <Typography.Title level={4}>{t('nav.courses')}</Typography.Title>
      <Row gutter={[16, 16]}>
        {all.map(course => (
          <Col key={course.id} xs={24} sm={12} lg={8}>
            <Card
              title={course.name}
              extra={
                <Button
                  type={course.isEnrolled ? 'default' : 'primary'}
                  size="small"
                  icon={course.isEnrolled ? <CheckOutlined /> : <PlusOutlined />}
                  onClick={() => handleEnroll(course)}
                >
                  {course.isEnrolled ? t('course.enrolled') : t('course.enroll')}
                </Button>
              }
              hoverable
              onClick={() => navigate(`/courses/${course.id}`)}
            >
              <p style={{ color: '#666', fontSize: 13, minHeight: 40 }}>{course.description || '暂无描述'}</p>
              <div style={{ marginTop: 8 }}>
                <Tag>{t('course.teacher')}: {course.teacherName || '-'}</Tag>
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}
