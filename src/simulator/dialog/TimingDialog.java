package simulator.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import simulator.CS;
import simulator.Messages;
import simulator.Simulator;
import simulator.configurables.FixedOnsetConfig;
import simulator.configurables.OnsetConfig;
import simulator.configurables.StimulusOnsetConfig;
import simulator.configurables.TimingConfiguration;
import simulator.configurables.USConfiguration;
import simulator.configurables.VariableOnsetConfig;
import simulator.configurables.USConfiguration.Relationship;
import simulator.editor.OnsetsEditor;
import simulator.editor.USEditor;
import simulator.util.ButtonColumn;
import simulator.util.ColumnGroup;
import simulator.util.Distributions;
import simulator.util.GroupableTableColumnModel;
import simulator.util.GroupableTableHeader;
import simulator.util.ValuesTableModel;
import simulator.util.VariableDistribution;

/**
 * Demo renderer just to prove they can be used.
 */
class GroupableTableCellRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 * @param table
	 * @param value
	 * @param selected
	 * @param focused
	 * @param row
	 * @param column
	 * @return
	 */
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean selected, boolean focused, int row, int column) {
		JTableHeader header = table.getTableHeader();
		Color color = UIManager.getColor("TableHeader.background");
		setHorizontalAlignment(SwingConstants.CENTER);
		setText(value != null ? value.toString() : " ");
		setBorder(UIManager.getBorder("TableHeader.CellBorder"));
		return this;
	}
}

public class TimingDialog extends JDialog {

	/**
	 * Comparator for sorting timing configs by trial type and position in
	 * trial.
	 */

