/*
 * Copyright (c) 2014 Soartex Fanver Team.
 */

package net.soartex.texture_patcher;

import javax.swing.table.AbstractTableModel;

final class TableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Object[][] data;
    private final String[] columns = {"\u2713", "Mod Name", "Mod Version", "MC Version", "File Size", "Date Modified"};

    public TableModel(final Object[][] data) {
        this.data = data;
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public void setValueAt(final Object value, final int row, final int column) {
        data[row][column] = value;
    }

    @Override
    public String getColumnName(final int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return data[0][columnIndex].getClass();
    }
}
