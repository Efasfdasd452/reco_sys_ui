import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, List, Tag, Typography, Button } from 'antd'
import { BookOutlined, CheckCircleOutlined, StarOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'

const { Title, Text } = Typography

export default function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [courses, setCourses] = useState([])
  const [history, setHistory] = useState([])

  useEffect(() => {
    api.course.my().then(setCourses).catch(() => {})
    api.learning.history(0, 5).then(d => setHistory(d?.content || [])).catch(() => {})
  }, [])

  return (
    <div>
      <Title level={4}>你好，{user?.nickname || user?.username} 👋</Title>
      <Row gutter={[16, 16]} style={{ marginTop: 16, marginBottom: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic title={t('course.my')} value={courses.length} prefix={<BookOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="已完成答题" value={history.length} prefix={<CheckCircleOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            hoverable
            onClick={() => navigate('/recommend')}
            style={{ cursor: 'pointer', borderColor: '#1677ff' }}
          >
            <Statistic title={t('recommend.title')} value="查看" prefix={<StarOutlined />} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
      </Row>

      <Card title={t('course.my')} extra={<Button type="link" onClick={() => navigate('/courses')}>全部课程</Button>}>
        <List
          dataSource={courses.slice(0, 5)}
          locale={{ emptyText: '还未加入任何课程' }}
          renderItem={c => (
            <List.Item
              key={c.id}
              actions={[<Button type="link" onClick={() => navigate(`/recommend?courseId=${c.id}`)}>推荐习题</Button>]}
            >
              <List.Item.Meta title={c.name} description={`教师：${c.teacherName || '-'}`} />
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}
