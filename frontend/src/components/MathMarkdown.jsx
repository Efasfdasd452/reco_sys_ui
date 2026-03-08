import ReactMarkdown from 'react-markdown'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'
import 'katex/dist/katex.min.css'

const inlineComponents = { p: ({ children }) => <span>{children}</span> }

/**
 * 支持 LaTeX 数学公式的 Markdown 渲染组件
 * inline=true 时将 p 渲染为 span，可安全放入 Radio/Checkbox label 中
 */
export default function MathMarkdown({ children, inline }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkMath]}
      rehypePlugins={[rehypeKatex]}
      components={inline ? inlineComponents : undefined}
    >
      {children || ''}
    </ReactMarkdown>
  )
}
