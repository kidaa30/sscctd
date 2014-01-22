/**
 * SimExport.java
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
package simulator.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;import java.util.*;

import javax.swing.table.AbstractTableModel;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import simulator.*;
import simulator.configurables.ContextConfig.Context;
import simulator.util.GreekSymbol;

/**
 * Exports the results from the simulator to a spreadsheet. It uses the HSSF
 * free library provided from apache jakarta project. The HSSF allows numeric,
 * string, date or formula cell values to be written to or read from an XLS
 * file. Also we can do row and column sizing, cell styling (bold, italics,
 * borders,etc), and support for both built-in and user defined data formats. It
 * creates a workbook that has a different sheet for every model's group. Every
 * phase is represented with a different table.
 */
public class SimExport implements Runnable {
	private Map<String, SimGroup> groups;
	private Font groupFont, titleFont, tableTopFont, tableContFont;
	private Workbook wb;
	private Row row;
	private Cell cell;
	private CellStyle cs1, cs2, cs3, cs4, cs5;
	private final SimModel model;
	private SimView view;
	private String name;
	private File file;
	private boolean success;
	private ModelControl control;
    private Queue<ExportTask> exportTasks;
    private int jobs;

    private class ExportTask {
        int rowNum;
        int col;
        CellStyle style;
        Sheet sheet;
        Object value;

        public void action() {
            try {
                Row targetRow = sheet.getRow(rowNum);
                if(targetRow == null) {
                    targetRow = sheet.createRow(rowNum);
                }
                cell = targetRow.createCell(col);
                try {
                    cell.setCellValue((Double) value);
                } catch (ClassCastException e) {
                    cell.setCellValue((String) value);
                }
                cell.setCellStyle(style);
            } catch (NullPointerException e) {
                System.err.println(Messages.getString("writing.to.a.null.cell"));
            }
        }
    }

	/**
	 * SimExport's Constructor method.
	 * 
	 * @param view
	 *            the application's view.
	 * @param model
	 *            the current model where that values will come from.
	 * @param directory
	 *            the last chosen directory
	 */
	public SimExport(SimView view, SimModel model, String name, File file) {
		this.view = view;
		this.model = model;
		this.file = file;
		this.name = name;
		success = true;
        wb = new SXSSFWorkbook();
        createStyles();
        exportTasks = new LinkedList<ExportTask>();
        jobs = 0;
	}

	/**
	 * Create some cell styles for the workbooks's table. This could be borders,
	 * font style and size or even the background color.
	 * 
	 */
	private void createStyles() {

		// First style
		cs1 = wb.createCellStyle();

		groupFont = wb.createFont();
		groupFont.setFontHeightInPoints((short) 24);
		groupFont.setFontName("Courier New");
		groupFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		cs1.setFont(groupFont);
		cs1.setAlignment(CellStyle.ALIGN_CENTER);
		cs1.setBorderBottom(CellStyle.BORDER_THIN);
		cs1.setBorderLeft(CellStyle.BORDER_THIN);
		cs1.setBorderRight(CellStyle.BORDER_THIN);
		cs1.setBorderTop(CellStyle.BORDER_THIN);
		cs1.setFillBackgroundColor(HSSFColor.PALE_BLUE.index);

		// Second style
		cs2 = wb.createCellStyle();

		titleFont = wb.createFont();
		titleFont.setFontHeightInPoints((short) 14);
		titleFont.setFontName("Courier New");
		cs2.setFont(titleFont);

		// Third style
		cs3 = wb.createCellStyle();

		tableTopFont = wb.createFont();
		tableTopFont.setFontHeightInPoints((short) 12);
		tableTopFont.setFontName("Courier New");
		tableTopFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		cs3.setAlignment(CellStyle.ALIGN_CENTER);
		cs3.setBorderBottom(CellStyle.BORDER_THIN);
		cs3.setBorderLeft(CellStyle.BORDER_THIN);
		cs3.setBorderRight(CellStyle.BORDER_THIN);
		cs3.setBorderTop(CellStyle.BORDER_THIN);
		// cs3.setFillBackgroundColor(HSSFColor.GREY_25_PERCENT.index);
		cs3.setFillBackgroundColor(HSSFColor.RED.index);
		cs3.setFont(tableTopFont);

		// Fourth style
		cs4 = wb.createCellStyle();

		tableContFont = wb.createFont();
		tableContFont.setFontHeightInPoints((short) 12);
		tableContFont.setFontName("Courier New");
		cs4.setFont(tableContFont);
		cs4.setAlignment(CellStyle.ALIGN_CENTER);
		cs4.setBorderBottom(CellStyle.BORDER_THIN);
		cs4.setBorderLeft(CellStyle.BORDER_THIN);
		cs4.setBorderRight(CellStyle.BORDER_THIN);
		cs4.setBorderTop(CellStyle.BORDER_THIN);
		cs4.setFillBackgroundColor(HSSFColor.GREY_25_PERCENT.index);

		// Fifth(highlight) style
		cs5 = wb.createCellStyle();
		cs5.setFont(tableContFont);
		cs5.setAlignment(CellStyle.ALIGN_CENTER);
		cs5.setBorderBottom(CellStyle.BORDER_THIN);
		cs5.setBorderLeft(CellStyle.BORDER_THIN);
		cs5.setBorderRight(CellStyle.BORDER_THIN);
		cs5.setBorderTop(CellStyle.BORDER_THIN);
		cs5.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
		cs5.setFillPattern(CellStyle.SOLID_FOREGROUND);
	}

