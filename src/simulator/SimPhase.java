/**
 * SimPhase.java
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
 * Modified in December-2011
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 *
 */
package simulator;

import java.util.*;
import java.util.Map.Entry;

import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import extra166y.Ops;
import extra166y.Ops.DoubleOp;
import extra166y.ParallelDoubleArray;

/**
 * SimPhases is the class which models and processes a phase from the
 * experiment. It will process the sequence of stimuli and give results as
 * requested from the controller.
 */
public class SimPhase {

	/** Group this phase is for. **/
	private SimGroup group;
	/** Trial sequence. **/
	private List<Trial> orderedSeq;
	/** Stimuli mappings. **/
	protected Map<String, SimStimulus> stimuli;
	// Modified by J Gray, replace a cue with a CSC and hence a list of cues
	/** CSC cue maps. **/
	private Map<String, CueList> cues;
	/** Results map. **/
	protected Map<String, CueList> results;
	/** Probe results. **/
	private Map<String, CueList> probeResults;
	/** Set of CSs that have been in this trial. **/
	private Set<CS> presentCS;

	private String initialSeq;
	/** Number of trials to run. **/
	protected int trials;
	/** Whether to use a random ordering of trials. **/
	private boolean random;
	/** TD parameters. **/
	private Double lambdaPlus, lambdaMinus, betaPlus, betaMinus, gamma, delta;
	/** The current prediction of the US. **/
	protected double prediction;
	/** The previous prediction of the US. **/
	protected double lastPrediction;
	/** Counter for maximum trial duration. **/
	protected int maxMaxOnset;
	/** ITI configuration. **/
	protected ITIConfig itis;
	/** Salience of contextual stimulus **/
	protected double bgSalience;
	/** Timing configuration. **/
	private TimingConfiguration timingConfig;
	/** Context configuration. **/
	private ContextConfig contextCfg;
	/** Operation for adding together two double arrays. **/
	final Ops.BinaryDoubleOp addWeights = new Ops.BinaryDoubleOp() {
		@Override
		public double op(final double first, final double second) {
			return first + second;
		}
	};

	/**
	 * Message passing object to update progress in the GUI & fast-cancel the
	 * sim.
	 **/
	private volatile ModelControl control;

	/**
	 * SimPhase's Constructor method
	 * 
	 * @param seq
	 *            the sequence as it has been given by the user.
	 * @param order
	 *            the same sequence processed into an ArrayList.
	 * @param stimuli2
	 *            the stimuli that the sequence contains.
	 * @param sg
	 *            the parent group that this phase is belong to.
	 * @param random
	 *            if the phase must be executed in a random way
	 * @param onsetConfig
	 *            onsets configuration object
	 */
	public SimPhase(String seq, List<Trial> order,
			Map<String, SimStimulus> stimuli2, SimGroup sg, boolean random,
			TimingConfiguration timing, ITIConfig iti, ContextConfig context) {

		results = new TreeMap<String, CueList>();
		probeResults = new TreeMap<String, CueList>();
		initialSeq = seq;
		stimuli = stimuli2;
		orderedSeq = order;
		group = sg;
		this.random = random;
		this.trials = orderedSeq.size();
		this.cues = group.getCuesMap();
		for (Entry<String, CueList> entry : group.getCuesMap().entrySet()) {
			if (seq.contains(entry.getKey())) {
				cues.put(entry.getKey(), entry.getValue());
			}
		}
		lambdaPlus = 0.0;
		betaPlus = 0.0;
		lambdaMinus = 0.0;
		betaMinus = 0.0;
		delta = 0.0;
		bgSalience = 0.0;
		// Added to allow for variable distributions of onsets - J Gray
		// Added to control the onset of fixed onset stimulu
		timingConfig = timing;
		// timingConfig.setTrials(trials);
		iti.setTrials(trials);
		lastPrediction = prediction = 0.0f;
		maxMaxOnset = 0;
		// Added to allow ITI modelling.
		itis = iti;
		// progress = 0;
		// Added to make use of contexts per phase/group
		contextCfg = context;
		setPresentCS(new HashSet<CS>());
	}

	/**
	 * The TD algorithm.
	 * 
	 * @param sequence
	 *            list of trial strings in order
	 * @param tempRes
	 *            Map to populate with results
	 * @param probeResults2
	 */

