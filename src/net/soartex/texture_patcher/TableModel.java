package net.soartex.texture_patcher;

import javax.swing.table.AbstractTableModel;

final class TableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private final Object[][] data;

	private final String[] COLUMNS = {"\u2713","Mod Name","Mod Version","MC Version","File Size","Date Modified"};

	public TableModel (final Object[][] temp) {

		data = temp;

	}

	@Override public int getRowCount () {

		return data.length;

	}

	@Override public int getColumnCount () {

		return COLUMNS.length;

	}

	@Override public Object getValueAt (final int rowIndex, final int columnIndex) {

		return data[rowIndex][columnIndex];

	}

	@Override public String getColumnName (final int column) {

		return COLUMNS[column];

	}

	@Override public Class<?> getColumnClass (final int columnIndex) {

		return data[0][columnIndex].getClass();

	}

}
