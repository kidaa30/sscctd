/**
 * SimView.java
 * 
 * Created on 10-Mar-2005
 * City University
 * BSc Computing with Distributed Systems
 * Project title: Simulating Animal Learning
 * Project supervisor: Dr. Eduardo Alonso 
 * @author Dionysios Skordoulis
 *
 * Modified in October-2009
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Rocio Garcia Duran
 *
 * Modified in July-2011
 * The Centre for Computational and Animal Learning Research 
 * @supervisor Dr. Esther Mondragon 
 * email: e.mondragon@cal-r.org
 * @author Dr. Alberto Fernandez
 * email: alberto.fernandez@urjc.es
 *
 */
package simulator.applet;

import javax.swing.JApplet;
import simulator.Simulator;

//import sun.awt.VerticalBagLayout;   // modified by Alberto Fernandez: 18 July 2011

/**
 * SimViewApplet class is the main graphical user interface of the Simulator's
 * application in applet form. This launches the simulator from a webpage.
 */
public class SimViewApplet extends JApplet {

	@Override
	public void init() {
		new Simulator();
	}
}