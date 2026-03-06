import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Modal, Form, Input, Select, Space, Tag,
  Popconfirm, message, Typography,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import MDEditor from '@uiw/react-md-editor'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }

export default function ExerciseManagePage() {
  const { t } = useTranslation()
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [exercises, setExercises] = useState([])
  const [kpList, setKpList] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()
  const [content, setContent] = useState('')
  const [answerKey, setAnswerKey] = useState('')

  useEffect(() => {
    api.course.my().then(setCourses)
  }, [])

  useEffect(() => {
    if (courseId) {
      loadExercises(0)
      api.knowledge.listByCourse(courseId).then(flattenKp)
    }
  }, [courseId])

  const flattenKp = (tree) => {
    const flat = []
    const walk = (nodes) => nodes.forEach(n => { flat.push(n); if (n.children) walk(n.children) })
    walk(tree)
    setKpList(flat)
  }

  const loadExercises = (p = 0) => {
    api.exercise.listByCourse(courseId, p, 20)
      .then(data => { setExercises(data?.content || []); setTotal(data?.totalElements || 0); setPage(p) })
  }

  const openCreate = () => {
    setEditing(null)
    setContent('')
    setAnswerKey('')
    form.resetFields()
    form.setFieldsValue({ courseId, difficulty: 'MEDIUM', type: 'SHORT_ANSWER' })
    setModalOpen(true)
  }

  const openEdit = (ex) => {
    setEditing(ex)
    setContent(ex.content || '')
    setAnswerKey('')
    form.setFieldsValue({
      courseId: ex.courseId,
      type: ex.type,
      difficulty: ex.difficulty,
      knowledgePointIds: ex.knowledgePointIds || [],
    })
    setModalOpen(true)
  }

  const handleSubmit = async (values) => {
    const payload = { ...values, content, answerKey }
    try {
      if (editing) {
        await api.exercise.update(editing.id, payload)
        message.success('已更新')
      } else {
        await api.exercise.create(payload)
        message.success('已创建')
      }
      setModalOpen(false)
      loadExercises(page)
    } catch {}
  }

  const handleDelete = async (id) => {
    await api.exercise.delete(id)
    message.success('已删除')
    loadExercises(page)
  }

  const columns = [
    {
      title: '类型',
      dataIndex: 'type',
      width: 110,
      render: v => <Tag>{t(`exercise.type.${v}`)}</Tag>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 80,
      render: v => <Tag color={difficultyColor[v]}>{t(`exercise.difficulty.${v}`)}</Tag>,
    },
    {
      title: '题目（前60字）',
      dataIndex: 'content',
      render: c => c?.replace(/[#*`\[\]!]/g, '').slice(0, 60) + (c?.length > 60 ? '...' : ''),
    },
    {
      title: '知识点数',
      dataIndex: 'knowledgePointIds',
      width: 90,
      render: ids => ids?.length || 0,
    },
    {
      title: '操作',
      width: 130,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>习题管理</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
        {courseId && (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建习题
          </Button>
        )}
      </div>

      <Table
        dataSource={exercises}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={{
          total,
          pageSize: 20,
          current: page + 1,
          onChange: p => loadExercises(p - 1),
        }}
      />

      <Modal
        open={modalOpen}
        title={editing ? '编辑习题' : '新建习题'}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={800}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item label="课程" name="courseId" rules={[{ required: true }]}>
            <Select options={courses.map(c => ({ value: c.id, label: c.name }))} disabled={!!editing} />
          </Form.Item>
          <Space style={{ width: '100%' }} size={16}>
            <Form.Item label="类型" name="type" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select options={[
                { value: 'SINGLE_CHOICE', label: '单选题' },
                { value: 'MULTIPLE_CHOICE', label: '多选题' },
                { value: 'FILL_BLANK', label: '填空题' },
                { value: 'SHORT_ANSWER', label: '简答题' },
              ]} />
            </Form.Item>
            <Form.Item label="难度" name="difficulty" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Select options={[
                { value: 'EASY', label: '简单' },
                { value: 'MEDIUM', label: '中等' },
                { value: 'HARD', label: '困难' },
              ]} />
            </Form.Item>
          </Space>
          <Form.Item label="关联知识点" name="knowledgePointIds">
            <Select
              mode="multiple"
              options={kpList.map(kp => ({ value: kp.id, label: kp.name }))}
              placeholder="选择知识点（可多选）"
            />
          </Form.Item>
          <Form.Item label="题目内容（支持Markdown）" required>
            <MDEditor value={content} onChange={setContent} height={200} />
          </Form.Item>
          <Form.Item label="参考答案（客观题必填，用于自动批改）">
            <Input.TextArea
              value={answerKey}
              onChange={e => setAnswerKey(e.target.value)}
              rows={2}
              placeholder="单选：A  多选：AB  填空：答案文本"
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              {editing ? '保存修改' : '创建习题'}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
