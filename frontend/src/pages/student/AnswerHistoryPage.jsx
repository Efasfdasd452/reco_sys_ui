import { useEffect, useState } from 'react'
import { List, Tag, Button, Typography, Spin, Card, Popconfirm, message } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import MathMarkdown from '../../components/MathMarkdown'
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

  const handleClear = async () => {
    try {
      await api.learning.clearHistory()
      message.success('答题历史已清除')
      load(0)
    } catch {
      message.error('清除失败')
    }
  }

  if (loading && records.length === 0) return <Spin />

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>答题历史</Typography.Title>
        <Popconfirm
          title="清除全部答题历史"
          description="这将同时重置知识图谱掌握度，确认吗？"
          onConfirm={handleClear}
          okText="确认清除"
          cancelText="取消"
          okButtonProps={{ danger: true }}
        >
          <Button icon={<DeleteOutlined />} danger size="small">清除历史（测试用）</Button>
        </Popconfirm>
      </div>
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
                  <MathMarkdown>{record.answer}</MathMarkdown>
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