    public int makeExport() {
        row = null;
        cell = null;

        groups = model.getGroups();
        Set<String> setGroups = groups.keySet();
        Iterator<String> iterGroups = setGroups.iterator();

        // Generate iteration between groups.
        while (iterGroups.hasNext()) {
            exportGroup(iterGroups.next(), name);
        }
        return exportTasks.size();
    }

    public int initialTasks() {
        groups = model.getGroups();

        int jobs = 0;

        // Generate iteration between groups.
        for(SimGroup group : groups.values()) {
            for(SimPhase curPhase : group.getPhases()) {
                Map<String, CueList> results = curPhase.getResults();
                String[] cueNames = getCSNamesInPhase(results, curPhase);
                // Count realtime V
                // Regular
                for (int y = 1; y < cueNames.length + 1; y++) {
                    String cueName = cueNames[y - 1];
                    if (cueName.length() == 1
                            || view.isSetCompound()) {
                        jobs += results.get(cueName).getTrialCount()*(results.get(cueName).size()+1)*2;
                        jobs += results.get(cueName).getTrialCount()*2;
                    }
                }

                results = curPhase.getProbeResults();
                for(CueList cues : results.values()) {
                    jobs += cues.getTrialCount()*(cues.size()+1)*2;
                    jobs += cues.getTrialCount()*2;
                }


                //Probes
            }
        }
        return jobs;
    }

	public void doExport() throws IOException {
		FileOutputStream fileOut = new FileOutputStream(file);

		// Generate iteration between groups.
		while (!exportTasks.isEmpty() && !control.isCancelled()) {
			long estimatedCycle = System.nanoTime();
			ExportTask task = exportTasks.remove();
            task.action();
			control.incrementProgress(1);
			control.setEstimatedCycleTime((System.nanoTime()
					- estimatedCycle)/1000);
		}
		if (!control.isCancelled()) {
			wb.write(fileOut);
		}
		fileOut.close();
        control.incrementProgress(1);
	}

	/**
	 * Export the average response of a cue to excel.
	 * 
	 * @param cueName
	 * @param curPhase
	 * @param sheet
	 * @param rowPos
	 * @param colPos
	 * @param results
	 * @return
	 */

	protected int exportAverageResponse(String cueName, SimPhase curPhase,
			Sheet sheet, int rowPos, int colPos, Map<String, CueList> results) {
		String interfaceName;
        ExportTask task;
		//if (curPhase.isCueInStimuli(cueName)) {
			interfaceName = getInterfaceName(cueName);
			CueList cues = results.get(cueName);
			//row = sheet.createRow(rowPos);
			for (int z = 0; z <= cues.getTrialCount(); z++) {
				if (z == 0) {
                    task = new ExportTask();
                    task.rowNum = rowPos;
task.sheet = sheet;
					task.col = colPos;
					task.value = Messages.format("mean.response.0", interfaceName);
					task.style = cs3;
                    exportTasks.add(task);
				} else {
                    task = new ExportTask();
                    task.rowNum = rowPos;
task.sheet = sheet;
                    task.col = colPos + z;
                    task.value = cues.averageResponse(z - 1) ;
					task.style = cs4;
                    exportTasks.add(task);
                    this.control.incrementProgress(1);
					// sheet.setColumnWidth(z, (short) ((50 * 4) / ((double) 1 /
					// 20)));
				}

				if (this.control.isCancelled()) {
					return rowPos;
				}
			}
			rowPos++;
		//}
		return rowPos;
	}

