package wifi;

import java.util.ArrayList;
import rf.RF;

/**
 * A class that watches for incoming packets and places them in a queue for removal.
 * @author Kirah Taylor
 */
public class Receiver implements Runnable {
	
	ArrayList<byte[]> queue;
	RF theRF;
	
	public Receiver(RF theRF) {
		queue = new ArrayList();
		this.theRF = theRF;
	}

	public void run() {
		while(1 == 1) {
			byte[] packet = theRF.receive();
			queue.add(packet);
		}
		
	}
	
	
	
}
