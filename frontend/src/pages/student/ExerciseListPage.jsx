import { useEffect, useState } from 'react'
import { Select, List, Tag, Button, Typography, Spin } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }

export default function ExerciseListPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [exercises, setExercises] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [answeredIds, setAnsweredIds] = useState(new Set())

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) setCourseId(list[0].id)
    })
    api.learning.answeredIds().then(ids => setAnsweredIds(new Set(ids))).catch(() => {})
  }, [])

  useEffect(() => {
    if (courseId) load(0)
  }, [courseId])

  const load = (p = 0) => {
    setLoading(true)
    api.exercise.listByCourse(courseId, p, 20)
      .then(data => {
        setExercises(data?.content || [])
        setTotal(data?.totalElements || 0)
        setPage(p)
      })
      .finally(() => setLoading(false))
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('exercise.title')}</Typography.Title>
        <Select
          value={courseId}
          onChange={val => setCourseId(val)}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
      </div>

      {loading ? <Spin /> : (
        <List
          dataSource={exercises}
          locale={{ emptyText: courseId ? '该课程暂无习题' : '请先选择课程' }}
          pagination={{
            total,
            pageSize: 20,
            current: page + 1,
            onChange: p => load(p - 1),
            showTotal: t => `共 ${t} 题`,
          }}
          renderItem={(ex, idx) => (
            <List.Item
              key={ex.id}
              actions={[
                <Button type="primary" size="small" onClick={() => navigate(`/exercises/${ex.id}`)}>
                  {answeredIds.has(ex.id) ? '再次答题' : '开始答题'}
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={
                  <span>
                    <span style={{ marginRight: 8, color: '#999' }}>#{page * 20 + idx + 1}</span>
                    <Tag>{t(`exercise.type.${ex.type}`)}</Tag>
                    <Tag color={difficultyColor[ex.difficulty]}>{t(`exercise.difficulty.${ex.difficulty}`)}</Tag>
                    {answeredIds.has(ex.id) && (
                      <Tag color="green" icon={<CheckCircleOutlined />} style={{ marginRight: 8 }}>已答</Tag>
                    )}
                    <span style={{ fontSize: 14 }}>
                      {ex.content?.replace(/[#*`\[\]]/g, '').slice(0, 60)}
                      {ex.content?.length > 60 ? '...' : ''}
                    </span>
                  </span>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )
}