	private class TimingComparator implements
			Comparator<Entry<CS, StimulusOnsetConfig>> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Entry<CS, StimulusOnsetConfig> o1,
				Entry<CS, StimulusOnsetConfig> o2) {
			if (o1.getKey().getTrialString()
					.equals(o2.getKey().getTrialString())) {
				return o1.getKey().compareTo(o2.getKey());
			} else {
				return o1.getKey().getTrialString()
						.compareTo(o2.getKey().getTrialString());
			}
		}

	}

	/**
	 * Table model for timings.
	 * 
	 * @author J Gray
	 * 
	 */
	private class TimingValuesTableModel extends ValuesTableModel {

		/**
		 * OnsetValuesTableModel Constructor method.
		 */
		public TimingValuesTableModel() {
			super();
			col = 8;
			columnNames = new String[] { "US Length", "CS", "Duration Type",
					"Duration", "Conditioning Type", "ISI" };
		}

		/**
		 * Return an interface for the onsets column.
		 */

		@Override
		public Class getColumnClass(int column) {
			Class c = super.getColumnClass(column);
			int index = Simulator.getController().getModel().isTimingPerTrial() ? 1
					: 0;
			if (column == (index + 2)) {
				c = StimulusOnsetConfig.class;
			}
			return c;
		}

		/**
		 * CS names uneditable, only first cell editable for US length.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			if (col == 1) {
				return false;
			} else if (col == 0 && row > 0) {
				return false;
			}
			boolean timingsPerTrial = false;

			try {
				timingsPerTrial = Simulator.getController().getModel()
						.isTimingPerTrial();
			} catch (NullPointerException e) {
				// Ignore.
			}

			if (col == 2 && timingsPerTrial) {
				return false;
			}

			return true;
		}

		/*
		 * Don't need to implement this method unless your table's data can
		 * change.
		 */
		@Override
		public void setValueAt(Object value, int row, int col) {
			super.setValueAt(value, row, col);
			int index = Simulator.getController().getModel().isTimingPerTrial() ? 4
					: 3;
			if (col >= index) {
				USConfiguration neighbour = getConfig(row, index + 2);
				neighbour.setForwardDefault(Double
						.parseDouble((String) getValueAt(row, index)));
				Double offset;
				if (neighbour.getType().equals(Relationship.FORWARD)) {
					offset = Math.max(neighbour.getOffset(),
							neighbour.getForwardDefault());
				} else {
					offset = neighbour.getOffset();
				}
				super.setValueAt(offset.toString(), row, index + 3);
			}
		}

		/**
		 * Initialize the table with default values, or those from the provided
		 * onset configuration of available.
		 * 
		 * @param iniValues
		 */
		public void setValuesTable(boolean iniValues) {
			Vector<Object[]> data1 = new Vector<Object[]>();

			boolean timingsPerTrial = false;

			try {
				timingsPerTrial = Simulator.getController().getModel()
						.isTimingPerTrial();
			} catch (NullPointerException e) {
				// Ignore.
			}

			try {
				row = 0;
				col = timingsPerTrial ? 9 : 8;
				columnNames = timingsPerTrial ? new String[] { "US Length", "",
						"CS", "F (or V)", "(duration)", "Copy",
						"Fw (or Bw or Sm)", "(duration)", "Copy" }
						: new String[] { "US Length", "CS", "F (or V)",
								"(duration)", "Copy", "Fw (or Bw or Sm)",
								"(duration)", "Copy" };

				if (timings == null) {
					timings = new TimingConfiguration();
				}

				Set<Entry<CS, StimulusOnsetConfig>> stims = new TreeSet<Entry<CS, StimulusOnsetConfig>>(
						new TimingComparator());
				stims.addAll(timings.getDurations().getMap().entrySet());

				if (timingsPerTrial) {
					// Sort the set by trial string, but maintain order of
					// insert
				}
				String lastTrialString = "";
				for (Entry<CS, StimulusOnsetConfig> stim : stims) {
					Object record[] = new Object[col];
					record[0] = "";
					// Insert by trial display if using multiples
					if (timingsPerTrial) {

						record[1] = stim.getKey().getTrialString()
								.equals(lastTrialString) ? "" : stim.getKey()
								.getTrialString();
						lastTrialString = stim.getKey().getTrialString();
						record[2] = stim.getKey();

						StimulusOnsetConfig current = stim.getValue();
						record[3] = current.isFixed() ? types[0] : types[1];
						if (current.isFixed()) {
							FixedOnsetConfig onset = (FixedOnsetConfig) current;
							record[4] = onset == null ? "0" : onset
									.getNextOnset() + "";
						} else {
							VariableOnsetConfig onset = (VariableOnsetConfig) current;
							record[4] = onset == null ? "0.0" : onset.getMean()
									+ "";
						}
						record[5] = copyDown;

						USConfiguration currentTiming = timings
								.getRelation(stim.getKey());

						record[6] = currentTiming.getType();
						double offset;
						if (currentTiming.getType()
								.equals(Relationship.FORWARD)) {
							offset = Math.max(currentTiming.getOffset(),
									currentTiming.getForwardDefault());
						} else {
							offset = currentTiming.getOffset();
						}
						record[7] = offset + "";
						record[8] = copyDown;
					} else {
						record[1] = stim.getKey();
						StimulusOnsetConfig current = stim.getValue();
						record[2] = current.isFixed() ? types[0] : types[1];
						if (current.isFixed()) {
							FixedOnsetConfig onset = (FixedOnsetConfig) current;
							record[3] = onset == null ? "0" : onset
									.getNextOnset() + "";
						} else {
							VariableOnsetConfig onset = (VariableOnsetConfig) current;
							record[3] = onset == null ? "0.0" : onset.getMean()
									+ "";
						}
						record[4] = copyDown;
						USConfiguration currentTiming = timings
								.getRelation(stim.getKey());

						record[5] = currentTiming.getType();
						double offset;
						if (currentTiming.getType()
								.equals(Relationship.FORWARD)) {
							offset = Math.max(currentTiming.getOffset(),
									currentTiming.getForwardDefault());
						} else {
							offset = currentTiming.getOffset();
						}
						record[6] = offset + "";
						record[7] = copyDown;
					}
					data1.add(record);
				}
				if (!data1.isEmpty()) {
					data1.get(0)[0] = timings.getUsDuration() + "";
				}
				setData(data1);
				fireTableChanged(null); // notify everyone that we have a new
										// table.
			} catch (Exception e) {
				setData(new Vector()); // blank it out and keep going.
				e.printStackTrace();
			}
			if (!timings.isReinforced() && !data1.isEmpty()) {
				table.setValueAt("0", 0, 0);
				usCol = table.getColumnModel().getColumn(0);
				table.getColumnModel().removeColumn(usCol);
			}
		}
	}

	/** Types of duration. **/
	private String[] types;

	/** Cell editor providing a combobox. **/
	private DefaultCellEditor typeEditor;
	private static String copyDown = "\u25BC";
	private final JPanel contentPanel = new JPanel();
	private JTable table;
	/** configuration storage. **/
	private TimingConfiguration timings;

	/** Boolean indicating that the US should be hidden. **/
	private boolean usHidden;

	private TableColumn usCol;

	private String[] condTypes;

	private DefaultCellEditor condTypeEditor;

	Action fillTiming = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JTable table = (JTable) e.getSource();
			int modelRow = Integer.valueOf(e.getActionCommand());
			TableModel model = table.getModel();
			int index = timingsPerTrial() ? 1 : 0;
			int typeCol = 2 + index;
			// Get current timing
			double duration = getCSDuration(modelRow);
			String type = (String) model.getValueAt(modelRow, typeCol);
			for (int i = modelRow + 1; i < model.getRowCount(); i++) {
				model.setValueAt(type, i, typeCol);
				setCSDuration(i, duration);
			}
		}
	};

	Action fillConditioning = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JTable table = (JTable) e.getSource();
			int modelRow = Integer.valueOf(e.getActionCommand());
			TableModel model = table.getModel();
			// Get current conditioning
			double duration = getConditioningDuration(modelRow);
			Relationship type = getRelationship(modelRow);
			for (int i = modelRow + 1; i < model.getRowCount(); i++) {
				setConditioningDuration(i, duration);
				setRelationship(i, type);
			}
		}
	};


	/**
	 * Create the dialog.
	 */
	public TimingDialog(ActionListener listener) {
        setIconImages(Simulator.makeIcons());
		usHidden = false;
		setBounds(100, 100, 600, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			// CS duration types
			types = new String[] { "Fixed", "Variable" };
			JComboBox typeBox = new JComboBox(types);
			typeEditor = new DefaultCellEditor(typeBox);

			JComboBox condTypeBox = new JComboBox(new DefaultComboBoxModel(
					Relationship.values()));
			condTypeEditor = new DefaultCellEditor(condTypeBox);

			TimingValuesTableModel tm = new TimingValuesTableModel();
			tm.setValuesTable(true);
			table = new JTable() {

				// Overridden to return a combobox for duration type
				@Override
				public TableCellEditor getCellEditor(int row, int column) {
					int modelColumn = convertColumnIndexToModel(column);
					int typeCol = 2;
					boolean timingsPerTrial = false;

					try {
						timingsPerTrial = Simulator.getController().getModel()
								.isTimingPerTrial();
					} catch (NullPointerException e) {
						// Ignore.
					}
					if (timingsPerTrial) {
						typeCol++;
					}

					if (modelColumn == typeCol)
						return typeEditor;
					else if (modelColumn == (typeCol + 3))
						return condTypeEditor;
					else
						return super.getCellEditor(row, column);
				}

				// Working around a nullpointer thrown because we return an
				// interface
				// for the onsets column.
				@Override
				public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
					if (columnClass == null) {
						return null;
					} else {
						Object renderer = defaultRenderersByColumnClass
								.get(columnClass);
						if (renderer != null) {
							return (TableCellRenderer) renderer;
						} else {
							Class<?> superclass = columnClass.getSuperclass();
							if (superclass == null) {
								return getDefaultRenderer(Object.class);
							}
							return getDefaultRenderer(superclass);
						}
					}
				}
			};
			// Make window close on second enter press
			InputMap map = table.getInputMap(
					JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent();
			map.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			tm.setValuesTable(true);
			DefaultCellEditor singleClickEditor = new DefaultCellEditor(
					new JTextField());
			singleClickEditor.setClickCountToStart(1);
			table.setDefaultEditor(Object.class, singleClickEditor);
			table.setCellSelectionEnabled(false);
			// Add custom editors for onset and us relation objects
			table.setDefaultEditor(StimulusOnsetConfig.class,
					new OnsetsEditor());
			table.setDefaultEditor(USConfiguration.class, new USEditor());
			table.setCellSelectionEnabled(false);
			table.setColumnModel(new GroupableTableColumnModel());
			table.setTableHeader(new GroupableTableHeader(
					(GroupableTableColumnModel) table.getColumnModel()));
			table.setModel(tm);

			table.getColumnModel().getColumn(2).setPreferredWidth(20);
			table.doLayout();
			table.requestFocus();

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

	public double getConditioningDuration(int row) {
		TableModel model = table.getModel();
		int durationCol = timingsPerTrial() ? 7 : 6;
		return Double.parseDouble((String) model.getValueAt(row, durationCol));
	}

	public TimingConfiguration getConfig() {
		TableModel model = table.getModel();
		if (model.getRowCount() > 0) {
			String inputUsLength = (String) model.getValueAt(0, 0);
			inputUsLength = inputUsLength.isEmpty() ? "0" : inputUsLength;
			timings = new TimingConfiguration();
			timings.setUsDuration(Double.parseDouble(inputUsLength));
			timings.setDurations(getOnsetConfig());
			timings.setRelations(getUSConfig());
			timings.setConfigured(true);
		}
		return timings;
	}

	/**
	 * Produces a duration configuration from the table model.
	 * 
	 * @return a new OnsetConfig holding the input duration config.
	 */

	public USConfiguration getConfig(int row, int col) {
		CS cs = getCS(row);
		USConfiguration current = timings.getRelation(cs);
		TableModel model = table.getModel();
		double fwdDefault = 0;
		try {
			fwdDefault = current.getForwardDefault();
		} catch (NullPointerException e) {
		}
		current = new USConfiguration();
		current.setForwardDefault(fwdDefault);
		double offset = getConditioningDuration(row);
		current.setType(getRelationship(row));
		current.setOffset(offset);
		return current;
	}

	/**
	 * Get the CS of a row.
	 * 
	 * @param row
	 *            integer row number.
	 * @return the CS this row is about.
	 */

	public CS getCS(int row) {
		TableModel model = table.getModel();
		int csCol = timingsPerTrial() ? 2 : 1;
		return (CS) model.getValueAt(row, csCol);
	}

	public double getCSDuration(int row) {
		TableModel model = table.getModel();
		int durationCol = timingsPerTrial() ? 4 : 3;
		return Double.parseDouble((String) model.getValueAt(row, durationCol));
	}

	/**
	 * Produces a duration configuration from the table model.
	 * 
	 * @return a new OnsetConfig holding the input duration config.
	 */

	public OnsetConfig getOnsetConfig() {
		TableModel model = table.getModel();
		OnsetConfig onsets = new OnsetConfig();
		/** Duration configuration storage. **/
		StimulusOnsetConfig current = null;
		int index = timingsPerTrial() ? 1 : 0;
		int typeCol = 2 + index;
		for (int i = 0; i < model.getRowCount(); i++) {
			String type = (String) model.getValueAt(i, typeCol);
			double duration = getCSDuration(i);
			if (types[0].equals(type)) {
				current = new FixedOnsetConfig(duration);
			} else {
				current = new VariableOnsetConfig(duration,
						VariableDistribution.randomSeed(), 0,
						Distributions.EXPONENTIAL, false);
			}
			onsets.set(getCS(i), current);
		}
		return onsets;
	}

	public Relationship getRelationship(int row) {
		TableModel model = table.getModel();
		int relationCol = timingsPerTrial() ? 6 : 5;
		return (Relationship) model.getValueAt(row, relationCol);
	}

	/**
	 * Produces a mapping from CS names to US relationships.
	 * 
	 * @return a new OnsetConfig holding the input duration config.
	 */

	private Map<CS, USConfiguration> getUSConfig() {
		TableModel model = table.getModel();
		Map<CS, USConfiguration> relationships = new HashMap<CS, USConfiguration>();
		int col = Simulator.getController().getModel().isTimingPerTrial() ? 5
				: 4;
		for (int i = 0; i < model.getRowCount(); i++) {
			relationships.put(getCS(i), getConfig(i, col));
		}
		return relationships;
	}

	private void makeButtons() {
        int offset = timings.isReinforced() ? 1 : 0;
		int timingCol = timingsPerTrial() ? 4 : 3;
        timingCol += offset;
		int condCol = timingCol + 3;
		ButtonColumn fillDownTiming = new ButtonColumn(table, fillTiming,
				timingCol);
		ButtonColumn fillDownConditioning = new ButtonColumn(table,
				fillConditioning, condCol);
	}

	/**
	 * Produces a duration configuration from the table model by merging
	 * together all the cells.
	 * 
	 * @return a new OnsetConfig holding the input duration config.
	 */
	@Deprecated
	private OnsetConfig makeOnsetConfig() {
		TableModel model = table.getModel();
		OnsetConfig onsets = new OnsetConfig();
		for (int i = 0; i < model.getRowCount(); i++) {
			int index = Simulator.getController().getModel().isTimingPerTrial() ? 1
					: 0;
			onsets.set(getCS(i),
					(StimulusOnsetConfig) model.getValueAt(i, index + 2));
		}
		onsets.setConfigured(true);
		return onsets;
	}

	public void setConditioningDuration(int row, Double duration) {
		TableModel model = table.getModel();
		int durationCol = timingsPerTrial() ? 7 : 6;
		model.setValueAt(duration.toString(), row, durationCol);
	}

	public void setConfig(TimingConfiguration currentConfig) {
		timings = currentConfig;
		((TimingValuesTableModel) table.getModel()).setValuesTable(true);
		boolean timingsPerTrial = false;

		try {
			timingsPerTrial = Simulator.getController().getModel()
					.isTimingPerTrial();
		} catch (NullPointerException e) {
			// Ignore.
		}

		// Setup Column Groups
		GroupableTableColumnModel cm = (GroupableTableColumnModel) table
				.getColumnModel();
        int offset = timings.isReinforced() ? 1 : 0;
		ColumnGroup g_temporal = new ColumnGroup("CS Temporal Properties");
		if (timingsPerTrial) {
			g_temporal.add(cm.getColumn(2 + offset));
			g_temporal.add(cm.getColumn(3 + offset));
			g_temporal.add(cm.getColumn(4 + offset));
		} else {
			g_temporal.add(cm.getColumn(1 + offset));
			g_temporal.add(cm.getColumn(2 + offset));
			g_temporal.add(cm.getColumn(3 + offset));
		}
		ColumnGroup g_conditioning = new ColumnGroup(/*
													 * new
													 * GroupableTableCellRenderer
													 * (),
													 */"Conditioning");
		if (timingsPerTrial) {
			g_conditioning.add(cm.getColumn(5 + offset));
			g_conditioning.add(cm.getColumn(6 + offset));
			g_conditioning.add(cm.getColumn(7 + offset));
		} else {
			g_conditioning.add(cm.getColumn(4 + offset));
			g_conditioning.add(cm.getColumn(5  + offset));
			g_conditioning.add(cm.getColumn(6 + offset));
		}
		GroupableTableHeader header = (GroupableTableHeader) table
				.getTableHeader();
		cm.addColumnGroup(g_temporal);
		cm.addColumnGroup(g_conditioning);
		makeButtons();
	}

	public void setCSDuration(int row, Double duration) {
		TableModel model = table.getModel();
		int durationCol = timingsPerTrial() ? 4 : 3;
		model.setValueAt(duration.toString(), row, durationCol);
	}

	/**
	 * Set individual onset configs in the model.
	 * 
	 * @param currentConfig
	 */

	private void setOnsetConfig(OnsetConfig currentConfig) {
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String cue = (String) model.getValueAt(i, 1);
			StimulusOnsetConfig tmp = currentConfig.getMap().get(cue);
			model.setValueAt(tmp, i, 2);
		}
	}

	public void setRelationship(int row, Relationship relation) {
		TableModel model = table.getModel();
		int relationCol = timingsPerTrial() ? 6 : 5;
		model.setValueAt(relation, row, relationCol);
	}

	/**
	 * Set individual us configs in the model.
	 * 
	 * @param currentConfig
	 */

	private void setUSConfig(Map<String, USConfiguration> currentConfig) {
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String cue = (String) model.getValueAt(i, 1);
			USConfiguration tmp = currentConfig.get(cue);
			model.setValueAt(tmp, i, 3);
		}
	}

	/**
	 * @param contains
	 */
	public void setUSHidden(boolean hide) {
		usHidden = hide;
		if (hide) {
			timings.setUsDuration(0);
		} else if (!hide && timings.getUsDuration() == 0) {
			timings.setUsDuration(1 * Simulator.getController().getModel()
					.getTimestepSize());
		}
	}

	public boolean timingsPerTrial() {
		boolean timingsPerTrial = false;

		try {
			timingsPerTrial = Simulator.getController().getModel()
					.isTimingPerTrial();
		} catch (NullPointerException e) {
			// Ignore.
		}
		return timingsPerTrial;
	}
}