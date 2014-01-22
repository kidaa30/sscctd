/**
 * 
 */
package simulator.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import simulator.configurables.ContextConfig;
import simulator.dialog.ContextDialog;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class ContextEditor extends AbstractCellEditor implements
		ActionListener, TableCellEditor {

	/** Context configuration object. **/
	private ContextConfig currentConfig;
	private JButton button;
	/** Context editor dialog. **/
	private ContextDialog dialog;
	protected static final String EDIT = "edit";

	/**
	 * Create a new editor.
	 */
	public ContextEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		dialog = new ContextDialog(this);
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
			dialog.setContextConfig(currentConfig);
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
		currentConfig = (ContextConfig) value;
		dialog.setTitle(((String) table.getValueAt(row, 0)) + " Phase "
				+ (column / 5 + 1));
		return button;
	}

}
