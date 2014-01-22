/**
 * 
 */
package simulator.util;

import simulator.Simulator;

/**
 * Class representing a uniform distribution.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class UniformVariableDistribution extends VariableDistribution {

	/**
	 * @param mean
	 * @param sd
	 * @param seed
	 * @param trials
	 */
	public UniformVariableDistribution(double mean, float sd, long seed,
			int trials, boolean meanType) {
		super(mean, sd, seed, trials, meanType);
	}

	/**
	 * @param mean
	 * @param sd
	 * @param trials
	 */
	public UniformVariableDistribution(float mean, float sd, int trials) {
		super(mean, sd, trials);
	}

	/**
	 * Get the next onset in this variable distribution. Onsets are selected
	 * from an uniform distribution with the given mean. Variable distributions
	 * configured with the same mean will give the same sequence of onsets.
	 * 
	 * @return a double indicating the next duration.
	 */

	@Override
	protected double nextRandom() {
		double next = (random.nextDouble() + 0.5) * getMean();
        if (next == 0) return 0;
		return Math.max(next, Simulator.getController().getModel()
				.getTimestepSize());
	}

}
