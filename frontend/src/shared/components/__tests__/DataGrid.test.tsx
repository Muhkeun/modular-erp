import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import DataGrid from '../DataGrid'
import type { ColDef } from 'ag-grid-community'

// Mock AG Grid since it requires DOM measurements not available in jsdom
vi.mock('ag-grid-react', () => ({
  AgGridReact: (props: Record<string, unknown>) => {
    const rowData = props.rowData as Array<Record<string, unknown>> | undefined
    const columnDefs = props.columnDefs as ColDef[] | undefined
    return (
      <div data-testid="ag-grid-mock">
        <table>
          <thead>
            <tr>
              {(columnDefs || []).map((col: ColDef, i: number) => (
                <th key={i}>{String(col.headerName ?? col.field ?? '')}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {(rowData || []).map((row: Record<string, unknown>, ri: number) => (
              <tr key={ri}>
                {(columnDefs || []).map((col: ColDef, ci: number) => (
                  <td key={ci}>{String(row[col.field as string] ?? '')}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {props.pagination && <div data-testid="pagination">Page size: {String(props.paginationPageSize)}</div>}
      </div>
    )
  },
}))

interface TestRow {
  id: number
  name: string
  qty: number
}

const columns: ColDef<TestRow>[] = [
  { field: 'id', headerName: 'ID' },
  { field: 'name', headerName: 'Name' },
  { field: 'qty', headerName: 'Quantity' },
]

const rows: TestRow[] = [
  { id: 1, name: 'Widget A', qty: 10 },
  { id: 2, name: 'Widget B', qty: 25 },
]

describe('DataGrid', () => {
  it('renders grid with column headers and data', () => {
    render(<DataGrid rowData={rows} columnDefs={columns} />)
    expect(screen.getByTestId('ag-grid-mock')).toBeInTheDocument()
    expect(screen.getByText('ID')).toBeInTheDocument()
    expect(screen.getByText('Name')).toBeInTheDocument()
    expect(screen.getByText('Widget A')).toBeInTheDocument()
    expect(screen.getByText('25')).toBeInTheDocument()
  })

  it('renders with pagination enabled by default', () => {
    render(<DataGrid rowData={rows} columnDefs={columns} />)
    expect(screen.getByTestId('pagination')).toHaveTextContent('Page size: 20')
  })

  it('accepts custom page size', () => {
    render(<DataGrid rowData={rows} columnDefs={columns} pageSize={50} />)
    expect(screen.getByTestId('pagination')).toHaveTextContent('Page size: 50')
  })

  it('renders empty grid when no data', () => {
    render(<DataGrid rowData={[]} columnDefs={columns} />)
    expect(screen.getByTestId('ag-grid-mock')).toBeInTheDocument()
    const tbody = screen.getByTestId('ag-grid-mock').querySelector('tbody')
    expect(tbody?.children).toHaveLength(0)
  })
})
