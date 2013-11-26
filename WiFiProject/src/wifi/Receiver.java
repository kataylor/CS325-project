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
	
	//Added for Address Selectivity
	short ourMAC; //our MAC address. Used to determine if incoming packets are for us or not.
	short broadcastAddress = (short)((Integer.MAX_VALUE >>8) & 0xffff); //the broadcast address is a short of all 1's. (11111111 11111111)
	
	
	/**
	 * Constructor.
	 * @param theRF
	 * @param ourMAC
	 */
	public Receiver(RF theRF, short ourMAC) { //<<Changed constructor for Address Selectivity
		queue = new ArrayList<byte[]>();
		this.theRF = theRF;
		this.ourMAC = ourMAC;
	}
	
	/**
	 * Get the destination address of the received packet.
	 * 
	 * @return isValid is true if the incoming packet has our MAC address or if it has the broadcastAddress.
	 * @author Christoper Livingston
	 */
	public boolean forUs(byte[] packet){
		boolean isValid = false; 

		FrameMaker frame = new FrameMaker(packet);
		short destAddress = frame.getDest(packet);
		System.out.println("The destAddress is: " + destAddress);
		
		if(destAddress == ourMAC || destAddress == broadcastAddress){
			isValid = true;
		}
		return isValid;
	}
	
	/**
	 * 
	 */
	public void run() {
		for(;;) {
			byte[] packet = theRF.receive(); //blocks until something is received 
			
			//ADDED for Address selectivity.
			//Only packets with our MAC addresses will be queued for delivery.
			if(forUs(packet)){
				queue.add(packet); //add received item to the queue 
			}
			else{
				System.out.println("The recived packet is not for us...");
			}
		}
		
	}
	
	
	
}