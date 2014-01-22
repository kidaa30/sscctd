/**
 * SimController.java
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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;

import simulator.configurables.ContextConfig;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import simulator.editor.TrialStringEditor;
import simulator.graph.AverageResponseGraph;
import simulator.graph.ComponentGraph;
import simulator.graph.ResponseGraph;
import simulator.graph.SimGraph;
import simulator.util.*;
import simulator.util.io.SimExport;

/**
 * SimController is the main class of the simulator project. It controls the
 * behavior of the model which includes the groups and their phases and handles
 * the view components. The user has direct interaction through the controller
 * class.
 */
public class SimController implements ActionListener, PropertyChangeListener {

    public static String getTimeMessage(long remaining) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        long minutesOver = TimeUnit.MILLISECONDS.toMinutes(remaining)
                - TimeUnit.HOURS
                .toMinutes(TimeUnit.MILLISECONDS
                        .toHours(remaining));
        minutesOver = (int)(Math.round( minutesOver / 15.0) * 15);
        if(minutesOver == 60) {
            minutesOver = 0;
            hours += 1;
        }
        String timeRemaining = "Under a minute remaining";
        if(hours == 1 && minutesOver == 0) {
            timeRemaining = "About an hour remaining";
        } else if (hours > 0 && minutesOver == 0) {
            timeRemaining = String.format(
                    "About %d hours remaining",
                    hours);
        } else if (hours > 0) {
            timeRemaining = String.format(
                    "About %d %s, %d minutes remaining",
                    hours, hours == 1 ? "hour" : "hours",minutesOver);
        }else if (minutes == 1) {
            timeRemaining = "About a minute remaining";
        }
        else if (minutes > 0) {
            timeRemaining = String.format(
                    "About %d minutes remaining",
                    minutes);
        }

