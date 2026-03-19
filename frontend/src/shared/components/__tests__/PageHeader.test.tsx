import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import PageHeader from '../PageHeader'

describe('PageHeader', () => {
  it('renders title', () => {
    render(<PageHeader title="Purchase Orders" />)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Purchase Orders')
  })

  it('renders description when provided', () => {
    render(<PageHeader title="PO" description="Manage purchase orders" />)
    expect(screen.getByText('Manage purchase orders')).toBeInTheDocument()
  })

  it('does not render description when not provided', () => {
    const { container } = render(<PageHeader title="PO" />)
    expect(container.querySelector('p')).toBeNull()
  })

  it('renders actions when provided', () => {
    render(
      <PageHeader
        title="Orders"
        actions={<button>Create</button>}
      />
    )
    expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument()
  })

  it('renders breadcrumbs', () => {
    render(
      <PageHeader
        title="Detail"
        breadcrumbs={[
          { label: 'Home', path: '/' },
          { label: 'Orders', path: '/orders' },
          { label: 'Detail' },
        ]}
      />
    )
    expect(screen.getByText('Home')).toBeInTheDocument()
    expect(screen.getByText('Orders')).toBeInTheDocument()
    expect(screen.getAllByText('Detail')).toHaveLength(2) // breadcrumb + h1
  })
})
