import { useEffect, useState } from 'react'
import { List, Tag, Button, Typography, Spin, Card } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import ReactMarkdown from 'react-markdown'
import { api } from '../../api'

const statusColor = { SUBMITTED: 'orange', GRADING: 'blue', GRADED: 'green', AUTO_GRADED: 'cyan' }
const statusLabel = { SUBMITTED: '待批改', GRADING: '批改中', GRADED: '已批改', AUTO_GRADED: '自动批改' }
const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }

export default function AnswerHistoryPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [records, setRecords] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  const load = (p = 0) => {
    setLoading(true)
    api.learning.history(p, 20)
      .then(data => {
        setRecords(data?.content || [])
        setTotal(data?.totalElements || 0)
        setPage(p)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  if (loading && records.length === 0) return <Spin />

  return (
    <div>
      <Typography.Title level={4} style={{ marginBottom: 16 }}>答题历史</Typography.Title>
      <List
        dataSource={records}
        locale={{ emptyText: '暂无答题记录' }}
        loading={loading}
        pagination={{
          total,
          pageSize: 20,
          current: page + 1,
          onChange: p => load(p - 1),
          showTotal: t => `共 ${t} 条`,
        }}
        renderItem={record => (
          <Card key={record.id} size="small" style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ flex: 1 }}>
                <Tag color={statusColor[record.status]}>{statusLabel[record.status]}</Tag>
                {record.exerciseType && <Tag>{t(`exercise.type.${record.exerciseType}`)}</Tag>}
                {record.exerciseDifficulty && (
                  <Tag color={difficultyColor[record.exerciseDifficulty]}>
                    {t(`exercise.difficulty.${record.exerciseDifficulty}`)}
                  </Tag>
                )}
                {record.score != null && (
                  <Tag color="gold">得分: {record.score}</Tag>
                )}
                <span style={{ marginLeft: 8, color: '#aaa', fontSize: 12 }}>
                  {record.submittedAt ? new Date(record.submittedAt).toLocaleString() : ''}
                </span>
                <div style={{ marginTop: 8, color: '#333', fontSize: 13 }}>
                  <strong>我的答案：</strong>
                  <ReactMarkdown>{record.answer}</ReactMarkdown>
                </div>
                {record.teacherComment && (
                  <div style={{ marginTop: 8, color: '#1677ff', fontSize: 13 }}>
                    <strong>教师评语：</strong>{record.teacherComment}
                  </div>
                )}
              </div>
              <Button
                size="small"
                type="link"
                onClick={() => navigate(`/exercises/${record.exerciseId}`)}
              >
                查看题目
              </Button>
            </div>
          </Card>
        )}
      />
    </div>
  )
}
