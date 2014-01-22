/**
 * 
 */
package simulator.util;

/**
 * Provides a memory trace/fuzzy CS activation value.
 * 
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public enum MemoryTrace {

	SUTTON("Sutton") {
		@Override
		public double get(double decay, double dropOff, int timestep,
				boolean active) {
			return active ? 1 : 0;
		}
	},
	MONDRAGON("MONDRAGON") {
		/**
		 * Calculate a new trace using Mondragon's formula -
		 * 
		 * p(t/(t + td -d)) + (1-p)(th/(t+td-d))
		 * 
		 * @param decay
		 *            trace decay (d)
		 * @param dropOff
		 *            parameter governing how steep the drop off is after cs
		 *            offset (h)
		 * @param timestep
		 *            steps since last event (onset/offset) (t)
		 * @param active
		 *            boolean value for p (true = 1)
		 * @return a double holding the new trace value
		 */
		@Override
		public double get(double decay, double dropOff, int timestep,
				boolean active) {
			double p = active ? 1 : 0;
			double newTrace = timestep * (p + decay - p * decay)
					/ (timestep + timestep * dropOff - dropOff);

			return newTrace;
		}
	};
	private String nameStr;
	private static final long serialVersionUID = -6434454881295760000L;

	private MemoryTrace(String nameStr) {
		this.nameStr = nameStr;
	}

	public abstract double get(double decay, double dropOff, int timestep,
			boolean active);

	@Override
	public String toString() {
		return nameStr;
	}

}
