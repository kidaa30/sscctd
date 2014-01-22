package simulator.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import simulator.Simulator;
import simulator.configurables.ITIConfig;
import simulator.util.ValuesTableModel;

/**
 * Dialog for configuring inter-trial intervals.
 * 
 * @author J Gray
 * 
 */

public class ITIDialog extends JDialog {

	/**
	 * Table model for ITI configurations.
	 * 
	 * @author J Gray
	 * 
	 */

	private class ITIValuesTableModel extends ValuesTableModel {

		/**
		 * Default constructor.
		 */
		public ITIValuesTableModel() {
			super();
		}

		/**
		 * Initialize the table with default values and labels.
		 * 
		 * @param iniValues
		 */

		public void setValuesTable(boolean iniValues) {
			Vector data1 = new Vector();

			try {
				row = 2;
				col = 2;
				columnNames = new String[] { "ITI Component", "Value" };

				Object record[] = new Object[col];
				data1.add(new Object[] { "Minimum length",
						onsets.getMinimum() + "" });
				record[0] = "Mean variable period";
				record[1] = onsets.getMean() + "";
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

	/** ITI durations configuration object. **/
	private ITIConfig onsets;

	/**
	 * Create the dialog.
	 */
	public ITIDialog(ActionListener listener) {
        setIconImages(Simulator.makeIcons());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			table = new JTable(new ITIValuesTableModel());
			// Make single click start editing instead of needing double
			DefaultCellEditor singleClickEditor = new DefaultCellEditor(
					new JTextField());
			singleClickEditor.setClickCountToStart(1);
			table.setDefaultEditor(Object.class, singleClickEditor);
			table.setCellSelectionEnabled(false);
			JScrollPane scrollPane = new JScrollPane(table);
			contentPanel.add(scrollPane);
			// Make window close on second enter press
			InputMap map = table
					.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			map.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
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
	 * @return a new ITIConfig object
	 */

	public ITIConfig getConfig() {
		TableModel model = table.getModel();
		onsets.setMinimum(Double.parseDouble((String) model.getValueAt(0, 1)));
		onsets.setMean(Double.parseDouble((String) model.getValueAt(1, 1)));
		onsets.setConfigured(true);
		return onsets;
	}

	/**
	 * Fill the model with an ITI configuration.
	 * 
	 * @param currentConfig
	 *            the config to fill the table model with
	 */

	public void setITIConfig(ITIConfig currentConfig) {
		onsets = currentConfig;
		((ITIValuesTableModel) table.getModel()).setValuesTable(true);
	}

}
