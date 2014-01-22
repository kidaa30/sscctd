package simulator;

import simulator.util.Response;
import simulator.util.Trace;

/**
 * A CSC specifically for storing compound results.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class CompoundCueList extends CueList {

	/**
	 * @param symbol
	 * @param alpha
	 */
	public CompoundCueList(String symbol, double alpha, double threshold,
			double decay) {
		super(symbol, alpha, threshold, decay);
		getAverageWeights().clear();
	}

	/**
	 * @param symbol
	 * @param alpha
	 * @param trace
	 */
	public CompoundCueList(String symbol, double alpha, double threshold,
			double decay, Trace trace, Response decisionRule) {
		super(symbol, alpha, threshold, decay, trace, decisionRule);
		getAverageWeights().clear();
	}

	/**
	 * Removes a 0 from the associative weights vector when generating a new cue
	 * as weights are calculated externally.
	 */

	@Override
	public SimCue nextCue() {
		SimCue thisCue;
		if (!cueIt.hasNext()) {
			thisCue = super.nextCue();
			thisCue.getAssocValueVector().remove(0);
			thisCue.getAvgAssocValueVector().remove(0);
			thisCue.getResponses().remove(0);
		} else {
			thisCue = super.nextCue();
		}
		return thisCue;
	}

	/**
	 * Override to add in zeroing the 'working' assoc of compounds.
	 */

	@Override
	public void store() {
		super.store();
		for (int i = 0; i < cues.size(); i++) {
			SimCue cue = cues.get(i);
			cue.setAssocValue(0d);
		}
	}

}
