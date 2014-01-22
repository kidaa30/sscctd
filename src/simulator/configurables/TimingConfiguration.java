package simulator.configurables;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import simulator.CS;
import simulator.SimPhase;
import simulator.Simulator;
import simulator.Trial;
import simulator.configurables.ContextConfig.Context;
import simulator.configurables.USConfiguration.Relationship;

/**
 * TimingConfiguration.java
 * 
 * Created on Jan-2012 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class TimingConfiguration implements Serializable, DocumentListener {
	private class StartTimeComparator implements Comparator<Entry<CS, int[]>> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Entry<CS, int[]> o1, Entry<CS, int[]> o2) {
			if (o1.getValue()[0] > o2.getValue()[0]) {
				return 1;
			} else {
				return -1;
			}
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6434454881295769569L;

	/**
	 * Clear accumulated default settings.
	 */

	public static void clearDefaults() {
		defaultRelations.clear();
		OnsetConfig.clearDefaults();
	}

	/** mapping from cues to us configuration. **/
	private Map<CS, USConfiguration> relations;
	/** Default cue -> us configs. **/
	private static Map<CS, USConfiguration> defaultRelations = new HashMap<CS, USConfiguration>();
	/** Onset config **/
	private OnsetConfig durations;
	/** Default onsets. **/
	private static OnsetConfig defaultDurations = new OnsetConfig();
	/** US Duration. **/
	private double usDuration;
	private boolean configured;
	private boolean isReinforced;

	private int regenCount;

	private boolean generatedOnsets;
	private String trialString;
    private int trials;

	public TimingConfiguration() {
		this(1);
	}

    public int getTrials() {
        return trials;
    }

    public TimingConfiguration(double usDuration) {
		this.usDuration = usDuration;
		relations = new HashMap<CS, USConfiguration>();
		durations = new OnsetConfig();
		configured = false;
		isReinforced = true;
		regenCount = 0;
		generatedOnsets = false;
		trialString = "";
        trials = 0;
	}

	public void addOnset(CS stimulus, float mean, float sd, long seed) {
		durations.set(stimulus, mean, sd, seed);
	}

	public void addOnset(CS stimulus, int onset) {
		durations.set(stimulus, onset);
	}

	public void addUSConfig(CS cue, Relationship type, int offset) {
		relations.put(cue, new USConfiguration(type, offset));
	}

	/**
	 * Add a US timing relation.
	 * 
	 * @param cue
	 * @param relation
	 */

	public void addUSConfig(CS cue, USConfiguration relation) {
		relations.put(cue, relation);
	}

	/**
	 * Advance to the next shuffle of duration sequences.
	 */

	public void advance() {
		durations.advance();
		reset();
	}

	/**
	 * Takes a phase string and checks to see if this configuration covers all
	 * the cues in it. Sets it as unconfigured if the check fails.
	 * 
	 * @param Phase
	 *            string to check against
	 */
	public void checkFilled(String trials, boolean timingPerTrial) {
		List<CS> stimuli = new ArrayList<CS>(SimPhase.stringToCSList(trials));
		for(CS tmp : stimuli) {
			if(!relations.containsKey(tmp)) {
				configured = false;
				break;
			}
		}

	}

	/**
	 * @return the durations
	 */
	public OnsetConfig getDurations() {
		return durations;
	}

	/**
	 * 
	 * @param cs
	 *            name to get relationship for.
	 * @return the us relationship for this cue.
	 */

	public USConfiguration getRelation(CS cs) {
		return relations.get(cs);
	}

	public USConfiguration getRelation(String cs) {
		return getRelation(new CS(cs, 0, 0));
	}

	/**
	 * @return the relations
	 */
	public Map<CS, USConfiguration> getRelations() {
		return relations;
	}

	public double getTrace(CS cs, int timestep) {
		double trace = 0;

		return trace;
	}

	public double getUsDuration() {
		return usDuration;
	}

	/**
	 * @return
	 */
	public boolean hasVariableDurations() {
		return durations.hasRandomDurations();
	}

	public boolean hasZeroDurations() {
		return durations.hasZeroDurations();
	}

	public boolean isConfigured() {
		return configured;
	}

	public boolean isReinforced() {
		return isReinforced;
	}

	/**
	 * Produce a map of timings for stimuli & US given the set of constraints on
	 * CS relationships to US.
	 * 
	 * @param set
	 *            Cues to produce timings for.
	 * @param configuralsMap
	 * @return a map of cues -> start/end timestep.
	 */

	public Map<CS, int[]> makeTimings(Set<CS> set) {
		Map<CS, int[]> timings = new HashMap<CS, int[]>();

		double multiplier = Simulator.getController().getModel()
				.getTimestepSize();

		// Relative positions about t0
		double start;
		double end;
		double minStart = 0;
		double maxStart = Double.NEGATIVE_INFINITY;
		double maxEnd = Double.NEGATIVE_INFINITY;
		double minEnd = Double.POSITIVE_INFINITY;
        double firstCSStart = Double.POSITIVE_INFINITY;
		for (CS cue : set) {
			// Non-configurals only
			if (!cue.isConfigural() && !cue.isSerialConfigural()
					&& !cue.isCompound() && !Context.isContext(cue + "")) {
				USConfiguration relation = relations.get(cue);
				switch (relation.getType()) {
				case FORWARD:
					end = durations.getMean(cue) - relation.getOffset();
					start = end - durations.next(cue);
					break;
				case BACKWARD:
					start = relation.getOffset() + getUsDuration();
					end = start + durations.next(cue);
					break;
				case SIMULTANEOUS:
				default:
					start = -relation.getOffset();
					end = start + durations.next(cue);
					break;
				}
				start = Math.round(start / multiplier);
				end = Math.round(end / multiplier);
				timings.put(cue, new int[] { (int) start, (int) end });
				minStart = Math.min(start, minStart);
                firstCSStart = Math.min(start, firstCSStart);
				maxStart = Math.max(start, maxStart);
				maxEnd = Math.max(maxEnd, end);
				minEnd = Math.min(minEnd, end);
			}
		}
		double meanOfMeans = 0;
		int count = 0;
		for (CS lookup : set) {
			try {
				meanOfMeans += durations.getMean(lookup);
				count++;
			} catch (NullPointerException e) {
				// Ignore omega having no duration.
			}
		}
		meanOfMeans /= count;
		meanOfMeans = Math.round(meanOfMeans / multiplier);
		// Configurals
		/*
		 * for(CS cue : set) { if (cue.isConfigural()) { String parts =
		 * configuralsMap.get(cue.getName()); timings.put(cue, new int[]{(int)
		 * maxStart, (int) minEnd}); } }
		 */

		// Shift so to start from t0
		for (int[] timing : timings.values()) {
			timing[0] += -minStart;
			timing[1] += -minStart;
		}

        firstCSStart = Math.max(firstCSStart, 0);
		// Add total cs length
        //Changed to this firstCSStart bit to peg to the length of the CSs
        //when using backwards conditioning
        timings.put(CS.CS_TOTAL, new int[] { (int) firstCSStart, (int) (-minStart + maxEnd) });
		maxEnd = Math.max(getUsDuration(), maxEnd);
		timings.put(CS.OMEGA, new int[] { 0, (int) meanOfMeans });
		// Add US timings
		timings.put(
				CS.US,
				new int[] {
						(int) -minStart,
						(int) Math.round(-minStart
								+ (getUsDuration() / multiplier)) });
		// Add total length
		timings.put(CS.TOTAL, new int[] { 0, (int) (-minStart + maxEnd) });

		return timings;
	}

    public Map<CS, int[]> makeTimingsBasedOnMean(Set<CS> set) {
        Map<CS, int[]> timings = new HashMap<CS, int[]>();

        double multiplier = Simulator.getController().getModel()
                .getTimestepSize();

        // Relative positions about t0
        double start;
        double end;
        double minStart = 0;
        double maxStart = Double.NEGATIVE_INFINITY;
        double maxEnd = Double.NEGATIVE_INFINITY;
        double minEnd = Double.POSITIVE_INFINITY;
        double firstCSStart = Double.POSITIVE_INFINITY;
        for (CS cue : set) {
            // Non-configurals only
            if (!cue.isConfigural() && !cue.isSerialConfigural()
                    && !cue.isCompound() && !Context.isContext(cue + "")) {
                USConfiguration relation = relations.get(cue);
                switch (relation.getType()) {
                    case FORWARD:
                        end = durations.getMean(cue) - relation.getOffset();
                        start = end - durations.getMean(cue);
                        break;
                    case BACKWARD:
                        start = relation.getOffset() + getUsDuration();
                        end = start + durations.getMean(cue);
                        break;
                    case SIMULTANEOUS:
                    default:
                        start = -relation.getOffset();
                        end = start + durations.getMean(cue);
                        break;
                }
                start = Math.round(start / multiplier);
                end = Math.round(end / multiplier);
                timings.put(cue, new int[] { (int) start, (int) end });
                minStart = Math.min(start, minStart);
                firstCSStart = Math.min(start, firstCSStart);
                maxStart = Math.max(start, maxStart);
                maxEnd = Math.max(maxEnd, end);
                minEnd = Math.min(minEnd, end);
            }
        }
        double meanOfMeans = 0;
        int count = 0;
        for (CS lookup : set) {
            try {
                meanOfMeans += durations.getMean(lookup);
                count++;
            } catch (NullPointerException e) {
                // Ignore omega having no duration.
            }
        }
        meanOfMeans /= count;
        meanOfMeans = Math.round(meanOfMeans / multiplier);
        // Configurals
		/*
		 * for(CS cue : set) { if (cue.isConfigural()) { String parts =
		 * configuralsMap.get(cue.getName()); timings.put(cue, new int[]{(int)
		 * maxStart, (int) minEnd}); } }
		 */

        // Shift so to start from t0
        for (int[] timing : timings.values()) {
            timing[0] += -minStart;
            timing[1] += -minStart;
        }
        firstCSStart = Math.max(firstCSStart, 0);
        // Add total cs length
        timings.put(CS.CS_TOTAL, new int[] { (int) firstCSStart, (int) (-minStart + maxEnd) });
        maxEnd = Math.max(getUsDuration(), maxEnd);
        timings.put(CS.OMEGA, new int[] { 0, (int) meanOfMeans });
        // Add US timings
        timings.put(
                CS.US,
                new int[] {
                        (int) -minStart,
                        (int) Math.round(-minStart
                                + (getUsDuration() / multiplier)) });
        // Add total length
        timings.put(CS.TOTAL, new int[] { 0, (int) (-minStart + maxEnd) });

        return timings;
    }

	public Double next(CS cue) {
		return durations.next(cue);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		// This is a really icky hack to do on the fly conversion of
		// older timing config objects which used strings as keys, not CSs
		// I feel a bit ill looking at it.
		// Let this be a lesson: do not use Java serialization to produce save
		// files!
		for (Object key : relations.keySet()) {
			if (key instanceof String) {
				Map<CS, USConfiguration> onsetsNew = new HashMap<CS, USConfiguration>();
				for (Entry entry : relations.entrySet()) {
					onsetsNew.put(new CS((String) entry.getKey(), 0, 0),
							(USConfiguration) entry.getValue());
				}
				relations = onsetsNew;
				break;
			} else if(isTimingPerTrial()) {
				CS cs = (CS) key;
				//Could do a 'first check' and update as needed for missing
				//configs, rather than on load?
			}
		}
	}

	/**
	 * Regenerate the timing configurations, producing new random duration
	 * sequences for variable cues.
	 */

	public void regenerate() {
		durations.regenerate();
		reset();
		regenCount++;
	}

	public void reset() {
		durations.reset();
	}

	/**
	 * Restart the sequence of shuffled onsets.
	 */
	public void restartOnsets() {
		durations.restartOnsets();
		reset();
	}

	/**
	 * Generate a list of the sequences that occur a trial based on the timings.
	 * e.g. AB, for A F(10), fw(10 & B F(10), sm(0)
	 */

	public Map<List<List<CS>>, int[]> sequence(Set<CS> set,
			Map<CS, int[]> initial) {
		Map<CS, int[]> timings = new HashMap<CS, int[]>(initial);
		List<CS> csSequence = new ArrayList<CS>(timings.keySet());
		List<List<CS>> sequence = new ArrayList<List<CS>>();
		Map<List<List<CS>>, int[]> sequences = new HashMap<List<List<CS>>, int[]>();

		int endTime = timings.get(CS.CS_TOTAL)[1] + 1;
		int usTime = timings.get(CS.US)[0];
		// First step, remove US etc.

		timings.remove(CS.US);
		timings.remove(CS.CS_TOTAL);
		timings.remove(CS.TOTAL);
		timings.remove(CS.OMEGA);

		// Run through in 'real' time and record the sequence.
		// If using context, assume it is always on (never a break)
		for (CS cs : set) {
			if (Context.isContext(cs.getName())) {
				timings.put(cs, new int[] { 0, endTime });
				break;
			}
		}
		// Sort by start time.

		TreeSet<Entry<CS, int[]>> sortedTimings = new TreeSet<Entry<CS, int[]>>(
				new StartTimeComparator());
		sortedTimings.addAll(timings.entrySet());

		int sequenceStart = 0;
		int sequenceEnd = 0;

		for (int i = 0; i < Math.max(endTime, usTime) + 1; i++) {
			List<CS> csList = new ArrayList<CS>();
			boolean pause = true;
			for (Entry<CS, int[]> entry : sortedTimings) {
				if (i < entry.getValue()[1] && i >= entry.getValue()[0]) {
					csList.add(entry.getKey());
					pause = false;
				}
			}
			pause = pause || i == usTime;
			// Continue to add to the sequence as long as a pause is not reached
			if (!pause) {
				Collections.sort(csList); // This is the problem. Need to
											// identify by CS, not cs name.
				if (!csList.isEmpty()) {
					if (sequence.isEmpty()
							|| !sequence.get(sequence.size() - 1)
									.equals(csList)) {
						sequence.add(csList);
					}
				}
				// Otherwise this is a discrete sequence, record it
			} else if (i < usTime) {
				// Carry on while we haven't hit the US
			} else if (sequence.size() > 1) {
				for (CS cs : sequence.get(sequence.size() - 1)) {
					sequenceStart = Math.max(sequenceStart, timings.get(cs)[0]);
				}

				// Prune such that each CS occurs only once
				// with no null entries
				//But leave context at the end..
				ListIterator<List<CS>> seqIt = sequence.listIterator(sequence
						.size());
				for(List<CS> subSeq : sequence.subList(0, sequence
						.size() - 1)) {
					ListIterator<CS> it = subSeq.listIterator();

				}
				while (seqIt.hasPrevious()) {
					List<CS> working = seqIt.previous();
					ListIterator<CS> it = working.listIterator();
					if (seqIt.hasPrevious()) {
						List<CS> prev = seqIt.previous();
						seqIt.next();
						while (it.hasNext()) {
                            CS c = it.next();
							if (prev.contains(c) && !Context.isContext(c.getName())) {
								it.remove();
							}
						}
						if (working.isEmpty()) {
							seqIt.next();
							seqIt.remove();
						}
					}

				}

				sequences.put(sequence, new int[] { sequenceStart, i });
				sequence = new ArrayList<List<CS>>();
				sequenceStart = i + 1;
				// Ignore single element sequences
			} else {
				sequence.clear();
				sequenceStart = i + 1;
			}
		}
		return sequences;
	}

	/**
	 * Generate a list of the sequences that occur in this timing configuration
	 * based on the timings. e.g. AB, for A F(10), fw(10 & B F(10), sm(0)
	 * 
	 * @param set
	 * @return
	 */

	public List<List<List<CS>>> sequences(Collection<Trial> trials) {
		Set<List<List<CS>>> sequences = new HashSet<List<List<CS>>>();

        for (int i = 0; i < Simulator.getController().getModel()
                .getVariableCombinationNo(); i++) {
            for (Trial trial : trials) {
                Set<CS> set = new HashSet<CS>(trial.getCues());
                Map<CS, int[]> timings = makeTimingsBasedOnMean(set);
                sequences.addAll(sequence(set, timings).keySet());
                if (generatedOnsets) {
                    advance();
                } else {
                    regenerate();
                }
            }
        }
        generatedOnsets = true;
        restartOnsets();
        System.out.println(sequences);

        return new ArrayList<List<List<CS>>>(sequences);
	}

    /**
     * Generate a list of the compounds that occur in this timing configuration
     * based on the timings. e.g. AB, for A F(10), fw(10 & B F(10), sm(0)
     *
     * @param set
     * @return
     */

    public List<List<List<CS>>> compounds(Collection<Trial> trials) {
        Set<List<List<CS>>> sequences = new HashSet<List<List<CS>>>();
        Set<Trial> trialSet = new HashSet<Trial>(trials);



        //Check if variable durations are present
        int reps = 1;

        for (Trial trial : trialSet) {
            boolean repetitions = false;
            Set<CS> set = new HashSet<CS>(trial.getCues());

            for(CS cs : set) {
                try {
                    if(!durations.getMap().get(cs).isFixed()) {
                        repetitions = true;
                        reps = Simulator.getController().getModel().getVariableCombinationNo();
                        break;
                    }
                } catch (NullPointerException e) {

                }
            }

            if(!repetitions) {
                List<Trial> t = new ArrayList<Trial>();
                t.add(trial);
                trials.removeAll(t);
                trials.add(trial);
            }
        }

        for (int i = 0; i < reps; i++) {
            for (Trial trial : trials) {
                Set<CS> set = new HashSet<CS>(trial.getCues());
                Map<CS, int[]> timings = makeTimings(set);
                sequences.addAll(compound(set, timings).keySet());
            }
            if (generatedOnsets) {
                advance();
            } else {
                regenerate();
            }
        }
        generatedOnsets = true;
        restartOnsets();
        System.out.println(sequences);

        return new ArrayList<List<List<CS>>>(sequences);
    }

    /**
     * Generate a list of the sequences that occur a trial based on the timings.
     * e.g. AB, for A F(10), fw(10 & B F(10), sm(0)
     */

    public Map<List<List<CS>>, int[]> compound(Set<CS> set,
                                               Map<CS, int[]> initial) {
        Map<CS, int[]> timings = new HashMap<CS, int[]>(initial);
        List<CS> csSequence = new ArrayList<CS>(timings.keySet());
        List<List<CS>> sequence = new ArrayList<List<CS>>();
        Map<List<List<CS>>, int[]> sequences = new HashMap<List<List<CS>>, int[]>();

        int endTime = timings.get(CS.CS_TOTAL)[1] + 1;
        int usTime = timings.get(CS.US)[0];
        // First step, remove US etc.

        timings.remove(CS.US);
        timings.remove(CS.CS_TOTAL);
        timings.remove(CS.TOTAL);
        timings.remove(CS.OMEGA);

        // Run through in 'real' time and record the sequence.
        // If using context, assume it is always on (never a break)
        for (CS cs : set) {
            if (Context.isContext(cs.getName())) {
                timings.put(cs, new int[] { 0, endTime });
                break;
            }
        }
        // Sort by start time.

        TreeSet<Entry<CS, int[]>> sortedTimings = new TreeSet<Entry<CS, int[]>>(
                new StartTimeComparator());
        sortedTimings.addAll(timings.entrySet());

        int sequenceStart = 0;
        int sequenceEnd = 0;

        for (int i = 0; i < Math.max(endTime, usTime) + 1; i++) {
            List<CS> csList = new ArrayList<CS>();
            boolean pause = true;
            for (Entry<CS, int[]> entry : sortedTimings) {
                if (i < entry.getValue()[1] && i >= entry.getValue()[0]) {
                    csList.add(entry.getKey());
                    pause = false;
                }
            }
            pause = pause || i == usTime;
            Collections.sort(csList);
            if(!csList.isEmpty() && Context.isContext(csList.get(csList.size()-1).getName())) {
                CS c = csList.get(csList.size()-1);
                csList.remove(csList.size()-1);
                csList.add(0, c);
            }
            if (!sequence.isEmpty()) {
                    if(!sequence.get(sequence.size() - 1)
                    .equals(csList)) {
                pause = true;
                    }
            }
            // Continue to add to the sequence as long as a pause is not reached
            if (!pause) {
                Collections.sort(csList); // This is the problem. Need to
                // identify by CS, not cs name.
                if(!csList.isEmpty() && Context.isContext(csList.get(csList.size()-1).getName())) {
                    CS c = csList.get(csList.size()-1);
                    csList.remove(csList.size()-1);
                    csList.add(0, c);
                }
                if (csList.size() > 1) {
                        sequence.add(csList);
                }
                // Otherwise this is a discrete sequence, record it
            } else if (!sequence.isEmpty()) {
                for (CS cs : sequence.get(sequence.size() - 1)) {
                    sequenceStart = Math.max(sequenceStart, timings.get(cs)[0]);
                }

                // Prune such that each CS occurs only once
                // with no null entries
                //But leave context at the end..
                ListIterator<List<CS>> seqIt = sequence.listIterator(sequence
                        .size());
                while (seqIt.hasPrevious()) {
                    List<CS> working = seqIt.previous();
                    ListIterator<CS> it = working.listIterator();
                    if (seqIt.hasPrevious()) {
                        List<CS> prev = seqIt.previous();
                        seqIt.next();
                        while (it.hasNext()) {
                            if (prev.contains(it.next())) {
                                it.remove();
                            }
                        }
                        if (working.isEmpty()) {
                            seqIt.next();
                            seqIt.remove();
                        }
                    }

                }
                sequences.put(sequence, new int[] { sequenceStart, i });
                sequence = new ArrayList<List<CS>>();
                sequenceStart = i + 1;
                // Ignore single element sequences
            } else {
                sequence.clear();
                sequenceStart = i + 1;
            }
        }
        return sequences;
    }

	public void setConfigured(boolean b) {
		configured = b;
	}

	/**
	 * @param durations
	 *            the durations to set
	 */
	public void setDurations(OnsetConfig durations) {
		generatedOnsets = false;
		this.durations = durations;
	}

	public void setReinforced(boolean reinforced) {
		if (reinforced && !isReinforced) {
			usDuration = Simulator.getController().getModel().getTimestepSize();
		}
		isReinforced = reinforced;
		usDuration = isReinforced ? usDuration : 0;
	}

	/**
	 * @param relations
	 *            the relations to set
	 */
	public void setRelations(Map<CS, USConfiguration> relations) {
		this.relations = relations;
		generatedOnsets = false;
		for (Entry<CS, USConfiguration> entry : relations.entrySet()) {
			if (!defaultRelations.containsKey(entry.getKey())) {
				defaultRelations.put(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Add list of stimuli with default configs, removes any stimuli not in the
	 * list.
	 * 
	 * @param stimuli
	 */

	public void setStimuli(Collection<CS> stimuli) {
		Map<CS, USConfiguration> tmpRelation = new HashMap<CS, USConfiguration>(
				stimuli.size());
		CS legacyStim;
		for (CS stimulus : stimuli) {
			legacyStim = stimulus.copy();
			legacyStim.setStringPos(0);
			if (relations.containsKey(stimulus)) {
				tmpRelation.put(stimulus, relations.get(stimulus));
			} else if(relations.containsKey(legacyStim)) {
				USConfiguration tmp = relations.get(legacyStim);
				tmpRelation.put(stimulus, tmp);
				relations.remove(legacyStim);
			} else {
				USConfiguration relation = new USConfiguration();
				if (defaultRelations.containsKey(stimulus)) {
					relation.setType(defaultRelations.get(stimulus).getType());
					relation.setOffset(defaultRelations.get(stimulus)
							.getOffset());
				}
				tmpRelation.put(stimulus, relation);
			}
		}
		durations.setStimuli(stimuli);
		relations = tmpRelation;
		configured = true;
	}

	public void setTrials(int trials) {
        this.trials = trials;
		generatedOnsets = false;
		durations.setTrials(trials);
	}

	/**
	 * @param usDuration
	 *            the usDuration to set
	 */
	public void setUsDuration(double usDuration) {
		this.usDuration = usDuration;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		if (usDuration > 0) {
			sb.append("US(");
			sb.append(usDuration);
			sb.append(") ");
		}
		for (Entry<CS, StimulusOnsetConfig> entry : durations.getMap()
				.entrySet()) {
			sb.append(entry.getKey());
			sb.append(':');
			sb.append(entry.getValue());
			sb.append(',');
			sb.append(relations.get(entry.getKey()));
			sb.append(' ');
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	@Override
	public void changedUpdate(DocumentEvent evt) {
		int changeLength = evt.getLength();
		int changeOffset = evt.getOffset();
		Document doc = evt.getDocument();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	@Override
	public void insertUpdate(DocumentEvent evt) {
		Document doc = evt.getDocument();
		int docLength = doc.getLength();
		String text = "";
		try {
			text = doc.getText(0, doc.getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!text.equals(trialString)  && !"".equals(text)) {
			int changeLength = evt.getLength();
			int changeOffset = evt.getOffset();
			updateStimuli(changeLength, changeOffset, text, false);
			trialString = text;
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	@Override
	public void removeUpdate(DocumentEvent evt) {
		Document doc = evt.getDocument();
		int docLength = doc.getLength();
		String text = "";
		try {
			text = doc.getText(0, doc.getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!text.equals(trialString) && !"".equals(text)) {
			int changeLength = evt.getLength();
			int changeOffset = evt.getOffset();
			updateStimuli(changeLength, changeOffset, text, true);
			trialString = text;
		}
	}
	
	/**
	 * Make a new list of stimuli, replace equivalents in maps.
	 * @param length
	 * @param offset
	 * @param newString
	 */
	private void updateStimuli(int length, int offset, String newString, boolean remove) {
        newString = newString.replaceAll("\\s", "");
        if(!Simulator.getController().isReferenced(this)) {
            return;
        }
		Collection<CS> newStimuli = SimPhase.stringToCSList(newString);
        if(!Simulator.getController().getModel().isTimingPerTrial()) {
            setStimuli(newStimuli);
            return;
        }
		//Map of new stimuli by what their stringpos would have been
		HashMap<Integer, CS> tmp = new HashMap<Integer, CS>();
		Map<CS, USConfiguration> newRelations = new HashMap<CS, USConfiguration>();
		Map<CS, StimulusOnsetConfig> durMap = durations.getMap();
		Map<CS, StimulusOnsetConfig> tmpDurMap = new HashMap<CS, StimulusOnsetConfig>();
		for(CS cs : newStimuli) {
			Integer oldStringPos = cs.getStringPos();
			//If we've removed stuff, old stringpos was bigger
			if(remove) {
				if(oldStringPos > (offset - length)) {
					oldStringPos += length;
					tmp.put(oldStringPos, cs);
				} else {
					//Retain unchanged	
					newRelations.put(cs, relations.get(cs));
					tmpDurMap.put(cs, durMap.get(cs));
				}
				
			} else {
			// Otherwise it was smaller
				if(oldStringPos >= (offset + length)) {
					oldStringPos -= length;
					tmp.put(oldStringPos, cs);
				} else if (oldStringPos >= offset && oldStringPos < (offset + length)) {
					// This is new, add it.
					newRelations.put(cs, new USConfiguration());
					tmpDurMap.put(cs, new FixedOnsetConfig(0));
				} else {
					//Retain unchanged
					newRelations.put(cs, relations.get(cs));
					tmpDurMap.put(cs, durMap.get(cs));
				}
			}
			if(!relations.containsKey(cs) ) {
				newRelations.put(cs, new USConfiguration());
				tmpDurMap.put(cs, new FixedOnsetConfig(0));
			}
		}
		for(Entry<CS, USConfiguration> entry : relations.entrySet()) {
			CS cs = entry.getKey();
			if(tmp.containsKey(cs.getStringPos())) {
				newRelations.put(tmp.get(cs.getStringPos()), entry.getValue());
				tmpDurMap.put(tmp.get(cs.getStringPos()), durMap.get(cs));
			}
		}
		durMap.clear();
		durMap.putAll(tmpDurMap);
		relations = newRelations;
	}

	/**
	 * @return the isTimingPerTrial
	 */
	public boolean isTimingPerTrial() {
		return Simulator.getController().getModel().isTimingPerTrial();
	}

}