	protected void algorithm(List<Trial> sequence,
			Map<String, CueList> tempRes, boolean context,
			Map<String, CueList> probeResults2) {
		// Map cues to iterators of cues.
		Map<CS, CueList> tempMap;
		Map<CS, Queue<SimCue>> cueLog = new HashMap<CS, Queue<SimCue>>();
		HashSet<Trial> uniqSeq = new HashSet<Trial>();
		uniqSeq.addAll(sequence);
		Set<String> inThisRun = new HashSet<String>();
		List<CS> csActiveLastStep = new ArrayList<CS>();
		List<SimCue> activeLastStep = new ArrayList<SimCue>();
		if (context) {
			// Set the alpha on the context we're using here
			tempRes.get(contextCfg.getContext().toString()).setAlpha(
					contextCfg.getAlpha());
		}
		List<SimCue> activeList = new ArrayList<SimCue>();

		// Trials loop
        Set<String> csActiveThisTrial = new HashSet<String>();
        Set<String> probeCSActiveThisTrial = new HashSet<String>();

		for (int i = 1; i <= trials && !control.isCancelled(); i++) {
            csActiveThisTrial.clear();
            probeCSActiveThisTrial.clear();
            long count = System.currentTimeMillis();

			cueLog.clear();
			lastPrediction = 0d;// new Double(0);
			String curNameSt = sequence.get(i - 1).toString();
			SimStimulus currentSt = stimuli.get(curNameSt);
			tempMap = new HashMap<CS, CueList>();

			Trial trial = sequence.get(i - 1).copy();
			getPresentCS().addAll(trial.getCues());

			// Get the stimuli present this trial, copy them to the temporary
			// map
			// and collect their onsets.
			for (CS cs : trial.getCues()) {
				try {
					tempRes.get(cs.getName()).restart();
				} catch (NullPointerException e) {
				}
				tempMap.put(cs, tempRes.get(cs.getName()));
				cueLog.put(cs, new LinkedList<SimCue>());
				if (Simulator.getController().getModel().isZeroTraces()) {
					tempMap.get(cs).zeroTraces();
				}
			}

			int iti = (int) Math.round(itis.next()
					/ Simulator.getController().getModel().getTimestepSize());

			// Produce actual timings for this trial
			Map<CS, int[]> timings = timingConfig.makeTimings(tempMap.keySet());
			int trialLength = timings.get(CS.TOTAL)[1];

			// Serial configurals & compounds
			if (Simulator.getController().getModel().isSerialConfigurals()
					|| Simulator.getController().getModel().isSerialCompounds()) {
				// Identify the sequences in this trial
				Set<CS> set = new HashSet<CS>(trial.getCues());
				Map<List<List<CS>>, int[]> serial = timingConfig.sequence(set,
                        timingConfig.makeTimingsBasedOnMean(tempMap.keySet()));

				for (Entry<List<List<CS>>, int[]> entry : serial.entrySet()) {
					String realName = "";
					for (int m = 0; m < entry.getKey().size(); m++) {
						for (CS c : entry.getKey().get(m)) {
							realName += c;
						}
						if (m < entry.getKey().size() - 1) {
							realName += ConfiguralCS.SERIAL_SEP;
						}

					}

					CS configural = new CS(realName, 0, 0);

					// Inject into this trial.

					CueList compound = tempRes.containsKey(realName) ? tempRes
							.get(realName) : new CompoundCueList(realName, 0d,
							group.getModel().getThreshold(), group.getModel()
									.getDecay(), group.getModel()
									.getTraceType(), group.getModel()
									.getDecisionRule());
					tempRes.put(realName, compound);

					tempRes.get(configural.getName()).restart();
					tempMap.put(configural, tempRes.get(configural.getName()));
					if (Simulator.getController().getModel().isZeroTraces()) {
						tempMap.get(configural).zeroTraces();
					}
					getPresentCS().add(configural);
					trial.getCues().add(configural);
					currentSt.addPart(configural);
					CS timingCS = new ConfiguralCS(realName, 0, 0, realName,
							true);
					timings.put(timingCS, entry.getValue());
					if (Simulator.getController().getModel()
							.isSerialConfigurals()) {
						String serialConfigural = SimGroup.getKeyByValue(group
								.getModel().getConfigCuesNames(), realName);
						// Update the name of the compound to include this
						// configural
						String newName = compound.getSymbol()
								+ serialConfigural;
						tempRes.remove(realName);
						tempMap.remove(configural);
						cues.remove(realName);
						compound.setSymbol(newName);
						configural.setName(newName);
						timingCS.setName(newName);
						if (!tempRes.containsKey(newName)) {
							tempRes.put(newName, compound);
						}
						tempMap.put(configural, tempRes.get(newName));

						// Then make the configural
						configural = new ConfiguralCS(serialConfigural, 0, 0,
								realName, true);
                        try {
						    trial.getCues().add(configural);
                        } catch (NullPointerException e) {
                            System.err.println("Tried to add a null cue?");
                        }

						// Inject into this trial.

						tempRes.get(configural.getName()).restart();
						tempMap.put(configural,
								tempRes.get(configural.getName()));
						if (Simulator.getController().getModel().isZeroTraces()) {
							tempMap.get(configural).zeroTraces();
						}
						getPresentCS().add(configural);
						trial.getCues().add(configural);
						timings.put(configural, entry.getValue());
						currentSt.addPart(configural);
					}
				}
			}

			if (i <= trials) {
				maxMaxOnset = Math
						.max(maxMaxOnset, timings.get(CS.CS_TOTAL)[1]);
			}

			// Timesteps loop within each trial.

			// Run through all the timesteps, duration of the trial is the total
			// period
			// returned by the timings generator.
			for (int j = 0; j < (trialLength + iti) && !control.isCancelled(); j++) {
				activeList.clear();
				// Ready to update prediction
				prediction = 0d;// new Double(0);
				if (context) {
                    //Changed this to make the context last as long as the CSs, even
                    //when doing backwards conditioning
                    int csTime = timings.get(CS.CS_TOTAL)[1] - timings.get(CS.CS_TOTAL)[0];
					if (csTime - 1 == tempMap.get(
							contextCfg.getCS()).getIndex()) {
						tempMap.get(contextCfg.getCS()).restart();
                        try {
                            probeResults2.get(contextCfg.getCS()+trial.getProbeSymbol()).restart();
                        } catch (NullPointerException e) {
                            //Not a probe
                        }
					}
				}

				// Preloop to reset CSs that ended last timestep
				// for repeated CS serial compounds

				for (CS cs : trial.getCues()) {
					// Timing range for this CS
					int onset = 0;
					int offset = 0;
					if (cs.isSerialConfigural() || !cs.isConfigural()
							&& !Context.isContext(cs.getName())) {
						onset = timings.get(cs)[0];
						offset = timings.get(cs)[1];
					}
					if (!Context.isContext(cs + "")
							&& (cs.isSerialConfigural() || !cs.isConfigural())
							&& ((j == offset)) && !cs.isCompound()) {
						tempMap.get(cs).restart();
					}
				}

				// Collect CSs this timestep.
				List<CS> activeCS = new ArrayList<CS>();

				// Update each stimulus
				for (CS cs : trial.getCues()) {
					// Timing range for this CS
					int onset = 0;
					int offset = 0;
					if (cs.isSerialConfigural() || !cs.isConfigural()
							&& !Context.isContext(cs.getName())) {
						onset = timings.get(cs)[0];
						offset = timings.get(cs)[1];
					}

					// Prevent overlaps by repeated CSs
					boolean already = false;
					for (SimCue cue : activeList) {
						if (cue.getSymbol().equals(cs.getName())) {
							already = true;
						}
					}
					// Check if this stimulus is present right now, only update
					// it
					// and allow it to contribute to the prediction if it is.
					if (!already
							&& (cs.isSerialConfigural() || !cs.isConfigural())
							&& (Context.isContext(cs + "") || (onset <= j && j < offset))
							&& !cs.isCompound()) {

						// Lazily expand if we need to grow the CSC
						SimCue active = tempMap.get(cs).nextCue();
						// Start eligibility trace for the active component
						active.setActive(true);
						inThisRun.add(cs.getName());
						activeList.add(active);
                        csActiveThisTrial.add(cs.getName());

						// Log active CS as well as cue
						activeCS.add(cs);
						Queue<SimCue> log = cueLog.get(cs);
						if (log == null) {
							log = new LinkedList<SimCue>();
							log.add(active);
							cueLog.put(cs, log);
						} else {
							log.add(active);
						}

						// Update predictions
						// Prediction that the active components are making
						prediction += active.getLastAssocValue();
						// tempMap.get(curCue).restart();

					}
				}

				// To handle configurals on the fly, we make a sorted list of
				// the names of the active cues
				// then retrieve the configural that corresponds from the
				// group's map
				List<SimCue> bits = new ArrayList<SimCue>(activeList);
				Collections.sort(bits);
				StringBuilder sb = new StringBuilder();
				for (SimCue s : bits) {

					sb.append(s.getSymbol());
				}
				String configuralName = "";
				configuralName += SimGroup.getKeyByValue(group.getModel()
						.getConfigCuesNames(), sb.toString());
				CS configural = new ConfiguralCS(configuralName, 0, 0,
						sb.toString(), false);
				if (tempMap.containsKey(configural)) {
					// Lazily expand if we need to grow the CSC
                    SimCue active = null;
                    try {
					    active = tempMap.get(configural).nextCue();
                    } catch (NullPointerException e) {
                        System.err.println(e);
                    }
					// Start eligibility trace for the active component
					active.setActive(true);
					activeList.add(active);
					// Update predictions
					// Prediction that the active components are making
					prediction += active.getLastAssocValue();
					inThisRun.add(configural.getName());
                    csActiveThisTrial.add(configural.getName());
				}

				// If a gap in a sequence of some cues happens
				// reset the iterators.
				resetInactive(trial.getCues(), activeList, tempMap);

				// Make sure predictions are at least 0
				if (Simulator.getController().getModel()
						.isRestrictPredictions()) {
					prediction = Math.max(prediction, 0);
					lastPrediction = Math.max(lastPrediction, 0);
				}

				// Construct beta error term
				double betaError = getGamma() * prediction - lastPrediction;
				// System.out.println(betaError);
				// Is the US active right now?
				boolean usOn = ((timings.get(CS.US)[0]) <= j && j < (timings
						.get(CS.US)[1]));
				// Reinforce during US duration
				betaError += usOn ? (currentSt.isReinforced() ? getLambdaPlus()
						: 0) : 0;

				// Multiply by learning rate
				betaError *= currentSt.isReinforced() ? getBetaPlus()
						: getBetaMinus();
				// Update the cues
				updateCues(betaError, tempRes, tempMap.keySet());
				// Merge if a compound is present
				if (activeList.size() > 1
						&& !Simulator.getController().getModel()
								.isSerialCompounds()) {
					mergeCues(curNameSt.substring(0, curNameSt.length() - 1),
							tempRes, activeList);
					String merged = "";
					for (SimCue c : activeList) {
						merged += c.getSymbol();
					}
					inThisRun.add(merged);
                    csActiveThisTrial.add(merged);
				}
				// Use probe *cues*
				// Possible probe list
				Set<CS> possibleProbes = new HashSet<CS>(csActiveLastStep);
				possibleProbes.addAll(activeCS);
				if (trial.isProbe()) {
					for (CS cs : possibleProbes) {
						if (cs.isProbe()) {
							for (SimCue cue : activeList) {
								if (cue.getSymbol().equals(cs.getName())) {
                                    probeCSActiveThisTrial.add(updateProbeCues(cs.getProbeSymbol(),
                                            probeResults2, cue, trial));
                                    //And for the context as well if there
                                    if(context) {
                                        for(SimCue ctxt : activeList) {
                                            if(Context.isContext(ctxt.getSymbol())) {
                                                String probe = contextCfg.getSymbol() + "(" + cs.getProbeSymbol() + ")";
                                                probeCSActiveThisTrial.add(updateProbeCues(probe,
                                                        probeResults2, ctxt, trial));
                                                break;
                                            }
                                        }
                                    }
								}
							}
						}
					}
				}

				// Merge serial compounds if required.
				if (Simulator.getController().getModel().isSerialCompounds()) {
					// Determine the active compound
					CS activeCompound = null;
					for (Entry<CS, int[]> entry : timings.entrySet()) {
						int onset = timings.get(entry.getKey())[0];
						int offset = timings.get(entry.getKey())[1];
						if (onset <= j && j < offset
								&& !entry.getKey().isConfigural()
								&& entry.getKey().isSerialConfigural()) {
							activeCompound = entry.getKey();
							break;
						}
					}

					if (activeCompound != null) {
						/*
						 * List<String> activeThisStep = new
						 * ArrayList<String>(); for(SimCue cue : activeList) {
						 * activeThisStep.add(cue.getSymbol()); }
						 */
						Collections.sort(activeCS);
						// Identify if we are at a transition (or if the next
						// stage is a transition)
						// but not transitions from nothing.
						boolean proceed = /*
										 * !csActiveLastStep.isEmpty() &&
										 * !activeCS.equals(csActiveLastStep) &&
										 */
						!activeCS.isEmpty();

						csActiveLastStep.clear();
						csActiveLastStep.addAll(activeCS);

						// Merge if required
						if (proceed) {
							mergeSerialCues(activeCompound.getName(), tempRes,
									activeList, cueLog);
                            csActiveThisTrial.add(activeCompound.getName());
						}
						activeLastStep.clear();
						activeLastStep.addAll(activeList);
					}

				}

				// Update prediction
				lastPrediction = prediction;
			}
			// Ignore transitions post trial.
			activeLastStep.clear();
			csActiveLastStep.clear();
			// Store new prediction at the end of each trial
			store(tempRes, csActiveThisTrial);
			// Store probe trials if required
			if (trial.isProbe()) {
				store(probeResults2, probeCSActiveThisTrial);
			}
            control.incrementProgress(1);
            control.setEstimatedCycleTime(System.currentTimeMillis()
                    - count);
		}
	}

