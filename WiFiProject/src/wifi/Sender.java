/**
 * A class that sends items from the link layer.
 * @author Christopher Livingston
 */
package wifi;

import rf.RF;

import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Sender implements Runnable {
	RF theRF;
	int[] window = new int[0]; // Size will be changed in changeWindowSize
	Random randomGen = new Random();
	long timeout = (long) 1000; // timeout
	FrameMaker parser;
	int seqNum;
	long runStart;
	boolean dataSent = false;
	int retry = 0;
	Receiver receiver;
	int slotType;
	public BlockingQueue<byte[]> queue;
	byte[] data;
	private PrintWriter output;
	private int debugLevel;

	public Sender(RF theRF, int slotType, PrintWriter output, int debugLevel) {
		data = new byte[0];
		queue = new ArrayBlockingQueue<byte[]>(5);
		System.out.println("\tSender: Constructor ran");
		System.out.println("\t" + timeout);
		this.theRF = theRF;
		this.slotType = slotType;
		this.output = output;
		this.debugLevel = debugLevel;
		parser = new FrameMaker();
	}

	public void changeSlotType(int newType) {
		slotType = newType;
	}
	
	public void changeDebugLevel(int newLevel) {
		debugLevel = newLevel;
	}

	/**
	 * When the channel is busy.
	 */
	public void notIdle() {
		try {
			if (dataSent == true) {
				return;
			}
			if (debugLevel == 1) {
				output.println("The channel is not idle at: " + theRF.clock());
			}
			// while (theRF.inUse()) {
			// // waiting for the current transmission to end!
			// continue;
			// }
			// wait IFS
			Thread.sleep(RF.aSIFSTime);
			// If the medium is not idle restart and change the size of the
			// window...
			while (theRF.inUse()) {
				changeWindowSize();
				Thread.sleep(RF.aSIFSTime);
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
			if (slotType == 1) { // pick the maximum value from the window
									// always...
				int waitTime = window[window.length - 1];
				long currentTime = theRF.clock();
				long round = currentTime % 50;
				if (debugLevel == 1) {
					output.println("Changing Window Size at: " + theRF.clock());
				}
				Thread.sleep((waitTime + round) * 100);
				// transmit the frame
				transmit();
				
			} else { // pick a random value from the window...
				int ranVal = randomGen.nextInt(window.length - 1);
				int waitTime = window[ranVal];
				long currentTime = theRF.clock();
				long round = currentTime % 50;
				if (debugLevel == 1) {
					output.println("Changing Window Size at: " + theRF.clock());
				}
				// wait for the selected time.
				Thread.sleep((waitTime + round) * 100);

				// Transmit the frame
				transmit();

			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	/**
	 * Transmits the given frame. Will wait until an ACK is received before
	 * killing the Thread. Will resend the data if a timeout accrues while
	 * waiting for the ACK.
	 */
	public void transmit() {
		theRF.transmit(data);
		dataSent = true;
		timeout = 1000;
	}

	public void run() {
		for (;;) {
			try {
				data = queue.take();
			} catch (InterruptedException e) {
				
			}
			System.out.println("trying to run send");
			runStart = theRF.clock(); // used to check for timeout
			try {
				boolean waitIFS = false;

				System.out.println("\tSending.....");
				while (dataSent == false) {
					// if no one is currently transmitting and we have not
					// waited IFS yet
					if (!theRF.inUse() && waitIFS == false) {
						System.out.println("\tWaiting...");
						Thread.sleep(RF.aSIFSTime); // wait for the interframe
													// time.
						System.out.println("\tDone wainting.");
						waitIFS = true; // Set waitIFS to true for next
										// iteration of
										// the loop.
						continue;
					}

					// if no one is currently transmitting and we have waited
					// IFS
					else if (!theRF.inUse() && waitIFS == true) {
						System.out.println("\tgonna transmit");
						transmit();
					}

					// if the medium is busy
					else if (theRF.inUse()) {
						System.out.println("\tThe chanel is busy!");
						notIdle();
					}
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			if (debugLevel == 1) {
				output.println("Sent original packet at: " + theRF.clock());
			}
			dataSent = false;
		}
	}
	
	
}