	/**
	 * Export the components of a cue to excel.
	 *
	 * @param cueName
	 * @param curPhase
	 * @param sheet
	 * @param rowPos
	 * @param colPos
	 * @param results
	 * @return
	 */

	protected int exportComponents(String cueName, SimPhase curPhase,
			Sheet sheet, int rowPos, int colPos, Map<String, CueList> results) {
		String interfaceName;
        ExportTask task;
		//if (curPhase.isCueInStimuli(cueName)) {
			interfaceName = getInterfaceName(cueName);
			CueList cues = results.get(cueName);
			for (int x = 0; x < cues.size(); x++) {
				//row = sheet.createRow(rowPos);
				SimCue curcue = cues.get(x);

				for (int z = 0; z <= cues.getTrialCount(); z++) {

					if (z == 0) {
                        task = new ExportTask();
                        task.rowNum = rowPos;
task.sheet = sheet;
						task.col = colPos;
						task.value = Messages.format("0.component.1", interfaceName, x + 1);
						task.style = cs3;
                        exportTasks.add(task);
					} else {

                        task = new ExportTask();
                        task.rowNum = rowPos;
task.sheet = sheet;
                        task.col = colPos + z;
                        try {
						    task.value = curcue.getAssocValueVector()
								.get(z - 1).doubleValue();
                        } catch (IndexOutOfBoundsException e) {
                            System.err.println("Ran out of trials for cue "+(x)+" of " +interfaceName+" at t"+(z-1));
                            task.value = 0d;
                        }
						task.style = cs4;
                        exportTasks.add(task);
						// if(cues.getMaxCueList().get(z-1) < x) {
						// task.style = cs5);
						// }
						// sheet.setColumnWidth(z, (short) ((50 * 4) / ((double)
						// 1 / 20)));
                        this.control.incrementProgress(1);
					}

					if (this.control.isCancelled()) {
						return rowPos;
					}
				}

				rowPos++;
			}
		//}
		return rowPos;
	}

