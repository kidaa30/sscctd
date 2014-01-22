/**
 * SimView.java
 * 
 * Created on 10-Mar-2005
 * City University
 * BSc Computing with Distributed Systems
 * Project title: Simulating Animal Learning
 * Project supervisor: Dr. Eduardo Alonso 
 * @author Dionysios Skordoulis
 *
 * Modified in October-2009
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Rocio Garcia Duran
 *
 * Modified in July-2011
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Dr. Alberto Fernandez
 * email: alberto.fernandez@urjc.es
 *
 */
package simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import simulator.editor.ContextEditor;
import simulator.editor.ITIEditor;
import simulator.editor.TimingEditor;
import simulator.editor.TrialStringEditor;
import simulator.util.Distributions;
import simulator.util.GreekSymbol;
import simulator.util.Trace;
import simulator.util.ValuesTableModel;

//import sun.awt.VerticalBagLayout;   // modified by Alberto Fernandez: 18 July 2011

/**
 * SimView class is the main graphical user interface of the Simulator's
 * application. The main purposes is the managing of viewing objects. It
 * contains a menu bar on top with File -> New, Open, Save, Export and Quit,
 * Settings -> Groups, Phases, Combinations and last the Help -> Guide. Next, a
 * phase table where the user adds the group names, the sequences on every phase
 * and if he prefers to run randomly. The value table where the user adds the
 * values for every parameter that is needed and the text output which display
 * the results. It also contains buttons for interaction.
 */
public class SimView extends JFrame {

	/** This class is the values' table model */
	class CSValuesTableModel extends ValuesTableModel {

		/**
		 * ValuesTableModel's Constructor method.
		 */
		public CSValuesTableModel() {
			super();
		}

		/**
		 * Return the names of the table's column names.
		 * 
		 * @return an array with the table's column names.
		 */
		private String[] getColNames() {
			String[] s = new String[col];
			for (int c = 0; c < col; c++) {
				if (c == 0)
					s[c] = Messages.getString("SimView.csAlpha"); //$NON-NLS-1$
				else if (col == 2)
					s[c] = Messages.getString("SimView.value"); //$NON-NLS-1$
				else
					s[c] = Messages.getString("SimView.phaseSpace") + (c + 1); //$NON-NLS-1$
			}
			return s;
		}

		/**
		 * Initializes and configures the table with some initial values.
		 * 
		 * @param iniValues
		 *            . If true, we initialize all variables to "" or by default
		 *            without taking into account last values of cues
		 */
		public void setValuesTable(boolean iniValues) {
			// if data had some values before, then reuse them
			Vector data2 = new Vector((Vector) getData().clone());
			Vector data1 = new Vector();
			data = new Vector();

			try {
				row = model.getNumberAlphaCues();
				col = 2;
				columnNames = getColNames();

				TreeMap<String, Double> tm = model.getAlphaCues();
				Iterator<String> it = tm.keySet().iterator();
				// Split into two lists, sort the list of compounds
				// by interface name not letter
				List<String> cueNames = new ArrayList<String>();
				TreeMap<String, String> configuralNames = new TreeMap<String, String>();
				while(it.hasNext()) {
					String pair = it.next();
					if((isSetConfiguralCompounds() || model
							.isSerialConfigurals())
							&& model.getConfigCuesNames()
									.containsKey(pair)) {
						String compoundName = model.getConfigCuesNames().get(pair);
						String interfaceName = "c(" + compoundName + ")";
						configuralNames.put(interfaceName, pair);
					} else {
						cueNames.add(pair);
					}
				}				
				cueNames.addAll(configuralNames.values());
				it = cueNames.iterator();
				while (it.hasNext()) {
					String pair = it.next();
					// Disregard context cues, alphas for them are set elsewhere
					if (!Context.isContext(pair)) {

						if (pair.length() == 1) {
							Object record[] = new Object[col];
							int isInValues = -1;
							boolean configuralEmpty = false;

							for (int c = 0; c < col; c++) {
								// cue name
								if (c == 0) {
									record[c] = pair;
									// Alberto Fernandez August-2011
									if ((isSetConfiguralCompounds() || model
											.isSerialConfigurals())
											&& model.getConfigCuesNames()
													.containsKey(pair)) {
										String compoundName = model
												.getConfigCuesNames().get(pair);
										// String interfaceName = "�(" +
										// compoundName + ")";
										String interfaceName = "c(" + compoundName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
										record[c] = interfaceName;
									}
									// isInValues = isInValuesTable(data2,pair);
									isInValues = isInValuesTable(data2,
											(String) record[c]);
								}
								// cue value
								// If it exists already --> last value from
								// data2
								// else if (isInValues>-1 && !iniValues) {
								else if (isInValues > -1
										&& !iniValues
										&& !(((String) record[0]).length() > 1 && ((Object[]) data2
												.get(isInValues))[c] == "")) { //$NON-NLS-1$
									record[c] = ((Object[]) data2
											.get(isInValues))[c];
								}
								// Alberto Fernandez Sept-2011
								// If it didnt exist --> if c(AB) ==> A*B
								// otherwise default value
								else if (((String) record[0]).length() > 1) {

									// find record[0]= c(AB)
									Double val = 1.0;
									boolean found, missing = false;
									String compound = ((String) record[0])
											.substring(2, ((String) record[0])
													.length() - 1);
									if (compound.startsWith(Simulator.OMEGA
											+ "")) { //$NON-NLS-1$
										compound = compound.substring(1);
									}
									// Filter out dashes in compound name
									compound = compound.replaceAll(
											ConfiguralCS.SERIAL_SEP, "");
									// Dedupe
									Set<Character> compoundSet = new HashSet<Character>();
									for (char cs : compound.toCharArray()) {
										compoundSet.add(cs);
									}
									StringBuilder sb = new StringBuilder();
									for (char cs : compoundSet) {
										sb.append(cs);
									}
									compound = sb.toString();

									String cue;

                                    double maxVal = 0;
                                    double minVal = Double.POSITIVE_INFINITY;
                                    List<Double> alphas = new ArrayList<Double>();

									for (int i = 0; i < compound.length(); i++) {
										cue = compound.substring(i, i + 1);
										// find cue in Vector data2
										found = false;
										missing = false; // there is no value
															// for current cue

										if (Context.isContext(cue)) {
                                            alphas.add(model.getContexts().get(cue)
                                                    .getAlpha());
											maxVal = Math.max(maxVal, model.getContexts().get(cue)
													.getAlpha());
                                            minVal = Math.min(minVal, model.getContexts().get(cue)
                                                    .getAlpha());
											found = true;
										} else {

											for (int j = 0; j < data2.size(); j++) {
												Object cue_value_pair[] = (Object[]) data2
														.get(j);
												if (cue.equals(cue_value_pair[0])) {
													String s = (String) cue_value_pair[1];
													if (s != "") { //$NON-NLS-1$
                                                        alphas.add(Double.parseDouble(s));
														//maxVal = Math.max(maxVal, Double.parseDouble(s));
                                                        //minVal = Math.min(minVal, Double.parseDouble(s));
														found = true;
														break;
													}
												}
											}
										}
										if (!found) {
											val *= 0.2;
										}
									}
									if (!missing) { // found) {
                                        Collections.sort(alphas);
                                        try {
                                            maxVal = alphas.get(alphas.size() - 1);
                                        } catch(ArrayIndexOutOfBoundsException e) {
                                            maxVal = 0.2;
                                        }
                                        try {
                                            minVal = alphas.get(alphas.size() - 2);
                                        } catch(ArrayIndexOutOfBoundsException e) {
                                            minVal = 0.2;
                                        }
										record[c] = (new DecimalFormat("#.##########")).format(maxVal * minVal);
									} else {
										record[c] = ""; //$NON-NLS-1$
									}
								} else {
									record[c] = "0.2"; //$NON-NLS-1$
								}
							}
							data1.add(record);
						}
					}
					setData(data1);
					fireTableChanged(null); // notify everyone that we have a
											// new table.
				}
			} catch (Exception e) {
				setData(new Vector()); // blank it out and keep going.
				e.printStackTrace();
			}
		}
	}

	/*
	 * Overwrite the names alpha, lambda and beta to the corresponding greek
	 * characters in the ValuesTable
	 */
	private static class GreekRenderer extends DefaultTableCellRenderer {
		@Override
		public void setText(String name) {
			super.setText(GreekSymbol.getSymbol(name));
		}
	}

	/** This class is the other values' table model */
	class OtherValuesTableModel extends ValuesTableModel {

		/**
		 * 
		 */
		private static final String GAMMA = "gamma"; //$NON-NLS-1$
		/**
		 * 
		 */
		private static final String DELTA = "delta"; //$NON-NLS-1$
		// Modified to use TD variables (lambda, gamma)
		private final String[] values = { "0.95", "0.98" }; //$NON-NLS-1$ //$NON-NLS-2$
		private final String[] names = { DELTA, GAMMA };

		/**
		 * ValuesTableModel's Constructor method.
		 */
		public OtherValuesTableModel(int col) {
			super();
		}

		/**
		 * Return the names of the table's column names.
		 * 
		 * @return an array with the table's column names.
		 */
		private String[] getColNames() {
			String[] s = new String[col];
			for (int c = 0; c < col; c++) {
				if (c == 0)
					s[c] = Messages.getString("SimView.others"); //$NON-NLS-1$
				else if (col == 2)
					s[c] = Messages.getString("SimView.value"); //$NON-NLS-1$
				else
					s[c] = Messages.getString("SimView.phaseSpace") + c; //$NON-NLS-1$
			}
			return s;
		}

		/*
		 * Initializes and configures the table with some initial values.
		 * 
		 * @param iniValues. If true, we initialize all variables to "" or by
		 * default without taking into account last values of cues
		 */
		public void setInitialValuesTable() {
			Vector data1 = new Vector();
			col = 2;
			row = names.length;
			columnNames = getColNames();

			try {
				for (int r = 0; r < row; r++) { // row ser� 4 (betas y lambdas)
					Object record[] = new Object[col];
					record[0] = names[r];
					for (int c = 1; c < col; c++) {
						// Modified to use TD variables
						if (((String) record[0]).indexOf(GAMMA) != -1 && c == 1)
							record[c] = "0.98"; //$NON-NLS-1$
						else if (((String) record[0]).indexOf(DELTA) != -1
								&& c == 1)
							record[c] = "0.95"; //$NON-NLS-1$
						else
							record[c] = ""; //$NON-NLS-1$
					}
					data1.addElement(record);
				}
				setData(data1);
				fireTableChanged(null); // notify everyone that we have a new
										// table.
			} catch (Exception e) {
				setData(new Vector()); // blank it out and keep going.
				e.printStackTrace();
			}
		}

