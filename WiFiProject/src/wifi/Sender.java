package wifi;

import rf.RF;

/*
 * Takes the frame and transmits the data through the RF layer.
 * 
 * @implements Runnable
 */

public class Sender implements Runnable {
	RF theRF;
	byte[] data;

	/*
	 * Constructor. Takes an RF layer and the frame to be transmitted.
	 */
	public Sender(RF theRF, byte[] data) {
		this.theRF = theRF;
		this.data = data;
	}

	/*
	 * Needed to implement Runnable.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		boolean sent = false;
		while (sent == false) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Do nothing
			}
			if (theRF.inUse() != true) {// if no one is currently transmitting
				int bytesSent = theRF.transmit(data);
				if (bytesSent == data.length) {// if we sent the correct number
												// of bytes.
					System.out.println("We sent the packet!");
					// kill the thread here
					sent = true;
				}
			}
		}
	}

}