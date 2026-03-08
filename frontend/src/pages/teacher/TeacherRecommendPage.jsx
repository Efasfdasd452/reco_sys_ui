import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Select, Typography, Spin, Alert, Empty, Progress } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import MathMarkdown from '../../components/MathMarkdown'
import { api } from '../../api'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }
const difficultyLabel = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
const typeLabel = { SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', FILL_BLANK: '填空', SHORT_ANSWER: '简答' }

export default function TeacherRecommendPage() {
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [students, setStudents] = useState([])
  const [studentId, setStudentId] = useState(null)
  const [data, setData] = useState(null)
  const [loadingStudents, setLoadingStudents] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) setCourseId(list[0].id)
    })
  }, [])

  useEffect(() => {
    if (!courseId) return
    setStudents([])
    setStudentId(null)
    setData(null)
    setLoadingStudents(true)
    api.course.students(courseId)
      .then(list => {
        setStudents(list)
        if (list.length > 0) setStudentId(list[0].userId)
      })
      .finally(() => setLoadingStudents(false))
  }, [courseId])

  useEffect(() => {
    if (courseId && studentId) loadRecommend()
  }, [studentId])

  const loadRecommend = () => {
    setLoading(true)
    api.recommend.latestForStudent(courseId, studentId)
      .then(setData)
      .finally(() => setLoading(false))
  }

  const refresh = () => {
    setLoading(true)
    api.recommend.refreshForStudent(courseId, studentId)
      .then(setData)
      .finally(() => setLoading(false))
  }

  return (
    <div>
      <Typography.Title level={4} style={{ marginBottom: 16 }}>学生推荐管理</Typography.Title>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          value={courseId}
          onChange={v => { setCourseId(v); setData(null) }}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
        <Select
          value={studentId}
          onChange={v => { setStudentId(v); setData(null) }}
          options={students.map(s => ({ value: s.userId, label: s.nickname || s.username }))}
          style={{ width: 180 }}
          placeholder={loadingStudents ? '加载中...' : '选择学生'}
          loading={loadingStudents}
          disabled={!courseId || loadingStudents}
          notFoundContent={students.length === 0 ? '该课程暂无学生' : null}
        />
        <Button
          icon={<ReloadOutlined />}
          onClick={refresh}
          loading={loading}
          type="primary"
          disabled={!courseId || !studentId}
        >
          生成新推荐
        </Button>
        <Button
          onClick={loadRecommend}
          loading={loading}
          disabled={!courseId || !studentId}
        >
          查看最新推荐
        </Button>
      </div>

      {loading && <Spin />}

      {!loading && data && (
        <>
          {data.overallReason && (
            <Alert message={data.overallReason} type="info" showIcon style={{ marginBottom: 16 }} />
          )}
          {(!data.items || data.items.length === 0) ? (
            <Empty description="暂无推荐习题" />
          ) : (
            <List
              dataSource={data.items}
              renderItem={(item, idx) => (
                <Card
                  key={item.exerciseId}
                  style={{ marginBottom: 12 }}
                  size="small"
                  title={
                    <span>
                      第 {idx + 1} 题
                      <Tag color={difficultyColor[item.difficulty]} style={{ marginLeft: 8 }}>
                        {difficultyLabel[item.difficulty] || item.difficulty}
                      </Tag>
                      <Tag>{typeLabel[item.type] || item.type}</Tag>
                    </span>
                  }
                >
                  <MathMarkdown>{item.content}</MathMarkdown>
                  {item.kcDetails && item.kcDetails.length > 0 && (
                    <div style={{ marginTop: 8, padding: '8px 12px', background: '#f6ffed', borderRadius: 6 }}>
                      <div style={{ fontSize: 12, color: '#389e0d', marginBottom: 6 }}>
                        推荐理由
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
                      推荐理由：{item.reason}
                    </div>
                  )}
                </Card>
              )}
            />
          )}
        </>
      )}

      {!loading && !data && courseId && studentId && (
        <Alert message="点击「查看最新推荐」或「生成新推荐」" type="info" showIcon />
      )}

      {!courseId && (
        <Alert message="请先选择课程" type="warning" showIcon />
      )}
    </div>
  )
}
