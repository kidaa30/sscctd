package simulator.configurables;

import java.io.Serializable;

/**
 * USConfiguration.java
 * 
 * Class representing the configuration of a US relative to a particular cue.
 * 
 * Created on Jan-2012 City University BSc Computing with Artificial
 * Intelligence Project title: Building a TD Simulator for Real-Time Classical
 * Conditioning
 * 
 * @supervisor Dr. Eduardo Alonso
 * @author Jonathan Gray
 * 
 */

public class USConfiguration implements Serializable {

	public enum Relationship {
		FORWARD(0, "Forward", "Fw"), BACKWARD(1, "Backward", "Bw"), SIMULTANEOUS(
				2, "Simultaneous", "Sm");
		public static Relationship getById(int id) {
			switch (id) {
			case 0:
				return FORWARD;
			case 1:
				return BACKWARD;
			case 2:
			default:
				return SIMULTANEOUS;
			}
		}

		public static Relationship getByName(String value) {
			for (Relationship r : Relationship.values()) {
				if (r.name.equals(value)) {
					return r;
				}
			}
			return null;
		}

		private int id;

		private String name;

		private String shortName;

		private Relationship(int id, String name, String shortName) {
			this.id = id;
			this.name = name;
			this.shortName = shortName;
		}

		public int getId() {
			return id;
		}

		/**
		 * @return the shortName
		 */
		public String getShortName() {
			return shortName;
		}

		/**
		 * @param shortName
		 *            the shortName to set
		 */
		public void setShortName(String shortName) {
			this.shortName = shortName;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 8799452202231591514L;
	/** Offset amount. **/
	private double offset;
	/** Relationship to US. **/
	private Relationship type;

	private double forwardDefault;

	public USConfiguration() {
		this(Relationship.FORWARD, 0);
	}

	public USConfiguration(Relationship type, double offset2) {
		this.offset = offset2;
		this.type = type;
		forwardDefault = 0;
	}

	/**
	 * Get the minimum for forward durations.
	 */

	public double getForwardDefault() {
		return forwardDefault;
	}

	/**
	 * @return the offset
	 */
	public double getOffset() {
		return offset;
	}

	/**
	 * @return the type
	 */
	public Relationship getType() {
		return type;
	}

	/**
	 * Set the minimum for forward durations.
	 * 
	 * @param mean
	 */

	public void setForwardDefault(double mean) {
		forwardDefault = mean;
		if (type.equals(Relationship.FORWARD)) {
			offset = Math.max(forwardDefault, offset);
		}
	}

	/**
	 * @param offset
	 *            the offset to set
	 */
	public void setOffset(double offset) {
		this.offset = offset;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(Relationship type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return type.getShortName() + "(" + offset + ")";
	}
}
