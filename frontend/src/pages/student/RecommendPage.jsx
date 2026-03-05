import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Select, Typography, Spin, Alert, Collapse, Rate } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import ReactMarkdown from 'react-markdown'
import { api } from '../../api'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }

export default function RecommendPage() {
  const { t } = useTranslation()
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) { setCourseId(list[0].id) }
    })
  }, [])

  useEffect(() => {
    if (courseId) loadRecommend()
  }, [courseId])

  const loadRecommend = () => {
    setLoading(true)
    api.recommend.latest(courseId).then(setData).finally(() => setLoading(false))
  }

  const refresh = () => {
    setLoading(true)
    api.recommend.refresh(courseId).then(setData).finally(() => setLoading(false))
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('recommend.title')}</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
        <Button icon={<ReloadOutlined />} onClick={refresh} loading={loading}>
          {t('recommend.refresh')}
        </Button>
      </div>

      {loading && <Spin />}

      {!loading && data && (
        <>
          {data.overallReason && (
            <Alert message={data.overallReason} type="info" showIcon style={{ marginBottom: 16 }} />
          )}
          <List
            dataSource={data.items || []}
            locale={{ emptyText: t('recommend.empty') }}
            renderItem={(item, idx) => (
              <Card
                key={item.exerciseId}
                style={{ marginBottom: 12 }}
                size="small"
                title={
                  <span>
                    第 {idx + 1} 题
                    <Tag color={difficultyColor[item.difficulty]} style={{ marginLeft: 8 }}>
                      {t(`exercise.difficulty.${item.difficulty}`)}
                    </Tag>
                    <Tag>{t(`exercise.type.${item.type}`)}</Tag>
                  </span>
                }
              >
                <ReactMarkdown>{item.content}</ReactMarkdown>
                {item.reason && (
                  <div style={{ marginTop: 8, padding: '8px 12px', background: '#f6ffed', borderRadius: 6, fontSize: 13, color: '#389e0d' }}>
                    💡 {t('recommend.reason')}：{item.reason}
                  </div>
                )}
              </Card>
            )}
          />
        </>
      )}

      {!loading && !data && !loading && (
        <Alert message={t('recommend.empty')} type="info" />
      )}
    </div>
  )
}
