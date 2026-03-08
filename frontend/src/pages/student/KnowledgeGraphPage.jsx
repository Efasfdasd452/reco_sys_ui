import { useEffect, useRef, useState } from 'react'
import { Select, Spin, Empty } from 'antd'
import { ReloadOutlined, FullscreenOutlined, FullscreenExitOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { api } from '../../api'

// ── 类型元数据 ────────────────────────────────────────────────────
const NODE_TYPES = [
  { key: 'student',  label: '学生中心', color: '#faad14' },
  { key: 'kc',       label: '知识点',   color: '#1677ff' },
  { key: 'exercise', label: '习题',     color: '#722ed1' },
]
const EDGE_TYPES = [
  { key: 'mlkc',   label: '掌握度 mlkc', color: '#1677ff', dash: false },
  { key: 'pkc',    label: '出现率 pkc',  color: '#fa8c16', dash: true  },
  { key: 'exfr',   label: '遗忘率 exfr', color: '#722ed1', dash: false },
  { key: 'covers', label: '覆盖关系',    color: '#ccc',    dash: false },
]

// ─────────────────────────────────────────────────────────────────
export default function KnowledgeGraphPage() {
  const { t } = useTranslation()
  const containerRef = useRef(null)
  const graphRef     = useRef(null)

  const [courses,        setCourses]        = useState([])
  const [courseId,       setCourseId]       = useState(null)
  const [loading,        setLoading]        = useState(false)
  const [empty,          setEmpty]          = useState(false)
  const [graphData,      setGraphData]      = useState(null)
  const [shownNodeTypes, setShownNodeTypes] = useState(new Set(['student', 'kc', 'exercise']))
  const [shownEdgeTypes, setShownEdgeTypes] = useState(new Set(['mlkc', 'pkc', 'exfr', 'covers']))
  const [fullscreen,     setFullscreen]     = useState(false)
  // 空数组 = 显示全部；非空 = 只显示选中的
  const [selectedKcIds,  setSelectedKcIds]  = useState([])
  const [selectedExIds,  setSelectedExIds]  = useState([])

  useEffect(() => {
    api.course.my().then(list => {
      setCourses(list)
      if (list.length > 0) setCourseId(list[0].id)
    })
  }, [])

  useEffect(() => { if (courseId) loadGraph() }, [courseId])

  useEffect(() => {
    if (graphData) renderGraph(graphData, shownNodeTypes, shownEdgeTypes, fullscreen, selectedKcIds, selectedExIds)
  }, [graphData, shownNodeTypes, shownEdgeTypes, fullscreen, selectedKcIds, selectedExIds])

  const loadGraph = async () => {
    setLoading(true); setEmpty(false)
    try {
      const data = await api.knowledge.myGraph(courseId)
      const hasRealData = data?.nodes?.some(n => n.nodeType !== 'student')
      if (!hasRealData) { setEmpty(true); setGraphData(null); return }
      setSelectedKcIds([])
      setSelectedExIds([])
      setGraphData(data)
    } finally { setLoading(false) }
  }

  const toggleNodeType = key => setShownNodeTypes(prev => {
    const s = new Set(prev); s.has(key) ? s.delete(key) : s.add(key); return s
  })
  const toggleEdgeType = key => setShownEdgeTypes(prev => {
    const s = new Set(prev); s.has(key) ? s.delete(key) : s.add(key); return s
  })

  // ── 过滤 ──────────────────────────────────────────────────────
  const filterData = (data, nt, et, kcIds = [], exIds = []) => {
    const nodes = data.nodes.filter(n => {
      if (!nt.has(n.nodeType)) return false
      if (n.nodeType === 'kc'       && kcIds.length > 0 && !kcIds.includes(n.id)) return false
      if (n.nodeType === 'exercise' && exIds.length > 0 && !exIds.includes(n.id)) return false
      return true
    })
    const nodeIds = new Set(nodes.map(n => n.id))
    const edges   = data.edges.filter(e => et.has(e.edgeType) && nodeIds.has(e.source) && nodeIds.has(e.target))
    return { nodes, edges }
  }

  // ── 预算布局 ──────────────────────────────────────────────────
  const computePositions = (nodes, edges, W, H) => {
    const cx = W / 2, cy = H / 2
    const kcR = Math.min(W, H) * 0.27
    const exR = Math.min(W, H) * 0.46
    const pos = {}
    pos['student'] = { x: cx, y: cy }

    const kcNodes     = nodes.filter(n => n.nodeType === 'kc')
    const exNodes     = nodes.filter(n => n.nodeType === 'exercise')
    const coversEdges = edges.filter(e => e.edgeType === 'covers')

    kcNodes.forEach((n, i) => {
      const a = (2 * Math.PI * i / Math.max(kcNodes.length, 1)) - Math.PI / 2
      pos[n.id] = { x: cx + kcR * Math.cos(a), y: cy + kcR * Math.sin(a) }
    })

    const cnt = {}
    exNodes.forEach(ex => {
      const linked = coversEdges.filter(e => e.source === ex.id).map(e => e.target)
      let base = linked.length > 0 && pos[linked[0]]
        ? Math.atan2(pos[linked[0]].y - cy, pos[linked[0]].x - cx)
        : Math.random() * 2 * Math.PI
      const k = Math.round(base * 8)
      cnt[k] = (cnt[k] || 0) + 1
      const off = (cnt[k] - 1) * 0.22
      pos[ex.id] = { x: cx + exR * Math.cos(base + off), y: cy + exR * Math.sin(base + off) }
    })
    return pos
  }

  // ── 渲染 G6 ───────────────────────────────────────────────────
  const renderGraph = async (data, nt, et, isFullscreen, kcIds = [], exIds = []) => {
    const { Graph } = await import('@antv/g6')
    if (graphRef.current) { graphRef.current.destroy(); graphRef.current = null }
    if (!containerRef.current) return

    const filtered = filterData(data, nt, et, kcIds, exIds)
    if (filtered.nodes.length === 0) return

    const W = containerRef.current.offsetWidth || 800
    const H = graphHeight(isFullscreen)
    const pos = computePositions(filtered.nodes, filtered.edges, W, H)
    const raw = data.nodes

    // G6 v5 坐标必须在 style.x / style.y
    const gNodes = filtered.nodes.map(n => ({
      id: n.id,
      nodeType: n.nodeType,
      style: {
        x:    (pos[n.id] ?? { x: W / 2 }).x,
        y:    (pos[n.id] ?? { y: H / 2 }).y,
        ...nodeStyle(n),
      },
    }))

    const gEdges = filtered.edges.map((e, i) => ({
      id:       `e${i}`,
      source:   e.source,
      target:   e.target,
      edgeType: e.edgeType,
      label:    e.label ?? '',
      style:    edgeStyle(e),
    }))

    graphRef.current = new Graph({
      container: containerRef.current,
      width: W, height: H,
      data: { nodes: gNodes, edges: gEdges },
      node: {
        type: 'circle',
        style: {
          labelText:       d => nodeLabel(d, raw.find(n => n.id === d.id)),
          labelFontSize:   d => d.nodeType === 'student' ? 13 : d.nodeType === 'kc' ? 11 : 9,
          labelPlacement:  d => d.nodeType === 'student' ? 'center' : 'bottom',
          labelFontWeight: d => d.nodeType === 'student' ? 700 : 400,
          labelFill:       d => d.nodeType === 'student' ? '#7d4e00' : '#333',
        },
      },
      edge: {
        type: 'quadratic',
        style: {
          stroke:      d => d.style?.stroke ?? '#ccc',
          lineWidth:   d => d.style?.lineWidth ?? 1,
          opacity:     d => d.style?.opacity ?? 0.55,
          lineDash:    d => d.style?.lineDash,
          curveOffset: d => d.style?.curveOffset ?? 20,
          endArrow:           true,
          labelText:          d => d.label ?? '',
          labelFontSize:      9,
          labelBackground:    true,
          labelBackgroundFill: 'rgba(255,255,255,0.82)',
        },
      },
      layout:    { type: 'preset' },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
      animation: false,
    })
    graphRef.current.render()
  }

  // ── 节点样式：全部圆形，大小 + 颜色区分 ──────────────────────
  const nodeStyle = n => {
    if (n.nodeType === 'student') return {
      fill: '#faad14', lineWidth: 0,
      size: 58,
    }
    if (n.nodeType === 'kc') return {
      fill: masteryColor(n.masteryLevel ?? 0), lineWidth: 0,
      // 掌握度越高，圆越大
      size: 18 + Math.round((n.masteryLevel ?? 0) * 22),
    }
    // exercise: exfr 越高颜色越红，圆越大（越需要复习）
    return {
      fill: exfrColor(n.exfr ?? 0), lineWidth: 0,
      size: 10 + Math.round((n.exfr ?? 0) * 12),
    }
  }

  const nodeLabel = (d, raw) => {
    if (!raw) return ''
    if (raw.nodeType === 'student') return '我'
    return raw.label ?? ''
  }

  // mlkc / pkc 向相反方向弯曲，避免标签/线重叠
  const edgeStyle = e => {
    if (e.edgeType === 'mlkc')   return { stroke: '#1677ff', lineWidth: 1.2, opacity: 0.6, curveOffset:  40 }
    if (e.edgeType === 'pkc')    return { stroke: '#fa8c16', lineWidth: 1,   opacity: 0.6, curveOffset: -40, lineDash: [4, 3] }
    if (e.edgeType === 'exfr')   return { stroke: '#722ed1', lineWidth: 1.2, opacity: 0.6, curveOffset:  20 }
    return                              { stroke: '#ccc',    lineWidth: 0.8, opacity: 0.35, curveOffset:   0 }
  }

  const masteryColor = v => {
    if (v >= 0.8) return '#52c41a'
    if (v >= 0.5) return '#faad14'
    if (v > 0)    return '#4096ff'
    return '#bfbfbf'
  }
  const exfrColor = v => {
    if (v >= 0.7) return '#ff4d4f'
    if (v >= 0.3) return '#fa8c16'
    return '#b37feb'
  }

  const graphHeight = (isFs = fullscreen) =>
    isFs ? Math.max(500, window.innerHeight - 96) : Math.max(460, window.innerHeight - 220)

  // ── JSX ───────────────────────────────────────────────────────
  const topBar = (
    <div style={{
      display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '2px 0',
      padding: '6px 12px', background: '#fff',
      borderBottom: '1px solid #f0f0f0',
    }}>
      {/* 节点类型筛选 */}
      {NODE_TYPES.map(nt => (
        <Chip
          key={nt.key}
          color={nt.color}
          label={nt.label}
          shape="circle"
          active={shownNodeTypes.has(nt.key)}
          onClick={() => toggleNodeType(nt.key)}
        />
      ))}

      <span style={{ width: 1, height: 16, background: '#e0e0e0', margin: '0 6px', display: 'inline-block' }} />

      {/* 边类型筛选 */}
      {EDGE_TYPES.map(et => (
        <Chip
          key={et.key}
          color={et.color}
          label={et.label}
          shape="line"
          dash={et.dash}
          active={shownEdgeTypes.has(et.key)}
          onClick={() => toggleEdgeType(et.key)}
        />
      ))}

      {/* 右侧控件 */}
      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
        <Select
          size="small"
          value={courseId}
          onChange={setCourseId}
          options={courses.map(c => ({ value: c.id, label: c.name }))}
          style={{ width: 160 }}
        />
        <IconBtn title="刷新" onClick={loadGraph} loading={loading}>
          <ReloadOutlined />
        </IconBtn>
        <IconBtn title={fullscreen ? '退出全屏' : '全屏'} onClick={() => setFullscreen(f => !f)}>
          {fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
        </IconBtn>
      </div>

      {/* 第二行：按具体节点筛选（仅 graphData 有数据时显示） */}
      {graphData && (
        <div style={{ width: '100%', display: 'flex', gap: 10, padding: '4px 2px 2px', alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: 12, color: '#888', whiteSpace: 'nowrap' }}>筛选知识点：</span>
          <Select
            mode="multiple"
            size="small"
            allowClear
            placeholder="全部知识点"
            maxTagCount="responsive"
            value={selectedKcIds}
            onChange={setSelectedKcIds}
            options={graphData.nodes
              .filter(n => n.nodeType === 'kc')
              .map(n => ({ value: n.id, label: n.label || n.id }))}
            style={{ flex: 1, minWidth: 160, maxWidth: 380 }}
          />
          <span style={{ fontSize: 12, color: '#888', whiteSpace: 'nowrap' }}>筛选习题：</span>
          <Select
            mode="multiple"
            size="small"
            allowClear
            placeholder="全部习题"
            maxTagCount="responsive"
            value={selectedExIds}
            onChange={setSelectedExIds}
            options={graphData.nodes
              .filter(n => n.nodeType === 'exercise')
              .map(n => ({ value: n.id, label: n.label || n.id }))}
            style={{ flex: 1, minWidth: 160, maxWidth: 380 }}
          />
        </div>
      )}
    </div>
  )

  const canvas = (
    <div style={{ position: 'relative', background: '#fff', height: graphHeight(fullscreen) }}>
      {loading && (
        <div style={{
          position: 'absolute', inset: 0, zIndex: 2,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: 'rgba(255,255,255,0.75)',
        }}>
          <Spin description="加载图谱中..." />
        </div>
      )}
      {empty && !loading && (
        <Empty
          description="暂无答题记录，完成答题后图谱将自动生成"
          style={{ paddingTop: '15%' }}
        />
      )}
      <div
        ref={containerRef}
        style={{ width: '100%', height: '100%', display: empty ? 'none' : 'block' }}
      />
    </div>
  )

  if (fullscreen) {
    return (
      <div style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'flex', flexDirection: 'column', background: '#fff' }}>
        {topBar}
        {canvas}
      </div>
    )
  }
  return (
    <div style={{ background: '#fff', borderRadius: 8, overflow: 'hidden', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
      {topBar}
      {canvas}
    </div>
  )
}

// ── 图例/筛选 Chip 组件 ────────────────────────────────────────
function Chip({ color, label, shape, dash, active, onClick }) {
  return (
    <span
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 5,
        padding: '3px 10px', cursor: 'pointer', borderRadius: 3,
        opacity: active ? 1 : 0.28,
        transition: 'opacity .15s',
        userSelect: 'none', fontSize: 12, color: '#333',
        whiteSpace: 'nowrap',
      }}
    >
      {shape === 'circle' ? (
        <span style={{
          width: 11, height: 11, borderRadius: '50%',
          background: color, display: 'inline-block', flexShrink: 0,
        }} />
      ) : (
        <span style={{
          width: 22, height: 3, display: 'inline-block', flexShrink: 0,
          background: dash
            ? `repeating-linear-gradient(90deg, ${color} 0, ${color} 4px, transparent 4px, transparent 7px)`
            : color,
          borderRadius: 2,
        }} />
      )}
      {label}
    </span>
  )
}

// ── 极简图标按钮 ───────────────────────────────────────────────
function IconBtn({ children, title, onClick, loading }) {
  const [hover, setHover] = useState(false)
  return (
    <span
      title={title}
      onClick={!loading ? onClick : undefined}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        width: 28, height: 28, borderRadius: 6, cursor: loading ? 'default' : 'pointer',
        background: hover ? '#f5f5f5' : 'transparent',
        color: '#555', fontSize: 15, transition: 'background .15s',
        opacity: loading ? 0.5 : 1,
      }}
    >
      {loading ? <span style={{ fontSize: 13 }}>…</span> : children}
    </span>
  )
}
