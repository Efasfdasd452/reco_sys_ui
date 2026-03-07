import { useState, useEffect, useCallback } from 'react'
import {
  Card, Button, List, Tag, Modal, Form, Input, Typography,
  Space, Collapse, message, Empty, Tooltip
} from 'antd'
import {
  TeamOutlined, BookOutlined, PlusOutlined, LogoutOutlined, CopyOutlined
} from '@ant-design/icons'
import { api } from '../../api'

const { Text } = Typography

export default function StudentClassroomPage() {
  const [classrooms, setClassrooms] = useState([])
  const [joinClassroomOpen, setJoinClassroomOpen] = useState(false)
  const [joinCourseOpen, setJoinCourseOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [joinClassroomForm] = Form.useForm()
  const [joinCourseForm] = Form.useForm()

  const fetchClassrooms = useCallback(async () => {
    setLoading(true)
    try {
      const list = await api.classroom.joined()
      setClassrooms(list)
    } catch {} finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchClassrooms()
  }, [fetchClassrooms])

  const handleJoinClassroom = async (values) => {
    try {
      await api.classroom.join(values.inviteCode)
      message.success('加入班级成功')
      setJoinClassroomOpen(false)
      joinClassroomForm.resetFields()
      fetchClassrooms()
    } catch {}
  }

  const handleJoinCourse = async (values) => {
    try {
      await api.course.joinByCode(values.inviteCode)
      message.success('加入课程成功')
      setJoinCourseOpen(false)
      joinCourseForm.resetFields()
    } catch {}
  }

  const handleLeave = async (classroomId) => {
    try {
      await api.classroom.leave(classroomId)
      message.success('已退出班级')
      fetchClassrooms()
    } catch {}
  }

  const copyCode = (code) => {
    navigator.clipboard.writeText(code).then(() => message.success('已复制'))
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>我的班级</h2>
        <Space>
          <Button icon={<PlusOutlined />} onClick={() => setJoinCourseOpen(true)}>
            通过邀请码加入课程
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setJoinClassroomOpen(true)}>
            通过邀请码加入班级
          </Button>
        </Space>
      </div>

      {loading ? null : classrooms.length === 0 ? (
        <Card><Empty description="您还未加入任何班级，请使用邀请码加入" /></Card>
      ) : (
        <Collapse
          accordion={false}
          items={classrooms.map(cls => ({
            key: cls.id,
            label: (
              <Space>
                <span style={{ fontWeight: 600 }}>{cls.name}</span>
                <Tag icon={<TeamOutlined />}>{cls.studentCount || 0}人</Tag>
                {!cls.isActive && <Tag color="red">已解散</Tag>}
                <Text type="secondary" style={{ fontSize: 12 }}>
                  教师: {cls.teacherName}
                </Text>
              </Space>
            ),
            extra: (
              <Button
                danger
                size="small"
                icon={<LogoutOutlined />}
                onClick={e => {
                  e.stopPropagation()
                  Modal.confirm({
                    title: '确认退出该班级？',
                    onOk: () => handleLeave(cls.id),
                  })
                }}
              >退出</Button>
            ),
            children: (
              <div>
                {cls.description && (
                  <p style={{ color: '#666', marginBottom: 12 }}>{cls.description}</p>
                )}
                <div style={{ marginBottom: 8 }}>
                  <Text strong><BookOutlined /> 本班课程</Text>
                </div>
                {!cls.courses || cls.courses.length === 0 ? (
                  <Empty description="本班暂无课程" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <List
                    dataSource={cls.courses}
                    renderItem={course => (
                      <List.Item>
                        <List.Item.Meta
                          title={course.courseName}
                          description={course.description}
                        />
                        {course.inviteCode && (
                          <Space>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              课程码:
                            </Text>
                            <Text code style={{ fontSize: 12 }}>{course.inviteCode}</Text>
                            <Tooltip title="复制邀请码">
                              <Button
                                size="small"
                                type="text"
                                icon={<CopyOutlined />}
                                onClick={() => copyCode(course.inviteCode)}
                              />
                            </Tooltip>
                            <Button
                              size="small"
                              type="primary"
                              ghost
                              onClick={() => {
                                joinCourseForm.setFieldValue('inviteCode', course.inviteCode)
                                setJoinCourseOpen(true)
                              }}
                            >加入课程</Button>
                          </Space>
                        )}
                      </List.Item>
                    )}
                  />
                )}
              </div>
            ),
          }))}
        />
      )}

      {/* 加入班级 */}
      <Modal
        title="通过邀请码加入班级"
        open={joinClassroomOpen}
        onCancel={() => setJoinClassroomOpen(false)}
        onOk={() => joinClassroomForm.submit()}
        okText="加入"
      >
        <Form form={joinClassroomForm} onFinish={handleJoinClassroom} layout="vertical">
          <Form.Item name="inviteCode" label="班级邀请码" rules={[{ required: true, message: '请输入邀请码' }]}>
            <Input placeholder="请输入8位班级邀请码" maxLength={8} style={{ textTransform: 'uppercase' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 加入课程 */}
      <Modal
        title="通过邀请码加入课程"
        open={joinCourseOpen}
        onCancel={() => setJoinCourseOpen(false)}
        onOk={() => joinCourseForm.submit()}
        okText="加入"
      >
        <Form form={joinCourseForm} onFinish={handleJoinCourse} layout="vertical">
          <Form.Item name="inviteCode" label="课程邀请码" rules={[{ required: true, message: '请输入邀请码' }]}>
            <Input placeholder="请输入10位课程邀请码" maxLength={10} style={{ textTransform: 'uppercase' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
