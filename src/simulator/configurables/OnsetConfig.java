package simulator.configurables;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import simulator.CS;
import simulator.util.Distributions;

/**
 * OnsetConfig.java
 * 
 * Class to store a collection of duration configurations for stimuli.
 * 
 * Created on 01-Dec-2011 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class OnsetConfig implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4792950062566036011L;

	/**
	 * Clear accumulated default onsets.
	 */

	public static void clearDefaults() {
		defaultOnsets.clear();
	}

	/** Mapping from stimulus name to a duration configuration. **/
	private Map<CS, StimulusOnsetConfig> onsets;
	/** Defaults mapping. **/
	private static Map<CS, StimulusOnsetConfig> defaultOnsets = new HashMap<CS, StimulusOnsetConfig>();
	/** Indicator that this has been configured. **/
	private boolean set;
	/** Mean type. **/
	private boolean isGeo;
	/** Distribution type. **/
	private int type;

	private boolean hasRandom;

	public OnsetConfig() {
		onsets = new TreeMap<CS, StimulusOnsetConfig>();
		set = false;
		isGeo = false;
		type = Distributions.EXPONENTIAL;
	}

	/**
	 * Advance to the next shuffle of durations
	 */

	public void advance() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			onset.advance();
		}
	}

	/**
	 * 
	 * @return the mapping from stimulus name to duration configs.
	 */

	public Map<CS, StimulusOnsetConfig> getMap() {
		return onsets;
	}

	public double getMean(CS cue) {
		StimulusOnsetConfig onset = onsets.get(cue);
		return onset.isFixed() ? onset.getNextOnset() : onset.getMean();
	}

	/**
	 * @return the type of distribution in use.
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return
	 */
	public boolean hasRandomDurations() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			if (onset instanceof VariableOnsetConfig) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if there is at least one duration set to 0
	 */
	public boolean hasZeroDurations() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			if (onset.getMean() == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isConfigured() {
		return set;
	}

	/**
	 * Check whether a particular stimulus is fixed or variable.
	 * 
	 * @param stimulus
	 * @return true is the stimulus is fixed, false if variable.
	 */

	public boolean isFixed(String stimulus) {
		StimulusOnsetConfig onset = onsets.get(stimulus);
		return onset == null ? false : onset.isFixed();
	}

	/**
	 * @return the type of mean in use.
	 */
	public boolean isGeo() {
		return isGeo;
	}

	/**
	 * Merge another onset configuration into this one. The type and geometric
	 * mean settings are those of this config.
	 * 
	 * @param other
	 */

	public void merge(OnsetConfig other) {
		onsets.putAll(other.getMap());
		setType(type);
		setGeo(isGeo);
	}

	public double next(CS stimulus) {
		return onsets.get(stimulus).getNextOnset();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		for (Object key : onsets.keySet()) {
			if (key instanceof String) {
				Map<CS, StimulusOnsetConfig> onsetsNew = new HashMap<CS, StimulusOnsetConfig>();
				for (Entry entry : onsets.entrySet()) {
					onsetsNew.put(new CS((String) entry.getKey(), 0, 0),
							(StimulusOnsetConfig) entry.getValue());
				}
				onsets = onsetsNew;
				break;
			}
		}
	}

	/**
	 * Produce a new shuffle of durations
	 */
	public void regenerate() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			onset.regenerate();
		}
	}

	/**
	 * Reset all onset configurations to their starting state.
	 */

	public void reset() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			onset.reset();
		}
	}

	/**
	 * Restart the sequence of shuffled onsets used by advance.
	 */
	public void restartOnsets() {
		for (StimulusOnsetConfig onset : onsets.values()) {
			onset.restartOnsets();
		}
	}

	/**
	 * Set a new variable duration configuration for a stimulus.
	 * 
	 * @param stimulus
	 *            The stimulus this configuration applies to.
	 * @param mean
	 *            Mean for the variable distribution.
	 * @param sd
	 *            Standard deviation for the variable distribution.
	 * @param seed
	 *            Seed for the random number generator.
	 */

	public void set(CS stimulus, float mean, float sd, long seed) {
		onsets.put(stimulus,
				new VariableOnsetConfig(mean, seed, 0, type, isGeo));
	}

	/**
	 * Set a new fixed duration configuration for a stimulus.
	 * 
	 * @param stimulus
	 *            The stimulus this configuration applies to.
	 * @param onset
	 *            Duration in seconds.
	 */

	public void set(CS stimulus, int onset) {
		onsets.put(stimulus, new FixedOnsetConfig(onset));
	}

	/**
	 * Set a new duration configuration for a stimulus.
	 * 
	 * @param stimulus
	 *            The stimulus this configuration applies to.
	 * @param config
	 *            the configuration object.
	 */

	public void set(CS stimulus, StimulusOnsetConfig config) {
		onsets.put(stimulus, config);
		if (!defaultOnsets.containsKey(stimulus) && config.getMean() > 0) {
			defaultOnsets.put(stimulus, config);
		}
	}

	public void setConfigured(boolean set) {
		this.set = set;
	}

	/**
	 * @param isGeo
	 *            the type of mean to use.
	 */
	public void setGeo(boolean isGeo) {
		this.isGeo = isGeo;
		for (StimulusOnsetConfig onset : onsets.values()) {
			if (!onset.isFixed()) {
				((VariableOnsetConfig) onset).setGeometric(isGeo);
			}
		}
	}

	/**
	 * Ensure that only the stimuli in the provided list have duration
	 * configurations.
	 * 
	 * @param stimuli
	 *            the list of stimuli to store duration configs for.
	 */

	public void setStimuli(Collection<CS> stimuli) {
		// Prune onsets for stimuli that don't exist.
		Iterator<Entry<CS, StimulusOnsetConfig>> it = onsets.entrySet()
				.iterator();
		Collection<CS> legacyStims = new ArrayList<CS>();
		CS legacyStim;
		for(CS cs : stimuli) {
			legacyStim = cs.copy();
			legacyStim.setStringPos(0);
			legacyStim.setTrialString(cs.getTrialString());
			legacyStim.setHashCode(cs.getLocalStringPos());
			legacyStims.add(legacyStim);
		}
		while (it.hasNext()) {
			CS stimulus;
			stimulus = it.next().getKey();
			if (!stimuli.contains(stimulus) && !legacyStims.contains(stimulus)) {
				it.remove();
			}
		}
		for (CS stimulus : stimuli) {
			legacyStim = stimulus.copy();
			legacyStim.setStringPos(0);
			legacyStim.setTrialString(stimulus.getTrialString());
			if (onsets.containsKey(stimulus)) {
				// Deal with 'old' style cs without attached trial string.
				StimulusOnsetConfig onset = onsets.get(stimulus);
				onsets.remove(stimulus);
				onsets.put(stimulus, onset);
			} else if(onsets.containsKey(legacyStim)) {
				StimulusOnsetConfig tmp = onsets.get(legacyStim);
				onsets.put(stimulus, tmp);
				onsets.remove(legacyStim);
			}  else {
				if (defaultOnsets.containsKey(stimulus)) {
					set(stimulus, defaultOnsets.get(stimulus));
				} else {
					set(stimulus, 0);
				}
			}
		}
	}

	/**
	 * 
	 * @param trials
	 *            Number of random onsets required.
	 */

	public void setTrials(int trials) {
		for (StimulusOnsetConfig onset : onsets.values()) {
			onset.setTrials(trials);
			onset.reset();
		}
	}

	/**
	 * @param type
	 *            the type to use.
	 */
	public void setType(int type) {
		this.type = type;
		for (Entry<CS, StimulusOnsetConfig> onset : onsets.entrySet()) {
			if (!onset.getValue().isFixed()) {
				VariableOnsetConfig old = (VariableOnsetConfig) onset
						.getValue();
				List<List<Double>> shuffle = old.getShuffle();
				onsets.put(onset.getKey(),
						new VariableOnsetConfig(old.getMean(), old.getSeed(),
								old.getTrials(), type, old.isGeometric()));
				((VariableOnsetConfig) onsets.get(onset.getKey()))
						.setShuffle(shuffle);

			}
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (CS str : onsets.keySet()) {
			sb.append(str);
			sb.append(": ");
			sb.append(onsets.get(str));
			sb.append(" \n");
		}
		return sb.toString();
	}
}
