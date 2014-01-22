package simulator.configurables;

import java.io.Serializable;
import java.util.List;

import simulator.util.Distributions;
import simulator.util.GreekSymbol;
import simulator.util.VariableDistribution;

/**
 * VariableOnsetConfig.java
 * 
 * Class holding a configuration for a variable duration.
 * 
 * Created on 01-Dec-2011 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class VariableOnsetConfig implements StimulusOnsetConfig, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7336395455119352484L;
	/** Variable distribution for generating durations. **/
	private VariableDistribution varDist;
	/** Mean used by the distribution. **/
	private double mean = 0;
	/** Standard deviation for the distribution. **/
	private float sd = 0;
	/** Number of trials. **/
	private int trials;
	/** Distribution type. **/
	private int type;
	/** Mean type. **/
	private boolean isGeometric;

	public VariableOnsetConfig(double mean, long seed, int trials, int type,
			boolean geometric) {
		this.sd = 0;
		this.mean = mean;
		this.trials = trials;
		isGeometric = geometric;
		this.type = type;
		varDist = Distributions.getDistribution(type, mean, seed, trials,
				geometric);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simulator.configurables.StimulusOnsetConfig#advance()
	 */
	@Override
	public void advance() {
		varDist.advance();
	}

	/**
	 * @return the mean
	 */
	@Override
	public double getMean() {
		return mean;
	}

	/**
	 * Get the next onset from the distribution.
	 */

	@Override
	public double getNextOnset() {
		return varDist.next();
	}

	/**
	 * @return the standard deviation
	 */
	public float getSd() {
		return sd;
	}

	public long getSeed() {
		return varDist.getSeed();
	}

	/**
	 * Get the shuffle for the underlying variable distribution.
	 * 
	 * @return
	 */

	public List<List<Double>> getShuffle() {
		return varDist.getShuffle();
	}

	public int getTrials() {
		return trials;
	}

	/**
	 * @return the type of distribution used.
	 */
	public int getType() {
		return type;
	}

	@Override
	public boolean isFixed() {
		return false;
	}

	/**
	 * @return the isGeometric
	 */
	public boolean isGeometric() {
		return isGeometric;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simulator.configurables.StimulusOnsetConfig#regenerate()
	 */
	@Override
	public void regenerate() {
		varDist.regenerate();
	}

	@Override
	public void reset() {
		varDist.build();
		varDist.setIndex(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simulator.configurables.StimulusOnsetConfig#restartOnsets()
	 */
	@Override
	public void restartOnsets() {
		varDist.restartOnsets();
	}

	/**
	 * @param isGeometric
	 *            the isGeometric to set
	 */
	public void setGeometric(boolean isGeometric) {
		this.isGeometric = isGeometric;
		varDist.setMeanType(isGeometric);
	}

	/**
	 * @param mean
	 *            the mean to set
	 */
	public void setMean(double mean) {
		this.mean = mean;
	}

	/**
	 * @param sd
	 *            the standard deviation to set
	 */
	public void setSd(float sd) {
		this.sd = sd;
	}

	public void setShuffle(List<List<Double>> shuffle) {
		varDist.setShuffle(shuffle);
	}

	@Override
	public void setTrials(int num) {
		trials = num;
		varDist.setTrials(trials);
	}

	/**
	 * @param type
	 *            the type to use.
	 */
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "V("+ GreekSymbol.MU.getSymbol() + mean + ")";
	}

}
