package simulator.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import simulator.configurables.ITIConfig;
import simulator.dialog.ITIDialog;

/**
 * Cell editor for inter-trial interval durations.
 * 
 * @author J Gray
 * 
 */

public class ITIEditor extends AbstractCellEditor implements ActionListener,
		TableCellEditor {

	/** ITI configuration object. **/
	private ITIConfig currentConfig;
	private JButton button;
	/** ITI editor dialog. **/
	private ITIDialog dialog;
	protected static final String EDIT = "edit";

	/**
	 * Create a new editor.
	 */
	public ITIEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		dialog = new ITIDialog(this);
	}

	/**
	 * Open an editing dialog when clicked filled with the current
	 * configuration, then close it again afterwards.
	 */

	@Override
	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			dialog.setITIConfig(currentConfig);
			dialog.setVisible(true);

			fireEditingStopped(); // Make the renderer reappear.

		} else {
			currentConfig = dialog.getConfig();
			dialog.setVisible(false);
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
		currentConfig = (ITIConfig) value;
		dialog.setTitle(((String) table.getValueAt(row, 0)) + " Phase "
				+ (column / 5 + 1));
		return button;
	}

}
