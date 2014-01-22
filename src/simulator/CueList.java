/**
 * 
 */
package simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import simulator.util.MemoryTrace;
import simulator.util.Response;
import simulator.util.Trace;

/**
 * A cuelist, represents a CSC stimulus.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class CueList implements Iterable<SimCue> {

	/** List of cues. **/
	protected List<SimCue> cues;
	/** List iterator for cues. **/
	protected ListIterator<SimCue> cueIt;
	/** Cue symbol. **/
	private String symbol;
	/** Cue alpha. **/
	private double alpha = 0d;
	/** Trace type, true for bounded. **/
	private Trace traceType;
	/** Number of trials. **/
	private int trialCount;
	/** Average assocs. **/
	private List<Double> averageWeights;
	/** Average weights. **/
	private List<Double> averageResponse;
	/** Max cue index encountered. */
	private int maxCue;
	private List<Double> zeroFill;
	/** Threshold for a response. **/
	private double threshold;
	/** List of maximum cues for trials. **/
	private List<Integer> maxCueList;
	private Response decisionRule;
	private double decay;
	/** Presence trace type. **/
	private MemoryTrace memTrace;

	public CueList(String symbol, double alpha, double threshold, double decay) {
		this.alpha = alpha;
		this.symbol = symbol;
		cues = new ArrayList<SimCue>();
		cueIt = cues.listIterator();
		traceType = Trace.REPLACING;
		trialCount = 0;
		averageWeights = new ArrayList<Double>();
		averageWeights.add(0d);
		averageResponse = new ArrayList<Double>();
		// averageResponse.add(0d);
		maxCue = 0;
		zeroFill = new ArrayList<Double>();
		zeroFill.add(0d);
		this.threshold = threshold;
		maxCueList = new ArrayList<Integer>();
		maxCueList.add(0);
		this.decay = decay;
		memTrace = simulator.util.MemoryTrace.MONDRAGON;
	}

	public CueList(String symbol, double alpha, double threshold, double decay,
			Trace trace, Response decisionRule) {
		this(symbol, alpha, threshold, decay);
		traceType = trace;
		this.decisionRule = decisionRule;
	}

	public CueList(String symbol, double alpha, double threshold, double decay,
			Trace trace, Response decisionRule, MemoryTrace memTrace) {
		this(symbol, alpha, threshold, decay, trace, decisionRule);
		this.memTrace = memTrace;
	}

	/**
	 * Adds a cue to the end of the list.
	 * 
	 * @param cue
	 *            the cue to be added.
	 */

	public void add(SimCue cue) {
		cueIt.add(cue);
	}

	/**
	 * @param addCues
	 *            collection of cues to be added to this list.
	 */

	public void addAll(Collection<SimCue> addCues) {
		cues.addAll(addCues);
	}

	/**
	 * 
	 * @param trial
	 *            number to get average for.
	 * @return a double giving the average V for all cues at the given trial
	 */

	public double averageAssoc(int trial) {
		return averageWeights.get(trial);
	}

	public double averageResponse(int trial) {
        double response = 0;
        try {
            response = averageResponse.get(trial);
        } catch (IndexOutOfBoundsException e) {
            //Ignore and just return 0.
            System.err.println("Missing average response.");
        }
		return response;
	}

	/**
	 * Copy all the cues here into a new list with empty V vectors, but the
	 * working V set.
	 * 
	 * @return a copy of the cues in this list
	 */

	public CueList copy() {
		CueList newList = new CueList(symbol, alpha, threshold, decay,
				traceType, decisionRule);
		for (SimCue cue : cues) {
			SimCue tmp = cue.copy();
			tmp.setAssocValue(cue.getLastAssocValue());
			tmp.getAssocValueVector().add(cue.getLastAssocValue());
			tmp.getAvgAssocValueVector().add(cue.getLastAvgAssocValue());
			tmp.getResponses().add(
					cue.getResponses().get(cue.getResponses().size() - 1));
			newList.add(tmp);
		}
		newList.getAverageWeights().clear();
		newList.getAverageWeights().add(
				averageWeights.get(averageWeights.size() - 1));

		newList.getMaxCueList().clear();
		newList.getMaxCueList().add(maxCueList.get(maxCueList.size() - 1));
		return newList;
	}

	/**
	 * Expand the V vectors of all cues to the given new size.
	 * 
	 * @param newSize
	 *            new size of V vectors
	 */

	public void expand(int newSize) {
		for (SimCue cue : cues) {
			do {
				cue.getAssocValueVector().add(0d);
				cue.getAvgAssocValueVector().add(0d);
			} while (cue.getAssocValueSize() < newSize);
		}
		trialCount++;
	}

	/**
	 * 
	 * @param index
	 *            to return the cue at
	 * @return the cue at the given index.
	 */

	public SimCue get(int index) {
		return cues.get(index);
	}

	/**
	 * @return a double holding the alpha for this cue
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * @return the averageResponse
	 */
	public List<Double> getAverageResponse() {
		return averageResponse;
	}

	/**
	 * 
	 * @param threshold
	 *            threshold for a response.
	 * @param timeStep
	 *            step in time/cue to get response at.
	 * @return the simulated response for the cue at that timestep.
	 */
	@Deprecated
	public int getAverageResponse(double threshold, int trial) {
		int responses = 0;
		Random random = new Random();
		for (int i = 0; i < 60; i++) {
			double boundary = random.nextDouble();
			int response = averageAssoc(trial) > boundary * threshold ? 1 : 0;
			response = boundary < 4 / 60 ? 1 : response;
			responses += response;
		}
		return responses;
	}

	/**
	 * @return the list of average weights at each trial.
	 */
	public List<Double> getAverageWeights() {
		return averageWeights;
	}

	/**
	 * @return the decay
	 */
	public double getDecay() {
		return decay;
	}

	/**
	 * 
	 * @return the index of the last cue returned.
	 */

	public int getIndex() {
		return cueIt.previousIndex();
	}

	/**
	 * 
	 * @return the underlying cue list.
	 */

	public List<SimCue> getList() {
		return cues;
	}

	/**
	 * @return the maxCueList
	 */
	public List<Integer> getMaxCueList() {
		return maxCueList;
	}

	/**
	 * 
	 * @return the full name of this cue (generally the same as symbol.)
	 */

	public String getName() {
		return symbol;
	}

	public double getPresence(boolean active) {
		return memTrace.get(0.15, 0.2, maxCue + 1, active);
	}

	/**
	 * @param offset
	 * @param onset
	 * @param j
	 * @return
	 */
	public double getPresence(int offset, int onset, int timestep) {
		boolean active = (onset <= timestep) && (timestep < offset);
		int time = active ? timestep - onset : timestep - offset;
		double decay = Simulator.getController().getModel()
				.getActivationDecay();
		double dropoff = Simulator.getController().getModel()
				.getActivationDropoff();
		return memTrace.get(decay, dropoff, time + 1, active);
	}

	public double getResponse(double threshold, int time) {
		return cues.get(time).response(cues.get(time).getResponses().size() - 1);
	}

	/**
	 * 
	 * @return a string holding this CSC cue's symbol
	 */

	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * 
	 * @return the type of trace this csc uses.
	 */

	public Trace getTraceType() {
		return traceType;
	}

	/**
	 * @return the number of trials this CSC has been in.
	 */
	public int getTrialCount() {
		return trialCount;
	}

	/**
	 * 
	 * @return true if there are no cues in this list
	 */

	public boolean isEmpty() {
		return cues.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<SimCue> iterator() {
		return cues.iterator();
	}

	/**
	 * 
	 * @return either the next cue, or a newly created one with weight history
	 *         zero-padded to match the number of trials so far.
	 */

	public SimCue nextCue() {
		if (!cueIt.hasNext()) {
			SimCue cue = new SimCue(symbol, alpha, traceType, decisionRule);
			cue.getAssocValueVector().addAll(zeroFill);
			cue.getAvgAssocValueVector().addAll(zeroFill);
			cue.getResponses().addAll(zeroFill);
			cueIt.add(cue);
			cueIt.previous();
		}
		maxCue = cueIt.nextIndex();
		SimCue thisCue = cueIt.next();
		return thisCue;
	}

	/**
	 * Reset all the components.
	 */

	public void reset() {
		for (SimCue cue : cues) {
			cue.reset();
		}
	}

	/**
	 * Restart the cue iterator.
	 */

	public void restart() {
		cueIt = cues.listIterator();
	}

	/**
	 * 
	 * @param alpha
	 *            the salience of this CSC
	 */

	public void setAlpha(double alpha) {
		this.alpha = alpha;
		for (SimCue cue : cues) {
			cue.setAlpha(alpha);
		}
	}

	/**
	 * @param averageResponse
	 *            the averageResponse to set
	 */
	public void setAverageResponse(List<Double> averageResponse) {
		this.averageResponse = averageResponse;
	}

	/**
	 * @param averageWeights
	 *            a list of averaged weights to set.
	 */
	public void setAverageWeights(List<Double> averageWeights) {
		this.averageWeights = averageWeights;
	}

	/**
	 * @param decay
	 *            the decay to set
	 */
	public void setDecay(double decay) {
		this.decay = decay;
	}

	/**
	 * @param maxCueList
	 *            the maxCueList to set
	 */
	public void setMaxCueList(List<Integer> maxCueList) {
		this.maxCueList = maxCueList;
	}

	/**
	 * @param symbol
	 *            the symbol for this cue
	 */
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * @param threshold
	 *            the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * 
	 * @param trace
	 *            the type of trace this csc uses.
	 */

	public void setTraceType(Trace trace) {
		traceType = trace;
	}

	/**
	 * @param trialCount
	 *            the number of trials this CSC has been in.
	 */
	public void setTrialCount(int trialCount) {
		this.trialCount = trialCount;
	}

	/**
	 * 
	 * @return the number of cues.
	 */

	public int size() {
		return cues.size();
	}

	/**
	 * Push the working V value to the V vector for all cues.
	 */

	public void store() {
		double avg = 0;
		double avgResp = 0;
		for (int i = 0; i < cues.size(); i++) {
			SimCue cue = cues.get(i);
			cue.makeResponse(threshold, getDecay());
			if (i <= maxCue) {
				avg += cue.getLastAssocValue();
				avgResp += cue.response(trialCount);
			}
			cue.store();
		}
		avg /= maxCue + 1;
		avgResp /= maxCue + 1;
		averageWeights.add(avg);
		averageResponse.add(avgResp);
		trialCount++;
		zeroFill.add(0d);
		maxCueList.add(maxCue);
		maxCue = 0;
	}
	
	public void remakeResponses() {
		averageResponse.clear();
		for(SimCue cue : cues) {
			cue.getResponses().clear();
			cue.getAvgAssocValueVector().add(cue.getLastAvgAssocValue());
		}
		for(int t = 0; t < trialCount; t++) {
			double avgResp = 0;
			for (int i = 0; i < cues.size(); i++) {
				SimCue cue = cues.get(i);
                try {
				    cue.setAvgAssocValue(cue.getAvgAssocValueVector().get(t));
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Oops..");
                }
				cue.makeResponse(threshold, getDecay());
				if (i <= maxCueList.get(t)) {
					avgResp += cue.response(t);
				}
			}
			avgResp /= maxCueList.get(t) + 1;
			averageResponse.add(avgResp);			
		}
	}

	@Override
	public String toString() {
		return symbol + " α(" + alpha + "): " + cues;
	}

	/**
	 * Update all the cues in the list.
	 * 
	 * @param betaError
	 *            TD error term
	 * @param delta
	 *            trace decay
	 * @param gamma
	 *            discount factor
	 */

	public void update(double betaError, double delta, double gamma) {
		if (Math.abs(alpha * betaError) <= Double.MIN_VALUE) {
			for (int i = 0; i < cues.size(); i++) {
				cues.get(i).updateTrace(delta, gamma);
			}
		} else {
			for (int i = 0; i < cues.size(); i++) {
				cues.get(i).update(betaError, delta, gamma);
				cues.get(i).updateTrace(delta, gamma);
			}
		}
	}

	/**
	 * 
	 */
	public void zeroTraces() {
		for (SimCue cue : cues) {
			cue.setTrace(0d);
		}
	}

    public double weightedSum(CueList compound, int trial) {
        double weight = Simulator.getController().getModel().getSerialResponseWeight();
        return (averageResponse(trial)*weight + compound.averageResponse(trial)*(1 - weight)) / 2;
    }
}