	private void exportGroup(String groupName, String name) {
        ExportTask task;
		int rowPos = 0;
		int colPos = 0;
		final SimGroup group = groups.get(groupName);
		final Sheet sheet = wb.createSheet(group.getNameOfGroup());

		// Modified by Alberto Fern�ndez July-2011

		// Title = file name

		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;

		// Alberto Fernandez Oct-2011
		// sheet.addMergedRegion(new Region(rowPos, colPos, rowPos,
		// (short)(colPos + 3)));
		// Deprecated: Region(int rowFrom, short colFrom, int rowTo, short
		// colTo)
		// Current: CellRangeAddress(int firstRow, int lastRow, int firstCol,
		// int lastCol)

		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, colPos,
				(colPos + 3)));

		task.value = name; // write file name
		task.style = cs1;
        exportTasks.add(task);

		// Show procedural settings

		rowPos += 2;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("random.trial.combinations");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos + 3;
		task.value = (double) model.getCombinationNo();
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("random.cs.length.distributions");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos + 3;
		task.value = (double) model.getVariableCombinationNo();
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("timestep.length");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos + 3;
		task.value = model.getTimestepSize() ;
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("trace.type");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos + 3;
		task.value = model.getTraceType().toString();
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("mean.type");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos + 3;
		task.value = model.isGeometricMean() ? Messages.getString("SimView.geometric") : Messages.getString("SimView.arithmetic");
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("distribution.type");
		task.style = cs2;
        exportTasks.add(task);
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
				(short) colPos, (short) (colPos + 2)));
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos + 3;
		task.value = model.isExponential() ? Messages.getString("SimView.exponential") : Messages.getString("uniform");
		task.style = cs2;
        exportTasks.add(task);
		rowPos++;

		// Show Parameters

		// CS model

		rowPos += 2;

		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("cs.alpha");
		task.style = cs2;
        exportTasks.add(task);
		// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
		// (short)(colPos + 1)));

		rowPos++;

		AbstractTableModel cstmv = view.getCSValuesTableModel();
		for (int r = 0; r < cstmv.getRowCount(); r++) {
			// if (group.getCuesMap().containsKey(cstmv.getValueAt(r,0))){
            String tableName = (String) cstmv.getValueAt(r, 0);
            String cueName = model.interfaceName2cueName(tableName);
			if (group.getCuesMap()
					.containsKey(cueName)) {
				double value = new Double((String) cstmv.getValueAt(r, 1));
				//row = sheet.createRow(rowPos);
				task = new ExportTask();
                task.rowNum = rowPos;
                task.sheet = sheet;
                task.col = colPos;
				task.value = (String) cstmv.getValueAt(r, 0);
				task.style = cs3;
                exportTasks.add(task);
				task = new ExportTask();
                task.rowNum = rowPos;
                task.sheet = sheet;
                task.col = (colPos + 1);
				task.value = value ;
				task.style = cs4;
                exportTasks.add(task);
				rowPos++;
			}
		}
		if (model.isUseContext() && !model.contextAcrossPhase()) {
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
            task.rowNum = rowPos;
            task.sheet = sheet;
            task.col = colPos;
			task.value = Context.PHI.toString();
			task.style = cs3;
            exportTasks.add(task);
			task = new ExportTask();
            task.rowNum = rowPos;
            task.sheet = sheet;
            task.col = (colPos + 1);
			task.value = model.getContextAlpha() ;
			task.style = cs4;
            exportTasks.add(task);
			rowPos++;
		}

		// US model

		rowPos += 2;

		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = "US: ";
		task.style = cs2;
        exportTasks.add(task);
		// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
		// (short)(colPos + 1)));

		rowPos++;

		AbstractTableModel ustmv = view.getUSValuesTableModel();
		for (int r = 0; r < ustmv.getRowCount(); r++) {
			Object ovalue = ustmv.getValueAt(r, 1);
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = colPos;
			task.value = GreekSymbol.getSymbol((String) ustmv.getValueAt(r, 0));
			task.style = cs3;
            exportTasks.add(task);
			// task = new ExportTask();
            //task.rowNum = rowPos;
task.sheet = sheet;
            //task.col = (short)(colPos + 1));
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = (colPos + 1);
			if (!((String) ovalue).equals("")) {
				double value = new Double((String) ovalue);
				task.value = value;
			}
			task.style = cs4;
            exportTasks.add(task);
			rowPos++;
		}
		// Other values
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("other");
		task.style = cs2;
        exportTasks.add(task);
		// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
		// (short)(colPos + 1)));

		rowPos++;
		AbstractTableModel otmv = view.getOtherValuesTableModel();
		for (int r = 0; r < otmv.getRowCount(); r++) {
			Object ovalue = otmv.getValueAt(r, 1);
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = colPos;
			task.value = GreekSymbol.getSymbol((String) otmv.getValueAt(r, 0));
			task.style = cs3;
            exportTasks.add(task);
			// task = new ExportTask();
            //task.rowNum = rowPos;
task.sheet = sheet;
            //task.col = (short)(colPos + 1);
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = (colPos + 1);
			if (!((String) ovalue).equals("")) {
				double value = new Double((String) ovalue);
				task.value = value ;
			}
			task.style = cs4;
            exportTasks.add(task);
			rowPos++;
		}
		if (model.showResponse()) {
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = colPos;
			task.value = Messages.getString("response.threshold");
			task.style = cs3;
            exportTasks.add(task);
			task = new ExportTask();
            task.rowNum = rowPos;
task.sheet = sheet;
            task.col = (colPos + 1);
			task.value = model.getThreshold() ;
			task.style = cs4;
            exportTasks.add(task);
			rowPos++;
		}
		// Context alphas

		if (model.isUseContext()) {
			rowPos += 2;

			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.getString("context.alphas");
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
			// (short)(colPos + 1)));

			rowPos++;
			//row = sheet.createRow(rowPos);
			for (int c = 1; c < group.getNoOfPhases() + 1; c++) {
				task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos + c;
				task.value = "P" + c;
				task.style = cs3;
exportTasks.add(task);
			}
			rowPos++;
			for (String cue : group.getCuesMap().keySet()) {
				if (Context.isContext(cue)) {
					//row = sheet.createRow(rowPos);
					task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
					task.value = cue;
					task.style = cs4;
exportTasks.add(task);
					for (int c = 1; c < group.getNoOfPhases() + 1; c++) {
						task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos + c;
						try {
							double alpha = group.getPhases().get(c - 1)
									.isCueInStimuli(cue) ? group.getPhases()
									.get(c - 1).getResults().get(cue)
									.getAlpha() : null;
							task.value = alpha ;
						} catch (NullPointerException e) {
							// Ignore that, this context isn't here now.
						}
						task.style = cs3;
exportTasks.add(task);
					}
					rowPos++;
				}
			}
		}
		rowPos++;

		// Create group title on top of the page
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;

		// sheet.addMergedRegion(new Region(rowPos, colPos, rowPos,
		// (short)(colPos + 3)));
		sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos, colPos,
				(short) (colPos + 3)));

		task.value = group.getNameOfGroup();
		task.style = cs1;

		// Generate iteration between phases
		for (int i = 0; i < group.getPhases().size(); i++) {
			rowPos += 2;
			SimPhase curPhase = group.getPhases().get(i);

			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.format("phase.0", i + 1);
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
			// (short)(colPos + 1)));
			sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
					(short) colPos, (short) (colPos + 1)));

			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + 2);
			task.value = Messages.format("random.0", curPhase.isRandom());
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
					(short) (colPos + 2), (short) (colPos + 3)));

			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + 4);
			task.value = Messages.format("sequence.0", curPhase.intialSequence());
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
					(short) (colPos + 4), (short) (colPos + 10)));

			Map<String, CueList> results = curPhase.getResults();


			String[] cueNames = getCSNamesInPhase(results, curPhase);

			rowPos++;
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.getString("stimuli.temporal.parameters");
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			// sheet.addMergedRegion(new Region(rowPos, (short)(colPos), rowPos,
			// (short)(colPos + 1)));
			sheet.addMergedRegion(new CellRangeAddress(rowPos, rowPos,
					(short) colPos, (short) (colPos + 2)));
			rowPos++;

			List<CS> csInStim = new ArrayList<CS>();
			for (CS cue : curPhase.getPresentCS()) {
				if (curPhase.isCueInStimuli(cue)) {
					csInStim.add(cue);
				}
				//this.control.incrementProgress(1.0D);

				if (this.control.isCancelled()) {
					return;
				}
			}
            Collections.sort(csInStim);
			for (CS cue : csInStim) {
				if (!Context.isContext(cue.getName()) && !cue.isConfigural()
						&& !cue.isCompound()) {
					//row = sheet.createRow(rowPos);
					task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
					task.value = cue.getName();
					task.style = cs3;
exportTasks.add(task);
					task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + 1);
					task.value = curPhase.getTimingConfig().getDurations()
							.getMap().get(cue) + ""
							;
					task.style = cs4;
exportTasks.add(task);
					task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + 2);
					task.value = curPhase.getTimingConfig().getRelation(
							cue)  + ""
							;
					task.style = cs4;