	/**
	 * Returns an exact TreeMap copy from the TreeMap that is been given. It
	 * iterates through it's keys and puts their values into a new object.
	 * Modified Dec-2011 to use CSC cues. J Gray
	 * 
	 * @param cues2
	 *            the original TreeMap object which to copy from.
	 * @return
	 */
	protected TreeMap<String, CueList> copyKeysMapToTreeMap(
			Map<String, CueList> cues2) {
		TreeMap<String, CueList> reqMap = new TreeMap<String, CueList>();

		// Iterating over the elements in the set
		Iterator<Entry<String, CueList>> it = cues2.entrySet().iterator();
		while (it.hasNext()) {
			// Get element
			Entry<String, CueList> element = it.next();
			// Skip compounds
			if (element.getKey().length() == 1) {

				// Alberto Fernández July-2011: removed if
				// if (this.isCueInStimuli(element)){ // only the cues in the
				// phase
				CueList currentCsc = element.getValue();
				CueList cscValues = currentCsc.copy();
				reqMap.put(element.getKey(), cscValues);
				// }
			}
		}
		return reqMap;
	}

	/**
	 * Empty the HashMap with the results inside. This happens in case that the
	 * user chooses to keep the same information on the phase table but wishes
	 * to update their value.
	 * 
	 */
	public void emptyResults() {
		results.clear();
	}