		/*
		 * Initializes and configures the table with some initial values.
		 * 
		 * @param iniValues. If true, we initialize all variables to "" or by
		 * default without taking into account last values of cues
		 */
		public void setValuesTable(boolean iniValues, boolean allphases) {
			// if data had some values before, then reuse them
			Vector data2 = new Vector((Vector) getData().clone());

			Object record2[] = (Object[]) data2.firstElement();
		}

	}

	/** This class is the phases' table model */
	class PhasesTableModel extends ValuesTableModel {

		/**
		 * PhasesTableModel's Constructor method.
		 */
		public PhasesTableModel() {
			super();
		}

		/*
		 * Add a new row to the vector data
		 */
		public void addGroup() {
			row = model.getGroupNo();
			Object record[] = new Object[col];
			for (int c = 0; c < col; c++) {
				if (c == 0)
					record[c] = Messages.getString("SimView.group") + row; //$NON-NLS-1$
				else if (c % 5 == 0)
					record[c] = new ITIConfig(0);
				else if (c % 5 == 4)
					record[c] = new TimingConfiguration();
				else if (c % 5 == 3)
					record[c] = new Boolean(false);
				else if (c % 5 == 2)
					record[c] = new ContextConfig();
				else
					record[c] = ""; //$NON-NLS-1$
			}

			data.addElement(record);
			fireTableChanged(null); // notify everyone that we have a new table.
			if (!isUseContext()) {
				removeOmegaPhases();
			}
		}

		/*
		 * Add a new column to the vector data
		 */
		public void addPhase() {
			col = model.getPhaseNo() * 5 + 1;
			columnNames = getColNames();
			Vector newData = new Vector();
			for (Iterator it = data.iterator(); it.hasNext();) {
				Object record[] = new Object[col];
				Object[] oldRecord = (Object[]) it.next();
				System.arraycopy(oldRecord, 0, record, 0, oldRecord.length);
				record[record.length - 5] = ""; //$NON-NLS-1$
				record[record.length - 4] = new ContextConfig();
				record[record.length - 3] = new Boolean(false);
				record[record.length - 2] = new TimingConfiguration();
				record[record.length - 1] = new ITIConfig(0);
				newData.add(record);
			}
			data = newData;
			fireTableChanged(null); // notify everyone that we have a new table.
			if (!isUseContext()) {
				removeOmegaPhases();
			}
		}

		/**
		 * Return the names of the table's column names.
		 * 
		 * @return an array with the table's column names.
		 */
		private String[] getColNames() {
			String[] s = new String[col];
			for (int c = 0; c < col; c++) {
				if (c == 0)
					s[c] = GROUP_NAME;
				else if (c % 5 == 0)
					s[c] = ITI;
				else if (c % 5 == 2)
					s[c] = CONTEXT;
				else if (c % 5 == 3)
					s[c] = RANDOM;
				else if (c % 5 == 4)
					s[c] = TIMING;
				else
					s[c] = Messages.getString("SimView.phaseSpace") + (c / 5 + 1); //$NON-NLS-1$
			}
			return s;
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			if (getColumnName(col).equals(CONTEXT)
					&& !model.contextAcrossPhase()) {
				return false;
			}
			return true;
		}

		/*
		 * Remove last row of the vector data
		 */
		public void removeGroup() {
			row = model.getGroupNo();
			data.remove(data.size() - 1);
			fireTableChanged(null);
			if (!isUseContext()) {
				removeOmegaPhases();
			}
		}

		/*
		 * Remove the last column of the vector data
		 */
		public void removePhase() {
			if (col > 5) {
				col = model.getPhaseNo() * 5 + 1;
				columnNames = getColNames();
				Vector newData = new Vector();
				for (Iterator it = data.iterator(); it.hasNext();) {
					Object record[] = new Object[col];
					Object[] oldRecord = (Object[]) it.next();
					System.arraycopy(oldRecord, 0, record, 0, record.length);
					newData.add(record);
				}
				data = newData;
				fireTableChanged(null); // notify everyone that we have a new
										// table.
			}
			if (!isUseContext()) {
				removeOmegaPhases();
			}
		}

		/*
		 * Initializes and configures the table with some initial values.
		 */
		public void setPhasesTable() {
			clearHidden();
			data = new Vector();
			try {
				col = model.getPhaseNo() * 5 + 1;
				row = model.getGroupNo();
				columnNames = getColNames();

				for (int r = 0; r < row; r++) {
					Object record[] = new Object[col];
					for (int c = 0; c < col; c++) {
						if (c == 0)
							record[c] = Messages.getString("SimView.group") + (r + 1); //$NON-NLS-1$
						else if (c % 5 == 0)
							record[c] = new ITIConfig(0);
						else if (c % 5 == 4)
							record[c] = new TimingConfiguration();
						else if (c % 5 == 3)
							record[c] = new Boolean(false);
						else if (c % 5 == 2)
							record[c] = new ContextConfig();
						else
							record[c] = ""; //$NON-NLS-1$
					}
					data.addElement(record);
				}
				fireTableChanged(null); // notify everyone that we have a new
										// table.
				if (!isUseContext()) {
					removeOmegaPhases();
				}
			} catch (Exception e) {
				data = new Vector(); // blank it out and keep going.
				e.printStackTrace();
			}
		}
	}

	/** This class is the values' table model */
	class USValuesTableModel extends ValuesTableModel {

		/**
		 * US Names
		 */
		private static final String LAMBDA_MINUS = "lambda-"; //$NON-NLS-1$
		private static final String LAMBDA_PLUS = "lambda+"; //$NON-NLS-1$
		private static final String BETA_MINUS = "beta-"; //$NON-NLS-1$
		private static final String BETA_PLUS = "beta+"; //$NON-NLS-1$
		// Modified to use TD variables (lambda, gamma)
		private final String[] USnames = { BETA_PLUS, BETA_MINUS, LAMBDA_PLUS };// ,
																				// LAMBDA_MINUS};
		private final String[] USvalues = { "0.5", "0.495", "1", "0" }; //Added beta- value //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		/**
		 * ValuesTableModel's Constructor method.
		 */
		public USValuesTableModel(int col) {
			super();
		}

		/*
		 * Add a new column to the vector data
		 */
		public void addPhases() {
			col = model.getPhaseNo() + 1;
			columnNames = getColNames();
			Vector newData = new Vector();
			for (Iterator it = data.iterator(); it.hasNext();) {
				Object record[] = new Object[col];
				Object[] oldRecord = (Object[]) it.next();
				System.arraycopy(oldRecord, 0, record, 0, oldRecord.length);
				for (int i = oldRecord.length; i < col; i++)
					record[i] = ""; //$NON-NLS-1$
				newData.add(record);
			}
			data = newData;
			fireTableChanged(null); // notify everyone that we have a new table.
		}

		/**
		 * Return the names of the table's column names.
		 * 
		 * @return an array with the table's column names.
		 */
		private String[] getColNames() {
			String[] s = new String[col];
			for (int c = 0; c < col; c++) {
				if (c == 0)
					s[c] = Messages.getString("SimView.164"); //$NON-NLS-1$
				else if (col == 2)
					s[c] = Messages.getString("SimView.value"); //$NON-NLS-1$
				else
					s[c] = Messages.getString("SimView.phaseSpace") + c; //$NON-NLS-1$
			}
			return s;
		}

		/*
		 * Remove the last column of the vector data
		 */
		public void removePhases(int phases) {
			col = phases + 1;
			columnNames = getColNames();
			Vector newData = new Vector();
			for (Iterator it = data.iterator(); it.hasNext();) {
				Object record[] = new Object[col];
				Object[] oldRecord = (Object[]) it.next();
				System.arraycopy(oldRecord, 0, record, 0, record.length);
				newData.add(record);
			}
			data = newData;
			fireTableChanged(null); // notify everyone that we have a new table.
		}

		/*
		 * Initializes and configures the table with some initial values.
		 * 
		 * @param iniValues. If true, we initialize all variables to "" or by
		 * default without taking into account last values of cues
		 */
		public void setInitialValuesTable() {
			Vector data1 = new Vector();
			col = 2;
			row = USnames.length;
			columnNames = getColNames();

			try {
				for (int r = 0; r < row; r++) { // row ser� 4 (betas y lambdas)
					Object record[] = new Object[col];
					record[0] = USnames[r];
					for (int c = 1; c < col; c++) {
						record[c] = USvalues[r];
						// Modified to use TD variables
						/*
						 * if (((String)record[0]).indexOf(LAMBDA_PLUS)!=-1 &&
						 * c==1) record[c]="1"; //$NON-NLS-1$ else if
						 * (((String)record[0]).indexOf("gamma")!=-1 && c==1)
						 * record[c]="0.95"; //$NON-NLS-1$ //$NON-NLS-2$ else if
						 * (((String)record[0]).indexOf(LAMBDA_MINUS)!=-1 &&
						 * c==1) record[c]="0"; //$NON-NLS-1$ else if
						 * (((String)record[0]).indexOf("delta")!=-1 && c==1)
						 * record[c]="0.2"; //$NON-NLS-1$ //$NON-NLS-2$ else if
						 * (((String)record[0]).indexOf(BETA_PLUS)!=-1 && c==1)
						 * record[c]="0.5"; //$NON-NLS-1$ else if
						 * (((String)record[0]).indexOf(BETA_MINUS)!=-1 && c==1)
						 * record[c]="0.4"; //$NON-NLS-1$ else if
						 * (((String)record[0]).indexOf("T")!=-1 && c==1)
						 * record[c]="0.035"; //$NON-NLS-1$ //$NON-NLS-2$ else
						 * if (((String)record[0]).indexOf("omega")!=-1 && c==1)
						 * record[c]="0.001"; //$NON-NLS-1$ //$NON-NLS-2$ else
						 * record[c]=""; //$NON-NLS-1$
						 */}
					data1.addElement(record);
				}
				setData(data1);
				fireTableChanged(null); // notify everyone that we have a new
										// table.
			} catch (Exception e) {
				setData(new Vector()); // blank it out and keep going.
				e.printStackTrace();
			}
		}

