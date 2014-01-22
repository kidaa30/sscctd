/**
 * 
 */
package simulator;

import simulator.util.Response;
import simulator.util.Trace;

/**
 * Special version of a cuelist for probe cues to override the cue checking and
 * remove the probe notation N(MMM) -> N.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class ProbeCueList extends CompoundCueList {

	/**
	 * @param symbol
	 * @param alpha
	 * @param threshold
	 * @param decay
	 */
	public ProbeCueList(String symbol, double alpha, double threshold,
			double decay) {
		super(symbol, alpha, threshold, decay);
	}

	/**
	 * @param symbol
	 * @param alpha
	 * @param threshold
	 * @param decay
	 * @param trace
	 * @param decisionRule
	 */
	public ProbeCueList(String symbol, double alpha, double threshold,
			double decay, Trace trace, Response decisionRule) {
		super(symbol, alpha, threshold, decay, trace, decisionRule);
	}

	/**
	 * 
	 * @return the textual name of this cue, i.e. name + trial string.
	 */

	@Override
	public String getName() {
		return super.getSymbol();
	}

	/**
	 * Get the symbol for this cue, i.e. the letter name.
	 */

	@Override
	public String getSymbol() {
		String symbol = super.getSymbol();
		String[] parts = symbol.split("\\(");
		return parts[0];
	}

}
