/**
 * 
 */
package simulator.util;

import java.util.List;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public enum Response {
	CHURCH_KIRKPATRICK("Church-Kirkpatrick") {
		/**
		 * Simulate a response per minute according to the Church-Kirkpatrick
		 * rule. REF
		 * 
		 * @param threshold
		 *            Response threshold
		 * @param thresholds
		 *            Pregenerated list of variable thresholds for a response
		 * @param strength
		 *            V value of this cue.
		 * @return
		 */
		@Override
		public double get(double threshold, List<Double> thresholds,
				double strength, double decay) {
			int responses = 0;
			try {
				for (double boundary : thresholds) {
					int response = strength > boundary * threshold ? 1 : 0;
					response = boundary < 4 / thresholds.size() ? 1 : response;
					responses += response;
				}
			} catch (ArrayIndexOutOfBoundsException e) {// swallow this,
														// response is zero.
			} catch (IndexOutOfBoundsException e) {// swallow this, response is
													// zero.
			}
			return responses;
		}
	},
	LUDVIG("Ludvig") {
		/**
		 * Simulate a response per minute according to the Ludvig rule. REF
		 * 
		 * @param threshold
		 *            Response threshold
		 * @param thresholds
		 *            Pregenerated list of variable thresholds for a response
		 * @param strength
		 *            V value of this cue.
		 * @return
		 */
		@Override
		public double get(double threshold, List<Double> thresholds,
				double strength, double decay) {
			double responses = 0;
			for (double t : thresholds) {
				try {
					responses = responses * decay + strength
							* (strength > threshold ? 1 : 0);
				} catch (ArrayIndexOutOfBoundsException e) {// swallow this,
															// response is zero.
				} catch (IndexOutOfBoundsException e) {// swallow this, response
														// is zero.
				}
			}
			return responses;
		}

	};
	private String nameStr;

	private Response(String nameStr) {
		this.nameStr = nameStr;
	}

	public abstract double get(double threshold, List<Double> thresholds,
			double strength, double decay);

	@Override
	public String toString() {
		return nameStr;
	}
}
