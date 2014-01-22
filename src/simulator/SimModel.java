/**
 * SimModel.java
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import simulator.configurables.ContextConfig;
import simulator.util.Response;
import simulator.util.Trace;
import extra166y.Ops;
import extra166y.ParallelArray;

/**
 * SimModel is the main object model of the inputed data. It holds the users
 * values about the number of groups, phases and combinations. Updates the
 * groups with new values from the value table and concentrates on the right
 * creation of the final cue list. Every time that a new experiment starts, a
 * new experiment starts the old model object is disposed and anew one is
 * created, all other objects that link with this model are getting refreshed as
 * well. A disposal of the model means that all data stored is getting lost but
 * this is normal as a new experiment starts.
 */
public class SimModel implements Runnable {

	private int groupsNo, phasesNo, combinationNo,
			variableDistributionCombinationNo;
	private Map<String, SimGroup> groups;
	private Map<String, Double> values;
	// Alberto Fernández August-2011
	// Mapping for configural cues. <K,V> K=virtual name (lower case letter),
	// V=compound
	private Map<String, String> configCuesNames; // e.g. <a,AB>
	// Random seed for variable distributions.
	private long randomSeed = System.currentTimeMillis();
	// SimGroup parallel pool
	private ParallelArray<SimGroup> groupPool;
	/** Timestep size. **/
	private double timestepSize;

	// Alberto Fernandez Nov-2011
	List<String> listAllCues; // This is a sorted list of all existing cues
	/** Response threshold for simulated response graph. **/
	private Double threshold;
	/** Whether to simulate contextual stimulus. **/
	private boolean useContext;

	/** Procedure to run a group **/
	final Ops.Op<SimGroup, SimGroup> update = new Ops.Op<SimGroup, SimGroup>() {
		@Override
		public SimGroup op(final SimGroup current) {
			current.run();
			addCueNames(current.getCuesMap());
			return current;
		}
	};
	/** Boolean indicating whether simulated response stats & figures are shown. **/
	private boolean showResponse;
	private boolean isContextAcrossPhase;
	private boolean isCSC;
	private boolean isGeo;
	private boolean isExponential;
	private double contextAlpha;
	private Trace traceType;
	/** Decision rule type. **/
	private Response decisionRule;
	private int randomPhases;
	private ModelControl control;
	/** Decay for decision rule. **/
	private double decay;
	/** Timing per trial type. **/
	private boolean timingPerTrial;
	/** Are we using serial configurals? **/
	private boolean serialConfigurals;
	private boolean isZeroTrace;
	private boolean serialCompounds;
	/** Restrict predictions to >= 0 **/
	private boolean restrictPredictions;
	/** Decay for fuzzy activation. **/
	private double activationDecay;
	/** Dropoff for fuzzy activation. **/
	private double activationDropoff;

    public int getResponsesPerMinute() {
        return responsesPerMinute;
    }

    public void setResponsesPerMinute(int responsesPerMinute) {
        this.responsesPerMinute = responsesPerMinute;
    }

    private int responsesPerMinute;

    public double getSerialResponseWeight() {
        return serialResponseWeight;
    }

    public void setSerialResponseWeight(double serialResponseWeight) {
        this.serialResponseWeight = serialResponseWeight;
    }

    private double serialResponseWeight;

    public boolean isConfiguralCompounds() {
        return isConfiguralCompounds;
    }

    public void setConfiguralCompounds(boolean configuralCompounds) {
        isConfiguralCompounds = configuralCompounds;
    }

    private boolean isConfiguralCompounds;

