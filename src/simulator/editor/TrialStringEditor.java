/**
 * 
 */
package simulator.editor;

import simulator.Simulator;

import java.awt.Component;
import java.awt.event.InputMethodListener;

import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;


/**
 * Single click editor that attaches a document listener to the corresponding timing
 * configuration.
 * 
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 **/
public class TrialStringEditor extends DefaultCellEditor {

	/**
	 * 
	 */
	public TrialStringEditor() {
		super(new JTextField());
		setClickCountToStart(1);
	}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			JTextField ftf = (JTextField) super
					.getTableCellEditorComponent(table, value, isSelected, row,
							column);
			//ftf.setText(value.toString());
            //int offset = Simulator.getController().getModel().isUseContext() ? 3 : 2;
            DocumentListener target = (DocumentListener) table.getModel().getValueAt(row, column+3);
            ftf.getDocument().addDocumentListener(target);
			return ftf;
		}

}
