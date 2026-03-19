import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import StatsCard from '../StatsCard'

describe('StatsCard', () => {
  it('renders title and value', () => {
    render(<StatsCard title="Total Orders" value={42} />)
    expect(screen.getByText('Total Orders')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('renders change indicator with positive style', () => {
    render(<StatsCard title="Revenue" value="$1,200" change="+12%" changeType="positive" />)
    const change = screen.getByText('+12%')
    expect(change).toBeInTheDocument()
    expect(change).toHaveClass('text-emerald-600')
  })

  it('renders change indicator with negative style', () => {
    render(<StatsCard title="Costs" value="$800" change="-5%" changeType="negative" />)
    const change = screen.getByText('-5%')
    expect(change).toHaveClass('text-red-600')
  })

  it('renders neutral change by default', () => {
    render(<StatsCard title="Items" value={100} change="0%" />)
    const change = screen.getByText('0%')
    expect(change).toHaveClass('text-slate-500')
  })

  it('does not render change when not provided', () => {
    const { container } = render(<StatsCard title="Count" value={5} />)
    const paragraphs = container.querySelectorAll('p')
    // title + value = 2 paragraphs, no change paragraph
    expect(paragraphs).toHaveLength(2)
  })

  it('renders icon when provided', () => {
    render(<StatsCard title="Test" value={1} icon={<span data-testid="icon">I</span>} />)
    expect(screen.getByTestId('icon')).toBeInTheDocument()
  })
})
