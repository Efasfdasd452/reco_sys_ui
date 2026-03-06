import { useEffect, useState } from 'react'
import {
  Tree, Button, Modal, Form, Input, Select, Space, message, Typography, Card,
} from 'antd'
import { PlusOutlined, DeleteOutlined, ApartmentOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

export default function KnowledgeManagePage() {
  const { t } = useTranslation()
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [kpList, setKpList] = useState([])
  const [treeData, setTreeData] = useState([])
  const [modalOpen, setModalOpen] = useState(false)
  const [relModalOpen, setRelModalOpen] = useState(false)
  const [form] = Form.useForm()
  const [relForm] = Form.useForm()
  const [flat, setFlat] = useState([])

  useEffect(() => {
    api.course.list().then(setCourses)
  }, [])

  useEffect(() => {
    if (courseId) load()
  }, [courseId])

  const load = () => {
    api.knowledge.listByCourse(courseId).then(list => {
      setKpList(list)
      setTreeData(toTreeData(list))
      const f = []
      const walk = nodes => nodes.forEach(n => { f.push(n); if (n.children) walk(n.children) })
      walk(list)
      setFlat(f)
    })
  }

  const toTreeData = (nodes) =>
    nodes.map(n => ({
      title: (
        <Space>
          <span>{n.name}</span>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={e => { e.stopPropagation(); handleDelete(n.id) }}
          />
        </Space>
      ),
      key: n.id,
      children: n.children ? toTreeData(n.children) : [],
    }))

  const handleCreate = async (values) => {
    await api.knowledge.create({ ...values, courseId })
    message.success('已创建')
    setModalOpen(false)
    load()
  }

  const handleDelete = async (id) => {
    await api.knowledge.delete(id)
    message.success('已删除')
    load()
  }

  const handleAddRelation = async (values) => {
    await api.knowledge.addRelation(values.fromId, values.toId, values.type)
    message.success('关系已添加')
    setRelModalOpen(false)
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>知识点管理</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
        {courseId && (
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setModalOpen(true) }}>
              新增知识点
            </Button>
            <Button icon={<ApartmentOutlined />} onClick={() => { relForm.resetFields(); setRelModalOpen(true) }}>
              添加关系
            </Button>
          </Space>
        )}
      </div>

      {courseId && (
        <Card>
          {treeData.length > 0 ? (
            <Tree treeData={treeData} defaultExpandAll showLine />
          ) : (
            <div style={{ color: '#aaa', textAlign: 'center', padding: 32 }}>暂无知识点</div>
          )}
        </Card>
      )}

      {/* 新建知识点 */}
      <Modal
        open={modalOpen}
        title="新增知识点"
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item label="名称" name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="父知识点（留空为顶层）" name="parentId">
            <Select
              allowClear
              options={flat.map(kp => ({ value: kp.id, label: kp.name }))}
              placeholder="选择父知识点"
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>创建</Button>
        </Form>
      </Modal>

      {/* 添加知识点关系 */}
      <Modal
        open={relModalOpen}
        title="添加知识点关系（Neo4j图谱）"
        onCancel={() => setRelModalOpen(false)}
        footer={null}
      >
        <Form form={relForm} onFinish={handleAddRelation} layout="vertical">
          <Form.Item label="源知识点" name="fromId" rules={[{ required: true }]}>
            <Select options={flat.map(kp => ({ value: kp.id, label: kp.name }))} />
          </Form.Item>
          <Form.Item label="关系类型" name="type" rules={[{ required: true }]}>
            <Select options={[
              { value: 'PREREQUISITE_OF', label: '前置关系（A 是 B 的前置）' },
              { value: 'RELATED_TO', label: '相关关系' },
            ]} />
          </Form.Item>
          <Form.Item label="目标知识点" name="toId" rules={[{ required: true }]}>
            <Select options={flat.map(kp => ({ value: kp.id, label: kp.name }))} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>添加关系</Button>
        </Form>
      </Modal>
    </div>
  )
}