exportTasks.add(task);
					rowPos++;
				}
			}
			// US timing
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.getString("SimView.164");
			task.style = cs3;
exportTasks.add(task);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + 1);
			task.value = curPhase.getTimingConfig().getUsDuration() ;
			task.style = cs4;
exportTasks.add(task);

			rowPos += 2;

			// max = maximum number of trial of all the cues in the phase
			int max = 0;
			for (int j = 0; j < cueNames.length; j++) {
				CueList curCscCue = results.get(cueNames[j]);

				max = Math.max(curCscCue.getTrialCount(), max);
			}

			// Alberto Fernandez August-2011: export (1) cues, (2) compounds,
			// (3) configural cues

			// Realtime
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.getString("realtime.v");
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
			rowPos++;

			// first row: Trial names
			//row = sheet.createRow(rowPos);
			trialRow(max, colPos, rowPos, sheet);

			rowPos++;

			// export Cues
			for (int y = 1; y < cueNames.length + 1; y++) {
				String cueName = cueNames[y - 1];
				if (cueName.length() == 1
						&& (Character.isUpperCase(cueName.charAt(cueName
								.length() - 1)))) {
					rowPos = exportComponents(cueName, curPhase, sheet, rowPos,
							colPos, results);
				}
			}
			rowPos++;
			// export compound Cues
			for (int y = 1; view.isSetCompound() && y < cueNames.length + 1; y++) {
				String cueName = cueNames[y - 1];
				if (cueName.length() > 1) {
					rowPos = exportComponents(cueName, curPhase, sheet, rowPos,
							colPos, results);
				}
			}
			rowPos++;

			// Trial average
			//row = sheet.createRow(rowPos);
			task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
			task.value = Messages.getString("v.per.trial");
			task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);

			rowPos++;

			// first row: Trial names
			//row = sheet.createRow(rowPos);
			trialRow(max, colPos, rowPos, sheet);

			rowPos++;

			// export Cues
			for (int y = 1; y < cueNames.length + 1; y++) {
				String cueName = cueNames[y - 1];
				if (cueName.length() == 1
						&& (Character.isUpperCase(cueName.charAt(cueName
								.length() - 1)))) {
					rowPos = exportTrial(cueName, curPhase, sheet, rowPos,
							colPos, results);
				}
			}
			rowPos++;
			// export compound Cues
			for (int y = 1; view.isSetCompound() && y < cueNames.length + 1; y++) {
				String cueName = cueNames[y - 1];
				if (cueName.length() > 1) {
					rowPos = exportTrial(cueName, curPhase, sheet, rowPos,
							colPos, results);
				}
			}
			rowPos++;

			if (true) {
				// Response
				//row = sheet.createRow(rowPos);
				task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
				task.value = Messages.getString("realtime.response");
				task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);

				rowPos++;

				// first row: Trial names
				//row = sheet.createRow(rowPos);
				trialRow(max, colPos, rowPos, sheet);

				rowPos++;

				// export Cues
				for (int y = 1; y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					if (cueName.length() == 1
							&& (Character.isUpperCase(cueName.charAt(cueName
									.length() - 1)))) {
						rowPos = exportResponse(cueName, curPhase, sheet,
								rowPos, colPos, results);
					}
				}
				// export compound Cues
				for (int y = 1; view.isSetCompound() && y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					if (cueName.length() > 1) {
						rowPos = exportResponse(cueName, curPhase, sheet,
								rowPos, colPos, results);
					}
				}
				rowPos++;

				rowPos++;
				// Response
				//row = sheet.createRow(rowPos);
				task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
				task.value = Messages.getString("mean.response.per.trial");
				task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);

				rowPos++;

				// first row: Trial names
				//row = sheet.createRow(rowPos);
				trialRow(max, colPos, rowPos, sheet);

				rowPos++;

				// export Cues
				for (int y = 1; y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					if (cueName.length() == 1
							&& (Character.isUpperCase(cueName.charAt(cueName
									.length() - 1)))) {
						rowPos = exportAverageResponse(cueName, curPhase,
								sheet, rowPos, colPos, results);
					}
				}
				// export compound Cues
				for (int y = 1; view.isSetCompound() && y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					if (cueName.length() > 1) {
						rowPos = exportAverageResponse(cueName, curPhase,
								sheet, rowPos, colPos, results);
					}
				}
				rowPos++;
			}
			// Export probe trial results
			if (!curPhase.getProbeResults().isEmpty()) {
                rowPos = exportProbes(curPhase, sheet, rowPos,
								colPos, curPhase.getProbeResults());
			}
			rowPos++;

			// export configural cues (if any)
			rowPos++;

			boolean configural = false;
			int maxConfigural = 0;
			for (int p = 0; p < cueNames.length; p++) {
				if (Character.isLowerCase(cueNames[p].charAt(0))
						&& curPhase.isCueInStimuli(cueNames[p].charAt(0) + "")) {
					configural = true;
					maxConfigural = Math.max(maxConfigural,
							results.get(cueNames[p]).getTrialCount());
				}
			}

			if (configural) {
				// Realtime
				//row = sheet.createRow(rowPos);
				task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
				task.value = Messages.getString("realtime.v");
				task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);
				rowPos++;

				// first row: Trial names
				//row = sheet.createRow(rowPos);
				trialRow(maxConfigural, colPos, rowPos, sheet);

				rowPos++;

				for (int y = 1; y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					String interfaceName;
					if (cueName.length() == 1
							&& Character.isLowerCase(cueName.charAt(cueName
									.length() - 1))) {
						// export compound Cues
						rowPos = exportComponents(cueName, curPhase, sheet,
								rowPos, colPos, results);
					}
				}

				// Trial average
				//row = sheet.createRow(rowPos);
				task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = colPos;
				task.value = Messages.getString("v.per.trial");
				task.style = cs2;
