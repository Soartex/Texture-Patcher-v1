package net.soartex.texture_patcher;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Texture_Patcher Cell Coloring
 * 
 * @author GoldBattle
 * @version 1.3
 *
 */
final class TableRender extends DefaultTableCellRenderer{
	
	private static final long serialVersionUID = 1L;
	private ArrayList<Integer> rows1;
	private ArrayList<Integer> rows2;
	private Color color1;
	private Color color2;
	
	public TableRender(ArrayList<Integer> rows1, ArrayList<Integer> rows2, Color color1, Color color2){
		this.rows1=rows1;
		this.rows2=rows2;
		this.color1=color1;
		this.color2=color2;
	}	
	
	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Act as normal
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // Add custom behavior 1
        for(int i: rows1){
	        if(row == i) {
	            // this will customize that kind of border that will be use to highlight a row
	            setBorder(BorderFactory.createMatteBorder(1, 2, 1, 2, color1));
	        }
        }
        // Add custom behavior 2
        for(int i: rows2){
	        if(row == i) {
	            // this will customize that kind of border that will be use to highlight a row
	            setBorder(BorderFactory.createMatteBorder(1, 2, 1, 2, color2));
	        }
        }
        return this;
    }
}