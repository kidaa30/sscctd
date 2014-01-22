package simulator.util;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

/**
 * This class is the values' table model Extracted to own class. J Gray
 */
public class ValuesTableModel extends AbstractTableModel {

	protected int col;
	protected int row;
	protected String[] columnNames;
	protected Vector data;

	/**
	 * ValuesTableModel's Constructor method.
	 */
	public ValuesTableModel() {
		data = new Vector();
	}

	/**
	 * Return the names of the table's column names.
	 * 
	 * @return an array with the table's column names.
	 */
	private String[] getColNames() {
		String[] columnNames = { "", "" };
		return columnNames;
	}

	/*
	 * JTable uses this method to determine the default renderer/ editor for
	 * each cell. If we didn't implement this method, then the last column would
	 * contain text ("true"/"false"), rather than a check box.
	 */
	@Override
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/**
	 * Returns the total number of columns the table contains.
	 * 
	 * @return the number of columns.
	 */
	@Override
	public int getColumnCount() {
		return col;
	}

	/**
	 * Returns the title of a requested column.
	 * 
	 * @param the
	 *            requested column.
	 * @return the column name.
	 */
	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	/**
	 * Returns the names of the columns.
	 * 
	 * @return String[] of the column names.
	 */
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * Returns the number of columns.
	 * 
	 * @return number of columns.
	 */
	public int getColumns() {
		return col;
	}

	/**
	 * Returns the data.
	 * 
	 * @return Vector data.
	 */
	public Vector getData() {
		return data;
	}

	/**
	 * Returns the total number of rows the table contains.
	 * 
	 * @return the number of rows.
	 */
	@Override
	public int getRowCount() {
		return data.size();
	}

	/**
	 * Returns the number of rows.
	 * 
	 * @return number of rows.
	 */
	public int getRows() {
		return row;
	}

	/**
	 * Returns an Object from the table. The object belongs to the column and
	 * row position that are being given as arguments.
	 * 
	 * @return the value that the table contains on the specified column and
	 *         row.
	 */
	@Override
	public Object getValueAt(int row, int col) {
		return ((Object[]) data.elementAt(row))[col];
	}

	/*
	 * Don't need to implement this method unless your table's editable.
	 */
	@Override
	public boolean isCellEditable(int row, int col) {
		return false ? true : col > 0;
	}

	/*
	 * If exists the valueName in the data
	 */
	public int isInValuesTable(Vector vec, String valueName) {
		int i = -1;
		if (vec != null)
			for (int k = 0; k < vec.size(); k++) {
				if (((Object[]) vec.get(k))[0].equals(valueName)) {
					i = k;
					break;
				}
			}
		return i;
	}

	/**
	 * Set ColumnsNames.
	 * 
	 * @param names
	 *            to set
	 */
	public void setColumnNames(String[] s) {
		columnNames = s;
	}

	/**
	 * Set the number of Columns.
	 * 
	 * @param number
	 *            of columns
	 */
	public void setColumns(int c) {
		col = c;
	}

	/**
	 * Set the data.
	 * 
	 * @param Vector
	 *            of data
	 */
	public void setData(Vector v) {
		data = v;
	}

	/**
	 * Set the number of rows.
	 * 
	 * @param number
	 *            of rows
	 */
	public void setRows(int r) {
		row = r;
	}

	/*
	 * Don't need to implement this method unless your table's data can change.
	 */
	@Override
	public void setValueAt(Object value, int row, int col) {
		Object[] temp = (Object[]) data.get(row);
		temp[col] = value;
		fireTableCellUpdated(row, col);
	}

	/**
	 * Returns the size of the data.
	 * 
	 * @return number of columns.
	 */
	public int size() {
		return data.size();
	}
}