	/**
	 * SimModel's Constructor method.
	 */
	public SimModel() {
		values = new TreeMap<String, Double>();
		groups = new LinkedHashMap<String, SimGroup>();
		// Initial values of groups, phases and combinations.
		groupsNo = phasesNo = 1;
		combinationNo = 100; // 20; modified Alberto Fernández July-2011
		// Alberto Fernández August-2011
		configCuesNames = new TreeMap<String, String>();
		// Alberto Fernandez Nov-2011
		useContext = false;
		listAllCues = new ArrayList<String>();
		// if(useContext) {listAllCues.add(Simulator.OMEGA+"");}
		threshold = 0.875;
		decay = 0.5;
		showResponse = true;
		isContextAcrossPhase = false;
		isCSC = true;
		timestepSize = 1;
		isGeo = false;
		isExponential = true;
		traceType = Trace.REPLACING;
		decisionRule = Response.CHURCH_KIRKPATRICK;
		randomPhases = 0;
		variableDistributionCombinationNo = 100;
		setTimingPerTrial(false);
		serialConfigurals = true;
		restrictPredictions = true;
		activationDecay = 0.15;
		activationDropoff = 0.2;
        isConfiguralCompounds = false;
        serialResponseWeight = 0.85;
        responsesPerMinute = 100;
	}