		/*
		 * Initializes and configures the table with some initial values.
		 * 
		 * @param iniValues. If true, we initialize all variables to "" or by
		 * default without taking into account last values of cues
		 */
		public void setValuesTable(boolean iniValues, boolean allphases) {
			// if data had some values before, then reuse them
			Vector data2 = new Vector((Vector) getData().clone());
			if (allphases) {
				Object record2[] = (Object[]) data2.firstElement();
				if (record2.length <= (model.getPhaseNo() + 1))
					addUSPhases();
				else
					removePhases(model.getPhaseNo());
			} else
				removeUSPhases();
		}

	}

	// Alberto Fern�ndez July-2011
	// Font styles
	private static final String TABLE_FONT_NAME = "Dialog"; //$NON-NLS-1$
	private static final int TABLE_FONT_STYLE = 1;

	private static final int TABLE_FONT_SIZE = 13;

	private static final int TABLE_ROW_HEIGHT = 17;
	private static final int INITIAL_WIDTH = 800;
	private static final int INITIAL_HEIGHT = 700;
	private static final float AligmentY = 0;
	private SimModel model;

	/** Strings. **/
	public static final String GROUP_NAME = Messages
			.getString("SimView.groupName"); //$NON-NLS-1$

	public static final String ITI = Messages.getString("SimView.ITI"); //$NON-NLS-1$
	public static final String TIMING = Messages.getString("SimView.timing"); //$NON-NLS-1$
	public static final String CONTEXT = Messages.getString("SimView.context"); //$NON-NLS-1$ 
	public static final String RANDOM = Messages.getString("SimView.random"); //$NON-NLS-1$

	private Container cp;
	private JMenuBar menuBar;

	private JMenu menuFile, menuDesignSettings, menuHelp;
	private JMenuItem menuItemNew, menuItemOpen, menuItemSave, menuItemExport,
			menuItemQuit, menuItemComb, menuItemGuide, menuItemAbout, menuItemRPM;
	private JCheckBoxMenuItem menuItemUSAcrossPhases, menuItemSetCompound,
			menuItemSetConfiguralCompounds; // menuItemSetConfiguralCompounds by
											// Alberto Fern�ndez August-2011
	private JPanel mainPanel, bottomPanel;
	private JButton setVariablesBut, clearBut, runBut, dispGraphBut,
			addPhaseBut, removePhaseBut, addGroupBut, removeGroupBut;
	private JScrollPane phasesScroll, CSValuesScroll, USValuesScroll,
			outputScroll;
	private JTable phasesTable, CSValuesTable, USValuesTable;

	private PhasesTableModel phasesTableModel;
	private CSValuesTableModel CSValuesTableModel;
	private USValuesTableModel USValuesTableModel;
	private JTextArea outputArea;
	private JLabel bottomLabel;
	private boolean isUSAcrossPhases, isSetCompound, isSetConfiguralCompounds;
	private JMenuItem menuItemContext;
	private JScrollPane otherValuesScroll;
	private OtherValuesTableModel otherTableModel;
	private JTable otherValuesTable;
	private JMenuItem menuItemThreshold;
	private JMenu menuMeans, menuDistributions;
	private JMenuItem menuItemGeometric, menuItemArithmetic;
	private JMenuItem menuItemExp, menuItemUnif;
	private JMenuItem menuItemContextAcrossPhases;
	private JCheckBoxMenuItem menuItemCsc;
	private AbstractButton menuItemTimestep;
	/** Tracking for hidden context columns. **/
	private Map<TableColumn, Integer> hiddenColumns;
	/** Default context salience & salience for uniform contexts. **/
	private double contextAlpha;
	private JRadioButtonMenuItem menuItemSingleContext;
	private JMenu menuContext;
	private JMenu menuProcSettings;
	/** Eligibility trace menus. **/
	private JRadioButtonMenuItem menuItemBoundedTrace;
	private JRadioButtonMenuItem menuItemAccumTrace;
	private JMenu menuTrace;

	private JRadioButtonMenuItem menuItemReplaceTrace;

	private JMenuItem menuItemVarComb;

	/** Timing per phase. **/
	private JCheckBoxMenuItem menuItemTimingPerTrial;

	/** Zero traces between trials. **/
	private JCheckBoxMenuItem zeroTraces;

	/** Consider serial compounds. **/
	private JCheckBoxMenuItem menuItemSerialCompounds;

	/** Restrict predictions to above zero. **/
	private JCheckBoxMenuItem menuItemRestrictPredictions;

    public static int activeRow = -1;
    public static int activeCol = -1;

