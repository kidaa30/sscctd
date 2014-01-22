package simulator.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
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
import simulator.configurables.FixedOnsetConfig;
import simulator.configurables.StimulusOnsetConfig;
import simulator.configurables.VariableOnsetConfig;
import simulator.util.Distributions;
import simulator.util.ValuesTableModel;
import simulator.util.VariableDistribution;

/**
 * OnsetsDialog.java
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

public class OnsetsDialog extends JDialog {

	/**
	 * Table model for onsets.
	 * 
	 * @author J Gray
	 * 
	 */
	private class OnsetValuesTableModel extends ValuesTableModel {

		/**
		 * OnsetValuesTableModel Constructor method.
		 */
		public OnsetValuesTableModel() {
			super();
			col = 2;
			columnNames = new String[] { "Duration", "Duration (s)" };
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
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
				columnNames = new String[] { "Duration", "Length" };

				Object record[] = new Object[col];
				record[0] = current.isFixed() ? types[0] : types[1];
				if (current.isFixed()) {
					FixedOnsetConfig onset = (FixedOnsetConfig) current;
					record[1] = onset == null ? "0" : onset.getNextOnset() + "";
				} else {
					VariableOnsetConfig onset = (VariableOnsetConfig) current;
					record[1] = onset == null ? "0.0" : onset.getMean() + "";
				}
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
	private StimulusOnsetConfig current;
	/** Types of duration. **/
	private String[] types;

	/** Cell editor providing a combobox. **/
	private DefaultCellEditor typeEditor;

	/**
	 * Create the dialog.
	 */
	public OnsetsDialog(ActionListener listener) {
        setIconImages(Simulator.makeIcons());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			types = new String[] { "Fixed", "Variable" };
			JComboBox typeBox = new JComboBox(types);
			typeEditor = new DefaultCellEditor(typeBox);

			table = new JTable(new OnsetValuesTableModel()) {
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

	public StimulusOnsetConfig getOnsetConfig() {
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String type = (String) model.getValueAt(i, 0);
			double duration = Double.parseDouble(((String) model.getValueAt(i,
					1)));
			if (types[0].equals(type)) {
				current = new FixedOnsetConfig(duration);
			} else {
				current = new VariableOnsetConfig(duration,
						VariableDistribution.randomSeed(), 0,
						Distributions.EXPONENTIAL, false);
			}
		}
		return current;
	}

	public void setOnsetConfig(StimulusOnsetConfig currentConfig) {
		current = currentConfig;
		((OnsetValuesTableModel) table.getModel()).setValuesTable(true);
	}
}
