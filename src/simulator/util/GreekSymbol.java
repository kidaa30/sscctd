/**
 * 
 */
package simulator.util;

/**
 * City University
 * BSc Computing with Artificial Intelligence
 * Project title: Building a TD Simulator for Real-Time Classical Conditioning
 * @supervisor Dr. Eduardo Alonso 
 * @author Jonathan Gray
 **/
public enum GreekSymbol {
	LAMBDA_PLUS("lambda+","\u03BB+"),
	BETA_PLUS("beta+","\u03B2+"),
	LAMBDA_MINUS("lambda-","\u03BB+"),
	BETA_MINUS("beta-","\u03B2-"),
	DELTA("delta","\u03C1"),
	GAMMA("gamma","\u0263"),
	OMEGA("omega","\u03c9"),
    TAO("tao",""),
    MU("mu", "\u03bc");
	
	private String romanName;
	private String symbol;
	
	private GreekSymbol(String name, String symb) {
		romanName = name;
		symbol = symb;
	}
	
	public String getSymbol() {
		return symbol;
	}
	public String getName() {
		return romanName;
	}
	
	public static String getSymbol(String name) {
		for(GreekSymbol sy : values()) {
			if(sy.getName().equals(name)) {
				return sy.getSymbol();
			}
		}
		return name;
	}
	
	public static GreekSymbol getByName(String name) {
		for(GreekSymbol sy : values()) {
			if(sy.getName().equals(name)) {
				return sy;
			}
		}
		return null;
	}
}
