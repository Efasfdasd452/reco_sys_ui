import { useEffect, useRef, useState } from 'react'
import { Select, Typography, Spin, Card } from 'antd'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

export default function KnowledgeGraphPage() {
  const { t } = useTranslation()
  const containerRef = useRef(null)
  const graphRef = useRef(null)
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) setCourseId(list[0].id)
    })
  }, [])

  useEffect(() => {
    if (courseId) loadGraph()
  }, [courseId])

  const loadGraph = async () => {
    setLoading(true)
    try {
      const data = await api.knowledge.myGraph(courseId)
      renderGraph(data)
    } finally {
      setLoading(false)
    }
  }

  const renderGraph = async (data) => {
    const { Graph } = await import('@antv/g6')
    if (graphRef.current) { graphRef.current.destroy() }
    if (!containerRef.current || !data?.nodes?.length) return

    const nodes = data.nodes.map(n => ({
      id: n.id,
      label: n.label,
      style: {
        fill: masteryColor(n.masteryLevel),
        stroke: '#1677ff',
        radius: 20,
      },
    }))
    const edges = data.edges.map(e => ({
      source: e.source,
      target: e.target,
      label: e.label,
    }))

    graphRef.current = new Graph({
      container: containerRef.current,
      width: containerRef.current.offsetWidth,
      height: 500,
      data: { nodes, edges },
      node: { type: 'circle', style: { labelText: d => d.label } },
      edge: { type: 'line', style: { labelText: d => d.label } },
      layout: { type: 'force' },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    })
    graphRef.current.render()
  }

  const masteryColor = (level) => {
    if (level >= 0.8) return '#52c41a'
    if (level >= 0.5) return '#faad14'
    if (level >= 0.2) return '#1677ff'
    return '#d9d9d9'
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('knowledge.graph')}</Typography.Title>
        <Select
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 200 }}
        />
      </div>
      <Card>
        <div style={{ marginBottom: 8, fontSize: 13, color: '#666' }}>
          <span style={{ marginRight: 16 }}>颜色说明：</span>
          {[['#52c41a', '高掌握度(≥80%)'], ['#faad14', '中掌握度(≥50%)'], ['#1677ff', '低掌握度(≥20%)'], ['#d9d9d9', '未学习']].map(([c, l]) => (
            <span key={c} style={{ marginRight: 12 }}>
              <span style={{ display: 'inline-block', width: 12, height: 12, background: c, borderRadius: '50%', marginRight: 4, verticalAlign: 'middle' }} />
              {l}
            </span>
          ))}
        </div>
        {loading ? <Spin /> : <div ref={containerRef} style={{ width: '100%', height: 500 }} />}
      </Card>
    </div>
  )
}
