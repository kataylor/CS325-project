package wifi;

import java.util.ArrayList;
import rf.RF;

/**
 * A class that watches for incoming packets and places them in a queue for removal by LinkLayer.
 * @author Kirah Taylor
 */
public class Receiver implements Runnable {
	
	ArrayList<byte[]> queue; //the current queue in use; will be changed to a better data structure in the future
	RF theRF;
	
	public Receiver(RF theRF) {
		queue = new ArrayList<byte[]>();
		this.theRF = theRF;
	}
/**
 * 
 */
	public void run() {
		for(;;) {
			byte[] packet = theRF.receive(); //blocks until something is received 
			queue.add(packet); //add received item to the queue 
		}
		
	}
	
	
	
}