	/**
	 * Returns the phase's 'beta' value which represents the non-reinforced
	 * stimuli.
	 * 
	 * @return a 'beta' value for the non-reinforced stimuli.
	 */
	public Double getBetaMinus() {
		return betaMinus;
	}

	/**
	 * Returns the phase's 'beta' value which represents the reinforced stimuli.
	 * 
	 * @return a 'beta' value for the reinforced stimuli.
	 */
	public Double getBetaPlus() {
		return betaPlus;
	}

	/**
	 * @return the contextCfg
	 */
	public ContextConfig getContextConfig() {
		return contextCfg;
	}

	public double getContextSalience() {
		return bgSalience;
	}

	/**
	 * @return the cues
	 */
	public Map<String, CueList> getCues() {
		return cues;
	}

	/**
	 * @return the delta
	 */
	public Double getDelta() {
		return delta;
	}

	public Double getGamma() {
		return gamma;
	}

	/**
	 * @return the group
	 */
	public SimGroup getGroup() {
		return group;
	}

	/**
	 * @return the itis
	 */
	public ITIConfig getITI() {
		return itis;
	}

	/**
	 * Returns the phase's 'lambda' value which represents the non-reinforced
	 * stimuli.
	 * 
	 * @return a 'lambda' value for the non-reinforced stimuli.
	 */
	public Double getLambdaMinus() {
		return lambdaMinus;
	}

	/**
	 * Returns the phase's 'lambda' value which represents the reinforced
	 * stimuli.
	 * 
	 * @return a 'lambda' value for the reinforced stimuli.
	 */
	public Double getLambdaPlus() {
		return lambdaPlus;
	}

	/**
	 * Get the longest duration of all the trials in this phase.
	 * 
	 * @return an integer giving the maximum duration of the trials.
	 */

	public int getMaxDuration() {
		return maxMaxOnset;
	}

	/**
	 * Returns the total number of trials that this phase contains.
	 * 
	 * @return the number of trials.
	 */
	public int getNoTrials() {
		return trials;
	}

	public int getNumberOfValuesInResults() {
		int n = 0;
		Set<String> keys = this.results.keySet();
		Iterator<String> it = keys.iterator();

		while (it.hasNext()) {
			String element = it.next();
			CueList cues = this.results.get(element);
			for (SimCue cue : cues) {
				n += cue.getAssocValueSize();
			}
		}

		keys = this.probeResults.keySet();
		it = keys.iterator();

		while (it.hasNext()) {
			String element = it.next();
			CueList cues = this.probeResults.get(element);
			for (SimCue cue : cues) {
				n += cue.getAssocValueSize();
			}
		}

		return n;
	}

	/**
	 * @return the orderedSeq
	 */
	public List<Trial> getOrderedSeq() {
		return orderedSeq;
	}

	/**
	 * @return the presentCS
	 */
	public Set<CS> getPresentCS() {
		return presentCS;
	}

	/**
	 * @return the probeResults
	 */
	public Map<String, CueList> getProbeResults() {
		return probeResults;
	}

	/**
	 * Returns the results into a HashMap containing the cues that are
	 * participate in this phase or in the other group's phase's (their value
	 * remain the same) with their associative strengths. Modified to return a
	 * CSC cue-list. J Gray
	 * 
	 * @return the results from the algorithms process.
	 */
	public Map<String, CueList> getResults() {
		return results;
	}

	/**
	 * Returns the results into a HashMap containing the stimuli that are
	 * participate in this phase or in the other group's phase's (their value
	 * remain the same) with their associative strengths.
	 * 
	 * @return the stimuli of the phase.
	 */
	public Map<String, SimStimulus> getStimuli() {
		return stimuli;
	}

	/**
	 * @return the timingConfig
	 */
	public TimingConfiguration getTimingConfig() {
		return timingConfig;
	}

	/**
	 * @return the timing configuration for this phase
	 */
	public TimingConfiguration getTimingConfiguration() {
		return timingConfig;
	}

	/**
	 * Returns the initial sequence that was entered by the user on the phases
	 * table.
	 * 
	 * @return the initial sequence.
	 */
	public String intialSequence() {
		return initialSeq;
	}

