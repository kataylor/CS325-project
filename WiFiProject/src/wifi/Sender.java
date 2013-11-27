/**
 * A class that sends items from the link layer.
 * @author Christopher Livingston
 */
package wifi;

import rf.RF;

import java.util.Random;

public class Sender implements Runnable {
	RF theRF;
	byte[] data;
	int[] window = new int[0]; // Size will be changed in changeWindowSize
	Random randomGen = new Random();
	long timeout = (long) 10; // timeout
	FrameMaker parser;
	int seqNum;
	long runStart;
	boolean dataSent =false;

	public Sender(RF theRF, byte[] data) {
		System.out.println("Sender: Constructor ran");
		System.out.println(timeout);
		this.theRF = theRF;
		this.data = data;
		parser = new FrameMaker();
		seqNum = parser.getSequnceNumber(data);
	}

	/**
	 * When the channel is busy.
	 */
	public void notIdle() {
		try {
			if(dataSent == true){
				return;
			}
			System.out.println("In notIdel!");

			while (theRF.inUse()) {
				// waiting for the current transmission to end!
			}

			// wait IFS
			Thread.sleep(RF.aSIFSTime);

			// If the medium is not idle restart and change the size of the
			// window...
			if (theRF.inUse()) {
				changeWindowSize();
				notIdle();
			}

			// If the medium is idle.
			if (!theRF.inUse()) {
				// backoff and transmit.
				backOff();
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	public void changeWindowSize() {
		System.out.println("Changing the window size!");
		if (window.length == 0) {
			window = new int[RF.aCWmin];
		} else {
			if (((window.length) * 2) > RF.aCWmax) {
				window = new int[RF.aCWmax];
				// fill the window
				for (int i = 0; i < window.length; i++) {
					window[i] = i;
				}
			} else {
				window = new int[(window.length) * 2];
				// fill the window
				for (int i = 0; i < window.length; i++) {
					window[i] = i;
				}
			}
		}
	}

	/**
	 * Exponential back off and then transmit.
	 */
	public void backOff() {

		if (window.length == 0) {
			changeWindowSize();
		}

		try {
			// Pick a random value from the window.
			int ranVal = randomGen.nextInt(window.length - 1);
			int waitTime = window[ranVal];

			// wait for the selected time.
			Thread.sleep(waitTime * 100);

			if (theRF.inUse()) {
				Thread.sleep(RF.aSIFSTime);
			}

			// Transmit the frame
			transmit();
			
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	/**
	 * Transmits the given frame. Will wait until an ACK is received before killing the 
	 * Thread. Will resend the data if a timeout accrues while waiting for the ACK.
	 * 
	 * @author christoperlivingston
	 */
	public void transmit() {
		theRF.transmit(data);
		boolean gotACK = false;
		// wait for the right ACK before killing thread!
		while (gotACK == false) {
			//If we have received an ACK for our packet
			if(Receiver.ACKReceived(seqNum) == true){
				System.out.println("We sent the packet!");
				dataSent = true;
				gotACK = true;
				break;
				//kill the thread here
			}
			// Check for a timeout
			if(theRF.clock() - runStart == timeout){
				runStart = theRF.clock();//restart timeout clock time
				transmit();
				break;
			}
		}

	}

	public void run() {
		runStart = theRF.clock(); // used to check for timeout

		try {
			boolean waitIFS = false;

			System.out.println("Sending.....");
			while (dataSent == false) {
				// if no one is currently transmitting and we have not waited IFS yet
				if (!theRF.inUse() && waitIFS == false) {
					System.out.println("Waiting...");
					Thread.sleep(RF.aSIFSTime); // wait for the interframe time.
					System.out.println("Done wainting.");
					waitIFS = true; // Set waitIFS to true for next iteration of
									// the loop.
					continue;
				}

				// if no one is currently transmitting and we have waited IFS
				if (!theRF.inUse() && waitIFS == true) {
					transmit();
				}

				// if the medium is busy
				if (theRF.inUse()) {
					System.out.println("The chanel is busy!");
					notIdle();
				}
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		System.out.println("Sent.");
	}
}