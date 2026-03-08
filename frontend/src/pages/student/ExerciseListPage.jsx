import { useEffect, useRef, useState } from 'react'
import { Select, List, Tag, Button, Typography, Input, Space } from 'antd'
import { CheckCircleOutlined, SearchOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'
import MathMarkdown from '../../components/MathMarkdown'

const difficultyColor = { EASY: 'green', MEDIUM: 'orange', HARD: 'red' }
const PAGE_SIZE = 10

function getStem(content) {
  if (!content) return ''
  return content.split('\n').find(l => l.trim() && !/^[A-D]\./.test(l.trim())) || ''
}

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
  const [keyword, setKeyword] = useState('')
  const [sortDir, setSortDir] = useState('asc')
  const searchRef = useRef(null)

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) setCourseId(list[0].id)
    })
    api.learning.answeredIds().then(ids => setAnsweredIds(new Set(ids))).catch(() => {})
  }, [])

  useEffect(() => {
    if (courseId) load(0, keyword, sortDir)
  }, [courseId])

  const load = (p = 0, kw = keyword, dir = sortDir) => {
    setLoading(true)
    api.exercise.listByCourse(courseId, p, PAGE_SIZE, kw, 'pyExIndex', dir)
      .then(data => {
        setExercises(data?.content || [])
        setTotal(data?.totalElements || 0)
        setPage(p)
      })
      .finally(() => setLoading(false))
  }

  const onSearch = (val) => {
    setKeyword(val)
    load(0, val, sortDir)
  }

  const toggleSort = () => {
    const next = sortDir === 'asc' ? 'desc' : 'asc'
    setSortDir(next)
    load(0, keyword, next)
  }

  const exNum = (ex) => ex.pyExIndex != null ? `ex${ex.pyExIndex}` : `#${ex.id}`

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('exercise.title')}</Typography.Title>
        <Select
          value={courseId}
          onChange={val => { setCourseId(val); setKeyword(''); setSortDir('asc') }}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
          placeholder="选择课程"
        />
        <Input.Search
          ref={searchRef}
          placeholder="搜索题目内容..."
          allowClear
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
          onSearch={onSearch}
          onChange={e => { if (!e.target.value) onSearch('') }}
        />
        <Button
          icon={sortDir === 'asc' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
          onClick={toggleSort}
          title={sortDir === 'asc' ? '当前：序号升序' : '当前：序号降序'}
        >
          序号{sortDir === 'asc' ? '↑' : '↓'}
        </Button>
        {total > 0 && (
          <Typography.Text type="secondary">共 {total} 题</Typography.Text>
        )}
      </div>

      <List
        loading={loading}
        dataSource={exercises}
        locale={{ emptyText: courseId ? (keyword ? `未找到含"${keyword}"的题目` : '该课程暂无习题') : '请先选择课程' }}
        pagination={{
          total,
          pageSize: PAGE_SIZE,
          current: page + 1,
          onChange: p => load(p - 1),
          showSizeChanger: false,
          showQuickJumper: true,
        }}
        renderItem={ex => (
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
                <Space size={4} wrap>
                  <Typography.Text type="secondary" style={{ fontFamily: 'monospace', fontSize: 12 }}>
                    {exNum(ex)}
                  </Typography.Text>
                  <Tag>{t(`exercise.type.${ex.type}`)}</Tag>
                  <Tag color={difficultyColor[ex.difficulty]}>{t(`exercise.difficulty.${ex.difficulty}`)}</Tag>
                  {answeredIds.has(ex.id) && (
                    <Tag color="green" icon={<CheckCircleOutlined />}>已答</Tag>
                  )}
                  {ex.knowledgePointNames?.slice(0, 3).map(name => (
                    <Tag key={name} color="blue">{name}</Tag>
                  ))}
                  {ex.knowledgePointNames?.length > 3 && (
                    <Tag color="blue">+{ex.knowledgePointNames.length - 3}</Tag>
                  )}
                </Space>
              }
              description={
                <Typography.Text ellipsis style={{ maxWidth: '80%', fontSize: 13 }}>
                  <MathMarkdown inline>{getStem(ex.content)}</MathMarkdown>
                </Typography.Text>
              }
            />
          </List.Item>
        )}
      />
    </div>
  )
}
