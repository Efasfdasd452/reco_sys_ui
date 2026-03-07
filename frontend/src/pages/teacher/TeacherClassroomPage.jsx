import { useState, useEffect, useCallback } from 'react'
import {
  Row, Col, Card, Button, List, Tag, Modal, Form, Input, Tabs,
  Table, Popconfirm, message, Select, Space, Typography, Tooltip, Empty
} from 'antd'
import {
  PlusOutlined, DeleteOutlined, UserDeleteOutlined, TeamOutlined,
  BookOutlined, CopyOutlined, UserAddOutlined
} from '@ant-design/icons'
import { api } from '../../api'
import { useAuth } from '../../store/authStore'

const { Text } = Typography

export default function TeacherClassroomPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [classrooms, setClassrooms] = useState([])
  const [selected, setSelected] = useState(null)
  const [detail, setDetail] = useState(null)
  const [allCourses, setAllCourses] = useState([])
  const [loading, setLoading] = useState(false)

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [addCourseModalOpen, setAddCourseModalOpen] = useState(false)
  const [enrollModalOpen, setEnrollModalOpen] = useState(false)
  const [enrollTargetCourse, setEnrollTargetCourse] = useState(null)
  const [enrollAction, setEnrollAction] = useState('enroll') // 'enroll' | 'unenroll'
  const [selectedStudentIds, setSelectedStudentIds] = useState([])
  const [createForm] = Form.useForm()
  const [addCourseForm] = Form.useForm()

  const fetchClassrooms = useCallback(async () => {
    try {
      const list = isAdmin ? await api.classroom.all() : await api.classroom.my()
      setClassrooms(list)
    } catch {}
  }, [isAdmin])

  const fetchDetail = useCallback(async (id) => {
    if (!id) return
    setLoading(true)
    try {
      const d = await api.classroom.detail(id)
      setDetail(d)
    } catch {} finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchClassrooms()
    api.course.my().then(setAllCourses).catch(() => {})
  }, [fetchClassrooms])

  useEffect(() => {
    fetchDetail(selected)
  }, [selected, fetchDetail])

  const handleCreate = async (values) => {
    try {
      await api.classroom.create(values)
      message.success('班级创建成功')
      setCreateModalOpen(false)
      createForm.resetFields()
      fetchClassrooms()
    } catch {}
  }

  const handleDissolve = async (id) => {
    try {
      await api.classroom.dissolve(id)
      message.success('班级已解散')
      if (selected === id) { setSelected(null); setDetail(null) }
      fetchClassrooms()
    } catch {}
  }

  const handleRemoveStudent = async (studentId) => {
    try {
      await api.classroom.removeStudent(selected, studentId)
      message.success('已移除学生')
      fetchDetail(selected)
    } catch {}
  }

  const handleAddCourse = async (values) => {
    try {
      await api.classroom.addCourse(selected, values.courseId)
      message.success('课程已加入班级')
      setAddCourseModalOpen(false)
      addCourseForm.resetFields()
      fetchDetail(selected)
    } catch {}
  }

  const handleRemoveCourse = async (courseId) => {
    try {
      await api.classroom.removeCourse(selected, courseId)
      message.success('已移除课程')
      fetchDetail(selected)
    } catch {}
  }

  const openEnrollModal = (course, action) => {
    setEnrollTargetCourse(course)
    setEnrollAction(action)
    setSelectedStudentIds([])
    setEnrollModalOpen(true)
  }

  const handleEnroll = async () => {
    try {
      if (enrollAction === 'enroll') {
        await api.classroom.enrollStudents(selected, enrollTargetCourse.courseId, selectedStudentIds)
        message.success('学生已加入课程')
      } else {
        await api.classroom.unenrollStudents(selected, enrollTargetCourse.courseId, selectedStudentIds)
        message.success('学生已移出课程')
      }
      setEnrollModalOpen(false)
    } catch {}
  }

  const copyCode = (code) => {
    navigator.clipboard.writeText(code).then(() => message.success('已复制'))
  }

  const studentCols = [
    { title: '用户名', dataIndex: 'username' },
    { title: '昵称', dataIndex: 'nickname' },
    {
      title: '操作',
      render: (_, record) => (
        <Popconfirm title="确认移除该学生？" onConfirm={() => handleRemoveStudent(record.userId)}>
          <Button danger size="small" icon={<UserDeleteOutlined />}>移除</Button>
        </Popconfirm>
      ),
    },
  ]

  const courseCols = [
    { title: '课程名称', dataIndex: 'courseName' },
    {
      title: '课程邀请码',
      dataIndex: 'inviteCode',
      render: (code) => code ? (
        <Space>
          <Text code>{code}</Text>
          <Tooltip title="复制">
            <Button size="small" type="text" icon={<CopyOutlined />} onClick={() => copyCode(code)} />
          </Tooltip>
        </Space>
      ) : '-',
    },
    {
      title: '操作',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<UserAddOutlined />}
            onClick={() => openEnrollModal(record, 'enroll')}
          >加入课程</Button>
          <Button
            size="small"
            danger
            icon={<UserDeleteOutlined />}
            onClick={() => openEnrollModal(record, 'unenroll')}
          >移出课程</Button>
          <Popconfirm title="从班级移除该课程？" onConfirm={() => handleRemoveCourse(record.courseId)}>
            <Button danger size="small" icon={<DeleteOutlined />}>移除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // courses not yet in classroom
  const availableCourses = allCourses.filter(
    c => !detail?.courses?.some(dc => dc.courseId === c.id)
  )

  const studentOptions = (detail?.students || []).map(s => ({
    label: `${s.username}${s.nickname ? ' (' + s.nickname + ')' : ''}`,
    value: s.userId,
  }))

  return (
    <div>
      <Row gutter={16}>
        {/* 班级列表 */}
        <Col span={7}>
          <Card
            title="班级列表"
            extra={
              <Button
                type="primary"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => setCreateModalOpen(true)}
              >新建</Button>
            }
          >
            {classrooms.length === 0 ? (
              <Empty description="暂无班级" />
            ) : (
              <List
                dataSource={classrooms}
                renderItem={item => (
                  <List.Item
                    style={{
                      cursor: 'pointer',
                      background: selected === item.id ? '#e6f4ff' : undefined,
                      padding: '8px 12px',
                      borderRadius: 6,
                    }}
                    onClick={() => setSelected(item.id)}
                    actions={[
                      <Popconfirm
                        key="dissolve"
                        title="确认解散该班级？"
                        onConfirm={(e) => { e.stopPropagation(); handleDissolve(item.id) }}
                      >
                        <Button
                          danger size="small" type="text"
                          icon={<DeleteOutlined />}
                          onClick={e => e.stopPropagation()}
                        />
                      </Popconfirm>
                    ]}
                  >
                    <List.Item.Meta
                      title={item.name}
                      description={
                        <Space>
                          <Tag icon={<TeamOutlined />}>{item.studentCount || 0}人</Tag>
                          {!item.isActive && <Tag color="red">已解散</Tag>}
                          <Tooltip title="复制邀请码">
                            <Text
                              type="secondary"
                              style={{ fontSize: 12, cursor: 'pointer' }}
                              onClick={e => { e.stopPropagation(); copyCode(item.inviteCode) }}
                            >
                              码:{item.inviteCode}
                            </Text>
                          </Tooltip>
                        </Space>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>

        {/* 班级详情 */}
        <Col span={17}>
          {!selected ? (
            <Card><Empty description="请选择一个班级" /></Card>
          ) : (
            <Card
              title={detail?.name || '班级详情'}
              extra={
                detail && (
                  <Space>
                    <Text type="secondary">班级码:</Text>
                    <Text code>{detail.inviteCode}</Text>
                    <Button
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyCode(detail.inviteCode)}
                    >复制</Button>
                  </Space>
                )
              }
              loading={loading}
            >
              <Tabs
                items={[
                  {
                    key: 'students',
                    label: <span><TeamOutlined />学生管理 ({detail?.students?.length || 0})</span>,
                    children: (
                      <Table
                        rowKey="userId"
                        dataSource={detail?.students || []}
                        columns={studentCols}
                        size="small"
                        pagination={false}
                      />
                    ),
                  },
                  {
                    key: 'courses',
                    label: <span><BookOutlined />课程管理 ({detail?.courses?.length || 0})</span>,
                    children: (
                      <>
                        <div style={{ marginBottom: 12 }}>
                          <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={() => setAddCourseModalOpen(true)}
                          >添加课程</Button>
                        </div>
                        <Table
                          rowKey="courseId"
                          dataSource={detail?.courses || []}
                          columns={courseCols}
                          size="small"
                          pagination={false}
                        />
                      </>
                    ),
                  },
                ]}
              />
            </Card>
          )}
        </Col>
      </Row>

      {/* 创建班级 */}
      <Modal
        title="新建班级"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => createForm.submit()}
      >
        <Form form={createForm} onFinish={handleCreate} layout="vertical">
          <Form.Item name="name" label="班级名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 添加课程到班级 */}
      <Modal
        title="添加课程到班级"
        open={addCourseModalOpen}
        onCancel={() => setAddCourseModalOpen(false)}
        onOk={() => addCourseForm.submit()}
        okText="确认添加"
      >
        <Form form={addCourseForm} layout="vertical" onFinish={handleAddCourse}>
          <Form.Item name="courseId" label="选择课程" rules={[{ required: true, message: '请选择课程' }]}>
            <Select
              placeholder="请选择要加入的课程"
              options={availableCourses.map(c => ({ label: c.name, value: c.id }))}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 批量加入/移出课程 */}
      <Modal
        title={`${enrollAction === 'enroll' ? '批量加入' : '批量移出'}课程: ${enrollTargetCourse?.courseName || ''}`}
        open={enrollModalOpen}
        onCancel={() => setEnrollModalOpen(false)}
        onOk={handleEnroll}
        okText={enrollAction === 'enroll' ? '加入课程' : '移出课程'}
        okButtonProps={{ danger: enrollAction === 'unenroll' }}
      >
        <p style={{ color: '#666', marginBottom: 12 }}>
          不选择学生则对班级<strong>全部学生</strong>操作
        </p>
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="选择学生（不选则全部）"
          options={studentOptions}
          value={selectedStudentIds}
          onChange={setSelectedStudentIds}
          allowClear
        />
      </Modal>
    </div>
  )
}
