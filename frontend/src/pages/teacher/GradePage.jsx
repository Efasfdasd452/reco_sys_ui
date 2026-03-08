import { useEffect, useState } from 'react'
import {
  Card, List, Tag, Button, Form, Input, InputNumber,
  Modal, Select, Typography, message, Descriptions, Divider, Empty
} from 'antd'
import { useTranslation } from 'react-i18next'
import MathMarkdown from '../../components/MathMarkdown'
import { api } from '../../api'

const statusColor = { SUBMITTED: 'orange', GRADING: 'blue', GRADED: 'green', AUTO_GRADED: 'cyan' }
const statusLabel = { SUBMITTED: '待批改', GRADING: '批改中', GRADED: '已批改', AUTO_GRADED: '自动批改' }
const diffColor   = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }
const typeLabel   = { SINGLE_CHOICE: '单选', MULTIPLE_CHOICE: '多选', TRUE_FALSE: '判断', SHORT_ANSWER: '简答', ESSAY: '论述' }

export default function GradePage() {
  const { t } = useTranslation()
  const [courses, setCourses]   = useState([])
  const [courseId, setCourseId] = useState(null)
  const [records, setRecords]   = useState([])
  const [grading, setGrading]   = useState(null)  // 正在批改的记录
  const [loading, setLoading]   = useState(false)
  const [form] = Form.useForm()

  useEffect(() => { api.course.my().then(setCourses) }, [])

  useEffect(() => {
    if (!courseId) return
    setLoading(true)
    api.grade.pending(courseId).then(setRecords).finally(() => setLoading(false))
  }, [courseId])

  const openGrade = (record) => {
    setGrading(record)
    form.setFieldsValue({ score: record.score ?? '', teacherComment: record.teacherComment ?? '' })
  }

  const submitGrade = async (values) => {
    try {
      await api.grade.grade(grading.id, values)
      message.success('批改成功')
      setGrading(null)
      api.grade.pending(courseId).then(setRecords)
    } catch {}
  }

  const reload = () => {
    if (courseId) api.grade.pending(courseId).then(setRecords)
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>批改作业</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 220 }}
          placeholder="请选择课程"
          allowClear
        />
        {courseId && (
          <Button size="small" onClick={reload}>刷新</Button>
        )}
        {records.length > 0 && (
          <Tag color="orange">{records.length} 条待批改</Tag>
        )}
      </div>

      {!courseId ? (
        <Empty description="请先选择课程" />
      ) : (
        <List
          loading={loading}
          dataSource={records}
          locale={{ emptyText: '暂无待批改答题' }}
          renderItem={record => (
            <Card
              key={record.id}
              size="small"
              style={{ marginBottom: 12 }}
              title={
                <span>
                  <Tag color={statusColor[record.status]}>{statusLabel[record.status]}</Tag>
                  <Tag>{typeLabel[record.exerciseType] ?? record.exerciseType}</Tag>
                  {record.exerciseDifficulty && (
                    <Tag color={diffColor[record.exerciseDifficulty]}>{record.exerciseDifficulty}</Tag>
                  )}
                  <span style={{ marginLeft: 8, color: '#888', fontSize: 12 }}>
                    学生：{record.studentName ?? record.userId}
                  </span>
                  <span style={{ marginLeft: 12, color: '#aaa', fontSize: 12 }}>
                    提交时间：{new Date(record.submittedAt).toLocaleString()}
                  </span>
                </span>
              }
              extra={
                (record.status === 'SUBMITTED' || record.status === 'GRADING') && (
                  <Button type="primary" size="small" onClick={() => openGrade(record)}>
                    批改
                  </Button>
                )
              }
            >
              {/* 题目内容 */}
              <div style={{ background: '#fafafa', padding: '8px 12px', borderRadius: 6, marginBottom: 8, borderLeft: '3px solid #1677ff' }}>
                <div style={{ fontSize: 12, color: '#888', marginBottom: 4 }}>题目</div>
                <MathMarkdown>{record.exerciseContent ?? '(无题目内容)'}</MathMarkdown>
              </div>

              {/* 学生答案 */}
              <div style={{ marginBottom: record.exerciseAnswerKey ? 8 : 0 }}>
                <span style={{ fontSize: 12, color: '#888' }}>学生答案：</span>
                <span style={{ fontWeight: 500 }}>{record.answer}</span>
              </div>

              {/* 参考答案（仅教师可见） */}
              {record.exerciseAnswerKey && (
                <div style={{ background: '#f6ffed', padding: '6px 12px', borderRadius: 6, fontSize: 13, color: '#389e0d' }}>
                  <span style={{ fontSize: 12, color: '#888' }}>参考答案：</span>{record.exerciseAnswerKey}
                </div>
              )}

              {/* 已批改结果 */}
              {record.status === 'GRADED' && (
                <div style={{ marginTop: 8, color: '#666', fontSize: 13 }}>
                  得分：<strong>{record.score}</strong>
                  {record.teacherComment && <span style={{ marginLeft: 12 }}>评语：{record.teacherComment}</span>}
                </div>
              )}
            </Card>
          )}
        />
      )}

      {/* 批改弹窗 */}
      <Modal
        open={!!grading}
        title={`批改 — ${grading?.studentName ?? ''} 的作答`}
        onCancel={() => setGrading(null)}
        footer={null}
        width={560}
      >
        {grading && (
          <>
            <Descriptions size="small" column={1} style={{ marginBottom: 12 }}>
              <Descriptions.Item label="题目">
                <MathMarkdown>{grading.exerciseContent}</MathMarkdown>
              </Descriptions.Item>
              <Descriptions.Item label="学生答案">
                <strong>{grading.answer}</strong>
              </Descriptions.Item>
              {grading.exerciseAnswerKey && (
                <Descriptions.Item label="参考答案">
                  <span style={{ color: '#389e0d' }}>{grading.exerciseAnswerKey}</span>
                </Descriptions.Item>
              )}
            </Descriptions>
            <Divider style={{ margin: '8px 0' }} />
            <Form form={form} onFinish={submitGrade} layout="vertical">
              <Form.Item label="分数（0~100）" name="score" rules={[{ required: true, message: '请输入分数' }]}>
                <InputNumber min={0} max={100} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item label="评语（可选）" name="teacherComment">
                <Input.TextArea rows={3} placeholder="输入批改评语..." />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" block>提交批改</Button>
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </div>
  )
}
