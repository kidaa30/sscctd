package simulator.configurables;

/**
 * StimulusOnsetConfig.java
 * 
 * Interface for duration configuration objects.
 * 
 * Created on 01-Dec-2011 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public interface StimulusOnsetConfig {

	/**
	 * Advance to the next shuffle of durations.
	 */

	public void advance();

	/** Return the mean for the config. **/

	public double getMean();

	/**
	 * @return get the next stimulus duration.
	 */
	public double getNextOnset();

	/**
	 * @return whether this config is fixed (true) or variable (false).
	 */
	public boolean isFixed();

	/**
	 * Reshuffle the durations.
	 */
	public void regenerate();

	/**
	 * Reset this configuration to its start state.
	 */
	public void reset();

	/**
	 * Go to the start of the shuffle of durations.
	 */
	public void restartOnsets();

	/** Set the number of onsets required. **/

	public void setTrials(int trials);

}