exportTasks.add(task);
exportTasks.add(task);

				rowPos++;

				// first row: Trial names
				//row = sheet.createRow(rowPos);
				trialRow(maxConfigural, colPos, rowPos, sheet);

				rowPos++;

				// export Cues
				for (int y = 1; y < cueNames.length + 1; y++) {
					String cueName = cueNames[y - 1];
					if (cueName.length() == 1
							&& (Character.isLowerCase(cueName.charAt(cueName
									.length() - 1)))
							&& curPhase.isCueInStimuli(cueName)) {
						rowPos = exportTrial(cueName, curPhase, sheet, rowPos,
								colPos, results);
					}
				}
			}
			sheet.setColumnWidth(0, (short) ((50 * 5) / ((double) 1 / 20)));
		}
	}

	protected int exportProbes(SimPhase curPhase, Sheet sheet,
			int rowPos, int colPos, Map<String, CueList> results) {
        ExportTask task;

		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("probe.trials");
		task.style = cs2;
        exportTasks.add(task);
//exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("v.per.trial");
		task.style = cs2;
        exportTasks.add(task);
//exportTasks.add(task);
		rowPos++;
        //Get longest trial number
        int trials = 0;
        for(CueList c : results.values()) {
            trials = Math.max(trials, c.getTrialCount());
        }

		//row = sheet.createRow(rowPos);
		trialRow(trials, colPos, rowPos, sheet);
        rowPos++;

        for(String cueName : results.keySet()) {
		    rowPos = exportTrial(cueName, curPhase, sheet, rowPos, colPos, results) + 1;
        }
		//row = sheet.createRow(rowPos);

		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("realtime.v");
		task.style = cs2;
        exportTasks.add(task);
//exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
        trialRow(trials, colPos, rowPos, sheet);
        rowPos++;

        for(String cueName : results.keySet()) {
		    rowPos = exportComponents(cueName, curPhase, sheet, rowPos, colPos,
				results) + 1;

        }
		//row = sheet.createRow(rowPos);
		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("mean.response.per.trial");
		task.style = cs2;
        exportTasks.add(task);
//exportTasks.add(task);
		rowPos++;
		//row = sheet.createRow(rowPos);
        trialRow(trials, colPos, rowPos, sheet);
        rowPos++;

        for(String cueName : results.keySet()) {
		    rowPos = exportAverageResponse(cueName, curPhase, sheet, rowPos,
				colPos, results) + 1;
        }

		task = new ExportTask();
        task.rowNum = rowPos;
        task.sheet = sheet;
        task.col = colPos;
		task.value = Messages.getString("realtime.response");
		task.style = cs2;
        exportTasks.add(task);

		rowPos++;
		//row = sheet.createRow(rowPos);
        trialRow(trials, colPos, rowPos, sheet);
        rowPos++;
        for(String cueName : results.keySet()) {
		    rowPos = exportResponse(cueName, curPhase, sheet, rowPos, colPos,
				results) + 1;
        }

		return rowPos;
	}

	/**
	 * Export the response of a cue to excel.
	 *
	 * @param cueName
	 * @param curPhase
	 * @param sheet
	 * @param rowPos
	 * @param colPos
	 * @param results
	 * @return
	 */

	protected int exportResponse(String cueName, SimPhase curPhase,
			Sheet sheet, int rowPos, int colPos, Map<String, CueList> results) {
		String interfaceName;
        ExportTask task;
		//if (curPhase.isCueInStimuli(cueName)) {
			interfaceName = getInterfaceName(cueName);
			CueList cues = results.get(cueName);
			for (int x = 0; x < cues.size(); x++) {
				//row = sheet.createRow(rowPos);
				SimCue curcue = cues.get(x);
				long estimatedCycle = System.currentTimeMillis();
				for (int z = 0; z <= cues.getTrialCount(); z++) {

					if (z == 0) {
						task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos);
						task.value = Messages.format("0.component.1", interfaceName, x + 1);
						task.style = cs3;
exportTasks.add(task);
					} else {

						task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + z);
						task.value = curcue.response(z - 1) ;
						task.style = cs4;
exportTasks.add(task);
                        this.control.incrementProgress(1);

						// sheet.setColumnWidth(z, (short) ((50 * 4) / ((double)
						// 1 / 20)));
					}

					if (this.control.isCancelled()) {
						return rowPos;
					}
				}

				rowPos++;
			}
		//}
		return rowPos;
	}

	/**
	 * Helper function for dumping a trial average for a stimulus to an excel
	 * sheet.
	 *
	 * @param cueName
	 * @param curPhase
	 * @param sheet
	 * @param rowPos
	 * @param colPos
	 * @param results
	 * @return
	 */

	protected int exportTrial(String cueName, SimPhase curPhase, Sheet sheet,
			int rowPos, int colPos, Map<String, CueList> results) {
		// export Cues
		String interfaceName = getInterfaceName(cueName);
        ExportTask task;
		//if (curPhase.isCueInStimuli(cueName)) {
			CueList cues = results.get(cueName);
			for (int x = 0; x < 1; x++) {
				//row = sheet.createRow(rowPos);
				long estimatedCycle = System.currentTimeMillis();
				for (int z = 0; z <= cues.getTrialCount(); z++) {

					if (z == 0) {
						task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + z);
						task.value = interfaceName ;
						task.style = cs3;
exportTasks.add(task);
					} else {

						task = new ExportTask();
task.rowNum = rowPos;
task.sheet = sheet;
task.col = (colPos + z);
						task.value = cues.averageAssoc(z - 1) ;
						task.style = cs4;
exportTasks.add(task);
                        this.control.incrementProgress(1);
						// sheet.setColumnWidth(x, (short) ((50 * 4) / ((double)
						// 1 / 20)));
					}

					if (this.control.isCancelled()) {
						return rowPos;
					}
				}

				rowPos++;
				// }
			}
		//}
		return rowPos;
	}

	/**
	 * Helper function to correctly format the print name of a cue
	 *
	 * @param cueName
	 * @return
	 */

	protected String getInterfaceName(String cueName) {
        String iFace = model.cueName2InterfaceName(cueName);
        //Add hat for probes
        if(!iFace.startsWith("c") && iFace.contains("(")) {
            iFace += "^";
        }
        return iFace;
		/*String interfaceName;
		if (cueName.length() > 1) {
			if (Character.isUpperCase(cueName.charAt(cueName.length() - 1))) { // compound
				interfaceName = cueName; // no change
			} else { // configural compound
				String compoundName = cueName
						.substring(0, cueName.length() - 1);
				// interfaceName = "[" + compoundName + "�]";
				interfaceName = "[" + compoundName + "]";
			}
		} else if (Character.isLowerCase(cueName.charAt(0))
				&& !cueName.endsWith("ω")) {
			interfaceName = model.cueName2InterfaceName(cueName);
		} else {
			interfaceName = cueName; // no change
		}
		return interfaceName;*/
	}

	/**
	 * @return the model being exported.
	 */
	public SimModel getModel() {
		return model;
	}

	/**
	 * @return true if the export succeeded
	 */
	public boolean isSuccess() {
		return success;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
        jobs = 0;
		try {
            jobs = makeExport();
            control.setTotalProgress(jobs);
            control.madeExport(true);
            control.setProgress(0);
			doExport();
		} catch (IOException e) {
			success = false;
		}
	}

	/**
	 * @param control
	 */
	public void setControl(ModelControl control) {
		this.control = control;
	}

	/**
	 * Print a row of trial headers.
	 *
	 * @param num
	 * @param colPos
	 * @param row2
	 */

	private void trialRow(int num, int colPos, int row2, Sheet sheet) {
		for (int x = 1; x <= num; x++) {
            ExportTask task = new ExportTask();
            task.rowNum = row2;
			task.col = colPos + x;
			task.value = Messages.format("trial.0", x);
			task.style = cs3;
            task.sheet = sheet;
            exportTasks.add(task);
		}
	}

    private String[] getCSNamesInPhase(Map<String, CueList> results, SimPhase curPhase) {
        String names[] = {};
        names = results.keySet().toArray(names);
        List<String> inStimName = new ArrayList<String>();
        String cueNames[] = {};

        for (int j = 0; j < names.length; j++) {
            // long estimatedCycle = System.currentTimeMillis();
            if (curPhase.isCueInStimuli(names[j])) {
                inStimName.add(names[j]);
            }
            // this.control.incrementProgress(1.0D);
            // control.setEstimatedCycleTime(System.currentTimeMillis()-estimatedCycle);

            if (this.control.isCancelled()) {
                return cueNames;
            }
        }

        cueNames = inStimName.toArray(cueNames);
        return cueNames;
    }

}