        return timeRemaining;
    }

	/**
	 * Background task for running a simulation.
	 * 
	 * @author J Gray
	 * 
	 */

	class ExportTask extends SwingWorker<Void, Void> {
		/**
		 * Run the simulation in a worker thread, starts simulating and attaches
		 * a progress monitor periodically updated with task progress.
		 */
		@Override
		public Void doInBackground() {
			SimController.this.control = new ModelControl();
			int progress = 0;
			SimController.this.view.setUILocked(true);
            SimController.this.exporter.setControl(SimController.this.control);

            progressMonitor = new MyProgressMonitor(
                    view,
                    Messages.getString("SimController.exportMessage"), "", 0, 100); //$NON-NLS-1$ //$NON-NLS-2$
            progressMonitor.setMillisToPopup(0);
            progressMonitor.setMillisToDecideToPopup(0);
            progressMonitor.setProgress(0);
            //AccessibleContext ac = progressMonitor.getAccessibleContext();
            //JDialog dialog = (JDialog)ac.getAccessibleParent();
            //dialog.setSize(dialog.getWidth() + 100, dialog.getHeight() + 50);
            int dots = 0;

			setProgress(1);
            int jobs = SimController.this.exporter.initialTasks();
            SimController.this.totalProgress = SimController.this.exporter.initialTasks();
            progressMonitor.setMaximum(totalProgress + 2);
            SimController.this.simulate = new Thread(
                    SimController.this.exporter);
            SimController.this.simulate.start();
            boolean makingExport = true;


			try {
                while (!isCancelled() && simulate.isAlive()
                        && !progressMonitor.isCanceled()
                        && !control.isComplete()) {
                    if(makingExport && control.madeExport()) {
                        totalProgress = control.getTotalProgress() + 2;
                        progressMonitor.setMaximum(totalProgress);
                        progressMonitor.setProgress(1);
                        makingExport = false;
                    }
                    // Update progress
                    Thread.sleep(100);
                    progress = (int) control.getProgress();
                    // setProgress(Math.min(progress, totalProgress));
                    // int progress = (Integer) evt.getNewValue();
                    progressMonitor.setProgress(progress);
                    long remaining = (long) (control.getEstimatedCycleTime() * (totalProgress - control
                            .getProgress())) / 1000;
                    String msg = SimController.getTimeMessage(remaining);
                    if(makingExport) {
                        msg = Messages.getString("collecting.results");
                    }
                    for(int i = 0; i < dots; i++) {
                        msg += ".";
                    }
                    progressMonitor.setNote(msg);
                    dots++;
                    dots = dots%4;
                }
                if (progressMonitor.isCanceled()) {
                    cancel(true);
                }
            } catch (InterruptedException ignore) {
                System.err.println("Interrupted!");
            }
            return null;
		}

		/**
		 * Kill the progress monitor, re-enable the gui and update the output if
		 * the simulation wasn't cancelled.
		 */
		@Override
		public void done() {
			view.getGlassPane().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			view.getGlassPane().setVisible(false);
			progressMonitor.setProgress(0);
			progressMonitor.close();
			if (!exporter.isSuccess()) {
				view.showError(Messages.getString("SimController.fileError"));
			}
			view.setUILocked(false);
		}
	}

	/**
	 * Background task for running a simulation.
	 * 
	 * @author J Gray
	 * 
	 */

	class RunTask extends SwingWorker<Void, Void> {

		/**
		 * Run the simulation in a worker thread, starts simulating and attaches
		 * a progress monitor periodically updated with task progress.
		 */
		@Override
		public Void doInBackground() {
			int progress = 0;
			view.setUILocked(true);
			control = new ModelControl();
			getModel().setControl(control);
			// execute the algorithm
			simulate = new Thread(getModel());
			simulate.start();
			// Initialize progress property.
			totalProgress = getModel().totalNumPhases() + 2;
			progressMonitor = new MyProgressMonitor(
					view,
					Messages.getString("SimController.runMessage"), "", 0, totalProgress + 1); //$NON-NLS-1$ //$NON-NLS-2$
			progressMonitor.setMillisToPopup(0);
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setProgress(0);
			setProgress(1);
            //AccessibleContext ac = progressMonitor.getAccessibleContext();
            //JDialog dialog = (JDialog)ac.getAccessibleParent();
            //dialog.setSize(dialog.getWidth() + 100, dialog.getHeight() + 50);
            int dots = 0;
			try {
				while (!isCancelled() && simulate.isAlive()
						&& !progressMonitor.isCanceled()
						&& !control.isComplete()) {
					// Update progress
					Thread.sleep(1000);
					progress = (int) control.getProgress();
					// setProgress(Math.min(progress, totalProgress));
					// int progress = (Integer) evt.getNewValue();
					progressMonitor.setProgress(progress);
					long remaining = (long) (control.getEstimatedCycleTime() * (totalProgress - control
							.getProgress()));
                    String msg = SimController.getTimeMessage(remaining);
                    for(int i = 0; i < dots; i++) {
                        msg += ".";
                    }
                    progressMonitor.setNote(msg);
                    dots++;
                    dots = dots%4;
				}
				if (progressMonitor.isCanceled()) {
					cancel(true);
				}
			} catch (InterruptedException ignore) {
				System.err.println("Interrupted!");
			}
			return null;
		}

		/**
		 * Kill the progress monitor, re-enable the gui and update the output if
		 * the simulation wasn't cancelled.
		 */
		@Override
		public void done() {
			view.setUILocked(false);
			if (!isCancelled()) {
				view.setStatusComponent(true, "dispGraph"); //$NON-NLS-1$
				view.setOutput(getModel().textOutput(view.isSetCompound()));
				view.setStatusComponent(true, "Export"); //$NON-NLS-1$
				view.setStatusComponent(true, "Save"); //$NON-NLS-1$
			} else {
				// getModel().cancel();
				control.setCancelled(true);
				simulate.interrupt();
				simulate = null;
				view.setStatusComponent(false, "dispGraph"); //$NON-NLS-1$
				view.setOutput("");
				view.setStatusComponent(false, "Export"); //$NON-NLS-1$
				view.setStatusComponent(false, "Save"); //$NON-NLS-1$
			}
			view.getGlassPane().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			view.getGlassPane().setVisible(false);
			progressMonitor.setProgress(0);
			progressMonitor.close();
		}
	}

	private SimModel model;

    public SimView getView() {
        return view;
    }

    public void setView(SimView view) {
        this.view = view;
    }

    private SimView view;
	/** Monitor/progress bar. **/
	private MyProgressMonitor progressMonitor;
	/** Task for running the simulation. **/
	private RunTask task;

	/** Thread for simulating. **/
	private volatile Thread simulate;
	/** Excel exporter. **/
	private SimExport exporter;
	private String lastDirectory = FileSystemView.getFileSystemView().getHomeDirectory().getPath(); // Alberto Fern�ndez Sept-2011  //$NON-NLS-1$
	private boolean cscMode;
	private ExportTask exporterTask;

	private volatile ModelControl control;

	private int totalProgress;

	/**
	 * SimController's Constructor method.
	 * 
	 * @param m
	 *            the SimPhase Object, the model on the structure.
	 * @param v
	 *            the SimView Object, the view in the application.
	 */
	public SimController(SimModel m, SimView v) {
		setModel(m);
		view = v;
		view.addButtonListeners(this); // add actionListeners on the buttons
		view.addMenuListeners(this); // add actionListeners on the menuitems
		view.setStatusComponent(false, "run"); //$NON-NLS-1$
		view.setStatusComponent(false, "dispGraph"); //$NON-NLS-1$
		view.setStatusComponent(false, "Export"); //$NON-NLS-1$
		view.setStatusComponent(false, "Save"); //$NON-NLS-1$
		cscMode = true;
	}

	//

	/*
	 * Actions performed whenever the user clicks a button or a menu item.
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// The user chooses to start a new experiment, a new model is created
		// and all components status are being reset to their initial value.
		if (e.getActionCommand() == "New") { //$NON-NLS-1$
			newModel();
		}
		if (e.getActionCommand() == "SetSerialCompound") {
			// Enable compounds if serial compounds are considered.
			if (view.isUseSerialCompounds()) {
				view.setIsSetCompound(true);
                view.setIsSetConfiguralCompounds(true);
                view.setStatusComponent(view.isSetConfiguralCompounds(),
                        "SetConfiguralCompounds");
			}
		}
		if (e.getActionCommand() == "restrictPredictions") {
			model.setRestrictPredictions(view.isRestrictPredictions());
		}
		if (e.getActionCommand() == "timingPerTrial") {
			view.updateTimingConfigs();
			view.updatePhasesColumnsWidth();
			model.setTimingPerTrial(view.timingPerTrial());
		}

		// The user chooses to open a saved experiment from the the saved files.
		// The file contains the values that were added to run the experiment.
		if (e.getActionCommand() == "Open") { //$NON-NLS-1$
			JFileChooser fc = new JFileChooser();
			// Modified Alberto Fern�ndez Sept-2011 : manage current directory
			// fc.setCurrentDirectory(new File("."));
			fc.setCurrentDirectory(new File(lastDirectory));
			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension(Messages
                    .getString("SimController.extension")); //$NON-NLS-1$
			filter.setDescription(Messages.getString("SimController.fileType")); //$NON-NLS-1$
			fc.setFileFilter(filter);
			int returnVal = fc.showOpenDialog(view);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					File file = fc.getSelectedFile();
					loadObjects(file);
				} catch (FileNotFoundException fe) {
					view.showError(Messages
							.getString("SimController.fileError")); //$NON-NLS-1$
				} catch (IOException ioe) {
					view.showError(Messages
							.getString("SimController.fileError")); //$NON-NLS-1$
				} catch (VersionException ve) {
					view.showError(Messages
							.getString("SimController.versionError")); //$NON-NLS-1$
				} catch (ClassNotFoundException ce) {
					view.showError(Messages
							.getString("SimController.fileError")); //$NON-NLS-1$
				}
			}
		}

		// The user chooses to save the current experiment from into a specific
		// file.
		// The file will contain the values that were added to run the
		// experiment.
		if (e.getActionCommand() == "Save") { //$NON-NLS-1$
			JFileChooser fc = new JFileChooser();
			// Modified Alberto Fern�ndez Sept-2011 : manage current directory
			// fc.setCurrentDirectory(new File("."));

			fc.setCurrentDirectory(new File(lastDirectory));
			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension(Messages.getString("SimController.extension")); //$NON-NLS-1$
			filter.setDescription(Messages.getString("SimController.fileType")); //$NON-NLS-1$
			fc.setFileFilter(filter);
			int returnVal = fc.showSaveDialog(view);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					String path = fc.getSelectedFile().getParent();
					lastDirectory = path; // Alberto Fern�ndez Sept-2011

					String name = fc.getSelectedFile().getName();

					if (!name.contains(Messages
							.getString("SimController.dotExtension")))name += Messages.getString("SimController.dotExtension"); //$NON-NLS-1$ //$NON-NLS-2$

					File file = new File(path, name);
					saveToObjects(file);
				} catch (FileNotFoundException fe) {
					view.showError(Messages
							.getString("SimController.fileError")); //$NON-NLS-1$
				} catch (IOException ioe) {
					view.showError(Messages
							.getString("SimController.fileError")); //$NON-NLS-1$
				}
			}
		}

		// The uses chooses to save his results into a spreadsheet.
		if (e.getActionCommand() == "Export") { //$NON-NLS-1$
			doExport();
		}

		// The user chooses to quit. The application closes.
		if (e.getActionCommand() == "Quit") { //$NON-NLS-1$
			System.exit(0);
		}

		if (e.getActionCommand().equals("SetCsc")) { //$NON-NLS-1$
			cscMode = !cscMode;
		}

		// The user chooses to change the default value of number of
		// combinations
		if (e.getActionCommand() == "Combinations") { //$NON-NLS-1$
			int n = view
					.getIntInput(
							Messages.getString("SimController.randomMessage"), "" + getModel().getCombinationNo()); //$NON-NLS-1$ //$NON-NLS-2$
			// Check if 'Cancel' was pressed
			if (n != -1) {
				getModel().setCombinationNo(n);
			}
		}

		// The user chooses to change the default value of number of
		// combinations
		if (e.getActionCommand() == "VarDistCombinations") { //$NON-NLS-1$
			int n = view
					.getIntInput(
							Messages.getString("SimController.randomMessage"), "" + getModel().getVariableCombinationNo()); //$NON-NLS-1$ //$NON-NLS-2$
			// Check if 'Cancel' was pressed
			if (n != -1) {
				getModel().setVariableCombinationNo(n);
			}
		}

        //Update max rpm
        if (e.getActionCommand() == "rpm") { //$NON-NLS-1$
            int n = view
                    .getIntInput(
                            Messages.getString("SimController.rpmMessage"), "" + getModel().getResponsesPerMinute()); //$NON-NLS-1$ //$NON-NLS-2$
            // Check if 'Cancel' was pressed
            if (n != -1) {
                getModel().setResponsesPerMinute(n);
            }
        }

		// The user chooses to change the default timestep size
		if (e.getActionCommand() == "timestep") { //$NON-NLS-1$
			Double n = view
					.getDoubleInput(
							Messages.getString("SimController.timeStepMessage"), "" + getModel().getTimestepSize()); //$NON-NLS-1$ //$NON-NLS-2$
			// Check if 'Cancel' was pressed
			if (n > 0) {
				getModel().setTimestepSize(n);
				view.updatePhasesColumnsWidth();
			}
		}

		// The user chooses to change the default value of response threshold
		if (e.getActionCommand() == "threshold") { //$NON-NLS-1$
			if (view.isSetResponse()) {
				double n = view
						.getDoubleInput(
								Messages.getString("SimController.responseMessage"), "" + getModel().getThreshold()); //$NON-NLS-1$ //$NON-NLS-2$
				// Check if 'Cancel' was pressed
				if (n != -1) {
					getModel().setThreshold(n);
				}
			}
			getModel().setShowResponse(view.isSetResponse());
		}

		// The user chooses to select/deselect setting the US across phases
		if (e.getActionCommand() == "SetUSAcrossPhases") { //$NON-NLS-1$
			view.setIsUSAcrossPhases(!(view.isUSAcrossPhases()));
			view.setStatusComponent(view.isUSAcrossPhases(),
					"SetUSAcrossPhases"); //$NON-NLS-1$

			if (view.isUSAcrossPhases())
				view.addUSPhases();
			else
				view.removeUSPhases();

			if (!view.isUSAcrossPhases()
					|| (view.isUSAcrossPhases() && getModel().getPhaseNo() == 1))
				view.getUSValuesTable().setAutoResizeMode(
						JTable.AUTO_RESIZE_ALL_COLUMNS);
			else {
				view.getUSValuesTable().setAutoResizeMode(
						JTable.AUTO_RESIZE_OFF);
				view.updateUSValuesColumnsWidth();
			}
		}

		// The user chooses to select/deselect setting the US across phases
		if (e.getActionCommand() == "SetContextAcrossPhases") { //$NON-NLS-1$
			getModel().setContextAcrossPhase(view.isOmegaAcrossPhases());
			view.toggleContext(view.isUseContext());
			getModel().setUseContext(view.isUseContext());
		}

		if (e.getActionCommand() == "SingleContext") { //$NON-NLS-1$
			getModel().setContextAcrossPhase(false);
			double n = view
					.getDoubleInput(
							Messages.getString("SimController.contextAlphaMessage"), "" + view.getContextAlpha()); //$NON-NLS-1$ //$NON-NLS-2$
			if (n > 0) {
				view.toggleContext(view.isUseContext());
				getModel().setUseContext(view.isUseContext());
				view.setOmegaSalience(n);
			}
		}

		// The user chooses to select/deselect the compound values of CS
		if (e.getActionCommand() == "SetCompound") { //$NON-NLS-1$
			view.setIsSetCompound(!(view.isSetCompound()));
			view.setStatusComponent(view.isSetCompound(), "SetCompound"); //$NON-NLS-1$
            if(view.isSetCompound()) {
                view.setIsSetConfiguralCompounds(true);
                view.setStatusComponent(view.isSetConfiguralCompounds(),
                        "SetConfiguralCompounds");
            }
		}

		// Added by Alberto Fern�ndez August-2011
		// The user chooses to select/deselect the configural compounds option
		if (e.getActionCommand() == "SetConfiguralCompounds") { //$NON-NLS-1$
			view.setIsSetConfiguralCompounds(!(view.isSetConfiguralCompounds()));
			view.setStatusComponent(view.isSetConfiguralCompounds(),
					"SetConfiguralCompounds"); //$NON-NLS-1$
		}

		// The user chooses to add a phase
		if (e.getActionCommand() == "addPhase") { //$NON-NLS-1$
			getModel().setPhaseNo(getModel().getPhaseNo() + 1);
			view.addPhase();
		}

		// The user chooses to remove the last phase
		if (e.getActionCommand() == "removePhase") { //$NON-NLS-1$
			if (getModel().getPhaseNo() > 1) {
				getModel().setPhaseNo(getModel().getPhaseNo() - 1);
				view.removePhase();
			}
		}

		// The user chooses to add a group
		if (e.getActionCommand() == "addGroup") { //$NON-NLS-1$
			getModel().setGroupNo(getModel().getGroupNo() + 1);
			view.addGroup();
		}

		// The user chooses to remove the last group
		if (e.getActionCommand() == "removeGroup") { //$NON-NLS-1$
			if (getModel().getGroupNo() > 1) {
				getModel().setGroupNo(getModel().getGroupNo() - 1);
				view.removeGroup();
			}
		}

		// The user chooses to read the guide
		if (e.getActionCommand() == "Guide") { //$NON-NLS-1$
			try {
				java.awt.Desktop
						.getDesktop()
						.browse(new URI(Messages.getString("SimController.guideURL"))); //$NON-NLS-1$
				// SimGuide simGuide = new SimGuide("User's Guide");
				// simGuide.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
				// simGuide.pack();
				// simGuide.setVisible(true);
			} catch (Exception ex) {
			}
		}

		// Update the context switch.
		if (e.getActionCommand() == "SetContext") { //$NON-NLS-1$
			view.toggleContext(view.isUseContext());
			getModel().setUseContext(view.isUseContext());
		}

		// The user chooses to read the information about the Simulator
		if (e.getActionCommand() == "About") { //$NON-NLS-1$
			view.showAboutLogo("/Extras/R&W_About");//("/Extras/logo5-final.jpg");E.Mondragon 30 Sept 2011 //$NON-NLS-1$
		}

		// The user presses the 'Set Variables' button which will set initial
		// values for the variables.
		if (e.getActionCommand() == "setVariables") { //$NON-NLS-1$
			doSetVariables();
		}

		// The user presses the 'Clear All' button which clears all the values
		// from the table.
		if (e.getActionCommand() == "clearAll") { //$NON-NLS-1$
			clearModel(getModel().getGroupNo(), getModel().getPhaseNo(),
					getModel().getCombinationNo());
			getModel().setUseContext(view.isUseContext());
			VariableDistribution.newRandomSeed();
		}

		// The user presses the 'Run' button which updates the model with the
		// values and runs the algorithm
		if (e.getActionCommand() == "run") { //$NON-NLS-1$
            doSetVariables();
			// check again the model and also the values
			if (checkModelTable()) {
				// update the CS and US tables following the model
				view.getCSValuesTableModel().setValuesTable(false);
				view.getUSValuesTableModel().setValuesTable(false,
						view.isUSAcrossPhases());
				if (!view.isUSAcrossPhases()
						|| (view.isUSAcrossPhases() && getModel().getPhaseNo() == 1))
					view.getUSValuesTable().setAutoResizeMode(
							JTable.AUTO_RESIZE_ALL_COLUMNS);
				else {
					view.getUSValuesTable().setAutoResizeMode(
							JTable.AUTO_RESIZE_OFF);
					view.updateUSValuesColumnsWidth();
				}

				// check the CS and US values
				if (checkCSValuesTable() && checkUSValuesTable()
						&& checkOtherValuesTable()) {
					view.getGlassPane().setCursor(
							Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					view.getGlassPane().setVisible(true);

					// Update values from the CS view to the model
					AbstractTableModel cstm = view.getCSValuesTableModel();
					for (int i = 0; i < cstm.getRowCount(); i++)
						for (int j = 1; j < cstm.getColumnCount(); j++) {
							getModel().updateValues(
									(String) cstm.getValueAt(i, 0), j,
									(String) cstm.getValueAt(i, j));
						}
					// Update values from the US view to the model
					AbstractTableModel ustm = view.getUSValuesTableModel();
					for (int i = 0; i < ustm.getRowCount(); i++)
						for (int j = 1; j <= getModel().getPhaseNo(); j++) {
							if (!view.isUSAcrossPhases()) {
								getModel().updateValues(
										(String) ustm.getValueAt(i, 0), j,
										(String) ustm.getValueAt(i, 1));
							} else {
								getModel().updateValues(
										(String) ustm.getValueAt(i, 0), j,
										(String) ustm.getValueAt(i, j));
							}
						}
					// Update other values (gamma, delta, omega)
					// Update values from the US view to the model
					AbstractTableModel otm = view.getOtherValuesTableModel();
					for (int i = 0; i < otm.getRowCount(); i++)
						for (int j = 1; j <= getModel().getPhaseNo(); j++) {
							getModel().updateValues(
									(String) otm.getValueAt(i, 0), j,
									(String) otm.getValueAt(i, 1));
						}

					// update CS and US values on all the groups
					getModel().updateValuesOnGroups();
					// Update the context switch.
					getModel().setUseContext(view.isUseContext());
					getModel()
							.setContextAcrossPhase(view.isOmegaAcrossPhases());
					// execute the algorithm
					/*
					 * model.startCalculations();
					 * 
					 * view.setStatusComponent(true, "dispGraph");
					 * view.setOutput
					 * (model.textOutput(view.getIsSetCompound()));
					 * view.getGlassPane
					 * ().setCursor(Cursor.getPredefinedCursor(Cursor
					 * .DEFAULT_CURSOR)); view.getGlassPane().setVisible(false);
					 * view.setStatusComponent(true, "Export");
					 * view.setStatusComponent(true, "Save");
					 */
					getModel().setCSC(cscMode);
					task = new RunTask();
					task.addPropertyChangeListener(this);
					task.execute();
				}
			}
		}

		// The user presses the 'Display Graph' button which displays the graphs
		// with the current results
		if (e.getActionCommand() == "dispGraph") { //$NON-NLS-1$
			// SimGraph.clearDefaults();
			view.getGlassPane().setCursor(
					Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			view.getGlassPane().setVisible(true);
			for (int i = 0; i < getModel().getPhaseNo(); i++) {

                Object[] messageArguments = {
                        (i + 1)
                };

                //Messages.applyPattern(Messages.getString("SimController.meanCSGraphTitle"));
                //String output = formatter.format(messageArguments);

				SimGraph simGraph = new SimGraph(Messages.format("SimController.meanCSGraphTitle",messageArguments), getModel(), i, view.isSetCompound()); //$NON-NLS-1$

				simGraph.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				simGraph.pack();
				simGraph.setLocation(50 + i * 20, 50 + i * 20);
				simGraph.setVisible(true);

				ImageIcon icon = createImageIcon("/simulator/extras/TD.png", ""); //$NON-NLS-1$ //$NON-NLS-2$
				simGraph.setIconImage(icon.getImage());

				if (getModel().showResponse() && getModel().isCSC()) {

					SimGraph respGraph = new ResponseGraph(Messages.format("SimController.responseGraphTitle",messageArguments), getModel(), i, view.isSetCompound()); //$NON-NLS-1$

					respGraph
							.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					respGraph.pack();
					respGraph.setLocation(60 + i * 20, 50 + i * 20);
					respGraph.setVisible(true);

					respGraph.setIconImage(icon.getImage());

					SimGraph avgRespGraph = new AverageResponseGraph(Messages.format("SimController.avgResponseGraphTitle",messageArguments), getModel(), i, view.isSetCompound()); //$NON-NLS-1$

					avgRespGraph
							.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					avgRespGraph.pack();
					avgRespGraph.setLocation(60 + i * 20, 50 + i * 20);
					avgRespGraph.setVisible(true);

					avgRespGraph.setIconImage(icon.getImage());
					avgRespGraph
							.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				}

				if (getModel().isCSC()) {

					SimGraph componentGraph = new ComponentGraph(Messages.format("SimController.componentGraphTitle",messageArguments), getModel(), i, view.isSetCompound()); //$NON-NLS-1$

					componentGraph
							.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					componentGraph.pack();
					componentGraph.setLocation(70 + i * 20, 50 + i * 20);
					componentGraph.setVisible(true);

					componentGraph.setIconImage(icon.getImage());
				}

			}
			view.getGlassPane().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			view.getGlassPane().setVisible(false);
		}

	}

	/**
	 * 
	 */
	private void doSetVariables() {
		view.setStatusComponent(false, "dispGraph"); //$NON-NLS-1$
		view.setOutput("");
		view.setStatusComponent(false, "Export"); //$NON-NLS-1$
		view.setStatusComponent(false, "Save"); //$NON-NLS-1$
		if (checkModelTable()) {
			view.setStatusComponent(true, "run"); //$NON-NLS-1$
			view.getCSValuesTableModel().setValuesTable(false);
			view.getUSValuesTableModel().setValuesTable(false,
					view.isUSAcrossPhases());
			view.getOtherValuesTableModel().setValuesTable(false,
					view.isOmegaAcrossPhases());
			if (!view.isUSAcrossPhases()
					|| (view.isUSAcrossPhases() && getModel().getPhaseNo() == 1)) {
				view.getUSValuesTable().setAutoResizeMode(
						JTable.AUTO_RESIZE_ALL_COLUMNS);
			} else {
				view.getUSValuesTable().setAutoResizeMode(
						JTable.AUTO_RESIZE_OFF);
				view.updateUSValuesColumnsWidth();
			}
		}
		
	}

	/**
	 * 
	 */
	private void doExport() {
		// Alberto Fernandez Sept-2011
					// new SimExport(view, model);
					String[] dir = new String[1];
					dir[0] = lastDirectory;
					// Choose a file to store the values.
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(new File(dir[0])); // AF Sept-2011
					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("xlsx");
					filter.setDescription("Spreadsheet");
					fc.setFileFilter(filter);
					int returnVal = fc.showSaveDialog(view);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						String path = fc.getSelectedFile().getParent();
						dir[0] = path; // Alberto Fern�ndez Sept-2011
						String name = fc.getSelectedFile().getName();
						if (!name.contains(".xlsx"))
							name += ".xlsx"; // Alberto Fernandez: changed from xls ->
												// xlsx
						boolean okToContinue = true;
						File file = new File(path, name);
						if (file.exists()) {
							int response = JOptionPane.showConfirmDialog(null,
									"Overwrite existing file?", "Confirm Overwrite",
									JOptionPane.OK_CANCEL_OPTION,
									JOptionPane.QUESTION_MESSAGE);
							if (response == JOptionPane.CANCEL_OPTION) {
								okToContinue = false;
							}

						}
						if (okToContinue) {
							exporter = new SimExport(view, getModel(), name, file);
							exporterTask = new ExportTask();
							exporterTask.addPropertyChangeListener(this);
							exporterTask.execute();
						}
					}
					lastDirectory = dir[0];
		
	}

	/**
	 * Returns true if the ValuesTable has been checked successfully
	 */
	private boolean checkCSValuesTable() {
		boolean cont = true;
		AbstractTableModel tm = view.getCSValuesTableModel();

		// First check that table contains legal values.
		for (int row = 0; row < tm.getRowCount(); row++) {
			// Checking values
			String tmp = (String) tm.getValueAt(row, 1);

			// NOT EMPTY ALPHA FIELD
			if (tmp.length() > 0) {
				try {
					new Double(tmp);
				} catch (Exception ex) {
					cont = false;
					view.showError(Messages
							.getString("SimController.entryWrongMessage")); //$NON-NLS-1$
					break;
				}
			}
			// EMPTY ALPHA FIELD
			else {
				cont = false;
				view.showError(Messages
						.getString("SimController.alphaEmptyMessage")); //$NON-NLS-1$
				break;
			}
		}
		return cont;
	}

	/**
	 * Returns true if the ModelTable has been checked successfully
	 */
	private boolean checkModelTable() {

		boolean cont = true;
		// Get the experiment's model table so we can process the information.
		AbstractTableModel tm = view.getPhasesTableModel();

		// First check that table contains legal values.
		for (int row = 0; row < getModel().getGroupNo(); row++) {
			// Checking group names
			if (((String) tm.getValueAt(row, 0)).length() == 0) {
				cont = false;
				view.showError(Messages
						.format("SimController.groupNameInRow", row + 1)); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
			// Checking phases values
			for (int col = 1; col < getModel().getPhaseNo() + 1; col++) {
				String tmp = (String) tm.getValueAt(row, 5 * col - 4);
				if (tmp.length() == 0) {
					view.showAbout(Messages
							.format("SimController.phaseWarningOne", col, (String) tm.getValueAt(row, 0))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					tm.setValueAt("0", row, 5 * col - 4); //$NON-NLS-1$
				}

				// Checking onsets are set and in start state. J Gray
				TimingConfiguration tmpOnset = (TimingConfiguration) tm
						.getValueAt(row, 5 * col - 1);
				tmpOnset.getDurations().setType(view.getDistributionType());
				tmpOnset.getDurations().setGeo(view.getMeanType());
				tmpOnset.reset();
				tmpOnset.checkFilled(tmp, view.timingPerTrial());
				if (tmpOnset.hasZeroDurations()) {
					view.showAbout(Messages
							.format("SimController.durationWarning", col, (String) tm.getValueAt(row, 0))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (!tmpOnset.isConfigured()) {
					view.showAbout(Messages
							.format("SimController.timingWarningOne", col, (String) tm.getValueAt(row, 0))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					cont = false;
				}

				ITIConfig iti = (ITIConfig) tm.getValueAt(row, 5 * col);
				iti.setType(view.getDistributionType());
				iti.setGeo(view.getMeanType());
			}
		}
		if (cont) {
			getModel().reinitialize();
			for (int row = 0; row < getModel().getGroupNo(); row++) {
				String gName = (String) tm.getValueAt(row, 0); // first column
																// is the
																// group's name
				// Create a new group for every row of the table.
				SimGroup sg = new SimGroup(gName, getModel().getPhaseNo(),
						getModel().getCombinationNo(), getModel());
				for (int c = 1; c < getModel().getPhaseNo() + 1; c++) {
					boolean isRandom = false;
					boolean success = false;
					getModel().setIsGeometricMean(view.getMeanType());
					getModel().setIsExponential(view.isExponential());
					getModel().setContextAlpha(view.getContextAlpha());
					getModel().setTraceType(view.getTraceType());
					getModel().setTimingPerTrial(view.timingPerTrial());
					getModel().setZeroTraces(view.getZeroTraces());
					getModel().setSerialConfigurals(
							view.isSetConfiguralCompounds()
									&& view.isUseSerialCompounds());
					getModel()
							.setSerialCompounds(
									view.isUseSerialCompounds()
											&& view.isSetCompound());
                    getModel().setConfiguralCompounds(view
                            .isSetConfiguralCompounds());
					// If context is off, use the empty context for this
					// simulation.
					ContextConfig context = !getModel().isUseContext() ? ContextConfig.EMPTY
							: (ContextConfig) tm.getValueAt(row, c * 5 - 3);
					String trialString = (String) tm.getValueAt(row, c * 5 - 4);
					// Remove whitespace
					trialString = trialString.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
					// Get whether the phase is random
					isRandom = ((Boolean) tm.getValueAt(row, c * 5 - 2))
							.booleanValue();
					success = sg
							.addPhase(trialString, isRandom, c, view
									.isSetConfiguralCompounds(),
									(TreeMap<String, String>) getModel()
											.getConfigCuesNames(),
									(TimingConfiguration) tm.getValueAt(row,
											c * 5 - 1), (ITIConfig) tm
											.getValueAt(row, c * 5), context);

					if (!success) { // Modified Alberto Fern�ndez August-2011
									// Modified J Gray Dec-2011
						view.showError(Messages
								.format("SimController.phase", c)); //$NON-NLS-1$ //$NON-NLS-2$
						cont = false;
						break;
					}
				}
				if (cont)
					getModel().addGroupIntoMap(gName, sg);
			}
			getModel().addValuesIntoMap();
		}
		return cont;
	}

	/**
	 * Returns true if the OtherValuesTable has been checked successfully
	 */
	private boolean checkOtherValuesTable() {
		boolean cont = true;
		AbstractTableModel tm = view.getOtherValuesTableModel();

		// First check that table contains legal values.
		for (int row = 0; row < tm.getRowCount(); row++) {
			for (int col = 1; col < tm.getColumnCount(); col++) {
				String tmp = (String) tm.getValueAt(row, col);

				// PHASE 1 IS EMPTY
				if (col == 1 && tmp.length() == 0) {
					cont = false;
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < tm.getRowCount(); i++) {
						sb.append((String) tm.getValueAt(i, 0));
						sb.append(", "); //$NON-NLS-1$
					}
					sb.append(Messages
							.getString("SimController.otherEmptyMessage")); //$NON-NLS-1$
					view.showError(sb.toString());
					break;
				}
				// NOT EMPTY FIELD
				if (tmp.length() > 0) {
					// NOT EMPTY FIELD
					try {
						new Double(tmp);
					} catch (Exception ex) {
						cont = false;
						view.showError(Messages
								.getString("SimController.entryWrongMessage")); //$NON-NLS-1$
						break;
					}
				}
			}
			if (!cont)
				break;
		}
		return cont;
	}

	/**
	 * Returns true if the ValuesTable has been checked successfully
	 */
	private boolean checkUSValuesTable() {
		boolean cont = true;
		AbstractTableModel tm = view.getUSValuesTableModel();

		// First check that table contains legal values.
		for (int row = 0; row < tm.getRowCount(); row++) {
			for (int col = 1; col < tm.getColumnCount(); col++) {
				String tmp = (String) tm.getValueAt(row, col);

				// PHASE 1 IS EMPTY
				if (col == 1
						&& tmp.length() == 0
						&& !tm.getValueAt(row, 0).equals(
								Messages.getString("SimController.84"))) { //$NON-NLS-1$
					cont = false;
					view.showError(Messages
							.getString("SimController.USEmptyMessage")); //$NON-NLS-1$
					break;
				}
				// NOT EMPTY FIELD
				if (tmp.length() > 0) {
					try {
						new Double(tmp);
					} catch (Exception ex) {
						cont = false;
						view.showError(Messages
								.getString("SimController.entryWrongMessage")); //$NON-NLS-1$
						break;
					}
				}
			}
			if (!cont)
				break;
		}
		return cont;
	}

	/**
	 * Clears up the tables from any values that they may contain and also
	 * re-initiate the initial status on the menuitems and buttons.
	 * 
	 * @param g
	 *            the previous number of groups.
	 * @param p
	 *            the previous number of phases.
	 * @param c
	 *            the previous number of combination.
	 */
	private void clearModel(int g, int p, int c) {
		ITIConfig.resetDefaults();
		TimingConfiguration.clearDefaults();
		// Distributions.resetSeed();
		// VariableDistribution.resetSeed();
		view.reset();
		view.clearHidden();
		setModel(new SimModel());
		view.updateModel(getModel());

		getModel().setGroupNo(g);
		getModel().setPhaseNo(p);
		getModel().setCombinationNo(c);

		view.getPhasesTableModel().setPhasesTable();
		view.updatePhasesColumnsWidth();

		view.getCSValuesTableModel().setValuesTable(true);

		view.getUSValuesTableModel().setInitialValuesTable();
		if (view.isUSAcrossPhases())
			view.addUSPhases();

		if (!view.isUSAcrossPhases()
				|| (view.isUSAcrossPhases() && getModel().getPhaseNo() == 1))
			view.getUSValuesTable().setAutoResizeMode(
					JTable.AUTO_RESIZE_ALL_COLUMNS);
		else {
			view.getUSValuesTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			view.updateUSValuesColumnsWidth();
		}
		view.getPhasesTable().getColumnModel().getColumn(1).setCellEditor(new TrialStringEditor());

	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	// E. Mondragon 28 Sept 2011

	private ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = this.getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} // else ;
		System.err.println(Messages.getString("SimController.fnfError") + path); //$NON-NLS-1$
		return null;
	}

	/**
	 * @return the model
	 */
	public SimModel getModel() {
		return model;
	}

	/**
	 * Helper function for loading a td experiment configuration serialized to a
	 * tdl file.
	 * 
	 * @param file
	 *            to load.
	 * @throws IOException
	 *             if there's a problem loading the file.
	 * @throws VersionException
	 *             if the file is of an incompatible version.
	 * @throws ClassNotFoundException
	 *             if an object can't be unflattened.
	 */

	private void loadObjects(final File file) throws IOException,
			VersionException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
		double version = in.readDouble();
		if (version < 0.9) {
			throw new VersionException();
		}
		clearModel(in.readInt(), in.readInt(), in.readInt());
		view.setIsUSAcrossPhases(in.readBoolean());
		view.setIsSetConfiguralCompounds(in.readBoolean());
		view.setStatusComponent(view.isSetConfiguralCompounds(),
				"SetConfiguralCompounds"); //$NON-NLS-1$
		getModel().setTimestepSize(in.readDouble());
		getModel().setThreshold(in.readDouble());
		getModel().setShowResponse(in.readBoolean());
		view.setStatusComponent(getModel().showResponse(), "threshold"); //$NON-NLS-1$
		// Set the context configuration.
		boolean context = true;
		if (in.readBoolean()) {
			in.readBoolean();
			view.setStatusComponent(true, "SingleContext"); //$NON-NLS-1$
		} else if (!in.readBoolean()) {
			view.setStatusComponent(true, "SetContext"); //$NON-NLS-1$
			context = false;
		} else {
			view.setStatusComponent(true, "SetContextAcrossPhases"); //$NON-NLS-1$
		}
		double omegaAlpha = in.readDouble();
		view.setOmegaSalience(omegaAlpha);
		getModel().setUseContext(context);
		view.setIsSetCompound(in.readBoolean());
		view.setStatusComponent(in.readBoolean(), "geo"); //$NON-NLS-1$
		view.setStatusComponent(in.readBoolean(), "exp"); //$NON-NLS-1$
		if (version == 0.9) {
			view.setTraceType(in.readBoolean() ? Trace.BOUNDED
					: Trace.ACCUMULATING);
		} else if (version < 0.97) {
			Trace trace = Trace.fromOld((simulator.SimCue.Trace) in
					.readObject());
			view.setTraceType(trace);
		} else {
			view.setTraceType((Trace) in.readObject());
		}
		if (version > 0.95) {
			getModel().setVariableCombinationNo(in.readInt());
		}
		view.clearHidden();
		Vector phasesModel = (Vector) in.readObject();
		
		view.getCSValuesTableModel().setData((Vector) in.readObject());
		view.getUSValuesTableModel().setData((Vector) in.readObject());
		view.getOtherValuesTableModel().setData((Vector) in.readObject());
		VariableDistribution.newRandomSeed();

		if (version > 0.96) {
			// Per trial timings
			boolean on = in.readBoolean();
			getModel().setTimingPerTrial(on);
			view.setTimingPerTrial(on);

			// Serial compounds & configurals
			view.setUseSerialCompounds(in.readBoolean());
			// Zero traces per trial
			view.setZeroTraces(in.readBoolean());
			if (version > 0.98) {
				view.setRestrictPredictions(in.readBoolean());
			}
		}
		view.getPhasesTableModel().setData(phasesModel);
		view.getPhasesTable().createDefaultColumnsFromModel();
		view.getPhasesTable().getColumnModel().getColumn(1).setCellEditor(new TrialStringEditor());
		view.updatePhasesColumnsWidth();
		view.toggleContext(context);
		in.close();
		view.updateTimingConfigs();
	}

	/**
	 * Clears up the tables from any values that they may contain and also
	 * re-initiate the initial status on the menuitems and buttons.
	 */
	private void newModel() {
		ITIConfig.resetDefaults();
		TimingConfiguration.clearDefaults();
		// Distributions.resetSeed();
		// VariableDistribution.resetSeed();
		view.reset();
		view.clearHidden();
		setModel(new SimModel());
		view.updateModel(getModel());

		view.getPhasesTableModel().setPhasesTable();
		view.updatePhasesColumnsWidth();

		view.getCSValuesTableModel().setValuesTable(true);

		view.getUSValuesTableModel().setInitialValuesTable();
		view.getUSValuesTable().setAutoResizeMode(
				JTable.AUTO_RESIZE_ALL_COLUMNS);
		view.getPhasesTable().getColumnModel().getColumn(1).setCellEditor(new TrialStringEditor());
		
	}

	/**
	 * Invoked when task's progress property changes.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) { //$NON-NLS-1$
		// int progress = (Integer) evt.getNewValue();
		// progressMonitor.setProgress(progress);
		// long remaining = (long)
		// (control.getEstimatedCycleTime()*(totalProgress-control.getProgress()));
		// String timeRemaining = String.format("%d min, %d sec",
		// TimeUnit.MILLISECONDS.toMinutes(remaining),
		// TimeUnit.MILLISECONDS.toSeconds(remaining) -
		// TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remaining))
		// );
		//            String message = timeRemaining; //$NON-NLS-1$
		// progressMonitor.setNote(message);
			if (progressMonitor.isCanceled() || task.isDone()) {
				if (progressMonitor.isCanceled()) {
					task.cancel(true);
					control.setCancelled(true);
				}
			}
		}

	}

	/**
	 * Helper function for saving a simulation configuration to a tdl file.
	 * 
	 * @param file
	 *            to save to.
	 * @throws IOException
	 *             if there's a problem reading the file.
	 */

	private void saveToObjects(final File file) throws IOException {
		boolean okToContinue = true;
		if (file.exists()) {
			int response = JOptionPane
					.showConfirmDialog(
							null,
							Messages.getString("SimController.overwrite"), Messages.getString("SimController.confirmOverwrite"), //$NON-NLS-1$ //$NON-NLS-2$
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) {
				okToContinue = false;
			}
		}
		if (okToContinue) {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(file));
			// Save file version
			out.writeDouble(Simulator.VERSION);
			// Number of groups
			out.writeInt(getModel().getGroupNo());
			// Number of phases
			out.writeInt(getModel().getPhaseNo());
			// Number of combinations for randomness
			out.writeInt(getModel().getCombinationNo());
			// Whether US is across phases
			out.writeBoolean(view.isUSAcrossPhases());
			// Alberto Fernandez August-2011, whether configurals are used
			out.writeBoolean(!getModel().getConfigCuesNames().isEmpty());
			// Timestep length
			out.writeDouble(getModel().getTimestepSize());
			// Response threshold
			out.writeDouble(getModel().getThreshold());
			// Whether decision rule sim is enabled
			out.writeBoolean(getModel().showResponse());
			// Whether single context is set
			out.writeBoolean(getModel().isUseContext()
					&& !getModel().contextAcrossPhase());
			// Whether no context is set
			out.writeBoolean(getModel().isUseContext());
			// Default context salience
			out.writeDouble(view.getContextAlpha());
			// Whether compounds are on
			out.writeBoolean(view.isSetCompound());
			// Mean type is geometric
			out.writeBoolean(getModel().isGeometricMean());
			// Variable distribution
			out.writeBoolean(getModel().isExponential());
			// Trace type
			out.writeObject(getModel().getTraceType());
			// Random combos for variable durations
			out.writeInt(getModel().getVariableCombinationNo());
			ValuesTableModel tmp = view.getPhasesTableModel();
			out.writeObject(tmp.getData());

			ValuesTableModel tmv = view.getCSValuesTableModel();
			out.writeObject(tmv.getData());

			ValuesTableModel ustmv = view.getUSValuesTableModel();
			out.writeObject(ustmv.getData());

			ValuesTableModel otmv = view.getOtherValuesTableModel();
			out.writeObject(otmv.getData());
			// Per trial timings
			out.writeBoolean(getModel().isTimingPerTrial());
			// Serial compounds & configurals
			out.writeBoolean(getModel().isSerialCompounds()
					|| getModel().isSerialConfigurals());
			// Zero traces per trial
			out.writeBoolean(getModel().isZeroTraces());
			// Restrict predictions
			out.writeBoolean(getModel().isRestrictPredictions());
			out.close();
		}
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(SimModel model) {
		this.model = model;
	}

    /**
     * Nasty hack, to let a timing config check if it is should
     * be updating.
     * @param conf
     * @return
     */

    public boolean isReferenced(TimingConfiguration conf) {
        try {
            int col = SimView.activeCol + 3;
            TimingConfiguration target = (TimingConfiguration) view.getPhasesTableModel().getValueAt(SimView.activeRow, col);
            return conf.equals(target);
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

}
