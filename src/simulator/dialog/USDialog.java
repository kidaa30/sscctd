package simulator.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import simulator.Simulator;
import simulator.configurables.USConfiguration;
import simulator.configurables.USConfiguration.Relationship;
import simulator.util.ValuesTableModel;

/**
 * USDialog.java
 * 
 * Dialog for editing stimulus duration configurations.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class USDialog extends JDialog {

	/**
	 * Table model for US.
	 * 
	 * @author J Gray
	 * 
	 */
	private class USValuesTableModel extends ValuesTableModel {

		/**
		 * OnsetValuesTableModel Constructor method.
		 */
		public USValuesTableModel() {
			super();
			col = 2;
			columnNames = new String[] { "Type", "ISI (s)" };
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
		}

		/*
		 * Don't need to implement this method unless your table's data can
		 * change.
		 */
		@Override
		public void setValueAt(Object value, int row, int col) {
			Relationship neighbour = (Relationship) getValueAt(0, 0);
			double offset = col == 1 ? Double.parseDouble(((String) value))
					: Double.parseDouble(((String) getValueAt(0, 1)));
			String fwdDefault = Math.max(offset, current.getForwardDefault())
					+ "";
			if (neighbour.equals(Relationship.FORWARD)) {
				if (col == 0) {
					setValueAt(fwdDefault, 0, 1);
					fireTableChanged(null);
				} else {
					value = fwdDefault;
				}
			}
			super.setValueAt(value, row, col);
		}

		/**
		 * Initialize the table with default values, or those from the provided
		 * onset configuration of available.
		 * 
		 * @param iniValues
		 */
		public void setValuesTable(boolean iniValues) {
			Vector data1 = new Vector();

			try {
				row = 1;
				col = 2;
				columnNames = new String[] { "Type", "ISI (s)" };

				Object record[] = new Object[col];
				record[0] = current.getType();
				double offset;
				if (current.getType().equals(Relationship.FORWARD)) {
					offset = Math.max(current.getOffset(),
							current.getForwardDefault());
				} else {
					offset = current.getOffset();
				}
				record[1] = offset + "";
				data1.add(record);

				setData(data1);
				fireTableChanged(null); // notify everyone that we have a new
										// table.
			} catch (Exception e) {
				setData(new Vector()); // blank it out and keep going.
				e.printStackTrace();
			}
		}
	}

	private final JPanel contentPanel = new JPanel();
	private JTable table;
	/** Duration configuration storage. **/
	private USConfiguration current;

	/** Cell editor providing a combobox. **/
	private DefaultCellEditor typeEditor;

	/**
	 * Create the dialog.
	 */
	public USDialog(ActionListener listener) {
        setIconImages(Simulator.makeIcons());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JComboBox typeBox = new JComboBox(new DefaultComboBoxModel(
					Relationship.values()));
			typeEditor = new DefaultCellEditor(typeBox);

			table = new JTable(new USValuesTableModel()) {
				// Overridden to return a combobox for duration type
				@Override
				public TableCellEditor getCellEditor(int row, int column) {
					int modelColumn = convertColumnIndexToModel(column);

					if (modelColumn == 0)
						return typeEditor;
					else
						return super.getCellEditor(row, column);
				}
			};
			// Make window close on second enter press
			InputMap map = table
					.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			map.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			table.getColumnModel().getColumn(1).setPreferredWidth(10);
			table.getColumnModel().getColumn(1).setMaxWidth(10);
			table.doLayout();
			// Make single click start editing instead of needing double
			DefaultCellEditor singleClickEditor = new DefaultCellEditor(
					new JTextField());
			singleClickEditor.setClickCountToStart(1);
			table.setDefaultEditor(Object.class, singleClickEditor);
			table.setCellSelectionEnabled(false);

			JScrollPane scrollPane = new JScrollPane(table);
			contentPanel.add(scrollPane);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(listener);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

	/**
	 * Produces a duration configuration from the table model.
	 * 
	 * @return a new OnsetConfig holding the input duration config.
	 */

	public USConfiguration getConfig() {
		TableModel model = table.getModel();
		try {
			double fwdDefault = current.getForwardDefault();
			current = new USConfiguration();
			current.setForwardDefault(fwdDefault);
			double offset = Double.parseDouble((String) model.getValueAt(0, 1));
			current.setType((Relationship) model.getValueAt(0, 0));
			current.setOffset(offset);
		} catch (NullPointerException e) {
		}
		return current;
	}

	public void setConfig(USConfiguration currentConfig) {
		current = currentConfig;
		((USValuesTableModel) table.getModel()).setValuesTable(true);
	}
}
