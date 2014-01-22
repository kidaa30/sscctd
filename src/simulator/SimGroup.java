/**
 * SimGroup.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import simulator.configurables.ContextConfig;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.ITIConfig;
import simulator.configurables.TimingConfiguration;
import extra166y.ParallelArray;

/**
 * SimGroup is the class which models a group from the experiment. It will
 * process any new sequences of stimuli and adds them to its ArrayList of
 * phases. It is an intermediate between the Controller and the Phase itself. It
 * contains all the necessary variables need to run a simulation and it keeps a
 * record of the results but in a global view, meaning that the results will be
 * an extract from all phases together.
 */
public class SimGroup implements Runnable {

	public static String getKeyByValue(Map<String, String> configCuesNames,
			String value) {
		String key = null;
		int count = 0;
		for (Map.Entry<String, String> entry : configCuesNames.entrySet()) {
			if (entry.getValue().equals(value)) {
				key = entry.getKey();
				count++;
			}
		}

		return key;
	}

	/**
	 * Helper function for configurals - produce the powerset of the set of
	 * cues, this is the possible set of configural cues.
	 * 
	 * @param originalSet
	 *            Set of cues in a trial
	 * @return the powerset, excluding null
	 */

	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<T>());
			return sets;
		}
		List<T> list = new ArrayList<T>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
		for (Set<T> set : powerSet(rest)) {
			Set<T> newSet = new HashSet<T>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	// Alberto Fernández August-2011
	// Added boolean parameters isConfiguralCompounds and
	// configuralCompoundsMapping.
	protected ArrayList<SimPhase> phases;
	private Map<String, CueList> cues;
	private String nameOfGroup;
	private int noOfPhases, noOfCombinations, count;
	/** Threaded array. **/
	private ParallelArray<SimPhase> phasePool;

	/** At least one random phase indicator. **/
	private boolean hasRandom;

	/** The model this group belongs to. **/
	private SimModel model;

	/**
	 * Create a group
	 * 
	 * @param n
	 *            name of the Group
	 * @param np
	 *            number of phases
	 * @param rn
	 *            number of combinations
	 * @return true if the method completed without any errors and false
	 *         otherwise.
	 */
	public SimGroup(String n, int np, int rn, SimModel model) {
		nameOfGroup = n;
		noOfPhases = np;
		noOfCombinations = rn;
		count = 1;
		cues = new TreeMap<String, CueList>();
		phases = new ArrayList<SimPhase>(noOfPhases);
		this.setModel(model);
	}

	/**
	 * Adds a new phase in the group's arraylist. The stimuli sequence of the
	 * given is being processed mainly so it could be added as a new SimPhase
	 * object and secondary it might produce new cues which weren't on previous
	 * phases. This new cues are added on the group's cue list as well.
	 * 
	 * @param seqOfStimulus
	 *            the stimuli sequence of the given phase.
	 * @param boolean to know if the phase is going to be randomly executed
	 * @param int the number of the current phase
	 * @param boolean to know if the phase is going to use configural cues
	 * @param mapping
	 *            with configural compounds from their "virtual" name
	 * @return true if the method completed without any errors and false
	 *         otherwise.
	 */

	public boolean addPhase(String seqOfStimulus, boolean isRandom,
			int phaseNum, boolean isConfiguralCompounds,
			TreeMap<String, String> configuralCompoundsMapping,
			TimingConfiguration timings, ITIConfig iti, ContextConfig context) {

		seqOfStimulus = seqOfStimulus.toUpperCase(); // Sequence is always
														// stored in upper case.
														// Alberto Fernández
														// August-2011

		List<Trial> order = new ArrayList<Trial>(50);
		Set<Trial> trials = new HashSet<Trial>();
		String sep = "/";
		String[] listedStimuli = seqOfStimulus.toUpperCase().split(sep);
		// CSC of cues. J Gray
		CueList cscCues;

		if (!cues.containsKey(context.getSymbol())
				&& !context.getContext().equals(Context.EMPTY)) {
			// Modified by J Gray to add CSC cues.
			cscCues = new CueList(context.getSymbol(), 0, model.getThreshold(),
					model.getDecay(), model.getTraceType(),
					model.getDecisionRule());
			cues.put(context.getSymbol(), cscCues);
		}

		// Added by Alberto Fernández
		// Introduce "virtual" cues (lower case letters) in case of configural
		// compounds.

		int noStimuli = listedStimuli.length;
		Map<String, SimStimulus> stimuli = new HashMap<String, SimStimulus>();
        Set<String> configuralsAddedThisGroup = new HashSet<String>();

		for (int i = 0; i < noStimuli; i++) {
			String selStim = listedStimuli[i], repStim = "", cuesName = "", stimName = "";
			boolean reinforced = false;
			boolean oktrials = false, okcues = false, okreinforced = false;
			boolean probe = false; // Is this a probe trial

			if (model.isUseContext()) {
				cuesName = context.getSymbol();
			}

			String compound = "";
			int noStimRep = 1;
			for (int n = 0; n < selStim.length(); n++) {
				char selChar = selStim.charAt(n);

				if (Character.isDigit(selChar) && !oktrials) {
					repStim += selChar;
				} else if (selChar == '^') { // This is a probe trial
					probe = true;
				} else if (Character.isLetter(selChar) && !okcues) {
					oktrials = true;
					cuesName += selChar;
					if (!cues.containsKey(selChar + "")) {
						// Modified by J Gray to add CSC cues.
						cscCues = new CueList(selChar + "", 0,
								model.getThreshold(), model.getDecay(),
								model.getTraceType(), model.getDecisionRule());
						cues.put(selChar + "", cscCues);
					}
					compound += selChar;
				} else if ((selChar == '-' || selChar == '+') && !okreinforced) {
					oktrials = true;
					okcues = true;
					reinforced = (selChar == '+');
					okreinforced = true;


				} else
					return false;
			}
            // Added by Alberto Fernández August-2011
            if ((model.isUseContext() || compound.length() > 1)
                    && isConfiguralCompounds) {
                Set<String> compoundSet = new HashSet<String>();
                for (char c : compound.toCharArray()) {
                    compoundSet.add(c + "");
                }
                if (!model.isSerialConfigurals()) {

                    for (Set<String> set : powerSet(compoundSet)) {
                        String s = "";
                        List<String> bits = new ArrayList<String>(set);
                        Collections.sort(bits);
                        for (String str : bits) {
                            s += str;
                        }
                        // Add configural cue as a "virtual" cue (lower
                        // case letter)
                        s = model.isUseContext() ? context.getSymbol()
                                + s : s;
                        if (s.length() > 1) {
                            String virtualCueName = getKeyByValue(
                                    configuralCompoundsMapping, s);
                            if (virtualCueName == null) {
                                if (configuralCompoundsMapping
                                        .isEmpty()) {
                                    virtualCueName = "a";
                                } else {
                                    char c = configuralCompoundsMapping
                                            .lastKey().charAt(0);
                                    c = (char) (c + 1);
                                    while(!Character.isLetter(c) || Character.toUpperCase(c) == c || Context.isContext(c+"")) {
                                        c++;
                                    }
                                    virtualCueName = "" + c;
                                }
                                configuralCompoundsMapping.put(
                                        virtualCueName, s);
                            }
                            cuesName += virtualCueName;
                            configuralsAddedThisGroup.add(virtualCueName);
                            if (!cues.containsKey(virtualCueName + "")) {
                                // Modified to use CSCs. J Gray
                                cscCues = new CueList(virtualCueName
                                        + "", 0, model.getThreshold(),
                                        model.getDecay(),
                                        model.getTraceType(),
                                        model.getDecisionRule());
                                //cues.put(virtualCueName + "", cscCues);
                            }
                            String compoundName = s + virtualCueName;

                        }
                    }
                }

            }

			int stringPos = 0;
			//Find start position of this trial string
			for(int s = 0; s < i; s++) {
				stringPos += listedStimuli[s].length() + 1;
			}

            //Determine how many trial strings identical with this
            //precede it
            int trialNum = 0;
            for(int s = i - 1; s >= 0; s--) {
                if(listedStimuli[s].equals(listedStimuli[i])) {
                    trialNum++;
                }
            }
			
			stimName = cuesName + (reinforced ? '+' : '-');
			Trial trial = new Trial(stimName, probe,
					(model.isTimingPerTrial() ? i : 0), selStim, stringPos, trialNum);
			trials.add(trial);

			if (repStim.length() > 0)
				noStimRep = Integer.parseInt(repStim);

			if (stimuli.containsKey(stimName))
				stimuli.get(stimName).addTrials(noStimRep);
			else
				stimuli.put(stimName, new SimStimulus(stimName, noStimRep,
						cuesName, reinforced));

			for (int or = 0; or < noStimRep; or++)
				order.add(trial);
		}
		timings.setTrials(order.size());
        timings.restartOnsets();
		List<List<List<CS>>> sequences = timings.sequences(new HashSet<Trial>(
				order));
        if(!model.isSerialConfigurals()
                && isConfiguralCompounds) {
            sequences = timings.compounds(new ArrayList<Trial>(order));
        }
		if (model.isSerialConfigurals() || isConfiguralCompounds) {
			Map<String, CueList> tmp = new TreeMap<String, CueList>();
			for (List<List<CS>> sequence : sequences) {
				String realName = "";
				for (int i = 0; i < sequence.size(); i++) {
					for (CS c : sequence.get(i)) {
						realName += c;
					}
					if (model.isSerialConfigurals() && i < sequence.size() - 1) {
						realName += ConfiguralCS.SERIAL_SEP;
					}

				}
				String virtualCueName = getKeyByValue(
						configuralCompoundsMapping, realName);
				if (virtualCueName == null) {
					if (configuralCompoundsMapping.isEmpty()) {
						virtualCueName = "a";
					} else {
						char c = configuralCompoundsMapping.lastKey().charAt(0);
						c = (char) (c + 1);
                        while(!Character.isLetter(c) || Character.toUpperCase(c) == c || Context.isContext(c+"")) {
                            c++;
                        }
						virtualCueName = "" + c;
					}
					configuralCompoundsMapping.put(virtualCueName, realName);
				}
				if (!tmp.containsKey(virtualCueName + "")) {
					// Modified to use CSCs. J Gray
					cscCues = new CueList(virtualCueName + "", 0,
							model.getThreshold(), model.getDecay(),
							model.getTraceType(), model.getDecisionRule());
					tmp.put(virtualCueName + "", cscCues);
				}
			}
            Iterator<Entry<String, CueList>> iter = cues.entrySet().iterator();

            cues.putAll(tmp);
		}
		// Set indicator that this group has at least one randomised phase
		hasRandom = hasRandom ? hasRandom : isRandom;
        timings.restartOnsets();
		return addPhaseToList(seqOfStimulus, order, stimuli, isRandom, timings,
				iti, context);
	}

	/**
	 * Add a phase to this groups list of phases.
	 * 
	 * @param seqOfStimulus
	 * @param order
	 * @param stimuli
	 * @param isRandom
	 * @param timings
	 * @param iti
	 * @return
	 */

	protected boolean addPhaseToList(String seqOfStimulus, List<Trial> order,
			Map<String, SimStimulus> stimuli, boolean isRandom,
			TimingConfiguration timings, ITIConfig iti, ContextConfig context) {
		return phases.add(new SimPhase(seqOfStimulus, order, stimuli, this,
				isRandom, timings, iti, context));
	}

	/**
	 * Empties every phase's results. It iterates through the phases and calls
	 * the SimPhase.emptyResults() method. This method cleans up the results
	 * variable.
	 * 
	 */
	public void clearResults() {
		for (int i = 0; i < noOfPhases; i++) {
			SimPhase sp = phases.get(i);
			sp.emptyResults();
		}
		count = 1;
	}

	/**
	 * Checks if this is the name of a configural cue (i.e. contains lowercase
	 * characters)
	 * 
	 * @param cueName
	 *            the cue to check
	 * @return true if this is a configural cue
	 */

	protected boolean configuralCue(String cueName) {
		return !cueName.equals(cueName.toUpperCase());
	}

	/**
	 * 
	 * @return a count of how many phases are random in this group
	 */

	public int countRandom() {
		int count = 0;
		for (SimPhase phase : phases) {
			if (phase.isRandom()
					|| phase.getTimingConfig().hasVariableDurations()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 
	 * @return a map of context names to their configurations.
	 */

	public Map<String, ContextConfig> getContexts() {
		Map<String, ContextConfig> contexts = new HashMap<String, ContextConfig>();

		for (SimPhase phase : phases) {
			contexts.put(phase.getContextConfig().getSymbol(),
					phase.getContextConfig());
		}

		return contexts;
	}

	/**
	 * Returns the number of trials that have been produced so far.
	 * 
	 * @return the number of trials so far.
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Returns the TreeMap which contains the cues and their values. An
	 * important object on overall group result processing.
	 * 
	 * @return the group's cues.
	 */
	public Map<String, CueList> getCuesMap() {
		return cues;
	}

	/**
	 * @return the model
	 */
	public SimModel getModel() {
		return model;
	}

	/**
	 * Returns the group's current name. By default shall be "Group n" where n
	 * is the position that has on the table.
	 * 
	 * @return the name of the group.
	 */
	public String getNameOfGroup() {
		return nameOfGroup;
	}

	/**
	 * @return the noOfPhases
	 */
	public int getNoOfPhases() {
		return noOfPhases;
	}

	public int getNumberOfValuesInResults() {
		int n = 0;

		for (SimPhase phase : this.phases) {
			n += phase.getNumberOfValuesInResults();
			n += phase.getPresentCS().size() * 2;
		}
		return n;
	}

	/**
	 * Returns the ArrayList which contains the SimPhases, the phases that run
	 * on this specific group.
	 * 
	 * @return the group's phases.
	 */
	public List<SimPhase> getPhases() {
		return phases;
	}

	/**
	 * 
	 * @return a boolean indicating that this group has at least one random
	 *         phase.
	 */

	public boolean hasRandom() {
		return hasRandom;
	}

	/**
	 * Adds one more value to the count variable which represents the trials so
	 * far.
	 */
	public void nextCount() {
		count++;
	}

	/**
	 * Returns the number of combinations that shall be run if the user has
	 * chosen a random sequence.
	 * 
	 * @return the number of combinations.
	 */
	public int noOfCombin() {
		return noOfCombinations;
	}

	/**
	 * @return the number of runs of the algorithm in this group
	 */
	public int numRandom() {
		int count = 0;
		for (SimPhase phase : phases) {
			int increment = phase.isRandom() ? model.getCombinationNo() : 0;
			increment = phase.getTimingConfig().hasVariableDurations() ? Math
					.max(increment, 1) * model.getVariableCombinationNo()
					: increment;
			count += increment;
		}
		return count;
	}

	/**
	 * Returns the results from the simulation. They are included into a String
	 * object. The method iterates through each phase and returns the phase's
	 * results.
	 * 
	 * @return the group's results represented in a string.
	 */
	public String phasesOutput(boolean compound,
			Map<String, String> configCuesNames) {
		StringBuffer result = new StringBuffer();

		// For all phases
		for (int i = 0; i < noOfPhases; i++) {
			SimPhase sp = phases.get(i);
			result.append("(Phase ").append(i + 1).append(" , Seq: ")
					.append(sp.intialSequence()).append(" Rand: ")
					.append(sp.isRandom()).append(")").append("\n\n");

			Map<String, CueList> results = sp.getResults();

			// Alberto Fern·ndez August-2011


            //Collect the compounds in this phase
            Map<String, String> compounds = new HashMap<String, String>();


            for(String c : results.keySet()) {
                if(sp.isCueInStimuli(c)) {
                    if(c.length() > 1) {
                        String[] strings = c.split(ConfiguralCS.SERIAL_SEP);
                        for(char cue : strings[strings.length-1].toCharArray()) {
                            compounds.put(cue + "", c);
                        }
                    }
                }
            }

			// Output Cues
			Iterator<Entry<String, CueList>> iterCue = results.entrySet()
					.iterator();
			while (iterCue.hasNext()) {
				Entry<String, CueList> pairCue = iterCue.next();
				CueList tempCscCue = pairCue.getValue();
				String cueName = tempCscCue.getSymbol();
				if (sp.isCueInStimuli(cueName)
						&& (compound || cueName.length() == 1)) { // Don't
																	// output
																	// cues not
																	// in this
																	// phase
					if (!configuralCue(cueName)
							&& (cueName.startsWith(Simulator.OMEGA + "") || cueName
									.equals(cueName.toUpperCase()))) {
						result.append("Cue : ").append(cueName);
					} else if (cueName.length() == 1) {
						String compoundName, interfaceName;
						// configural cue
						compoundName = configCuesNames.get(cueName);
						interfaceName = "c(" + compoundName + ")";
						result.append("Cue : ").append(interfaceName);
					} else {
						String compoundName, interfaceName;
						// configural compound
						compoundName = cueName.substring(0,
								cueName.length() - 1);
						interfaceName = "[" + compoundName + "]";
						result.append("Cue : ").append(interfaceName);
					}
					if (!cueName.contains(Simulator.OMEGA + "")) {
						result.append("\n\n").append("Realtime.")
								.append("\n\n");

						for (int z = 0; z < tempCscCue.size()
								&& z < sp.getMaxDuration(); z++) {
							SimCue tempCue = tempCscCue.get(z);
							// Last-but-one V value (i.e. the predicted for next
							// time.)
							result.append("Component ").append(z + 1)
									.append(" V = ")
									.append(tempCue.getLastPrediction());
							result.append('\n');
						}
					}
					result.append('\n').append("Trial\n\n");

					for (int z = 0; z < tempCscCue.getTrialCount(); z++) {
						// Last-but-one V value (i.e. the predicted for next
						// time.)
						result.append("Trial ").append(z + 1).append(" V = ")
								.append(tempCscCue.averageAssoc(z));
						result.append('\n');
					}
					if (model.showResponse()
							&& !cueName.contains(Simulator.OMEGA + "")) {
						result.append('\n').append("Simulated Response\n\n");
						for (int z = 0; z < tempCscCue.size()
								&& z < sp.getMaxDuration(); z++) {
							SimCue tempCue = tempCscCue.get(z);
							result.append("Response t").append(z + 1)
									.append(" = ");
							result.append(tempCue.response(tempCue.getResponses().size() - 1));
							result.append('\n');
						}
                        /*if(compounds.containsKey(pairCue.getKey()) && Simulator.getController().getModel().isSerialCompounds()) {
                            result.append('\n').append("Simulated Serial Response\n\n");
                            for (int z = 0; z < tempCscCue.getTrialCount(); z++) {
                                // Last-but-one V value (i.e. the predicted for next
                                // time.)
                                result.append("Trial ").append(z + 1).append(" Response = ")
                                        .append(tempCscCue.weightedSum(results.get(compounds.get(pairCue.getKey())),z));
                                result.append('\n');
                            }
                        } */
					}
					result.append('\n');
				}
			}

		}
		return result.toString();
	}

	/**
	 * The Runnable's run method. This starts a new Thread. It actually runs
	 * every SimPhases.runSimulator() method which is the method that uses the
	 * formula on the phases.
	 */
	@Override
	public void run() {
		// Add to phasepool so we can still cancel them quickly if required
		phasePool = ParallelArray.createEmpty(noOfPhases, SimPhase.class,
				Simulator.fjPool);
		phasePool.asList().addAll(phases);
		for (int i = 0; i < noOfPhases; i++) {
			if (model.contextAcrossPhase()) {
				// Deal with different omega per phase
				for (Entry<String, CueList> entry : cues.entrySet()) {
					String realName = model.getConfigCuesNames().get(
							entry.getKey());
					realName = realName == null ? "" : realName;
				}
			}
			phases.get(i).runSimulator();
		}
	}

	/**
	 * @param control
	 *            the message passing object to use
	 */
	public void setControl(ModelControl control) {
		for (SimPhase phase : phases) {
			phase.setControl(control);
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
	 * @param noOfPhases
	 *            the noOfPhases to set
	 */
	public void setNoOfPhases(int noOfPhases) {
		this.noOfPhases = noOfPhases;
	}

    public int trialCount() {
        int count = 0;
        for(SimPhase p : phases) {
            int multiplier = p.isRandom() ? Simulator.getController().getModel().getCombinationNo() : 1;
            multiplier *= p.getTimingConfig().hasVariableDurations() ? Simulator.getController().getModel().getVariableCombinationNo() : 1;
            count += p.getTimingConfig().getTrials() * multiplier;
        }
        return count;
    }
}