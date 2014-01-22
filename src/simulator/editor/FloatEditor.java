/**
 * 
 */
package simulator.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class FloatEditor extends DefaultCellEditor {
	JFormattedTextField ftf;
	NumberFormat floatFormat;
	private float minimum;
	private boolean DEBUG = false;

	public FloatEditor() {
		super(new JFormattedTextField());
		ftf = (JFormattedTextField) getComponent();
		minimum = 0;

		// Set up the editor for the integer cells.
		floatFormat = NumberFormat.getNumberInstance();
		NumberFormatter floatFormatter = new NumberFormatter(floatFormat);
		floatFormatter.setFormat(floatFormat);
		floatFormatter.setMinimum(minimum);

		ftf.setFormatterFactory(new DefaultFormatterFactory(floatFormatter));
		ftf.setValue(minimum);
		ftf.setHorizontalAlignment(SwingConstants.TRAILING);
		ftf.setFocusLostBehavior(JFormattedTextField.PERSIST);

		// React when the user presses Enter while the editor is
		// active. (Tab is handled as specified by
		// JFormattedTextField's focusLostBehavior property.)
		ftf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
				"check");
		ftf.getActionMap().put("check", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ftf.isEditValid()) { // The text is invalid.
					ftf.setValue(ftf.getValue());
					ftf.postActionEvent(); // inform the editor
				} else
					try { // The text is valid,
						ftf.commitEdit(); // so use it.
						ftf.postActionEvent(); // stop editing
					} catch (java.text.ParseException exc) {
					}
			}
		});
	}

	// Override to ensure that the value remains an Integer.
	@Override
	public Object getCellEditorValue() {
		JFormattedTextField ftf = (JFormattedTextField) getComponent();
		Object o = ftf.getValue();
		if (o instanceof Float) {
			return o;
		} else if (o instanceof Number) {
			return new Float(((Number) o).intValue());
		} else {
			if (DEBUG) {
				System.out.println("getCellEditorValue: o isn't a Number");
			}
			try {
				return floatFormat.parseObject(o.toString());
			} catch (ParseException exc) {
				System.err.println("getCellEditorValue: can't parse o: " + o);
				return null;
			}
		}
	}

	// Override to invoke setValue on the formatted text field.
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		JFormattedTextField ftf = (JFormattedTextField) super
				.getTableCellEditorComponent(table, value, isSelected, row,
						column);
		ftf.setValue(value);
		return ftf;
	}

	// Override to check whether the edit is valid,
	// setting the value if it is and complaining if
	// it isn't. If it's OK for the editor to go
	// away, we need to invoke the superclass's version
	// of this method so that everything gets cleaned up.
	@Override
	public boolean stopCellEditing() {
		JFormattedTextField ftf = (JFormattedTextField) getComponent();
		if (ftf.isEditValid()) {
			try {
				ftf.commitEdit();
			} catch (java.text.ParseException exc) {
			}

		} else { // text is invalid
			return false; // don't let the editor go away
		}
		return super.stopCellEditing();
	}
}
