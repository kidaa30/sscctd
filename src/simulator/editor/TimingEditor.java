package simulator.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import simulator.CS;
import simulator.SimPhase;
import simulator.Simulator;
import simulator.configurables.TimingConfiguration;
import simulator.dialog.TimingDialog;

/**
 * TimingEditor.java
 * 
 * Created on Jan-2012 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class TimingEditor extends AbstractCellEditor implements
		TableCellEditor, ActionListener {

	private JButton button;
	private TimingDialog dialog;
	protected static final String EDIT = "edit";
	private String phaseString;
	private TimingConfiguration currentConfig;

	public TimingEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		dialog = new TimingDialog(this);

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
			List<CS> stimuli = new ArrayList<CS>();
			phaseString = phaseString == null ? "" : phaseString;
			stimuli.addAll(SimPhase.stringToCSList(phaseString));
			currentConfig.setStimuli(stimuli);

			// currentConfig.setStimuli(stimuli);
			dialog.setConfig(currentConfig);
			// Hide US duration for trial strings with no reinforcement
			// dialog.setUSHidden(!phaseString.contains("+"));
			dialog.setVisible(true);

		} else {
			currentConfig = dialog.getConfig();
			dialog.setVisible(false);
			fireEditingStopped(); // Make the renderer reappear.
		}
	}

	@Override
	public Object getCellEditorValue() {
		return currentConfig;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		currentConfig = (TimingConfiguration) value;
		int offset = 3;
		// If we aren't simming context, we need to go back one less column
		if (!table.getColumnName(column - offset).contains("Phase")) {
			offset--;
		}
		phaseString = (String) table.getValueAt(row, column - offset);
		dialog.setTitle(((String) table.getValueAt(row, 0)) + " Phase "
				+ (column / 5 + 1));
		return button;
	}

	/**
	 * @return
	 */
	private boolean timingPerTrial() {
		return Simulator.getController().getModel().isTimingPerTrial();
	}

}
