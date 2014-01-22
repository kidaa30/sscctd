package simulator.graph;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.XYSeries;

import simulator.CueList;
import simulator.SimCue;
import simulator.SimModel;

public class ComponentGraph extends SimGraph {

	public ComponentGraph(String s, SimModel m, int phase, boolean compound) {
		super(s, m, phase, compound);
	}

	@Override
	protected boolean drawStimulus(boolean compound, String cue) {
		return (compound || (!compound && cue.length() == 1))
				&& !cue.contains("ω");
	}

	@Override
	public List<String> getLabel() {
		List<String> labels = new ArrayList<String>();
		labels.add("Associative Strength");
		labels.add("Component");
		return labels;
	}

	/**
	 * Plot the current weight of a list of cues.
	 */

	@Override
	public XYSeries plot(String name, CueList cues, int maxDuration) {
		XYSeries tempXYSeries = new XYSeries(name);
		try {
			for (int y = 0; y < cues.size() && y < maxDuration; y++) {
				SimCue cue = cues.get(y);
                double value = 0d;
                try {
                    value = cue.getAssocValueVector().get(
                            cues.getTrialCount() - 1);
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Oops! Cue " + name
                            + " had less trials than we thought.");
                }
				tempXYSeries
						.add(y + 1,
								value);
			}
		} catch (NullPointerException e) {
			System.err.println("Oops! Cue " + name
					+ " had less trials than we thought.");
		}
		return tempXYSeries;
	}

}