	/**
	 * Adds the new cues from every phases into the SortedMap. The new cues will
	 * be sorted accordingly depending on the symbol character. Their initial
	 * Double value is null because it is only on the 1st stage of the
	 * experiment.
	 * 
	 * @param map
	 *            the cues HashMaps deriving from every new group.
	 */
	public void addCueNames(Map<String, CueList> map) {
		Iterator<Entry<String, CueList>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, CueList> pair = it.next();
			values.put(pair.getValue().getSymbol(), 0d);

			// Alberto Fernandez Nov-2011
			if (!listAllCues.contains(pair.getValue().getSymbol())) {
				listAllCues.add(pair.getValue().getSymbol());
			}
		}
	}

	/**
	 * Adds a new group into the experiment. Adds the group into the groups
	 * HashMap and adds the groups new cues in the cue list if it doesn't
	 * already exist. The cue list is variable for the value table that is
	 * created for the user to input the 'alpha', 'beta' and 'lambda' values.
	 * 
	 * @param name
	 *            the name of the group. By default the name is 'Group #', where
	 *            # an ascending integer starting from 1.
	 * @param group
	 *            the SimGroup object which contains the phases and all other
	 *            necessary variables to accomplish an experiment.
	 */
	public void addGroupIntoMap(String name, SimGroup group) {
		groups.put(name, group);
		addCueNames(group.getCuesMap());
		randomPhases += group.numRandom();
	}

	/**
	 * Add the 'lambda' values for each phase in the values of the model
	 */
	public void addValuesIntoMap() {
		// The initial values of the 'lambda' are null a value indicating that
		// they
		// haven't been assigned with any double values yet.
		for (int p = 1; p <= phasesNo; p++) {

			boolean atLeastOneGroupPlus = false;
			boolean atLeastOneGroupMinus = false;

			Iterator<Entry<String, SimGroup>> iterGroup = groups.entrySet()
					.iterator();
			while (iterGroup.hasNext()) {
				Entry<String, SimGroup> pairGroup = iterGroup.next();
				SimGroup group = pairGroup.getValue();
				if (group.getPhases().get(p - 1).getLambdaPlus() != null)
					atLeastOneGroupPlus = true;
				if (group.getPhases().get(p - 1).getLambdaMinus() != null)
					atLeastOneGroupMinus = true;
				if (atLeastOneGroupPlus && atLeastOneGroupMinus)
					break;
			}
			if (atLeastOneGroupPlus) {
				values.put("lambda p" + p, null);
				values.put("beta+ p" + p, null);
			}
			if (atLeastOneGroupMinus) {
				values.put("lambda p" + p, null);
				values.put("beta- p" + p, null);
			}
		}
	}

	public void clearConfiguralMap() {
		configCuesNames.clear();
	}

	public boolean contextAcrossPhase() {
		return isContextAcrossPhase;
	}

	// Alberto Fern·ndez August-2011
	/**
	 * Returns the external name of a configural cue or configural compound e.g.
	 * configural cues: a --> c(AB), configural compounds: ABb --> [AB]
	 */
	public String cueName2InterfaceName(String cueName) {
		String interfaceName;
		boolean configural = false;
		String configurals = "";
		// Scan for a lowercase character
		for (int i = 0; i < cueName.length(); i++) {
			if (cueName.charAt(i) > 96 && cueName.charAt(i) < 900) {
				configural = true;
				configurals += "" + cueName.charAt(i);
			}
		}

		if (configural && !cueName.equals(Simulator.OMEGA + "")) {
			String compoundName;
			if (cueName.length() == 1) {
				// configural cue
				// retrieve compound name
				compoundName = configCuesNames.get(cueName);
				// interfaceName = "¢(" + compoundName + ")";
				interfaceName = "c(" + compoundName + ")";
			} else if(cueName.contains("'")) {
                compoundName = cueName.replaceAll(configurals, "");
                // interfaceName = "[" + compoundName + "¢]";
                interfaceName = compoundName;
            } else {
				// configural compound - remove configural names

				compoundName = cueName.replaceAll(configurals, "");
				// interfaceName = "[" + compoundName + "¢]";
				interfaceName = "[" + compoundName + "]";
			}
		} else {
			interfaceName = cueName;
		}
		return interfaceName;
	}

	/**
	 * @return the activationDecay
	 */
	public double getActivationDecay() {
		return activationDecay;
	}

	/**
	 * @return the activationDropoff
	 */
	public double getActivationDropoff() {
		return activationDropoff;
	}

	/**
	 * Returns the number of alpha cues in the model
	 * 
	 * @return number of alpha cues in the model
	 */
	public TreeMap<String, Double> getAlphaCues() {
		TreeMap<String, Double> tm = new TreeMap<String, Double>();
		Iterator<String> it = values.keySet().iterator();
		while (it.hasNext()) {
			String pair = it.next();
			if (pair.indexOf("lambda") == -1 && pair.indexOf("beta") == -1)
				tm.put(pair, values.get(pair));
		}
		return tm;
	}

	/**
	 * Returns the number of different combinations that need to be processed on
	 * the sequence stimuli order in order to have a random stimuli execution.
	 * The number is set to a default of 20 but can rise into a 3 digit number
	 * as well. Although bigger the number is, longer the time to process.
	 * 
	 * @return the number of combinations for the random stimuli sequence.
	 */
	public int getCombinationNo() {
		return combinationNo;
	}

	public Map<String, String> getConfigCuesNames() {
		return configCuesNames;
	}

	/**
	 * 
	 * @return the default context alpha this model uses.
	 */

	public double getContextAlpha() {
		return contextAlpha;
	}

	/**
	 * @param contexts
	 */
	public Map<String, ContextConfig> getContexts() {
		Map<String, ContextConfig> contexts = new HashMap<String, ContextConfig>();
		for (SimGroup group : groups.values()) {
			contexts.putAll(group.getContexts());
		}
		return contexts;
	}

	/**
	 * Returns the keySet of the cues HashMap. In other words it returns the
	 * symbol that each cue from the experiment has. This is been used on the
	 * acomplise of the value table.
	 * 
	 * @return the keySet of the cues HashMap.
	 */
	public Set<String> getCueNames() {
		return values.keySet();
	}

	/**
	 * @return the decay
	 */
	public double getDecay() {
		return decay;
	}

	/**
	 * @return the decisionRule
	 */
	public Response getDecisionRule() {
		return decisionRule;
	}

	/**
	 * Returns the total number of groups. This number can always be changed by
	 * the user.
	 * 
	 * @return the number of groups of the experiment.
	 */
	public int getGroupNo() {
		return groupsNo;
	}

	/**
	 * Returns the HashMap of the groups. The mapping is described from a key
	 * value which is the the name of the groups and the actual values are
	 * SimGroup objects.
	 * 
	 * @return the group objects in a HashMap.
	 */
	public Map<String, SimGroup> getGroups() {
		return groups;
	}

	private String getKey(Map<String, String> map, String value) {
		Set<String> keys = map.keySet();
		boolean found = false;
		String key = null;
		Iterator<String> it = keys.iterator();
		while (!found && it.hasNext()) {
			key = it.next();
			if (map.get(key).equals(value)) {
				found = true;
			}
		}
		return key;
	}

	// Alberto Fernandez Nov-2011
	public List<String> getListAllCues() {
		return listAllCues;
	}

	/**
	 * Returns the number of alpha cues in the model
	 * 
	 * @return number of alpha cues in the model
	 */
	public int getNumberAlphaCues() {
		int cont = 0;
		Iterator<String> it = values.keySet().iterator();
		while (it.hasNext()) {
			String pair = it.next();
			if (pair.indexOf("lambda") != -1 || pair.indexOf("beta") != -1)
				cont++;
		}
		return (values.size() - cont);
	}

	/**
	 * Returns the number of phases that every group has. The phases are the
	 * same for every group as it is unwise to run an experiment with different
	 * number of phases.
	 * 
	 * @return the number of phases that every group has.
	 */
	public int getPhaseNo() {
		return phasesNo;
	}

	/**
	 * @return the randomSeed
	 */
	public long getRandomSeed() {
		return randomSeed;
	}

	/**
	 * Get the response threshold for this simulation.
	 * 
	 * @return
	 */

	public double getThreshold() {
		return threshold;
	}

	/**
	 * @return the timestepSize
	 */
	public double getTimestepSize() {
		return timestepSize;
	}

	/**
	 * @return true if this model uses bounded traces.
	 */
	public Trace getTraceType() {
		return traceType;
	}

	// Added by Alberto Fernández July-2011
	/**
	 * Returns the values of parameters in the model
	 * 
	 * @return values of parameters in the model
	 */
	public Map<String, Double> getValues() {
		return values;
	}

	/**
	 * @return the variableDistributionCombinationNo
	 */
	public int getVariableCombinationNo() {
		return variableDistributionCombinationNo;
	}

	/**
	 * Returns the internal name of a configural cue or configural compound e.g.
	 * configural cues: c(AB) --> a, configural compounds: [AB] --> ABb
	 */
	public String interfaceName2cueName(String interfaceName) {
		String cueName;
		if (interfaceName.contains("(")) { // is configural cue, e.g. c(AB)
			cueName = getKey(configCuesNames,
					interfaceName.substring(2, interfaceName.length() - 1));
		} else if (interfaceName.contains("[")) { // configural compound
			cueName = interfaceName.substring(1, interfaceName.length() - 2)
					+ getKey(configCuesNames, interfaceName.substring(1,
							interfaceName.length() - 1));
		} else {
			cueName = interfaceName;
		}
		return cueName;
	}

	/**
	 * 
	 * @return true if this is a CSC simulation.
	 */
	public boolean isCSC() {
		return isCSC;
	}

	/**
	 * 
	 * @return true if this model uses the exponential distribution.
	 */

	public boolean isExponential() {
		return isExponential;
	}

	/**
	 * @return true if this model uses the geometric mean for variable
	 *         distributions.
	 */
	public boolean isGeometricMean() {
		return isGeo;
	}

	/**
	 * @return the restrictPredictions
	 */
	public boolean isRestrictPredictions() {
		return restrictPredictions;
	}

	/**
	 * @return true if serial compounds are in use.
	 */
	public boolean isSerialCompounds() {
		return serialCompounds;
	}

	/**
	 * @return true if serial configurals are in use.
	 */
	public boolean isSerialConfigurals() {
		return serialConfigurals;
	}

	/**
	 * @return the timingPerTrial
	 */
	public boolean isTimingPerTrial() {
		return timingPerTrial;
	}

	/**
	 * @return the a boolean indicated that the context should be used
	 */
	public boolean isUseContext() {
		return useContext;
	}

	/**
	 * @return true if traces should be set to 0 between trials
	 */
	public boolean isZeroTraces() {
		return isZeroTrace;
	}

	/**
	 * Initializes the values and groups of the SimModel
	 */
	public void reinitialize() {
		values = new TreeMap();
		groups = new LinkedHashMap<String, SimGroup>();
		// Alberto Fernández August-2011
		configCuesNames = new TreeMap<String, String>();
		// Alberto Fernandez Nov-2011
		// listAllCues = new ArrayList<String>();
	}

	@Override
	public void run() {
		startCalculations();
	}

	/**
	 * @param activationDecay
	 *            the activationDecay to set
	 */
	public void setActivationDecay(double activationDecay) {
		this.activationDecay = activationDecay;
	}

	/**
	 * @param activationDropoff
	 *            the activationDropoff to set
	 */
	public void setActivationDropoff(double activationDropoff) {
		this.activationDropoff = activationDropoff;
	}

	/**
	 * Sets an new number of random combinations that take on the experiment
	 * when the user chooses random on the groups.
	 * 
	 * @param r
	 *            a number indicating the combinations. This is being used on a
	 *            iterating function that produces random stimuli position
	 *            following the given sequence.
	 */
	public void setCombinationNo(int r) {
		combinationNo = r;
	}

	public void setContextAcrossPhase(boolean on) {
		isContextAcrossPhase = on;
	}

	/**
	 * 
	 * @param alpha
	 *            the default context alpha this model uses.
	 */

	public void setContextAlpha(final double alpha) {
		contextAlpha = alpha;
	}

	/**
	 * @param control
	 */
	public void setControl(ModelControl control) {
		this.control = control;
		for (SimGroup group : groups.values()) {
			group.setControl(control);
		}
	}

	/**
	 * 
	 * @param on
	 *            set to true if using CSC cues
	 */

	public void setCSC(boolean on) {
		isCSC = on;
	}

	/**
	 * @param decay
	 *            the decay to set
	 */
	public void setDecay(double decay) {
		this.decay = decay;
	}

	/**
	 * @param decisionRule
	 *            the decisionRule to set
	 */
	public void setDecisionRule(Response decisionRule) {
		this.decisionRule = decisionRule;
	}

	/**
	 * Sets an new number of groups that taking place on the experiment.
	 * 
	 * @param g
	 *            a number indicating the new number of groups.
	 */
	public void setGroupNo(int g) {
		groupsNo = g;
	}

	/**
	 * 
	 * @param exp
	 *            set to true if this model uses the exponential distribution.
	 */

	public void setIsExponential(boolean exp) {
		isExponential = exp;
	}

	/**
	 * 
	 * @param on
	 *            set to true if this model uses the geometric mean.
	 */

	public void setIsGeometricMean(boolean on) {
		isGeo = on;
	}

	/**
	 * Sets an new number of phases that taking place on the experiment.
	 * 
	 * @param p
	 *            a number indicating the new number of phases.
	 */
	public void setPhaseNo(int p) {
		phasesNo = p;
	}

	/**
	 * @param randomSeed
	 *            the randomSeed to set
	 */
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	/**
	 * @param restrictPredictions
	 *            the restrictPredictions to set
	 */
	public void setRestrictPredictions(boolean restrictPredictions) {
		this.restrictPredictions = restrictPredictions;
	}

	/**
	 * 
	 * @param on
	 *            set to true if serial compounds are in use.
	 */

	public void setSerialCompounds(boolean on) {
		serialCompounds = on;
	}

	public void setSerialConfigurals(boolean on) {
		serialConfigurals = on;
	}

	/**
	 * 
	 * @param on
	 *            true if the simulated response should be shown.
	 */

	public void setShowResponse(boolean on) {
		showResponse = on;
	}

	/**
	 * 
	 * @param n
	 *            the threshold for simulated response.
	 */

	public void setThreshold(double n) {
		threshold = n;
	}

	/**
	 * @param timestepSize
	 *            the timestepSize to set
	 */
	public void setTimestepSize(double timestepSize) {
		this.timestepSize = timestepSize;
	}

	/**
	 * @param timingPerTrial
	 *            the timingPerTrial to set
	 */
	public void setTimingPerTrial(boolean timingPerTrial) {
		this.timingPerTrial = timingPerTrial;
	}

	/**
	 * @param useBoundedTrace
	 *            true if bounded traces are used.
	 */
	public void setTraceType(Trace trace) {
		traceType = trace;
	}

	/**
	 * @param useContext
	 *            whether context is used
	 */
	public void setUseContext(boolean on) {
		this.useContext = on;
		if (useContext) {
			if (!listAllCues.contains(Simulator.OMEGA + "")) {
				listAllCues.add(Simulator.OMEGA + "");
			}
		} else {
			listAllCues.remove(Simulator.OMEGA + "");
		}
	}

	/**
	 * @param variableDistributionCombinationNo
	 *            the variableDistributionCombinationNo to set
	 */
	public void setVariableCombinationNo(int variableDistributionCombinationNo) {
		this.variableDistributionCombinationNo = variableDistributionCombinationNo;
	}

	/**
	 * 
	 * @param on
	 *            Set to true to force traces to 0 at the end of each trial.
	 */

	public void setZeroTraces(boolean on) {
		isZeroTrace = on;
	}

	/**
	 * 
	 * @return true if the simulated response should be shown.
	 */

	public boolean showResponse() {
		return showResponse;
	}

	/**
	 * Starts the process of the calculation. It executes the run() method of
	 * every group which implements the Runnable for multi-thread calculations.
	 * This will speed up the processes because they are running in concurrent
	 * dimension.
	 */
	public void startCalculations() {
		/*
		 * Iterator<Entry<String, SimGroup>> iterGroup =
		 * groups.entrySet().iterator(); while (iterGroup.hasNext()) {
		 * Entry<String, SimGroup> pairGroup = iterGroup.next();
		 * pairGroup.getValue().run(); }
		 */

		// J Gray - 2012: This section wasn't actually running concurrent, but
		// now does
		listAllCues.clear();
		groupPool = ParallelArray.createEmpty(groupsNo, SimGroup.class,
				Simulator.fjPool);
		groupPool.asList().addAll(groups.values());
        try {
		    groupPool.withMapping(update).all();
		    control.incrementProgress(1);
        } catch (OutOfMemoryError e) {
            System.err.println("Ran outa memory. Sadness.");
        }
        control.setComplete(true);
        groupPool = null;
	}

	/**
	 * It produces a text output of all the group results. It adds the headers
	 * of the groups and then retrieves the text output of every group and adds
	 * it to one string which at the end it will be presented on the main view.
	 * 
	 * @return the final output of the results.
	 */
	public String textOutput(boolean compound) {
		StringBuffer result = new StringBuffer();
		String sep = "------------------------\n";
		Iterator<Entry<String, SimGroup>> iterGroup = groups.entrySet()
				.iterator();
		while (iterGroup.hasNext()) {
			Entry<String, SimGroup> pairGroup = iterGroup.next();
			SimGroup tempGroup = pairGroup.getValue();
			result.append(sep).append(tempGroup.getNameOfGroup()).append('\n')
					.append(sep);
			result.append(tempGroup.phasesOutput(compound, configCuesNames));
		}
		return result.toString();
	}

	public int totalNumPhases() {
		int total = 0;
		for (SimGroup group : groups.values()) {
			total += group.trialCount();
		}
		return total;
	}

	/**
	 * Updates the values from the SortedList to the values in the model
	 * Modified to add gamma and delta parameters.. J Gray
	 * 
	 * @param name
	 *            the key of the SortedList. In other words the name or symbol
	 *            of the cue or 'lambda' or 'beta'.
	 * @param phase
	 *            from which is going to update the value
	 * @param value
	 *            the value that is been given from the user.
	 */
	public void updateValues(String name, int phase, String value) {

		if (value.equals("")) {
			// beta+ p1, lambda+ p1 y lambda- p1 are never empty after
			// checkValuesTable()
			if (name.indexOf("beta+") != -1)
				values.put(name + " p" + phase, values.get("beta+ p1"));
			else if (name.indexOf("beta-") != -1)
				values.put(name + " p" + phase, values.get("beta+ p1")); // if
																			// beta-
																			// is
																			// empty,
																			// take
																			// beta+
			else if (name.indexOf("lambda+") != -1)
				values.put(name + " p" + phase, values.get("lambda+ p1"));
			else if (name.indexOf("lambda-") != -1)
				values.put(name + " p" + phase, values.get("lambda- p1"));
			else if (name.indexOf("gamma") != -1)
				values.put(name + " p" + phase, values.get("gamma p1"));
			else if (name.indexOf("delta") != -1)
				values.put(name + " p" + phase, values.get("delta p1"));
			else if (name.indexOf("ω") != -1)
				values.put(name + " p" + phase, values.get("ω p1"));
		} else if (name.indexOf("beta+") != -1 || name.indexOf("beta-") != -1
				|| name.indexOf("lambda+") != -1
				|| name.indexOf("lambda-") != -1 || name.indexOf("gamma") != -1
				|| name.indexOf("delta") != -1 || name.indexOf("ω") != -1)
			values.put(name + " p" + phase, new Double(value));
		else {
			// Alberto Fernández August-2011
			// here, the "virtual" name have to be considered:
			// values.put(name, new Double(value));
			if (name.length() > 1) {
				String compoundName = name.substring(2, name.length() - 1); // ex.
																			// c(AB)
																			// -->
																			// AB
				String virtualName = SimGroup.getKeyByValue(configCuesNames,
						compoundName);
				values.put(virtualName, new Double(value));
			} else {
				values.put(name, new Double(value));
			}
		}
	}

	/**
	 * Updates every group with the new values that derived from the value
	 * table. It iterates through every group and clears them from any values
	 * that they possible have and then continues with a new iteration through
	 * their cue HashMap and updates as necessary. The same with 'lambda' and
	 * 'beta' values.
	 */
	public void updateValuesOnGroups() {

		Iterator<Entry<String, SimGroup>> iterGroup = groups.entrySet()
				.iterator();
		while (iterGroup.hasNext()) {
			Entry<String, SimGroup> pairGroup = iterGroup.next();
			SimGroup tempGroup = pairGroup.getValue();
			tempGroup.clearResults();
			Iterator<Entry<String, CueList>> iterCue = tempGroup.getCuesMap()
					.entrySet().iterator();
			while (iterCue.hasNext()) {
				Entry<String, CueList> pairCue = iterCue.next();
				CueList tempCscCue = pairCue.getValue();
				tempCscCue.reset();
				tempCscCue.setAlpha(values.get(tempCscCue.getSymbol() + ""));
			}
			// Update the values of the lambdas and betas per phase in the
			// current group
			for (int p = 1; p <= phasesNo; p++) {
				if (values.containsKey("lambda+ p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setLambdaPlus(values.get("lambda+ p" + p));
				}
				if (values.containsKey("gamma p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setGamma(values.get("gamma p" + p));
				}
				if (values.containsKey("lambda- p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setLambdaMinus(values.get("lambda- p" + p));
				}
				if (values.containsKey("beta+ p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setBetaPlus(values.get("beta+ p" + p));
				}
				if (values.containsKey("beta- p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setBetaMinus(values.get("beta- p" + p));
				}
				if (values.containsKey("delta p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setDelta(values.get("delta p" + p));
				}
				if (values.containsKey("ω p" + p)) {
					tempGroup.getPhases().get(p - 1)
							.setContextSalience(values.get("ω p" + p));
				}
			}
		}
	}

}