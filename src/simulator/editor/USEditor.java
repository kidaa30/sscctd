/**
 * 
 */
package simulator.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import simulator.configurables.USConfiguration;
import simulator.dialog.USDialog;

/**
 * USEditor.java
 * 
 * Created on 01-Dec-2011 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */
public class USEditor extends AbstractCellEditor implements TableCellEditor,
		ActionListener {

	private USConfiguration currentConfig;
	private JButton button;
	private USDialog dialog;
	protected static final String EDIT = "edit";

	public USEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		dialog = new USDialog(this);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				fireEditingStopped(); // Make the renderer reappear.
			}

			@Override
			public void windowClosing(WindowEvent e) {
				fireEditingStopped(); // Make the renderer reappear.
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			dialog.setConfig(currentConfig);
			dialog.setVisible(true);

		} else {
			currentConfig = dialog.getConfig();
			dialog.setVisible(false);
			fireEditingStopped(); // Make the renderer reappear.
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	@Override
	public Object getCellEditorValue() {
		return currentConfig;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing
	 * .JTable, java.lang.Object, boolean, int, int)
	 */
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		currentConfig = (USConfiguration) value;
		dialog.setTitle("CS: " + (table.getModel().getValueAt(row, 1)));
		return button;
	}

}
