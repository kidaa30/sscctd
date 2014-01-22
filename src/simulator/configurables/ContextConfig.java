/**
 * 
 */
package simulator.configurables;

import java.io.Serializable;

import simulator.CS;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 */
public class ContextConfig implements Serializable {

	/**
	 * Enum holding context symbols. City University BSc Computing with
	 * Artificial Intelligence Project title: Building a TD Simulator for
	 * Real-Time Classical Conditioning
	 * 
	 * @supervisor Dr. Eduardo Alonso
	 * @author Jonathan Gray
	 * 
	 */

	public enum Context implements Serializable {
		EMPTY(""), PHI("\u03A6"), PSI("\u03A8"), THETA("\u0398"), XI("\u039E");
		/**
		 * 
		 * @return an array of all the contexts except the empty one.
		 */

		public static Context[] getList() {
			Context[] list = new Context[] { PHI, PSI, THETA, XI };
			return list;
		}

		/**
		 * Check if a cue is a context.
		 * 
		 * @param in
		 * @return
		 */

		public static boolean isContext(final String in) {
			for (Context ctxt : Context.values()) {
				if (ctxt.name.equals(in)) {
					return true;
				}
			}
			return false;
		}

		/** Symbol. **/
		private String name;

		private Context(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7555945696185751652L;

	/** Symbol this context is a config for. **/
	private Context context;
	/** Alpha for this context. **/
	private double alpha;
	/** Empty context. **/
	public static final ContextConfig EMPTY = new ContextConfig(Context.EMPTY,
			0);
	/** Default alpha. **/
	private static double defaultAlpha = 0.005;
	/** Default context. **/
	private static Context defaultContext = Context.PHI;
	/** Whether default has been updated. **/
	private static boolean defaultAlphaUpdated = false;
	private static boolean defaultContextUpdated = false;

	public static void clearDefault() {
		defaultAlpha = 0.005;
		defaultAlphaUpdated = false;
		defaultContext = Context.PHI;
		defaultContextUpdated = false;
	}

	public ContextConfig() {
		context = defaultContext;
		alpha = defaultAlpha;
	}

	public ContextConfig(final Context context, final double alpha) {
		this.context = context;
		this.alpha = alpha;
	}

	/**
	 * @return the alpha value
	 */
	public Double getAlpha() {
		return alpha;
	}

	/**
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	public CS getCS() {
		return new CS(context.toString(), 0, 0);
	}

	public String getSymbol() {
		return context.toString();
	}

	/**
	 * @param alpha
	 *            the alpha value to set
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
		if (!defaultAlphaUpdated) {
			defaultAlpha = alpha;
			defaultAlphaUpdated = true;
		}
	}

	/**
	 * @param context
	 *            the context to set
	 */
	public void setContext(Context context) {
		this.context = context;
		if (!defaultContextUpdated) {
			defaultContext = context;
			defaultContextUpdated = true;
		}
	}

	@Override
	public String toString() {
		return context + "(" + getAlpha().floatValue() + ")";
	}
}
