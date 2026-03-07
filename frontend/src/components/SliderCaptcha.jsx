import { useState, useRef, useEffect, useCallback } from 'react'
import { CheckCircleFilled, CloseCircleFilled, ReloadOutlined, LoadingOutlined } from '@ant-design/icons'

/**
 * 图片拼图滑块验证码组件
 *
 * 流程：
 * 1. 请求后端生成图片数据（打乱的背景条带 + 拼图块）
 * 2. 用 Canvas API 将条带按 stripOrder 重组为完整背景（带缺口）
 * 3. 将拼图块绘制到浮层 Canvas，跟随拖动条移动
 * 4. 松开时提交 token + sliderX（像素）到后端验证
 *
 * Props:
 *   onSuccess(passToken) — 验证通过后回调
 *   onReset              — 重置时回调
 */
export default function SliderCaptcha({ onSuccess, onReset }) {
  const [status, setStatus]   = useState('loading')  // loading|idle|dragging|success|fail
  const [captcha, setCaptcha] = useState(null)        // 后端返回的验证码数据
  const [dragX, setDragX]     = useState(0)           // 拖动条位置 px
  const [error, setError]     = useState(false)       // 加载失败

  const bgCanvasRef    = useRef(null)  // 背景图 Canvas
  const pieceCanvasRef = useRef(null)  // 拼图块 Canvas（随拖动条移动）
  const dragging       = useRef(false)
  const startMouseX    = useRef(0)
  const startDragX     = useRef(0)

  // 图片尺寸（与后端保持一致）
  const IMG_W    = captcha?.imageWidth  ?? 300
  const IMG_H    = captcha?.imageHeight ?? 150
  const PIECE_W  = 50
  const STRIP_W  = IMG_W / 10            // 30px per strip
  const TRACK_W  = IMG_W                  // 拖动条与图片等宽
  const THUMB_W  = 44
  const MAX_DRAG = IMG_W - PIECE_W        // 250px

  // -------------------------------------------------------------------------
  // 加载验证码数据
  // -------------------------------------------------------------------------
  const fetchCaptcha = useCallback(async () => {
    setStatus('loading')
    setDragX(0)
    setError(false)
    setCaptcha(null)
    try {
      const res  = await fetch('/api/auth/captcha/generate')
      const json = await res.json()
      setCaptcha(json.data)
      setStatus('idle')
    } catch {
      setError(true)
      setStatus('idle')
    }
  }, [])

  useEffect(() => { fetchCaptcha() }, [fetchCaptcha])

  // -------------------------------------------------------------------------
  // 背景 Canvas：重组打乱的条带
  // -------------------------------------------------------------------------
  useEffect(() => {
    if (!captcha || !bgCanvasRef.current) return
    const canvas = bgCanvasRef.current
    const ctx    = canvas.getContext('2d')
    ctx.clearRect(0, 0, IMG_W, IMG_H)

    const { bgStrips, stripOrder } = captcha
    // 并发加载所有条带图片
    Promise.all(
      bgStrips.map((b64, i) =>
        new Promise(resolve => {
          const img = new Image()
          img.onload = () => resolve({ img, origCol: stripOrder[i] })
          img.src = 'data:image/png;base64,' + b64
        })
      )
    ).then(items => {
      // 每条 strip 画到其原始列位置
      items.forEach(({ img, origCol }) => {
        ctx.drawImage(img, origCol * STRIP_W, 0, STRIP_W, IMG_H)
      })
    })
  }, [captcha, IMG_W, IMG_H, STRIP_W])

  // -------------------------------------------------------------------------
  // 拼图块 Canvas：将 pieceImage 画在正确的 Y 位置
  // -------------------------------------------------------------------------
  useEffect(() => {
    if (!captcha || !pieceCanvasRef.current) return
    const canvas = pieceCanvasRef.current
    const ctx    = canvas.getContext('2d')
    ctx.clearRect(0, 0, PIECE_W, IMG_H)

    // 半透明蓝色竖条（引导线）
    ctx.fillStyle = 'rgba(22, 119, 255, 0.18)'
    ctx.fillRect(0, 0, PIECE_W, IMG_H)

    const img = new Image()
    img.onload = () => {
      ctx.drawImage(img, 0, captcha.pieceY, PIECE_W, PIECE_W)
      // 在拼图块周围画投影
      ctx.shadowColor   = 'rgba(0,0,0,0.4)'
      ctx.shadowBlur    = 6
      ctx.strokeStyle   = 'rgba(255,255,255,0.9)'
      ctx.lineWidth     = 1.5
      ctx.strokeRect(0.75, captcha.pieceY + 0.75, PIECE_W - 1.5, PIECE_W - 1.5)
      ctx.shadowBlur    = 0
    }
    img.src = 'data:image/png;base64,' + captcha.pieceImage
  }, [captcha, IMG_H, PIECE_W])

  // -------------------------------------------------------------------------
  // 拖动事件
  // -------------------------------------------------------------------------
  const onThumbDown = (e) => {
    if (status !== 'idle') return
    dragging.current  = true
    startMouseX.current = e.clientX ?? e.touches?.[0].clientX
    startDragX.current  = dragX
    setStatus('dragging')
    e.preventDefault()
  }

  const onThumbTouchStart = (e) => {
    if (status !== 'idle') return
    dragging.current    = true
    startMouseX.current = e.touches[0].clientX
    startDragX.current  = dragX
    setStatus('dragging')
  }

  useEffect(() => {
    const onMove = (e) => {
      if (!dragging.current) return
      const cx    = e.touches ? e.touches[0].clientX : e.clientX
      const delta = cx - startMouseX.current
      setDragX(Math.max(0, Math.min(MAX_DRAG, startDragX.current + delta)))
    }

    const onUp = async () => {
      if (!dragging.current) return
      dragging.current = false
      if (status === 'success' || !captcha) return

      const sliderX = Math.round(dragX)
      try {
        const res  = await fetch(
          `/api/auth/captcha/verify?token=${captcha.token}&sliderX=${sliderX}`,
          { method: 'POST' }
        )
        const json = await res.json()
        if (json.data?.passed) {
          setStatus('success')
          onSuccess?.(json.data.passToken)
        } else {
          setStatus('fail')
          setTimeout(() => { fetchCaptcha(); onReset?.() }, 900)
        }
      } catch {
        setStatus('fail')
        setTimeout(() => { fetchCaptcha(); onReset?.() }, 900)
      }
    }

    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup',   onUp)
    window.addEventListener('touchmove', onMove, { passive: true })
    window.addEventListener('touchend',  onUp)
    return () => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup',   onUp)
      window.removeEventListener('touchmove', onMove)
      window.removeEventListener('touchend',  onUp)
    }
  }, [dragX, captcha, status, fetchCaptcha, onSuccess, onReset, MAX_DRAG])

  // -------------------------------------------------------------------------
  // 渲染
  // -------------------------------------------------------------------------
  const trackColor = status === 'success' ? '#b7eb8f'
                   : status === 'fail'    ? '#ffccc7'
                   : status === 'dragging'? '#bae0ff'
                   : '#e8e8e8'
  const thumbColor = status === 'success' ? '#52c41a'
                   : status === 'fail'    ? '#ff4d4f'
                   : '#1677ff'

  return (
    <div style={{ userSelect: 'none', width: IMG_W }}>

      {/* ── 图片区域 ── */}
      <div style={{ position: 'relative', width: IMG_W, height: IMG_H,
                    border: '1px solid #d9d9d9', borderRadius: 4, overflow: 'hidden',
                    background: '#f0f0f0', marginBottom: 8 }}>

        {/* 背景 Canvas */}
        <canvas
          ref={bgCanvasRef}
          width={IMG_W}
          height={IMG_H}
          style={{ display: 'block' }}
        />

        {/* 拼图块 Canvas（绝对定位，随 dragX 移动） */}
        {captcha && (
          <canvas
            ref={pieceCanvasRef}
            width={PIECE_W}
            height={IMG_H}
            style={{
              position: 'absolute',
              left: dragX,
              top: 0,
              cursor: status === 'idle' || status === 'dragging' ? 'grabbing' : 'default',
              transition: status === 'dragging' ? 'none' : 'left 0.15s',
            }}
          />
        )}

        {/* 加载遮罩 */}
        {status === 'loading' && (
          <div style={{
            position: 'absolute', inset: 0,
            background: 'rgba(255,255,255,0.7)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 22, color: '#1677ff',
          }}>
            <LoadingOutlined />
          </div>
        )}

        {/* 成功/失败遮罩 */}
        {(status === 'success' || status === 'fail') && (
          <div style={{
            position: 'absolute', inset: 0,
            background: status === 'success' ? 'rgba(82,196,26,0.15)' : 'rgba(255,77,79,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 36,
          }}>
            {status === 'success'
              ? <CheckCircleFilled style={{ color: '#52c41a' }} />
              : <CloseCircleFilled style={{ color: '#ff4d4f' }} />}
          </div>
        )}
      </div>

      {/* ── 拖动条 ── */}
      <div style={{
        position: 'relative',
        width: TRACK_W,
        height: 40,
        background: '#e8e8e8',
        borderRadius: 4,
        border: '1px solid #d9d9d9',
        overflow: 'hidden',
      }}>
        {/* 进度填充 */}
        <div style={{
          position: 'absolute', left: 0, top: 0, bottom: 0,
          width: dragX + THUMB_W / 2,
          background: trackColor,
          transition: status === 'dragging' ? 'none' : 'background 0.3s',
        }} />

        {/* 提示文字 */}
        <div style={{
          position: 'absolute', inset: 0, pointerEvents: 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 13, color: '#888',
        }}>
          {status === 'idle' && '拖动滑块完成拼图验证'}
          {status === 'success' && <span style={{ color: '#52c41a' }}>验证通过</span>}
          {status === 'fail'    && <span style={{ color: '#ff4d4f' }}>验证失败，请重试</span>}
          {status === 'loading' && '加载中...'}
        </div>

        {/* 拖动按钮 */}
        <div
          onMouseDown={onThumbDown}
          onTouchStart={onThumbTouchStart}
          style={{
            position: 'absolute',
            left: dragX,
            top: 0, bottom: 0,
            width: THUMB_W,
            background: thumbColor,
            borderRadius: 4,
            cursor: status === 'idle' || status === 'dragging' ? 'grab' : 'default',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontSize: 20,
            boxShadow: '2px 0 6px rgba(0,0,0,0.2)',
            transition: status === 'dragging' ? 'none' : 'left 0.15s, background 0.3s',
            zIndex: 2,
          }}
        >
          {status === 'success' ? '✓' : status === 'fail' ? '✗' : '›'}
        </div>
      </div>

      {/* ── 刷新按钮 ── */}
      {status !== 'success' && (
        <div style={{ textAlign: 'right', marginTop: 4 }}>
          <span
            onClick={() => { fetchCaptcha(); onReset?.() }}
            style={{ fontSize: 12, color: '#888', cursor: 'pointer' }}
          >
            <ReloadOutlined style={{ marginRight: 3 }} />
            换一张
          </span>
        </div>
      )}
    </div>
  )
}
