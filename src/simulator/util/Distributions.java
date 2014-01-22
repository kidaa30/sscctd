package simulator.util;

import java.io.Serializable;

/**
 * Factory class for producing distributions.
 * 
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/

public final class Distributions implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6118011662481327634L;
	public static int UNIFORM = 1;
	public static int EXPONENTIAL = 0;
	private static long SEED = System.currentTimeMillis();

	public static VariableDistribution getDistribution(int type, double mean,
			int trials, boolean geometric) {
		return getDistribution(type, mean, SEED, trials, geometric);
	}

	/**
	 * Factory for variable distributions.
	 * 
	 * @param type
	 *            of distribution (exponential/uniform)
	 * @param mean
	 *            for the distribution
	 * @param seed
	 *            random seed for generating.
	 * @param trials
	 *            number of trials this distribution must cover.
	 * @param geoMean
	 *            true to use geometric rather than arithmetic mean.
	 * @return a variable distribution to these specs.
	 */

	public static VariableDistribution getDistribution(int type, double mean,
			long seed, int trials, boolean geoMean) {
		switch (type) {
		case 1:
			return new UniformVariableDistribution(mean, 0, seed, trials,
					geoMean);
		default:
			return new VariableDistribution(mean, 0, seed, trials, geoMean);
		}
	}

	private Distributions() {
	}
}
