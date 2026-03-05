import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Form, Input, InputNumber, Modal, Select, Typography, message } from 'antd'
import { useTranslation } from 'react-i18next'
import ReactMarkdown from 'react-markdown'
import { api } from '../../api'

const statusColor = { SUBMITTED: 'orange', GRADING: 'blue', GRADED: 'green', AUTO_GRADED: 'cyan' }
const statusLabel = { SUBMITTED: '待批改', GRADING: '批改中', GRADED: '已批改', AUTO_GRADED: '自动批改' }

export default function GradePage() {
  const { t } = useTranslation()
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [records, setRecords] = useState([])
  const [grading, setGrading] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    api.course.list().then(setCourses)
  }, [])

  useEffect(() => {
    if (courseId) api.grade.pending(courseId).then(setRecords)
  }, [courseId])

  const openGrade = (record) => {
    setGrading(record)
    form.resetFields()
  }

  const submitGrade = async (values) => {
    try {
      await api.grade.grade(grading.id, values)
      message.success(t('grade.gradeSuccess'))
      setGrading(null)
      if (courseId) api.grade.pending(courseId).then(setRecords)
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('nav.grade')}</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
      </div>

      <List
        dataSource={records}
        locale={{ emptyText: courseId ? '暂无待批改答题' : '请先选择课程' }}
        renderItem={record => (
          <Card key={record.id} size="small" style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ flex: 1 }}>
                <Tag color={statusColor[record.status]}>{statusLabel[record.status]}</Tag>
                <span style={{ marginLeft: 8, color: '#666', fontSize: 13 }}>
                  提交时间: {new Date(record.submittedAt).toLocaleString()}
                </span>
                <div style={{ marginTop: 8 }}>
                  <strong>学生答案：</strong>
                  <ReactMarkdown>{record.answer}</ReactMarkdown>
                </div>
              </div>
              {record.status === 'SUBMITTED' && (
                <Button type="primary" size="small" onClick={() => openGrade(record)}>
                  批改
                </Button>
              )}
            </div>
          </Card>
        )}
      />

      <Modal
        open={!!grading}
        title="批改答题"
        onCancel={() => setGrading(null)}
        footer={null}
      >
        <Form form={form} onFinish={submitGrade} layout="vertical">
          <Form.Item label={t('grade.score')} name="score" rules={[{ required: true }]}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label={t('grade.comment')} name="teacherComment">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>{t('grade.submit')}</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
