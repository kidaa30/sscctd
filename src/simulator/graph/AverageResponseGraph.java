/**
 * 
 */
package simulator.graph;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.XYSeries;

import simulator.CueList;
import simulator.SimModel;

/**
 * City University BSc Computing with Artificial Intelligence Project title:
 * Building a TD Simulator for Real-Time Classical Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 **/
public class AverageResponseGraph extends SimGraph {

	/**
	 * @param s
	 * @param m
	 * @param phase
	 * @param compound
	 */
	public AverageResponseGraph(String s, SimModel m, int phase,
			boolean compound) {
		super(s, m, phase, compound);
	}

	@Override
	public List<String> getLabel() {
		List<String> labels = new ArrayList<String>();
		labels.add("Response");
		labels.add("Trial");
		return labels;
	}

	/**
	 * Plot the average response of a list of cues.
	 */

	@Override
	public XYSeries plot(String name, CueList alfa, int maxDuration) {
		XYSeries tempXYSeries = new XYSeries(name);
		try {
			int vectorSize = alfa.getTrialCount();
			for (int y = 0; y < vectorSize; y++) {
				tempXYSeries.add(y + 1, alfa.averageResponse(y));
			}
		} catch (NullPointerException e) {
			System.err.println("Oops! " + name
					+ " had less trials than we thought.");
		}
		return tempXYSeries;
	}

}