	/**
	 * Returns true if the cue is in the stimuli of the phase. (Returns true if
	 * the cue is taking part in the phase)
	 * 
	 * @param cue
	 *            the cue looked for
	 * @return if the cue is taking part in the current phase
	 */
	public boolean isCueInStimuli(CS cue) {
		for (SimStimulus stim : stimuli.values()) {
			if (stim.contains(cue.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the cue is in the stimuli of the phase. (Returns true if
	 * the cue is taking part in the phase)
	 * 
	 * @param cue
	 *            the cue looked for
	 * @return if the cue is taking part in the current phase
	 */
	public boolean isCueInStimuli(String cue) {
		boolean is = false;
        try {
            if(results.get(cue).getTrialCount() == 0) {
                 return false;
            }
        } catch (NullPointerException e) {
        }
		// looking for the cue in the stimuli
		for (SimStimulus s : stimuli.values()) {
			if (s.contains(cue)) {
				is = true;
				break;
			}
		}
		return is;
	}

	/**
	 * Check if a set of CSs has a match for a cue's name and if so whether
	 * that's a probe CS.
	 * 
	 * @param set
	 *            set to search.
	 * @param cue
	 *            cue name to find.
	 * @return true if the cue name is found
	 */

	private boolean isCueProbeByName(Set<CS> set, String cue) {
		for (CS cs : set) {
			if (cs.getName().equals(cue) && cs.isProbe()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return if the phase will be randomly executed
	 * 
	 * @return
	 */
	public boolean isRandom() {
		return random;
	}

	/**
	 * Merge cues together to form a compound cue.
	 * 
	 * @param compoundName
	 *            name of the compound
	 * @param tempRes
	 *            working results map
	 * @param active
	 *            list of active components
	 */

	protected void mergeCues(String compoundName, Map<String, CueList> tempRes,
			List<SimCue> active) {
		compoundName = "";
		for (SimCue cue : active) {
			compoundName += cue.getSymbol();
		}

		if (active.size() > 1) {
			CueList compound = tempRes.containsKey(compoundName) ? tempRes
					.get(compoundName) : new CompoundCueList(compoundName, 0d,
					group.getModel().getThreshold(), group.getModel()
							.getDecay(), group.getModel().getTraceType(), group
							.getModel().getDecisionRule());
			SimCue cue = compound.nextCue();
			double totalAssoc = 0;
			for (SimCue tmp : active) {
				totalAssoc += tmp.getLastAssocValue();
			}
			cue.setAssocValue(totalAssoc);

			tempRes.put(compoundName, compound);
		}
	}

	/**
	 * Merge serial cues to a serial compound. The value of a serial compound is
	 * formed from the addition of: the predicted value of the last component of
	 * the first CS in the sequence, and the predicted value of the first
	 * component of each succeeding CS. So for a sequence ABC, the compound
	 * takes the value of the last A + first B + first C. Here, we achieve this
	 * by calling this method with the active set of cues at each transition.
	 * 
	 * @param activeLastStep
	 */

	protected void mergeSerialCues(String compoundName,
			Map<String, CueList> tempRes, List<SimCue> active,
			Map<CS, Queue<SimCue>> activeLastStep) {
		// activeLastStep.addAll(active);
		if (!active.isEmpty()) {
			CueList compound = tempRes.containsKey(compoundName) ? tempRes
					.get(compoundName) : new CompoundCueList(compoundName, 0d,
					group.getModel().getThreshold(), group.getModel()
							.getDecay(), group.getModel().getTraceType(), group
							.getModel().getDecisionRule());
			SimCue cue = compound.nextCue();
			// Need to zero this on the first go
			double totalAssoc = cue.getLastAssocValue();
			double assoc = 0;
			double trace;
			/*
			 * for(SimCue tmp : active) { totalAssoc += tmp.getLastAssocValue();
			 * }
			 */
			// Sum other cues as well
			for (Queue<SimCue> q : activeLastStep.values()) {
				if (!q.isEmpty()) {
					SimCue tmp = q.remove();
					assoc = tmp.getLastAssocValue();
					trace = 1;//tmp.getLastTrace();
					totalAssoc += assoc * trace;
				}
			}
			totalAssoc = Math.min(totalAssoc, 1);
			cue.setAssocValue(totalAssoc);
			// Get average of active and what's in compound
			for(SimCue activeCue : active) {
				totalAssoc += activeCue.getLastAssocValue();
			}
			totalAssoc /= active.size() + 1;
			//cue.setAvgAssocValue(totalAssoc);

			tempRes.put(compoundName, compound);
		}

	}

	/**
	 * Helper function for random runs. Reconstitutes a map of lists of weights
	 * and a map of trial counts into a map of averaged cues.
	 * 
	 * @param tempRes
	 *            Map to return results into
	 * @param avgResult
	 *            Map of cue names -> lists of weights
	 * @param trialCounts
	 *            Map of trial counts for cscs.
	 */

	private void reconstitute(Map<String, CueList> tempRes,
			TreeMap<String, List<ParallelDoubleArray[]>> avgResult,
			Map<String, Integer> trialCounts, Map<String, List<Integer>> maxCues) {
		for (Entry<String, List<ParallelDoubleArray[]>> entry : avgResult
				.entrySet()) {
			CueList tmpCueList = entry.getKey().length() == 1 ? new CueList(
					entry.getKey(), cues.get(entry.getKey()).getAlpha(), group
							.getModel().getThreshold(), group.getModel()
							.getDecay(), group.getModel().getTraceType(), group
							.getModel().getDecisionRule())
					: new CompoundCueList(entry.getKey(), 0d, group.getModel()
							.getThreshold(), group.getModel().getDecay(), group
							.getModel().getTraceType(), group.getModel()
							.getDecisionRule());
			if (Context.isContext(tmpCueList.getSymbol())) {
				tmpCueList.setAlpha(contextCfg.getAlpha());
			}
			tmpCueList.setAverageWeights(entry.getValue().get(0)[0].asList());
			tmpCueList.setAverageResponse(entry.getValue().get(0)[1].asList());
			tmpCueList.setTrialCount(trialCounts.get(entry.getKey()));
			tmpCueList.setMaxCueList(maxCues.get(entry.getKey()));
			for (int i = 2; i < entry.getValue().size(); i++) {
				SimCue tmpCue;
				tmpCue = entry.getKey().length() == 1 ? new SimCue(
						entry.getKey(), cues.get(entry.getKey()).getAlpha())
						: new SimCue(entry.getKey(), 0d);
				tmpCue.setAssocValueVector(entry.getValue().get(i)[0].asList());
				tmpCue.setResponses(entry.getValue().get(i)[1].asList());
				tmpCue.setAvgAssocValueVector(entry.getValue().get(i)[2].asList());
				tmpCueList.add(tmpCue);
				tmpCueList.setSymbol(entry.getKey());
			}
			//tmpCueList.remakeResponses();
			if (!tmpCueList.isEmpty()) {
				tempRes.put(entry.getKey(), tmpCueList);
			}
		}
	}

	/**
	 * Reset the iterators of any cues from the first list not in the second.
	 * 
	 * @param set
	 *            list of cues to test
	 * @param activeList
	 *            list to not reset
	 * @param tempMap
	 *            iterators
	 */

	private int resetInactive(Set<CS> set, List<SimCue> activeList,
			Map<CS, CueList> tempMap) {
		Map<String, CS> names = new HashMap<String, CS>();
		int count = 0;
		for (CS cs : set) {
			if (!cs.isCompound()) {
				names.put(cs.getName(), cs);
			}
		}
		for (Entry entry : names.entrySet()) {
			boolean contains = false;
			for (SimCue cue : activeList) {
				if (cue.getSymbol().equals(entry.getKey())) {
					contains = true;
					break;
				}
			}
			if (!contains) {
                try {
				    tempMap.get(entry.getValue()).restart();
				    count++;
                } catch (NullPointerException e) {
                    //Oh just fucking deal with it
                }
			}
		}
		return count;
	}

	/**
	 * Helper function for random runs. Maintains a running total of weights and
	 * trial counts, averaged at each step.
	 * 
	 * @param tempRes
	 * @param avgResult
	 * @param trialCounts
	 * @param divide
	 */

	private void runningRandomTotal(TreeMap<String, CueList> tempRes,
			TreeMap<String, List<ParallelDoubleArray[]>> avgResult,
			Map<String, Integer> trialCounts, DoubleOp divide,
			Map<String, List<Integer>> maxCues) {
		ParallelDoubleArray[] avg;
		// Add the results to the averaged equivalent weights
		for (Entry<String, CueList> cues : tempRes.entrySet()) {
			List<ParallelDoubleArray[]> avgList = avgResult.get(cues.getKey());
			// Create a list for this cue if it is missing
			if (avgList == null) {
				avgList = new ArrayList<ParallelDoubleArray[]>();
				avgResult.put(cues.getKey(), avgList);
			}
			ParallelDoubleArray weights;
			weights = ParallelDoubleArray.createEmpty(cues.getValue()
					.getAverageWeights().size(), Simulator.fjPool);
			// Average weights first
			weights.asList().addAll(cues.getValue().getAverageWeights());
			try {
				avg = avgList.get(0);
			} catch (IndexOutOfBoundsException e) {
				avg = new ParallelDoubleArray[2];
				avg[0] = ParallelDoubleArray.createEmpty(cues.getValue()
						.getAverageWeights().size(), Simulator.fjPool);
				avgResult.get(cues.getValue().getSymbol()).add(avg);
			}
			avg[0].setLimit(weights.asList().size());
			avg[0].replaceWithMapping(addWeights,
					weights.replaceWithMapping(divide));
			// Average responses next
			weights.asList().clear();
			weights.asList().addAll(cues.getValue().getAverageResponse());
			try {
				avg = avgList.get(1);
			} catch (IndexOutOfBoundsException e) {
				avg[1] = ParallelDoubleArray.createEmpty(cues.getValue()
						.getAverageWeights().size(), Simulator.fjPool);
				avgResult.get(cues.getValue().getSymbol()).add(avg);
			}
			avg[1].setLimit(weights.asList().size());
			avg[1].replaceWithMapping(addWeights,
					weights.replaceWithMapping(divide));
			// Trial counts
			trialCounts.put(cues.getKey(), cues.getValue().getTrialCount());
			// Max components for each trial.
			List<Integer> totalMax = new ArrayList<Integer>();
			if (maxCues.containsKey(cues.getKey())) {
				for (int i = 0; i < cues.getValue().getMaxCueList().size(); i++) {
					int existingMax = 0;
					int newMax = 0;
					try {
						existingMax = maxCues.get(cues.getKey()).get(i);
					} catch (Exception e) {
						// System.err.println(i + "_" + cues.getKey());
					}
					try {
						newMax = cues.getValue().getMaxCueList().get(i);
					} catch (Exception e) {
						// System.err.println(i + " " + cues.getKey());
					}
					totalMax.add(Math.max(existingMax, newMax));
				}
				maxCues.put(cues.getKey(), totalMax);
			} else {
				maxCues.put(cues.getKey(), cues.getValue().getMaxCueList());
			}

			// Iterate through all the components of the cue and add their
			// weight vectors
			// to the average vector, expanding it if necessary
			for (int p = 0; p < cues.getValue().size()
					&& !cues.getValue().get(p).getAssocValueVector().isEmpty(); p++) {
				SimCue cue = cues.getValue().get(p);

				try {
					avg = avgList.get(p + 2);
				} catch (IndexOutOfBoundsException e) {
					avg = new ParallelDoubleArray[3];
					avg[0] = ParallelDoubleArray.createEmpty(cue
							.getAssocValueVector().size(), Simulator.fjPool);
					avg[1] = ParallelDoubleArray.createEmpty(cue.getResponses()
							.size(), Simulator.fjPool);
					avg[2] = ParallelDoubleArray.createEmpty(cue
							.getAvgAssocValueVector().size(), Simulator.fjPool);
					
					avgResult.get(cue.getSymbol()).add(avg);
				}
				// Weights first
				weights = ParallelDoubleArray.createEmpty(cue
						.getAssocValueVector().size(), Simulator.fjPool);
				weights.asList().addAll(cue.getAssocValueVector());
				int limit = Math.max(weights.asList().size(), avg[0].size());
				avg[0].setLimit(limit);
				weights.setLimit(limit);
				avg[0].replaceWithMapping(addWeights,
						weights.replaceWithMapping(divide));
				// Responses second
				weights = ParallelDoubleArray.createEmpty(cue.getResponses()
						.size(), Simulator.fjPool);
				weights.asList().addAll(cue.getResponses());
				limit = Math.max(weights.asList().size(), avg[1].size());
				weights.setLimit(limit);
				avg[1].setLimit(limit);
				avg[1].replaceWithMapping(addWeights,
						weights.replaceWithMapping(divide));
				// Avg'd weights last
				weights = ParallelDoubleArray.createEmpty(cue.getAvgAssocValueVector()
						.size(), Simulator.fjPool);
				weights.asList().addAll(cue.getAvgAssocValueVector());
				limit = Math.max(weights.asList().size(), avg[2].size());
				weights.setLimit(limit);
				avg[2].setLimit(limit);
				avg[2].replaceWithMapping(addWeights,
						weights.replaceWithMapping(divide));
			}
		}
	}

	/**
	 * Run a shuffled set of variable distributions.
	 * 
	 * @param sequence
	 * @param res
	 * @param context
	 * @param probeResults2
	 */

	public void runRandom(List<Trial> sequence, Map<String, CueList> res,
			boolean context, Map<String, CueList> probeResults2) {
		Map<String, Integer> trialCounts = new HashMap<String, Integer>();
		Map<String, Integer> probeTrialCounts = new HashMap<String, Integer>();
		Map<String, List<Integer>> maxCues = new HashMap<String, List<Integer>>();
		Map<String, List<Integer>> maxProbeCues = new HashMap<String, List<Integer>>();
		// Alberto Fernández July-2011
		// J Gray Dec-2011
		Ops.DoubleOp divide = new Ops.DoubleOp() {

			@Override
			public double op(double value) {
				return value / group.getModel().getVariableCombinationNo();
			}
		};
		// Shuffle process
		TreeMap<String, List<ParallelDoubleArray[]>> avgResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
		TreeMap<String, List<ParallelDoubleArray[]>> avgProbeResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
		TreeMap<String, CueList> tempRes, probeRes;
		for (int i = 0; i < group.getModel().getVariableCombinationNo()
				&& !control.isCancelled(); i++) {
			// Copies an exact copy of the result treemap and
			// runs the algorithm using this temporary copy.
			tempRes = copyKeysMapToTreeMap(cues);
			probeRes = new TreeMap<String, CueList>();
			// Time one cycle
			// Run the algorithm for this sequence
			algorithm(sequence, tempRes, context, probeRes);
			// Reshuffle onset sequence
            try {
			    timingConfig.advance();
            } catch (NoSuchElementException e) {
                System.err.println("Ran out of variable timings after "+(control.getProgress()-1)+" trials.");
            }
			itis.reset();

			// Add the results to the averaged equivalent weights
			runningRandomTotal(tempRes, avgResult, trialCounts, divide, maxCues);
			runningRandomTotal(probeRes, avgProbeResult, probeTrialCounts,
                    divide, maxProbeCues);
		}
		if (control.isCancelled()) {
			return;
		}
		// Reconstitute the lists of weights to an appropriate cue object
		reconstitute(res, avgResult, trialCounts, maxCues);
		reconstitute(probeResults2, avgProbeResult, probeTrialCounts,
				maxProbeCues);
	}

	/**
	 * This starts the execution of the algorithm. The method first checks if
	 * the sequence has to be executed in random order and then executes the
	 * same algorithm but in different execution style. If the sequence is
	 * random, creates a tempSequence from the original and runs a simple
	 * shuffle method. The shuffle methods use a random generator which provides
	 * a number from 0 to the end of the sequences length. Then swaps position
	 * with the previous number - position. Finally it calls the algorithm. The
	 * previous task is running iterative depending the number of combinations
	 * that the user has chosen. If the sequence is not supposed to run in
	 * random order it skips this step and goes straight to the algorithm.
	 * 
	 */
	public void runSimulator() {
		results = copyKeysMapToTreeMap(cues);
		probeResults = new TreeMap<String, CueList>();// copyKeysMapToTreeMap(cues);
		boolean context = group.getModel().isUseContext();
		// Sequence is running randomly
		if (isRandom()) {
			Map<String, Integer> trialCounts = new HashMap<String, Integer>();
			Map<String, Integer> probeTrialCounts = new HashMap<String, Integer>();
			Map<String, List<Integer>> maxCues = new HashMap<String, List<Integer>>();
			Map<String, List<Integer>> maxProbeCues = new HashMap<String, List<Integer>>();
			// Alberto Fernández July-2011
			// J Gray Dec-2011

			// end changes July-2011

			Ops.DoubleOp divide = new Ops.DoubleOp() {

				@Override
				public double op(double value) {
					return value / group.noOfCombin();
				}
			};

			// Shuffle process
			Random generator = new Random();
			TreeMap<String, List<ParallelDoubleArray[]>> avgResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
			TreeMap<String, List<ParallelDoubleArray[]>> avgProbeResult = new TreeMap<String, List<ParallelDoubleArray[]>>();
			TreeMap<String, CueList> tempRes, tempProbeRes;
			for (int i = 0; i < group.noOfCombin() && !control.isCancelled(); i++) {
				List<Trial> tempSeq = orderedSeq;
				int n;
				for (int x = 0; x < trials && orderedSeq.size() > 1; x++) {
					n = generator.nextInt(orderedSeq.size() - 1);
					Trial swap = tempSeq.get(x);
					tempSeq.remove(x);
					tempSeq.add(n, swap);
				}
				// Copies an exact copy of the result treemap and
				// runs the algorithm using this temporarily copy.
				tempRes = copyKeysMapToTreeMap(cues);
				tempProbeRes = new TreeMap<String, CueList>();// copyKeysMapToTreeMap(cues);

				// Run the algorithm for this sequence
				if (timingConfig.hasVariableDurations()) {
					runRandom(tempSeq, tempRes, context, tempProbeRes);
				} else {

					algorithm(tempSeq, tempRes, context, tempProbeRes); // Alberto
																		// Fern·ndez
																		// July-2011

				}

				// Reset onset sequence
                timingConfig.restartOnsets();
				itis.reset();

				// Add the results to the averaged equivalent weights
				runningRandomTotal(tempRes, avgResult, trialCounts, divide,
						maxCues);
				// Add the results to the averaged equivalent weights
				runningRandomTotal(tempProbeRes, avgProbeResult,
						probeTrialCounts, divide, maxProbeCues);
			}
			if (control.isCancelled()) {
				return;
			}
			// Reconstitute the lists of weights to an appropriate cue object
			results = new TreeMap<String, CueList>();
			reconstitute(results, avgResult, trialCounts, maxCues);
			probeResults = new TreeMap<String, CueList>();
			reconstitute(probeResults, avgProbeResult, probeTrialCounts,
					maxProbeCues);
			// control.incrementProgress(1);//(100d/(group.noOfCombin()+1))/group.getNoOfPhases());
		}
		// A standard sequence
		else {
			if (timingConfig.hasVariableDurations()) {
				runRandom(orderedSeq, results, context, probeResults);
			} else {
				algorithm(orderedSeq, results, context, probeResults); // Alberto
																		// Fern·ndez
																		// July-2011
			}
		}
		cues.putAll(results);
	}

	/**
	 * Sets the phase's 'beta' value which represents the non-reinforced
	 * stimuli.
	 * 
	 * @param l
	 *            'beta' value for the non-reinforced stimuli.
	 */
	public void setBetaMinus(Double l) {
		betaMinus = l;
	}

	/**
	 * Sets the phase's 'beta' value which represents the reinforced stimuli.
	 * 
	 * @param l
	 *            'beta' value for the reinforced stimuli.
	 */
	public void setBetaPlus(Double l) {
		betaPlus = l;
	}

	/**
	 * @param contextCfg
	 *            the contextCfg to set
	 */
	public void setContextConfig(ContextConfig contextCfg) {
		this.contextCfg = contextCfg;
	}

	public void setContextSalience(double salience) {
		bgSalience = salience;
	}

	/**
	 * @param control
	 */
	public void setControl(ModelControl control) {
		this.control = control;
	}

	/**
	 * @param cues
	 *            the cues to set
	 */
	public void setCues(Map<String, CueList> cues) {
		this.cues = cues;
	}

	/**
	 * @param delta
	 *            the delta to set
	 */
	public void setDelta(Double delta) {
		this.delta = delta;
	}

	/**
	 * Sets the phase's 'gamma' value which represents the discount factor.
	 * 
	 * @param
	 */
	public void setGamma(Double g) {
		gamma = g;
	}

	/**
	 * @param group
	 *            the group to set
	 */
	public void setGroup(SimGroup group) {
		this.group = group;
	}

	/**
	 * @param itis
	 *            the itis to set
	 */
	public void setITI(ITIConfig itis) {
		this.itis = itis;
	}

	/**
	 * Sets the phase's 'lambda' value which represents the non-reinforced
	 * stimuli.
	 * 
	 * @param l
	 *            'lambda' value for the non-reinforced stimuli.
	 */
	public void setLambdaMinus(Double l) {
		lambdaMinus = l;
	}

	/**
	 * Sets the phase's 'lambda' value which represents the reinforced stimuli.
	 * 
	 * @param l
	 *            'lambda' value for the reinforced stimuli.
	 */
	public void setLambdaPlus(Double l) {
		lambdaPlus = l;
	}

	/**
	 * @param orderedSeq
	 *            the orderedSeq to set
	 */
	public void setOrderedSeq(List<Trial> orderedSeq) {
		this.orderedSeq = orderedSeq;
	}

	/**
	 * @param presentCS
	 *            the presentCS to set
	 */
	public void setPresentCS(Set<CS> presentCS) {
		this.presentCS = presentCS;
	}

	/**
	 * @param probeResults
	 *            the probeResults to set
	 */
	public void setProbeResults(Map<String, CueList> probeResults) {
		this.probeResults = probeResults;
	}

	/**
	 * Set the random attribute for this phase
	 * 
	 * @param random
	 */
	public void setRandom(boolean random) {
		this.random = random;
	}

	/**
	 * @param timingConfig
	 *            the timingConfig to set
	 */
	public void setTimingConfig(TimingConfiguration timingConfig) {
		this.timingConfig = timingConfig;
	}

	/**
	 * @param timings
	 *            set the timing configuration for this phase
	 */
	public void setUsLength(TimingConfiguration timings) {
		timingConfig = timings;
	}

	/**
	 * Store the cues' values for this trial.
	 * 
	 * @param tempRes
	 * @param current
	 */

	protected void store(Map<String, CueList> tempRes, Set<String> current) {
		for (CueList cue : tempRes.values()) {
			// Changed to contains to accommodate lazy compound formation
			if (current.contains(cue.getSymbol())) {
				cue.store();
				cue.restart();
			}
		}
	}

	/**
	 * Update cues according to TD algorithm.
	 * 
	 * @param betaError
	 *            beta error term
	 * @param tempRes
	 *            results map
	 * @param set
	 *            set of cues to update
	 */

	protected void updateCues(double betaError, Map<String, CueList> tempRes,
			Set<CS> set) {
		// Update by name, not CS to avoid dupe updates for repeated CSs
		Set<String> nameSet = new HashSet<String>();
		for (CS updating : set) {
			nameSet.add(updating.getName());
		}
		// For each stimulus in the trial
		for (String updating : nameSet) {
			// For each component of the stimulus
            try {
			    tempRes.get(updating).update(betaError, delta, gamma);
            } catch (NullPointerException e) {
                //Bah humbug
            }
		}
	}

	/**
	 * Update the probe cues map.
	 * 
	 * @param compoundName
	 * @param tempRes
	 * @param cue
	 * @param trial
	 */

	protected String updateProbeCues(String compoundName,
			Map<String, CueList> tempRes, SimCue cue, Trial trial) {
		// Identify results by cuename+trialname to make probe cues uniquely
		// identifiable per trial type.
		compoundName += trial.getProbeSymbol();
		CueList compound = tempRes.containsKey(compoundName) ? tempRes
				.get(compoundName) : new CompoundCueList(compoundName, 0d,
				group.getModel().getThreshold(), group.getModel().getDecay(),
				group.getModel().getTraceType(), group.getModel()
						.getDecisionRule());
		SimCue compoundCue = compound.nextCue();
		compoundCue.setAssocValue(cue.getLastAssocValue());

		tempRes.put(compoundName, compound);
        return compoundName;
	}
	
	/**
	 * Takes a trial description string and returns a list of all the CS'
	 * in it.
	 * @param phaseString
	 * @return
	 */
	
	public static Collection<CS> stringToCSList(String phaseString) {
        phaseString = phaseString.replaceAll("\\s", "");
		List<CS> stimuli = new ArrayList<CS>();
		phaseString = phaseString == null ? "" : phaseString;
		boolean isTimingPerTrial = Simulator.getController().getModel().isTimingPerTrial();
		// A CS has an index (which of that character is this in the trial string)
		int index = 0;
		// A group - which substring is it in
		int group = 0;
		//And a string position - which position in the overall string is it
		int stringPos = 0;
		// Break into trial strings
		String[] trials = phaseString.split("/");
		Map<Character, Integer> indexes = new HashMap<Character, Integer>();
		
		for (String trial : trials) {
			for (Character c : trial.toCharArray()) {
				if (Character.isLetter(c)) {
					// Remember which number of this character this is
					if (!indexes.containsKey(c)) {
						indexes.put(c, 0);
					}
					// Remember which of the / separated strings this is
					// if this isn't a timings per trial string situation
					if(!isTimingPerTrial) {
						stringPos = 0;
					}
					index = indexes.get(c);
					CS cs = new CS(c.toString(), index, group, false, stringPos);
					cs.setTrialString(trial);
					stimuli.add(cs);

				}
				//String pos count includes non-CS characters
				stringPos++;
				// Next of this character is a new CS
				if (Character.isLetter(c) && isTimingPerTrial) {
					indexes.put(c, index + 1);
				}
			}
			stringPos++;
			if (isTimingPerTrial) {
				indexes.clear();
				group++;
			}
		}
		return stimuli;
	}

}
