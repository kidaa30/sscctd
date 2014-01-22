package simulator.graph;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.XYSeries;

import simulator.CueList;
import simulator.SimModel;

public class ResponseGraph extends SimGraph {

	public ResponseGraph(String s, SimModel m, int phase, boolean compound) {
		super(s, m, phase, compound);
	}

	@Override
	public List<String> getLabel() {
		List<String> labels = new ArrayList<String>();
		labels.add("Response");
		labels.add("Time");
		return labels;
	}

	/**
	 * Plot the simulated response of a cue against time.
	 */

	@Override
	public XYSeries plot(String name, CueList cues, int maxDuration) {
		XYSeries tempXYSeries = new XYSeries(name);
		try {
			for (int y = 0; y < cues.size() && y < maxDuration; y++) {
				tempXYSeries.add(y + 1, cues.getResponse(threshold, y));
			}
		} catch (NullPointerException e) {
			System.err.println("Oops! " + name
					+ " had less components than we thought.");
		}
		return tempXYSeries;
	}
}