	/**
	 * SimView's Constructor method.
	 * 
	 * @param m
	 *            - the SimModel Object, the model on the structure which
	 *            contains the groups with their phases and also some parameters
	 *            that are needed for the simulator.
	 */
	public SimView(SimModel m) {
        // ImageIcon icon =
        // createImageIcon("..simulator/R&W.png","");//R&W.png", "");
        // E.Mondragon 28 Sept 2011
        List<Image> icon = new ArrayList<Image>();
        icon.add(createImageIcon("/simulator/extras/icon_16.png", "")
                .getImage());
        icon.add(createImageIcon("/simulator/extras/icon_32.png", "")
                .getImage());
        icon.add(createImageIcon("/simulator/extras/icon_256.png", "")
                .getImage());
        icon.add(createImageIcon("/simulator/extras/icon_512.png", "")
                .getImage());
        this.setIconImages(icon);
		contextAlpha = 0.005;
		model = m;
		isUSAcrossPhases = false;
		isSetCompound = false;
		isSetConfiguralCompounds = false;
		cp = this.getContentPane();
		hiddenColumns = new LinkedHashMap<TableColumn, Integer>();

		cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));

		center();
		createMenu();
		createGUI2();

		// Modified by J Gray. Dec-2011
		this.setTitle(Messages.getString("SimView.title")); //$NON-NLS-1$
		// modified by Alberto Fernandez: 19 July 2011


		this.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
	}

	/**
	 * Responds to the user when he presses a button.
	 */
	public void addButtonListeners(ActionListener event) {
		setVariablesBut.addActionListener(event);
		clearBut.addActionListener(event);
		runBut.addActionListener(event);
		dispGraphBut.addActionListener(event);
		addPhaseBut.addActionListener(event);
		removePhaseBut.addActionListener(event);
		removeGroupBut.addActionListener(event);
		addGroupBut.addActionListener(event);

	}

	/**
	 * Creates, initializes, configures and adds a values table into the frame.
	 * Columns and rows are depending on model's parameters.
	 */
	private void addCSValuesTable() {
		CSValuesTableModel = new CSValuesTableModel();

		CSValuesTable = new JTable(CSValuesTableModel) {
			@Override
			public boolean editCellAt(int row, int column, EventObject e) {
				boolean b = super.editCellAt(row, column, e);
				if (b) {
					TableCellEditor tce = getCellEditor(row, column);
					DefaultCellEditor dce = (DefaultCellEditor) tce;
					Component c = dce.getComponent();
					JTextComponent jtc = (JTextComponent) c;
					jtc.requestFocus();

					// Alberto Fernandez Oct-2011

					// This avoids to press a double enter
					/*
					 * InputMap inputMap = jtc.getInputMap(); KeyStroke key =
					 * KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
					 * inputMap.put(key, "none"); //$NON-NLS-1$
					 */

					jtc.addFocusListener(new java.awt.event.FocusAdapter() {
						@Override
						public void focusGained(java.awt.event.FocusEvent evt) {
							JTextComponent jtc = (JTextComponent) evt
									.getComponent();
							jtc.setCaretPosition(jtc.getCaretPosition());
						}

						@Override
						public void focusLost(java.awt.event.FocusEvent evt) {
							int col = CSValuesTable.getColumnCount();
							if (col > 0) {
								if (CSValuesTable.getCellEditor() != null) {
									CSValuesTable.getCellEditor()
											.stopCellEditing();
								}
							}
							// else; //take care about the other rows if you
							// need to.

						}
					});
				}
				return b;
			}
		};
		CSValuesTable.setDefaultRenderer(String.class, new GreekRenderer());
		CSValuesTableModel.setValuesTable(false);
		CSValuesTable.setCellSelectionEnabled(false);
		CSValuesTable.requestFocus();

		CSValuesScroll = new JScrollPane(CSValuesTable);
	}

	/*
	 * Add a new group to the phasesTableModel
	 */
	public void addGroup() {
		phasesTableModel.addGroup();
		updatePhasesColumnsWidth();
	}

	/**
	 * Responds to the user when he presses a menu option.
	 */
	public void addMenuListeners(ActionListener event) {
		menuItemNew.addActionListener(event);
		menuItemOpen.addActionListener(event);
		menuItemSave.addActionListener(event);
		menuItemExport.addActionListener(event);
		menuItemQuit.addActionListener(event);
		menuItemComb.addActionListener(event);
		menuItemUSAcrossPhases.addActionListener(event);
		menuItemSingleContext.addActionListener(event);
		menuItemContextAcrossPhases.addActionListener(event);
		menuItemContext.addActionListener(event);
		menuItemSetCompound.addActionListener(event);
		menuItemSetConfiguralCompounds.addActionListener(event); // Alberto
																	// Fernandez
																	// August-2011
        menuItemRPM.addActionListener(event);
		menuItemGuide.addActionListener(event);
		menuItemAbout.addActionListener(event);
		menuItemThreshold.addActionListener(event);
		menuItemCsc.addActionListener(event);
		menuItemTimestep.addActionListener(event);
		menuItemVarComb.addActionListener(event);
		menuItemTimingPerTrial.addActionListener(event);
		menuItemSerialCompounds.addActionListener(event);
		menuItemRestrictPredictions.addActionListener(event);
	}

	/*
	 * Add Omega across phases
	 */
	public void addOmegaPhases() {
		/*
		 * otherTableModel.addPhases();
		 * otherValuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		 * updateOtherValuesColumnsWidth();
		 */
		// int colCount = (phasesTableModel.getColumnCount()-1)/5;
		// ContextConfig.clearDefault();
		Iterator<Entry<TableColumn, Integer>> it = hiddenColumns.entrySet()
				.iterator();
		for (int i = 0; i < (phasesTableModel.getColumnCount() - 1) / 5
				&& it.hasNext(); i++) {
			Entry<TableColumn, Integer> column = it.next();
			int oldIndex = column.getValue();
			phasesTable.getColumnModel().addColumn(column.getKey());
			int currIndex = phasesTable.getColumnModel().getColumnCount() - 1;
			phasesTable.getColumnModel().moveColumn(currIndex, oldIndex);
		}
		hiddenColumns.clear();
	}

	/**
	 * Creates, initializes, configures and adds a values table into the frame.
	 * Columns and rows are depending on model's parameters.
	 */
	private void addOtherValuesTable() {

		otherTableModel = new OtherValuesTableModel(1);

		otherValuesTable = new JTable(otherTableModel) {
			@Override
			public boolean editCellAt(int row, int column, EventObject e) {
				boolean b = super.editCellAt(row, column, e);
				if (b) {
					TableCellEditor tce = getCellEditor(row, column);
					DefaultCellEditor dce = (DefaultCellEditor) tce;
					Component c = dce.getComponent();
					JTextComponent jtc = (JTextComponent) c;
					jtc.requestFocus();

					// Alberto Fernandez Oct-2011

					// This avoids to press a double enter
					/*
					 * InputMap inputMap = jtc.getInputMap(); KeyStroke key =
					 * KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
					 * inputMap.put(key, "none"); //$NON-NLS-1$
					 */

					jtc.addFocusListener(new java.awt.event.FocusAdapter() {
						@Override
						public void focusGained(java.awt.event.FocusEvent evt) {
							JTextComponent jtc = (JTextComponent) evt
									.getComponent();
							jtc.setCaretPosition(jtc.getCaretPosition());
						}

						@Override
						public void focusLost(java.awt.event.FocusEvent evt) {
							int col = otherValuesTable.getColumnCount();
							if (col > 0) {
								if (otherValuesTable.getCellEditor() != null) {
									otherValuesTable.getCellEditor()
											.stopCellEditing();
								}
							}
							// else; //take care about the other rows if you
							// need to.
						}
					});
				}
				return b;
			}
		};
		otherValuesTable.setDefaultRenderer(String.class, new GreekRenderer());
		otherTableModel.setInitialValuesTable();
		otherValuesTable.setCellSelectionEnabled(false);
		otherValuesTable.requestFocus();
		otherValuesTable.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				int row = otherValuesTable.rowAtPoint(p);
				String tip;
				switch (row) {
				case 0:
					tip = Messages.getString("SimView.deltaTip"); //$NON-NLS-1$
					break;
				case 1:
					tip = Messages.getString("SimView.gammaTip"); //$NON-NLS-1$
					break;
				case 2:
					tip = "Context stimulus salience."; //$NON-NLS-1$
					break;
				default:
					tip = ""; //$NON-NLS-1$
				}
				otherValuesTable.setToolTipText(tip);
			}// end MouseMoved
		}); // end MouseMotionAdapter

		otherValuesScroll = new JScrollPane(otherValuesTable);
	}

	/*
	 * Add a new phase to the phasesTableModel
	 */
	public void addPhase() {
		phasesTableModel.addPhase();
		updatePhasesColumnsWidth();
	}

	/**
	 * Creates, initializes, configures and adds a phase table into the frame.
	 * Columns and rows are depending on model's parameters.
	 */
	private void addPhaseTable() {
		phasesTableModel = new PhasesTableModel();
		phasesTable = new JTable(phasesTableModel) {

			@Override
			public boolean editCellAt(int row, int column, EventObject e) {
				boolean b = super.editCellAt(row, column, e);
                activeRow = row;
                activeCol = column;
				if (b
						&& getColumnName(column).contains(
								Messages.getString("SimView.phase"))) { //$NON-NLS-1$
					TableCellEditor tce = getCellEditor(row, column);
					DefaultCellEditor dce = (DefaultCellEditor) tce;
					Component c = dce.getComponent();
					JTextComponent jtc = (JTextComponent) c;
					jtc.setFont(new Font(TABLE_FONT_NAME, TABLE_FONT_STYLE,
							TABLE_FONT_SIZE)); // Alberto Fernandez July-2011
					jtc.requestFocus();

					// Alberto Fernandez Oct-2011
					// Removed this because it was leading to no enter being
					// passed at all when
					// forcing descendant cell editors to close on hitting enter
					// JTables are a nightmare.
					// This avoids to press a double enter
					/*
					 * InputMap inputMap = jtc.getInputMap(); KeyStroke key =
					 * KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
					 * inputMap.put(key, "none"); //$NON-NLS-1$
					 */

					jtc.addFocusListener(new java.awt.event.FocusAdapter() {
						@Override
						public void focusGained(java.awt.event.FocusEvent evt) {
                            super.focusGained(evt);
							/*JTextComponent jtc = (JTextComponent) evt
									.getComponent();
							jtc.setCaretPosition(jtc.getCaretPosition());*/
						}

						@Override
						public void focusLost(java.awt.event.FocusEvent evt) {
							int col = CSValuesTable.getColumnCount();
							if (col > 0) {
								if (CSValuesTable.getCellEditor() != null) {
									CSValuesTable.getCellEditor()
											.stopCellEditing();
								}
							}
							// else; //take care about the other rows if you
							// need to.

						}
					});
					// Treat clicks on 'same context' as wanting to change the
					// salience and act as
					// though that menu item had been clicked.
				} else if (!b
						&& getColumnName(column).contains(
								Messages.getString("SimView.context"))) {
					menuItemSingleContext.doClick();
				}
				return b;
			}

			/**
			 * Overridden to autosize columns after an edit and update the
			 * timing configurations to reflect phase strings.
			 */
			@Override
			public void editingStopped(ChangeEvent e) {
				super.editingStopped(e);
				updateTimingConfigs();
				updatePhasesColumnsWidth();

			}
		};

		phasesTable.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				int columnId = phasesTable.columnAtPoint(p);
				String column = phasesTable.getColumnName(columnId);
				String tip = Messages.getString("SimView.temporalTip"); //$NON-NLS-1$
				if (column.equals(GROUP_NAME)) {
					tip = Messages.getString("SimView.groupNameTip"); //$NON-NLS-1$
				} else if (column.contains(Messages.getString("SimView.phase"))) { //$NON-NLS-1$
					tip = Messages.getString("SimView.trialStringTip"); //$NON-NLS-1$
				} else if (column.equals(RANDOM)) {
					tip = Messages.getString("SimView.randomTip"); //$NON-NLS-1$
				} else if (column.equals(ITI)) {
					tip = Messages.getString("SimView.ITITip"); //$NON-NLS-1$
				} else if (column.equals(CONTEXT)) {
					tip = Messages.getString("SimView.contextTip"); //$NON-NLS-1$
				}
				phasesTable.setToolTipText(tip);
			}// end MouseMoved
		}); // end MouseMotionAdapter
		// Make single click start editing instead of needing double
		DefaultCellEditor singleClickEditor = new DefaultCellEditor(
				new JTextField());
		singleClickEditor.setClickCountToStart(1);
		phasesTable.setDefaultEditor(Object.class, singleClickEditor);

		phasesTableModel.setPhasesTable();
		phasesTable.setCellSelectionEnabled(false);
		phasesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		phasesTable.setFont(new Font(TABLE_FONT_NAME, TABLE_FONT_STYLE,
				TABLE_FONT_SIZE)); // Alberto Fern�ndez July-2011
		phasesTable.setRowHeight(TABLE_ROW_HEIGHT);

		phasesTable.requestFocus();

		phasesScroll = new JScrollPane(phasesTable);
		// Added a custom editor for variable/fixed onsets. J Gray Dec-2011
		phasesTable.setDefaultEditor(TimingConfiguration.class,
				new TimingEditor());
		// Added a custom editor for ITI durations. J Gray Dec-2011
		phasesTable.setDefaultEditor(ITIConfig.class, new ITIEditor());
		phasesTable.setDefaultEditor(ContextConfig.class, new ContextEditor());
		phasesTable.getColumnModel().getColumn(1).setCellEditor(new TrialStringEditor());
	}

	/*
	 * Add US across phases
	 */
	public void addUSPhases() {
		USValuesTableModel.addPhases();
		USValuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		updateUSValuesColumnsWidth();
	}

	/**
	 * Creates, initializes, configures and adds a values table into the frame.
	 * Columns and rows are depending on model's parameters.
	 */
	private void addUSValuesTable() {

		USValuesTableModel = new USValuesTableModel(1);

		USValuesTable = new JTable(USValuesTableModel) {
			@Override
			public boolean editCellAt(int row, int column, EventObject e) {
				boolean b = super.editCellAt(row, column, e);
				if (b) {
					TableCellEditor tce = getCellEditor(row, column);
					DefaultCellEditor dce = (DefaultCellEditor) tce;
					Component c = dce.getComponent();
					JTextComponent jtc = (JTextComponent) c;
					jtc.requestFocus();

					// Alberto Fernandez Oct-2011

					// This avoids to press a double enter
					/*
					 * InputMap inputMap = jtc.getInputMap(); KeyStroke key =
					 * KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
					 * inputMap.put(key, "none"); //$NON-NLS-1$
					 */

					jtc.addFocusListener(new java.awt.event.FocusAdapter() {
						@Override
						public void focusGained(java.awt.event.FocusEvent evt) {
							JTextComponent jtc = (JTextComponent) evt
									.getComponent();
							jtc.setCaretPosition(jtc.getCaretPosition());
						}

						@Override
						public void focusLost(java.awt.event.FocusEvent evt) {
							int col = USValuesTable.getColumnCount();
							if (col > 0) {
								if (USValuesTable.getCellEditor() != null) {
									USValuesTable.getCellEditor()
											.stopCellEditing();
								}
							}
							// else; //take care about the other rows if you
							// need to.
						}
					});
				}
				return b;
			}
		};
		USValuesTable.setDefaultRenderer(String.class, new GreekRenderer());
		USValuesTableModel.setInitialValuesTable();
		USValuesTable.setCellSelectionEnabled(false);
		USValuesTable.requestFocus();
		USValuesTable.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				int row = USValuesTable.rowAtPoint(p);
				String tip;
				switch (row) {
				case 0:
					tip = Messages.getString("SimView.betaPlusTip"); //$NON-NLS-1$
					break;
				case 1:
					tip = Messages.getString("SimView.betaMinusTip"); //$NON-NLS-1$
					break;
				case 2:
					tip = Messages.getString("SimView.lambdaPlusTip"); //$NON-NLS-1$
					break;
				case 3:
					tip = Messages.getString("SimView.lambdaMinusTip"); //$NON-NLS-1$
					break;
				case 4:
					tip = "Decay factor for eligibility traces."; //$NON-NLS-1$
					break;
				case 5:
					tip = "Discount factor."; //$NON-NLS-1$
					break;
				case 6:
					tip = "Response threshold."; //$NON-NLS-1$
					break;
				case 7:
					tip = "Context stimulus salience."; //$NON-NLS-1$
					break;
				default:
					tip = ""; //$NON-NLS-1$
				}
				USValuesTable.setToolTipText(tip);
			}// end MouseMoved
		}); // end MouseMotionAdapter

		USValuesScroll = new JScrollPane(USValuesTable);
	}

	/**
	 * Positions the frame into the center of the screen. It uses the
	 * Toolkit.getDefaultToolkit().getScreenSize() method to retrieve the
	 * screens actual size and from the it calculates the center,
	 */
	private void center() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - INITIAL_WIDTH) / 2;
		int y = (screenSize.height - INITIAL_HEIGHT) / 2;
		this.setLocation(x, y);
	}

	/**
	 * Clears the list of hidden context columns.
	 */
	public void clearHidden() {
		hiddenColumns.clear();
	}

	/**
	 * Clear the Area of the results.
	 */
	public void clearOutputArea() {
		outputArea.setText(""); //$NON-NLS-1$
	}

	/*
	 * Creates the bottom panel with the logo
	 */
	private JPanel createBottomPanel() {
		JPanel aboutPanel = new JPanel();
		aboutPanel.setMinimumSize(new Dimension(800, 58));

		aboutPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		aboutPanel.setBackground(Color.WHITE);
		// modified by Alberto Fernandez: 19 July 2011
		// ImageIcon icon = createImageIcon("/Extras/logo6-final.jpg", "");

		// ImageIcon icon = createImageIcon("../Extras/logo6-final.png", "");
		ImageIcon icon = createImageIcon(
				"/simulator/extras/SSCC-TD-BOTTOM-LOGO.png", ""); //$NON-NLS-1$ //$NON-NLS-2$

		JLabel label = new JLabel(icon);
		aboutPanel.add(label);

		// aboutPanel.setBorder(new SimBackgroundBorder(icon.getImage(), true));
		return aboutPanel;

	}

	/**
	 * Creates, initializes and configures the view components.. This view can
	 * not change its size
	 */
	private void createGUI2() {

		mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED));

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5, 5, 5, 5);

		// Phases panel
		JPanel phasePanel = new JPanel();
		phasePanel.setLayout(new BorderLayout());
		addPhaseTable();
		phasePanel.add(phasesScroll, BorderLayout.CENTER);

		// Add/remove buttons to add or remove phases or groups

		JPanel phasesButtonPanel = new JPanel();
		JLabel phasesLabel = new JLabel(Messages.getString("SimView.phases")); //$NON-NLS-1$

		addPhaseBut = new JButton("+");addPhaseBut.setFont(new Font("Courier", Font.BOLD, 18)); //$NON-NLS-1$ //$NON-NLS-2$

		// addPhaseBut.setMargin(new Insets(-7,10,-6,10)); // Insets(int top,
		// int left, int bottom, int right)
		// addPhaseBut = new JButton("+"); E Mondragon July 30, 2011
		addPhaseBut.setFocusPainted(false);// E.Mondragon August 1st, 2011
		addPhaseBut.updateUI();//
		addPhaseBut.setActionCommand("addPhase"); //$NON-NLS-1$

		removePhaseBut = new JButton("-");removePhaseBut.setFont(new Font("Courier", Font.BOLD, 18)); //$NON-NLS-1$ //$NON-NLS-2$

		// removePhaseBut.setMargin(new Insets(-7,10,-6,10));//E Mondragon July
		// 30, 2011
		// removePhaseBut = new JButton("-"); E Mondragon July 30, 2011
		removePhaseBut.setFocusPainted(false);// E.Mondragon August 1st, 2011
		removePhaseBut.setActionCommand("removePhase"); //$NON-NLS-1$

		phasesButtonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		phasesButtonPanel.add(phasesLabel);
		phasesButtonPanel.add(removePhaseBut);
		phasesButtonPanel.add(addPhaseBut);
		phasePanel.add(phasesButtonPanel, BorderLayout.NORTH);

		JPanel groupsButtonPanel = new JPanel();
		JLabel groupsLabel = new JLabel(Messages.getString("SimView.groups")); //$NON-NLS-1$

		addGroupBut = new JButton("+");addGroupBut.setFont(new Font("Courier", Font.BOLD, 18)); //$NON-NLS-1$ //$NON-NLS-2$
		// addGroupBut.setMargin(new Insets(-7,5,-7,5));//E Mondragon July 30,
		// 2011
		// addGroupBut = new JButton("+"); E Mondragon July 30, 2011
		addGroupBut.setFocusPainted(false);// E.Mondragon August 1st, 2011
		addGroupBut.setActionCommand("addGroup"); //$NON-NLS-1$

		removeGroupBut = new JButton("-");removeGroupBut.setFont(new Font("Courier", Font.BOLD, 18)); //$NON-NLS-1$ //$NON-NLS-2$
		// removeGroupBut.setMargin(new Insets(-7,5,-7,5));//E Mondragon July
		// 30, 2011
		// removeGroupBut = new JButton("-"); E Mondragon July 30, 2011
		removeGroupBut.setFocusPainted(false);// E.Mondragon August 1st, 2011
		removeGroupBut.setActionCommand("removeGroup"); //$NON-NLS-1$

		// A. Fernandez modification E.Mondragon 10/10/2011

		String OS = System.getProperty("os.name"); //$NON-NLS-1$
		System.out.println(OS);
		if (OS.toUpperCase().contains("WINDOWS")) { //$NON-NLS-1$
			System.out.println("es windows"); //$NON-NLS-1$
			addPhaseBut.setMargin(new Insets(-7, 10, -6, 10)); // Insets(int
																// top, int
																// left, int
																// bottom, int
																// right)
			removePhaseBut.setMargin(new Insets(-7, 10, -6, 10));// E Mondragon
																	// July 30,
																	// 2011
			addGroupBut.setMargin(new Insets(-7, 9, -6, 9));// E Mondragon July
															// 30, 2011
			removeGroupBut.setMargin(new Insets(-7, 9, -6, 9));// E Mondragon
																// July 30, 2011
		}

		// end

		groupsButtonPanel.setLayout(new BoxLayout(groupsButtonPanel,
				BoxLayout.Y_AXIS));
		groupsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		addGroupBut.setAlignmentX(Component.CENTER_ALIGNMENT);
		removeGroupBut.setAlignmentX(Component.CENTER_ALIGNMENT);
		groupsButtonPanel.add(groupsLabel);
		groupsButtonPanel.add(removeGroupBut);
		// groupsButtonPanel.add(addGroupBut);
		JPanel jp = new JPanel(); // Alberto Fernandez Oct-2011
		jp.add(addGroupBut);
		groupsButtonPanel.add(jp);

		phasePanel.add(groupsButtonPanel, BorderLayout.WEST);

		// Variables panel
		JPanel variablePanel = new JPanel();
		variablePanel.setLayout(new BorderLayout());

		setVariablesBut = new JButton(Messages.getString("SimView.setParams")); //$NON-NLS-1$
		setVariablesBut.setActionCommand("setVariables"); //$NON-NLS-1$
		JPanel varButPanel = new JPanel();
		varButPanel.add(setVariablesBut);
		addCSValuesTable();
		addUSValuesTable();
		addOtherValuesTable();
		runBut = new JButton(Messages.getString("SimView.run")); //$NON-NLS-1$
		runBut.setActionCommand("run"); //$NON-NLS-1$
		JPanel runButPanel = new JPanel();
		runButPanel.add(runBut);

		JPanel valuesPanel = new JPanel();
		valuesPanel.setLayout(new GridLayout(3, 1));
		valuesPanel.add(CSValuesScroll);
		valuesPanel.add(USValuesScroll);
		valuesPanel.add(otherValuesScroll);

		variablePanel.add(varButPanel, BorderLayout.NORTH);
		variablePanel.add(valuesPanel, BorderLayout.CENTER);

		variablePanel.add(runButPanel, BorderLayout.SOUTH);

		// Result panel
		JPanel resultPanel = new JPanel();
		resultPanel.setLayout(new BorderLayout());

		clearBut = new JButton(Messages.getString("SimView.clear")); //$NON-NLS-1$
		clearBut.setActionCommand("clearAll"); //$NON-NLS-1$
		JPanel clearButPanel = new JPanel();
		clearButPanel.add(clearBut);
		outputArea = new JTextArea(10, 22);
		outputArea.setEditable(false);
		outputArea.setFont(new Font("Serif", Font.PLAIN, 16)); //$NON-NLS-1$
		outputScroll = new JScrollPane(outputArea);
		dispGraphBut = new JButton(Messages.getString("SimView.graphs")); //$NON-NLS-1$
		dispGraphBut.setActionCommand("dispGraph"); //$NON-NLS-1$
		JPanel dispGraphButPanel = new JPanel();
		dispGraphButPanel.add(dispGraphBut);

		resultPanel.add(clearButPanel, BorderLayout.NORTH);
		resultPanel.add(outputScroll, BorderLayout.CENTER);
		resultPanel.add(dispGraphButPanel, BorderLayout.SOUTH);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weighty = 0.4;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(phasePanel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.weighty = 0.6;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.BOTH;
		mainPanel.add(variablePanel, c.clone());

		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.weighty = 0.6;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.BOTH;

		mainPanel.add(resultPanel, c.clone());

		cp.add(mainPanel);
		cp.add(createBottomPanel());

	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	private ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = this.getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println(Messages.getString("SimView.404Error") + path); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Creates and initializes every element that is necessary for the menu
	 * component. Sets mnemonics on them and actionCommands for the easier
	 * process during events.
	 */
	private void createMenu() {
		menuBar = new JMenuBar();

		menuFile = new JMenu(Messages.getString("SimView.file")); //$NON-NLS-1$
		menuFile.setMnemonic(KeyEvent.VK_F);

		menuItemNew = new JMenuItem(
				Messages.getString("SimView.new"), KeyEvent.VK_N); //$NON-NLS-1$
		menuItemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.ALT_MASK));
		menuItemNew.setActionCommand("New"); //$NON-NLS-1$
		menuFile.add(menuItemNew);

		menuItemOpen = new JMenuItem(
				Messages.getString("SimView.open"), KeyEvent.VK_O); //$NON-NLS-1$
		menuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.ALT_MASK));
		menuItemOpen.setActionCommand("Open"); //$NON-NLS-1$
		menuFile.add(menuItemOpen);

		menuItemSave = new JMenuItem(
				Messages.getString("SimView.save"), KeyEvent.VK_S); //$NON-NLS-1$
		menuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.ALT_MASK));
		menuItemSave.setActionCommand("Save"); //$NON-NLS-1$
		menuFile.add(menuItemSave);

		menuItemExport = new JMenuItem(
				Messages.getString("SimView.export"), KeyEvent.VK_E); //$NON-NLS-1$
		menuItemExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				ActionEvent.ALT_MASK));
		menuItemExport.setActionCommand("Export"); //$NON-NLS-1$
		menuFile.add(menuItemExport);

		menuFile.addSeparator();

		menuItemQuit = new JMenuItem(
				Messages.getString("SimView.quit"), KeyEvent.VK_Q); //$NON-NLS-1$
		menuItemQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				ActionEvent.ALT_MASK));
		menuItemQuit.setActionCommand("Quit"); //$NON-NLS-1$
		menuFile.add(menuItemQuit);

		menuBar.add(menuFile);

		menuDesignSettings = new JMenu(
				Messages.getString("SimView.designSettings")); //$NON-NLS-1$
		menuProcSettings = new JMenu(
				Messages.getString("SimView.procedureSettings")); //$NON-NLS-1$
		menuDesignSettings.setMnemonic(KeyEvent.VK_S);
		menuProcSettings.setMnemonic(KeyEvent.VK_P);

		menuItemComb = new JMenuItem(
				Messages.getString("SimView.numCombinations"), KeyEvent.VK_R); //$NON-NLS-1$
		menuItemComb.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				ActionEvent.ALT_MASK));
		menuItemComb.setActionCommand("Combinations"); //$NON-NLS-1$
		menuProcSettings.add(menuItemComb);
		menuItemVarComb = new JMenuItem(
				Messages.getString("SimView.numVariableCombinations"), KeyEvent.VK_V); //$NON-NLS-1$
		menuItemVarComb.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
				ActionEvent.ALT_MASK));
		menuItemVarComb.setActionCommand("VarDistCombinations"); //$NON-NLS-1$
		menuProcSettings.add(menuItemVarComb);

		menuItemTimingPerTrial = new JCheckBoxMenuItem(
				Messages.getString("SimView.timingPerTrial"), false); //$NON-NLS-1$
		// menuItemTimingPerPhase.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
		// ActionEvent.ALT_MASK));
		menuItemTimingPerTrial.setActionCommand("timingPerTrial"); //$NON-NLS-1$
		menuProcSettings.add(menuItemTimingPerTrial);

		zeroTraces = new JCheckBoxMenuItem(
				Messages.getString("SimView.zeroTrace"), false); //$NON-NLS-1$
		zeroTraces.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				ActionEvent.ALT_MASK));
		zeroTraces.setActionCommand("zeroTraces"); //$NON-NLS-1$
		menuProcSettings.add(zeroTraces);


		menuItemRestrictPredictions = new JCheckBoxMenuItem(
				Messages.getString("SimView.restrictPredictions"), true); //$NON-NLS-1$
		menuItemRestrictPredictions.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_0, ActionEvent.ALT_MASK));
		menuItemRestrictPredictions.setActionCommand("restrictPredictions"); //$NON-NLS-1$
		menuProcSettings.add(menuItemRestrictPredictions);

		menuItemTimestep = new JMenuItem(
				Messages.getString("SimView.timestep"), KeyEvent.VK_T); //$NON-NLS-1$
		((JMenuItem) menuItemTimestep).setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_T, ActionEvent.ALT_MASK));
		menuItemTimestep.setActionCommand("timestep"); //$NON-NLS-1$
		menuProcSettings.add(menuItemTimestep);

		menuItemThreshold = new JCheckBoxMenuItem(
				Messages.getString("SimView.responseThreshold"), true); //$NON-NLS-1$
		menuItemThreshold.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				ActionEvent.ALT_MASK));
		menuItemThreshold.setActionCommand("threshold"); //$NON-NLS-1$
		menuProcSettings.add(menuItemThreshold);

        menuItemRPM = new JMenuItem(
                Messages.getString("SimView.rpm")); //$NON-NLS-1$
        menuItemRPM.setActionCommand("rpm"); //$NON-NLS-1$
        menuProcSettings.add(menuItemRPM);

		menuItemUSAcrossPhases = new JCheckBoxMenuItem(
				Messages.getString("SimView.usPerPhase"), false); //$NON-NLS-1$
		menuItemUSAcrossPhases.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_U, ActionEvent.ALT_MASK));
		menuItemUSAcrossPhases.setActionCommand("SetUSAcrossPhases"); //$NON-NLS-1$
		menuDesignSettings.add(menuItemUSAcrossPhases);

		menuTrace = new JMenu(Messages.getString("SimView.traceMenu"));
		ButtonGroup trace = new ButtonGroup();
		menuItemAccumTrace = new JRadioButtonMenuItem(
				Messages.getString("SimView.accumTrace"), false);

		menuTrace.add(menuItemAccumTrace);
		menuItemBoundedTrace = new JRadioButtonMenuItem(
				Messages.getString("SimView.boundedTrace"), false);
		menuTrace.add(menuItemBoundedTrace);
		menuItemReplaceTrace = new JRadioButtonMenuItem(
				Messages.getString("SimView.replacingTrace"), true);
		menuTrace.add(menuItemReplaceTrace);
		menuProcSettings.add(menuTrace);
		trace.add(menuItemAccumTrace);
		menuItemAccumTrace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
				ActionEvent.CTRL_MASK));
		trace.add(menuItemBoundedTrace);
		menuItemBoundedTrace.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_2, ActionEvent.CTRL_MASK));
		trace.add(menuItemReplaceTrace);
		menuItemReplaceTrace.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_3, ActionEvent.CTRL_MASK));

		menuContext = new JMenu(Messages.getString("SimView.contextSim")); //$NON-NLS-1$
		menuDesignSettings.add(menuContext);
		ButtonGroup contexts = new ButtonGroup();
		menuItemContext = new JRadioButtonMenuItem(
				Messages.getString("SimView.noContext"), true); //$NON-NLS-1$
		menuItemContext.setActionCommand("SetContext"); //$NON-NLS-1$
		menuItemContext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
				ActionEvent.ALT_MASK));
		contexts.add(menuItemContext);
		menuContext.add(menuItemContext);
		menuItemSingleContext = new JRadioButtonMenuItem(
				Messages.getString("SimView.sameContext"), false); //$NON-NLS-1$
		menuItemSingleContext.setActionCommand("SingleContext"); //$NON-NLS-1$
		menuItemSingleContext.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_2, ActionEvent.ALT_MASK));
		contexts.add(menuItemSingleContext);
		menuItemContextAcrossPhases = new JRadioButtonMenuItem(
				Messages.getString("SimView.diffContext"), false); //$NON-NLS-1$
		menuItemContextAcrossPhases.setActionCommand("SetContextAcrossPhases"); //$NON-NLS-1$
		menuItemContextAcrossPhases.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_3, ActionEvent.ALT_MASK));
		contexts.add(menuItemContextAcrossPhases);
		menuContext.add(menuItemContextAcrossPhases);
		menuContext.add(menuItemSingleContext);

		menuItemCsc = new JCheckBoxMenuItem(
				Messages.getString("SimView.cscMode"), true); //$NON-NLS-1$
		menuItemCsc.setActionCommand("SetCsc"); //$NON-NLS-1$
		// menuSettings.add(menuItemCsc);

		menuItemSetCompound = new JCheckBoxMenuItem(
				Messages.getString("SimView.compounds"), false); //$NON-NLS-1$
		menuItemSetCompound.setActionCommand("SetCompound"); //$NON-NLS-1$
		menuItemSetCompound.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, ActionEvent.ALT_MASK));
		menuDesignSettings.add(menuItemSetCompound);

		menuItemSerialCompounds = new JCheckBoxMenuItem(
				Messages.getString("SimView.serialCompounds"), false); //$NON-NLS-1$
		menuItemSerialCompounds.setActionCommand("SetSerialCompound"); //$NON-NLS-1$
		// menuItemSerialCompounds.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
		// ActionEvent.ALT_MASK));
		menuDesignSettings.add(menuItemSerialCompounds);

		// Added by Alberto Fernandez August-2011
		// Option Configural Compounds

		menuItemSetConfiguralCompounds = new JCheckBoxMenuItem(
				Messages.getString("SimView.configurals"), false); //$NON-NLS-1$
		menuItemSetConfiguralCompounds
				.setActionCommand("SetConfiguralCompounds"); //$NON-NLS-1$
		menuItemSetConfiguralCompounds.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.ALT_MASK));
		menuDesignSettings.add(menuItemSetConfiguralCompounds);

		menuMeans = new JMenu(Messages.getString("SimView.meanType")); //$NON-NLS-1$
		menuProcSettings.add(menuMeans);

		menuDistributions = new JMenu(Messages.getString("SimView.distType")); //$NON-NLS-1$
		menuProcSettings.add(menuDistributions);

		ButtonGroup distributions = new ButtonGroup();
		menuItemExp = new JRadioButtonMenuItem(
				Messages.getString("SimView.exponential"), true); //$NON-NLS-1$
		menuItemExp.setActionCommand("exp"); //$NON-NLS-1$
		menuItemExp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				ActionEvent.CTRL_MASK));
		distributions.add(menuItemExp);
		menuItemUnif = new JRadioButtonMenuItem(
				Messages.getString("SimView.uniform"), true); //$NON-NLS-1$
		menuItemUnif.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
				ActionEvent.CTRL_MASK));
		distributions.add(menuItemUnif);
		menuDistributions.add(menuItemExp);
		menuDistributions.add(menuItemUnif);

		ButtonGroup means = new ButtonGroup();
		menuItemGeometric = new JRadioButtonMenuItem(
				Messages.getString("SimView.geometric"), false); //$NON-NLS-1$
		menuItemGeometric.setActionCommand("geo"); //$NON-NLS-1$
		menuItemGeometric.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
				ActionEvent.CTRL_MASK));
		means.add(menuItemGeometric);
		menuItemArithmetic = new JRadioButtonMenuItem(
				Messages.getString("SimView.arithmetic"), true); //$NON-NLS-1$
		menuItemArithmetic.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.CTRL_MASK));
		means.add(menuItemArithmetic);
		menuMeans.add(menuItemGeometric);
		menuMeans.add(menuItemArithmetic);

		menuBar.add(menuDesignSettings);
		menuBar.add(menuProcSettings);

		menuHelp = new JMenu(Messages.getString("SimView.help")); //$NON-NLS-1$
		menuHelp.setMnemonic(KeyEvent.VK_H);
		menuBar.add(menuHelp);

		menuItemGuide = new JMenuItem(
				Messages.getString("SimView.guide"), KeyEvent.VK_G); //$NON-NLS-1$
		menuItemGuide.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
				ActionEvent.ALT_MASK));
		menuItemGuide.setActionCommand("Guide"); //$NON-NLS-1$
		menuHelp.add(menuItemGuide);

		menuItemAbout = new JMenuItem(
				Messages.getString("SimView.about"), KeyEvent.VK_A); //$NON-NLS-1$
		menuItemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.ALT_MASK));
		menuItemAbout.setActionCommand("About"); //$NON-NLS-1$
		menuHelp.add(menuItemAbout);

		this.setJMenuBar(menuBar);
	}

	public double getContextAlpha() {
		return contextAlpha;
	}

	/**
	 * Returns the values' table.
	 * 
	 * @return the values' table.
	 */
	public JTable getCSValuesTable() {
		return CSValuesTable;
	}

	/**
	 * Returns the values' table model. This helps to get the table's contents
	 * or set some new ones.
	 * 
	 * @return the values' table model.
	 */
	public CSValuesTableModel getCSValuesTableModel() {
		return CSValuesTableModel;
	}

	/**
	 * 
	 * @return An integer indicating the type of distribution form to use.
	 */

	public int getDistributionType() {
		int type = Distributions.EXPONENTIAL;
		if (menuItemUnif.isSelected()) {
			type = Distributions.UNIFORM;
		}
		return type;
	}

	/**
	 * It displays a JOptionPane.showInputDialog on top of the view's frame
	 * asking a double. The request message and current value are provided as
	 * arguments.
	 * 
	 * @param s
	 *            the message that is been displayed to the screen.
	 * @param cur
	 *            the current value that the variable, that will be change, has.
	 * @return the new value of the variable.
	 */
	public double getDoubleInput(String s, String cur) {
		String input = JOptionPane.showInputDialog(s, cur);
		if (input == null)
			return -1;
		else
			return Double.parseDouble(input);
	}

	/**
	 * It displays a JOptionPane.showInputDialog on top of the view's frame
	 * asking an integer. The request message and current value are provided as
	 * arguments.
	 * 
	 * @param s
	 *            the message that is been displayed to the screen.
	 * @param cur
	 *            the current value that the variable, that will be change, has.
	 * @return the new value of the variable.
	 */
	public int getIntInput(String s, String cur) {
		String input = JOptionPane.showInputDialog(s, cur);
		if (input == null)
			return -1;
		else
			return Integer.parseInt(input);
	}

	/**
	 * 
	 * @return true is the geometric mean option is selected.
	 */

	public boolean getMeanType() {
		return menuItemGeometric.isSelected();
	}

	/**
	 * @return the table holding gamma & delta values.
	 */
	public JTable getOtherValuesTable() {
		return otherValuesTable;
	}

	public OtherValuesTableModel getOtherValuesTableModel() {
		return otherTableModel;
	}

	/**
	 * Returns the phases' table.
	 * 
	 * @return the phases' table.
	 */
	public JTable getPhasesTable() {
		return phasesTable;
	}

	/**
	 * Returns the phases' table model. This helps to get the tables contents or
	 * set some new ones.
	 * 
	 * @return the phases' table model.
	 */
	public PhasesTableModel getPhasesTableModel() {
		return phasesTableModel;
	}

	/**
	 * 
	 * @return true if the bounded accumulating trace type is selected.
	 */

	public Trace getTraceType() {
		if (menuItemBoundedTrace.isSelected()) {
			return Trace.BOUNDED;
		} else if (menuItemAccumTrace.isSelected()) {
			return Trace.ACCUMULATING;
		}
		return Trace.REPLACING;
	}

	/**
	 * Returns the values' table.
	 * 
	 * @return the values' table.
	 */
	public JTable getUSValuesTable() {
		return USValuesTable;
	}

	/**
	 * Returns the values' table model. This helps to get the table's contents
	 * or set some new ones.
	 * 
	 * @return the values' table model.
	 */
	public USValuesTableModel getUSValuesTableModel() {
		return USValuesTableModel;
	}

	/**
	 * 
	 * @return true if traces should be forced to zero between trials.
	 */

	public boolean getZeroTraces() {
		return zeroTraces.isSelected();
	}

	/**
	 * @return true if the exponential menu item is selected.
	 */
	public boolean isExponential() {
		return menuItemExp.isSelected();
	}

	/**
	 * 
	 * @return a boolean indicating whether to use a different context stimulus
	 *         per phase
	 */

	public boolean isOmegaAcrossPhases() {
		return menuItemContextAcrossPhases.isSelected();
	}

	/**
	 * 
	 * @return true is predictions are restricted to >= 0.
	 */

	public boolean isRestrictPredictions() {
		return menuItemRestrictPredictions.isSelected();
	}

	/**
	 * Return if the compounds values are going to be shown
	 * 
	 * @return boolean
	 */
	public boolean isSetCompound() {
		return isSetCompound;
	}

	/**
	 * Return if the compounds values are going to be used
	 * 
	 * @return boolean
	 */
	public boolean isSetConfiguralCompounds() {
		return isSetConfiguralCompounds;
	}

	/**
	 * @return a boolean indicating whether the user has selected to show
	 *         response stats.
	 */

	public boolean isSetResponse() {
		return menuItemThreshold.isSelected();
	}

	/**
	 * Return if the cues of lambdas and betas are updatable across phases
	 * 
	 * @return boolean
	 */
	public boolean isUSAcrossPhases() {
		return isUSAcrossPhases;
	}

	/**
	 * 
	 * @return a boolean indicating whether to use the context stimulus.
	 */
	public boolean isUseContext() {
		return !menuItemContext.isSelected();
	}

	/**
	 * 
	 * @return true if serial compounds should be considered.
	 */

	public boolean isUseSerialCompounds() {
		return menuItemSerialCompounds.isSelected();
	}

	/*
	 * Order a vector of cues by phases
	 * 
	 * @param vec Vector of records with cues to order
	 * 
	 * @param nlambdabeta number of lambda and beta cues in vec
	 * 
	 * @return Vector vector of cues ordered by phases
	 */
	private Vector orderByPhase(Vector vec, int nlambdabeta) {
		Vector v = new Vector();

		// alphas
		for (int j = 0; j < (vec.size() - nlambdabeta); j++) {
			v.addElement(vec.get(j));
		}
		// lambdas and betas per phase
		for (int i = 1; i <= model.getPhaseNo(); i++) {
			for (int k = (vec.size() - nlambdabeta); k < vec.size(); k++) { // finding
																			// lambda+,
																			// lambda-,
																			// beta+,
																			// beta-
				String cuename = (String) ((Object[]) vec.get(k))[0];
				if (("" + cuename.charAt(cuename.length() - 1)).equals("" + i))v.addElement(vec.get(k)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return v;
	}

	/*
	 * Remove the last group of the phasesTableModel
	 */
	public void removeGroup() {
		phasesTableModel.removeGroup();
		updatePhasesColumnsWidth();
	}

	/*
	 * Remove Omega across phases
	 */
	public void removeOmegaPhases() {
		// otherTableModel.removePhases(1);
		// otherValuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		ContextConfig.clearDefault();
		hiddenColumns.clear();
		for (TableColumn column : Collections.list(phasesTable.getColumnModel()
				.getColumns())) {
			if (column.getHeaderValue().equals(CONTEXT)) {
				hiddenColumns.put(column, column.getModelIndex());
			}
		}
		for (TableColumn column : Collections.list(phasesTable.getColumnModel()
				.getColumns())) {
			if (column.getHeaderValue().equals(CONTEXT)) {
				phasesTable.getColumnModel().removeColumn(column);
			}
		}
	}

	/*
	 * Remove the last phase of the phasesTableModel
	 */
	public void removePhase() {
		phasesTableModel.removePhase();
		updatePhasesColumnsWidth();
	}

	/*
	 * Remove US across phases
	 */
	public void removeUSPhases() {
		USValuesTableModel.removePhases(1);
		USValuesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	/**
	 * Reset the menus, tables and output to starting state.
	 */

	public void reset() {
		// Context off
		menuItemContext.setSelected(true);
		// Phases table reeanbled
		getPhasesTable().setEnabled(true);
		// Export off
		setStatusComponent(false, "Export"); //$NON-NLS-1$
		// Save off
		setStatusComponent(false, "Save"); //$NON-NLS-1$
		// Set variables button on
		setStatusComponent(true, "setVariables"); //$NON-NLS-1$
		// Run button off
		setStatusComponent(false, "run"); //$NON-NLS-1$
		// Figures button off
		setStatusComponent(false, "dispGraph"); //$NON-NLS-1$
		// Clear output
		clearOutputArea();
		// US per phase off
		setIsUSAcrossPhases(false);
		setStatusComponent(false, "SetUSAcrossPhases"); //$NON-NLS-1$
		// Compounds off
		setStatusComponent(false, "SetCompound"); //$NON-NLS-1$
		// Added by Alberto Fernandez August-2011
		setIsSetCompound(false);
		// Configurals off
		setStatusComponent(false, "SetConfiguralCompounds"); //$NON-NLS-1$
		setIsSetConfiguralCompounds(false);
		// Exponential distribution
		menuItemExp.setSelected(true);
		// Arithmetic mean
		menuItemArithmetic.setSelected(true);
		// Decision rule sim off
		menuItemThreshold.setSelected(true);
		// Bounded traces on
		setTraceType(Trace.REPLACING);
		otherTableModel.setInitialValuesTable();
		// Zero'd traces not on.
		zeroTraces.setSelected(false);
		// Serials off
		menuItemSerialCompounds.setSelected(false);
		// Timings by trial off
		menuItemTimingPerTrial.setSelected(false);
		menuItemRestrictPredictions.setSelected(true);
		getPhasesTable().getColumnModel().getColumn(1).setCellEditor(new TrialStringEditor());
	}

	public void setContextAlpha(double contextAlpha) {
		this.contextAlpha = contextAlpha;
	}

	/**
	 * Set if the compounds values are going to be shown
	 * 
	 * @param boolean
	 */
	public void setIsSetCompound(boolean b) {
		isSetCompound = b;
		menuItemSetCompound.setSelected(b);
	}

	/******************************************************************************************************************/
	/******************************************************************************************************************/

	/********************************************************/
	/********************************************************/

	/**
	 * Set if the configural compounds values are going to be used
	 * 
	 * @param boolean
	 */
	public void setIsSetConfiguralCompounds(boolean b) {
		isSetConfiguralCompounds = b;
	}

	/********************************************************/
	/********************************************************/

	/**
	 * Set if the cues of lambdas and betas are updatable across phases
	 * 
	 * @param boolean
	 */
	public void setIsUSAcrossPhases(boolean b) {
		isUSAcrossPhases = b;
	}

	/********************************************************/
	/********************************************************/

	/**
	 * Bulk set the alpha value for all contexts in a table and make all
	 * contexts phi.
	 * 
	 * @param alpha
	 */

	public void setOmegaSalience(double alpha) {
		ContextConfig.clearDefault();
		contextAlpha = alpha;
		for (TableColumn column : Collections.list(phasesTable.getColumnModel()
				.getColumns())) {
			if (column.getHeaderValue().equals(CONTEXT)) {
				int col = column.getModelIndex();
				for (int i = 0; i < phasesTableModel.getRowCount(); i++) {
					((ContextConfig) phasesTableModel.getValueAt(i, col))
							.setAlpha(alpha);
					((ContextConfig) phasesTableModel.getValueAt(i, col))
							.setContext(Context.PHI);
					phasesTableModel.fireTableCellUpdated(i, col);
				}
			}
		}
	}

	/**
	 * Sets the output for the JTextArea object. Adds the String that is been
	 * passed from the application and then it displays it.
	 * 
	 * @param msg
	 */
	public void setOutput(String msg) {
		outputArea.setText(msg);
	}

	public void setRestrictPredictions(boolean on) {
		menuItemRestrictPredictions.setSelected(on);
	}

	/**
	 * Sets the status of a component, it could be a button or a menu item. This
	 * helps the smooth procedure of the application. It stops the user to
	 * choose an inappropriate action.
	 * 
	 * @param mode
	 *            set the component accordingly to this boolean value.
	 * @param b
	 *            the component that needs to be change. The string contains the
	 *            actionCommand.
	 */
	public void setStatusComponent(boolean mode, String b) {
		if (b.equals(setVariablesBut.getActionCommand()))
			setVariablesBut.setEnabled(mode);
		if (b.equals(clearBut.getActionCommand()))
			clearBut.setEnabled(mode);
		if (b.equals(runBut.getActionCommand()))
			runBut.setEnabled(mode);
		if (b.equals(dispGraphBut.getActionCommand()))
			dispGraphBut.setEnabled(mode);
		if (b.equals(menuItemSave.getActionCommand()))
			menuItemSave.setEnabled(mode);
		if (b.equals(menuItemExport.getActionCommand()))
			menuItemExport.setEnabled(mode);
		if (b.equals(menuItemUSAcrossPhases.getActionCommand()))
			menuItemUSAcrossPhases.setState(mode);
		if (b.equals(menuItemSetCompound.getActionCommand()))
			menuItemSetCompound.setState(mode);
		if (b.equals(menuItemSetConfiguralCompounds.getActionCommand()))
			menuItemSetConfiguralCompounds.setState(mode);
		if (b.equals(menuItemContext.getActionCommand()))
			menuItemContext.setSelected(mode);
		if (b.equals(menuItemContextAcrossPhases.getActionCommand()))
			menuItemContextAcrossPhases.setSelected(mode);
		if (b.equals(menuItemSingleContext.getActionCommand()))
			menuItemSingleContext.setSelected(mode);
		if (b.equals(menuItemExp.getActionCommand()))
			menuItemExp.setSelected(mode);
		if (b.equals(menuItemGeometric.getActionCommand()))
			menuItemGeometric.setSelected(mode);
	}

	/**
	 * Enable or disable the timing per trial option.
	 * 
	 * @param on
	 */

	public void setTimingPerTrial(boolean on) {
		menuItemTimingPerTrial.setSelected(on);
	}

	/**
	 * 
	 * @param on
	 *            set to true to use bounded accumulating trace type
	 */

	public void setTraceType(Trace trace) {
		switch (trace) {
		case BOUNDED:
			menuItemBoundedTrace.setSelected(true);
			break;
		case ACCUMULATING:
			menuItemAccumTrace.setSelected(true);
			break;
		default:
			menuItemReplaceTrace.setSelected(true);
		}
	}

	/**
	 * @param lock
	 *            true to lock the UI
	 */
	public void setUILocked(boolean lock) {
		menuBar.setEnabled(!lock);
		addGroupBut.setEnabled(!lock);
		addGroupBut.setEnabled(!lock);
		setVariablesBut.setEnabled(!lock);
		clearBut.setEnabled(!lock);
		runBut.setEnabled(!lock);
		dispGraphBut.setEnabled(!lock);
		addPhaseBut.setEnabled(!lock);
		removePhaseBut.setEnabled(!lock);
	}

	/**
	 * 
	 * @param on
	 *            set to true to use serial compounds.
	 */

	public void setUseSerialCompounds(boolean on) {
		menuItemSerialCompounds.setSelected(on);
	}

	/**
	 * 
	 * @param on
	 *            set to true to force traces to zero between trials.
	 */

	public void setZeroTraces(boolean on) {
		zeroTraces.setSelected(on);
	}

	/**
	 * showAbout displays information dialogs directly to the user.
	 * 
	 * @param message
	 *            - the String to be displayed.
	 */
	public void showAbout(String message) {
		JOptionPane
				.showMessageDialog(
						this,
						message,
						Messages.getString("SimView.aboutTitle"), JOptionPane.PLAIN_MESSAGE); //NO_OPTION //$NON-NLS-1$
	}



	/**
	 * showAbout displays information dialogs directly to the user.
	 * 
	 * @param message
	 *            - the String to be displayed.
	 */
	public void showAboutLogo(String path) {
		// Modified by E Mondragon July 29, 2011
		JFrame.setDefaultLookAndFeelDecorated(false);

		JFrame about = new JFrame();

		JPanel aboutPanel = new JPanel();
		aboutPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		aboutPanel.setBackground(Color.WHITE);

		// modified by E.Mondragon. July 29, 2011
		// ImageIcon icon = createImageIcon(path, "About");
		ImageIcon icon = createImageIcon(
				"/simulator/extras/SSCC-TD-Help-About.png", ""); //$NON-NLS-1$ //$NON-NLS-2$

		aboutPanel.setBorder(new SimBackgroundBorder(icon.getImage(), true));
		about.getContentPane().add(aboutPanel);
		about.pack();
		about.setLocation(200, 200);
		about.setSize(520, 325);
		about.setVisible(true);
		about.setTitle(Messages.getString("SimView.title"));//E.Mondragon 30 Sept 2011 //$NON-NLS-1$
		ImageIcon icon2 = createImageIcon("/simulator/extras/SSCC-TD.png", "");//R&W.png", ""); E.Mondragon 30 Sept 2011 //$NON-NLS-1$ //$NON-NLS-2$
		about.setIconImage(icon2.getImage());// E.Mondragon 30 Sept 2011

	}

	/**
	 * showError displays error message dialogs directly to the user.
	 * 
	 * @param errMessage
	 *            - the String to be displayed.
	 */
	public void showError(String errMessage) {
		JOptionPane
				.showMessageDialog(
						this,
						errMessage,
						Messages.getString("SimView.errorTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
	}

	/**
	 * 
	 * @return true is timings should be set per trial
	 */

	public boolean timingPerTrial() {
		return menuItemTimingPerTrial.isSelected();
	}

	/**
	 * Switch the omega/context variable on or off.
	 * 
	 * @param on
	 */

	public void toggleContext(boolean on) {
		if (on /* && getIsOmegaAcrossPhases() */) {
			addOmegaPhases();
		} else {
			removeOmegaPhases();
		}
	}

	public void updateModel(SimModel m) {
		model = m;
	}

	/**
	 * Set the width for the cells of the TableModel
	 */
	public void updateOtherValuesColumnsWidth() {
		otherValuesTable.getColumnModel().getColumn(0).setPreferredWidth(70);
		for (int i = 1; i < otherValuesTable.getColumnCount(); i++) {
			otherValuesTable.getColumnModel().getColumn(i)
					.setPreferredWidth(70);
		}
	}

	/**
	 * Set the width for the cells of the TableModel
	 */
	public void updatePhasesColumnsWidth() {
		for (int i = 0; i < phasesTable.getColumnCount(); i++) {
			final TableColumn tableColumn = phasesTable.getColumnModel()
					.getColumn(i);
			final TableCellRenderer headRend = phasesTable.getTableHeader()
					.getDefaultRenderer();
			tableColumn.setPreferredWidth(headRend
					.getTableCellRendererComponent(phasesTable,
							tableColumn.getHeaderValue(), false, false, -1, i)
					.getPreferredSize().width + 10);
			for (int j = 0; j < phasesTable.getRowCount(); j++) {
				int width = phasesTable
						.getCellRenderer(j, i)
						.getTableCellRendererComponent(phasesTable,
								phasesTable.getValueAt(j, i), false, false, j,
								i).getPreferredSize().width;
				tableColumn.setPreferredWidth(Math.max(width + 10,
						tableColumn.getPreferredWidth()));
			}

		}
	}

	/**
	 * Update all the timing configurations in the table to match their
	 * respective phase strings.
	 */

	public void updateTimingConfigs() {
		for (int i = 0; i < phasesTable.getColumnCount(); i++) {
			final TableColumn tableColumn = phasesTable.getColumnModel()
					.getColumn(i);
			if (((String) tableColumn.getHeaderValue()).equals(TIMING)) {
				for (int j = 0; j < phasesTable.getRowCount(); j++) {
					int offset = 3;
					// If we aren't simming context, we need to go back one less
					// column
					if (!phasesTable.getColumnName(i - offset).contains(
							Messages.getString("SimView.phase"))) { //$NON-NLS-1$
						offset--;
					}
					String phaseString = (String) phasesTable.getValueAt(j, i
							- offset);
					List<CS> stimuli = new ArrayList<CS>();
					phaseString = phaseString == null ? "" : phaseString; //$NON-NLS-1$
					stimuli.addAll(SimPhase.stringToCSList(phaseString));

					TimingConfiguration timings = ((TimingConfiguration) phasesTable
							.getValueAt(j, i));
					timings.setStimuli(stimuli);
					if (!phaseString.contains("+") && !phaseString.isEmpty()) {
						timings.setReinforced(false);
					} else {
						timings.setReinforced(true);
					}
				}
			}

		}
	}

	/**
	 * Set the width for the cells of the TableModel
	 */
	public void updateUSValuesColumnsWidth() {
		USValuesTable.getColumnModel().getColumn(0).setPreferredWidth(70);
		for (int i = 1; i < USValuesTable.getColumnCount(); i++) {
			USValuesTable.getColumnModel().getColumn(i).setPreferredWidth(70);
		}
	}

}