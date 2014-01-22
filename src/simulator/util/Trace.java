/**
 * 
 */
package simulator.util;

import java.io.Serializable;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public enum Trace implements Serializable {
	BOUNDED("Bounded accumulating") {
		/**
		 * Calculate a new trace using Sutton & Barto's original formulation.
		 * x(t+1) = x(t) + delta*(y(t) - x(t))
		 * 
		 * @param delta
		 *            decay factor
		 * @return a double holding the new trace value
		 */
		@Override
		public double get(double delta, double gamma, double lastTrace,
				boolean active) {
			// If the cue was activated recently, start the trace.
			double newTrace = active ? 1 : 0;
			newTrace -= lastTrace;
			newTrace *= delta;
			newTrace += lastTrace;
			return newTrace;
		}
	},
	REPLACING("Replacing") {
		/**
		 * Calculate a new accumulating trace bounded to 1.
		 * 
		 * @param delta
		 * @param gamma
		 * @return a double holding the new trace value
		 */

		@Override
		public double get(double delta, double gamma, double lastTrace,
				boolean active) {
			// If the cue was activated recently, start the trace.
			double newTrace = active ? 1 : 0;
			newTrace += lastTrace * delta * gamma;
			return Math.min(1, newTrace);
		}
	},
	ACCUMULATING("Accumulating") {
		/**
		 * Calculate a new trace using Sutton's TDLambda formulation. x(t+1) =
		 * x(t)*delta*gamma + y(t)
		 * 
		 * @param delta
		 *            trace decay
		 * @param gamma
		 *            discount factor
		 * @return a double holding the new trace value
		 */
		@Override
		public double get(double delta, double gamma, double lastTrace,
				boolean active) {
			// If the cue was activated recently, start the trace.
			double newTrace = active ? 1 : 0;
			newTrace += lastTrace * delta * gamma;
			return newTrace;
		}
	};
	private String nameStr;
	private static final long serialVersionUID = -6434454881295760000L;

	/**
	 * @param oldTrace
	 * @return the equivalent new trace
	 */
	public static Trace fromOld(simulator.SimCue.Trace oldTrace) {
		for (Trace trace : values()) {
			if (oldTrace.toString().equals(trace.toString())) {
				return trace;
			}
		}
		return null;
	}

	private Trace(String nameStr) {
		this.nameStr = nameStr;
	}

	public abstract double get(double delta, double gamma, double lastTrace,
			boolean active);

	@Override
	public String toString() {
		return nameStr;
	}
}
