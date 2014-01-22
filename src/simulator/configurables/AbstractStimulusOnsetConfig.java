package simulator.configurables;

import java.io.Serializable;

public abstract class AbstractStimulusOnsetConfig implements
		StimulusOnsetConfig, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1682642430671855842L;

	@Override
	public double getMean() {
		return 0;
	}

	@Override
	public double getNextOnset() {
		return 0;
	}

	@Override
	public boolean isFixed() {
		return false;
	}

	@Override
	public void reset() {

	}

	@Override
	public void setTrials(int trials) {

	}

}
