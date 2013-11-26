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
	int[] window = new int[0]; //Size will be changed in changeWindowSize
	Random randomGen = new Random();
	long timeout = (long)10; //timeout stuff

	public Sender(RF theRF, byte[] data) {
		System.out.println("Sender: Constructor ran");
		System.out.println(timeout);
		this.theRF = theRF;
		this.data = data;
	}

	/**
	 * When the channel is busy.
	 */
	public void notIdle() {
		try {
			System.out.println("In notIdel!");
			
			while (theRF.inUse()) {
				// waiting for the current transmission to end!
			}
			
			// wait IFS
			Thread.sleep(RF.aSIFSTime);

			// If the medium is not idle restart and change the size of the window...
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
	
	public void changeWindowSize(){
		System.out.println("Changing the window size!");
		if(window.length == 0){
			window = new int[RF.aCWmin];
		}
		else{
			if(((window.length)*2) > RF.aCWmax){
				window = new int[RF.aCWmax];
				//fill the window
				for(int i=0; i<window.length; i++){
					window[i] =i;
				}
			}
			else{
				window = new int[(window.length) *2];
				//fill the window
				for(int i=0; i<window.length; i++){
					window[i] =i;
				}
			}
		}
	}

	/**
	 * Exponential back off and then transmit.
	 */
	public void backOff() {
		boolean sent = false;

		if(window.length == 0){
			changeWindowSize();
		}

		try {
			while (sent != true) {
				// Pick a random value from the window.
				int ranVal = randomGen.nextInt(window.length - 1);
				int waitTime = window[ranVal];

				// wait for the selected time.
				Thread.sleep(waitTime * 100);

				if (theRF.inUse()) {
					Thread.sleep(RF.aSIFSTime);
				}

				// Transmit the frame
				int bytesSent = theRF.transmit(data);
				if (bytesSent == data.length) {// if we sent the correct number of bytes.
					System.out.println("We sent the packet! From backoff!");
					sent = true;
				}

			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	public void run() {
		long currentTime = theRF.clock(); //used to check for timeout
		
		try {
			boolean sent = false;
			boolean waitIFS = false;

			while (sent == false) {
				System.out.println("Sending.....");

				// if no one is currently transmitting and we have not waited IFS yet
				if (!theRF.inUse() && waitIFS == false) {
					System.out.println("Waiting...");
					Thread.sleep(RF.aSIFSTime); // wait for the interframe time.
					System.out.println("Done wainting.");
					waitIFS = true; // Set waitIFS to true for next iteration of the loop.
					continue;
				}

				// if no one is currently transmitting and we have waited IFS
				if (!theRF.inUse() && waitIFS == true) {
					int bytesSent = theRF.transmit(data);
					if (bytesSent == data.length) {// if we sent the correct number of bytes.
						System.out.println("We sent the packet!");
						// kill the thread here
						sent = true;
						continue;
					}
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