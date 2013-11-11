package wifi;

import rf.RF;

public class Sender implements Runnable {
	RF theRF;
	byte[] data;

	public Sender(RF theRF, byte[] data) {
		theRF = this.theRF;
		data = this.data;
	}

	public void run() {
		boolean sent = false;
		while (sent == false) {
			if (theRF.inUse() != true) {// if no one is currently transmitting
				int bytesSent = theRF.transmit(data);
				if (bytesSent == data.length) {// if we sent the correct number of bytes.
					System.out.println("We sent the packet!");
					// kill the thread here
					sent = true;
				}
			}
		}
	}

}