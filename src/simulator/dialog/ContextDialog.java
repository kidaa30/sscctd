/**
 * 
 */
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
import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.util.ValuesTableModel;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class ContextDialog extends JDialog {

	/**
	 * Table model for ITI configurations.
	 * 
	 * @author J Gray
	 * 
	 */

	private class ContextValuesTableModel extends ValuesTableModel {

		/**
		 * Default constructor.
		 */
		public ContextValuesTableModel() {
			super();
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
		}

		/**
		 * Initialize the table with default values and labels.
		 * 
		 * @param iniValues
		 */

		public void setValuesTable(boolean iniValues) {
			Vector data1 = new Vector();

			try {
				row = 1;
				col = 2;
				columnNames = new String[] { "Context", "\u03B1" };

				data1.add(new Object[] { context.getContext(),
						context.getAlpha().floatValue() + "" });
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

	/** Context configuration object. **/
	private ContextConfig context;

	/**
	 * Create the dialog.
	 */
	public ContextDialog(ActionListener listener) {
        setIconImages(Simulator.makeIcons());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			// Fill with contexts.
			JComboBox typeBox = new JComboBox(Context.getList());
			final DefaultCellEditor typeEditor = new DefaultCellEditor(typeBox);

			table = new JTable(new ContextValuesTableModel()) {
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
	 * Get the configuration from the table model.
	 * 
	 * @return a new ContextConfig object
	 */

	public ContextConfig getConfig() {
		TableModel model = table.getModel();
		context.setContext((Context) model.getValueAt(0, 0));
		context.setAlpha(Double.parseDouble((String) model.getValueAt(0, 1)));
		return context;
	}

	/**
	 * Fill the model with a context configuration.
	 * 
	 * @param currentConfig
	 *            the config to fill the table model with
	 */

	public void setContextConfig(ContextConfig currentConfig) {
		context = currentConfig;
		((ContextValuesTableModel) table.getModel()).setValuesTable(true);
	}

}
