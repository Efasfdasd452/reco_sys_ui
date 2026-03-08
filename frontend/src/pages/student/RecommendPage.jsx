import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Select, Typography, Spin, Alert, Progress } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import MathMarkdown from '../../components/MathMarkdown'
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
                <MathMarkdown>{item.content}</MathMarkdown>
                {item.kcDetails && item.kcDetails.length > 0 && (
                  <div style={{ marginTop: 8, padding: '8px 12px', background: '#f6ffed', borderRadius: 6 }}>
                    <div style={{ fontSize: 12, color: '#389e0d', marginBottom: 6 }}>
                      💡 推荐理由
                      {item.exerciseExfr != null && (
                        <Tag color={item.exerciseExfr > 0.5 ? 'red' : item.exerciseExfr > 0.2 ? 'orange' : 'green'} style={{ fontSize: 11, marginLeft: 8 }}>
                          遗忘率 {Math.round(item.exerciseExfr * 100)}%
                        </Tag>
                      )}
                    </div>
                    {item.kcDetails.map(kc => (
                      <div key={kc.kcName} style={{ marginBottom: 6 }}>
                        <div style={{ fontSize: 12, color: '#555', marginBottom: 3 }}>{kc.kcName}</div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 2, paddingLeft: 8 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <span style={{ fontSize: 11, color: '#888', minWidth: 80 }}>掌握度 (mlkc)</span>
                            <Progress
                              percent={Math.round(kc.mastery * 100)}
                              size="small"
                              style={{ flex: 1, minWidth: 80 }}
                              strokeColor={kc.mastery >= 0.8 ? '#52c41a' : kc.mastery >= 0.5 ? '#faad14' : '#ff4d4f'}
                            />
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <span style={{ fontSize: 11, color: '#888', minWidth: 80 }}>出现概率 (pkc)</span>
                            <Progress
                              percent={Math.round((kc.pkc ?? 0) * 100)}
                              size="small"
                              style={{ flex: 1, minWidth: 80 }}
                              strokeColor="#1677ff"
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {(!item.kcDetails || item.kcDetails.length === 0) && item.reason && (
                  <div style={{ marginTop: 8, padding: '8px 12px', background: '#f6ffed', borderRadius: 6, fontSize: 13, color: '#389e0d' }}>
                    💡 {item.reason}
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
