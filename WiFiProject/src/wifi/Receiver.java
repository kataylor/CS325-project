package wifi;

import java.io.PrintWriter;
import java.util.ArrayList;
import rf.RF;

/**
 * A class that watches for incoming packets and places them in a queue for removal by LinkLayer.
 * @author Kirah Taylor
 */
public class Receiver implements Runnable {
	
	
	//Added for Address Selectivity
	short ourMAC; //our MAC address. Used to determine if incoming packets are for us or not.
	short broadcastAddress = (short)((Integer.MAX_VALUE >>8) & 0xffff); //the broadcast address is a short of all 1's. (11111111 11111111)
	ArrayList<byte[]> queue; //the current queue in use; will be changed to a better data structure in the future
	static ArrayList<Integer> ACKqueue;
	RF theRF;
	FrameMaker parser;
	private PrintWriter output;
	int[][] sequences;
	
	/**
	 * Constructor.
	 * @param theRF
	 * @param ourMAC
	 */
	public Receiver(RF theRF, short ourMAC, PrintWriter output) { //<<Changed constructor for Address Selectivity
		queue = new ArrayList<byte[]>();
		ACKqueue = new ArrayList<Integer>();
		this.theRF = theRF;
		parser = new FrameMaker();
		this.ourMAC = ourMAC;
		this.output = output;
		sequences = new int[1000][2];
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
		
		if(destAddress == ourMAC || destAddress == broadcastAddress){
			isValid = true;
		}
		return isValid;
	}
	
	public static boolean ACKReceived(int seqNum){
		for(int i = 0; i<ACKqueue.size(); i++){
			if(ACKqueue.get(i) == seqNum){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 */
	public void run() {
		for(;;) {
			byte[] packet = theRF.receive(); //blocks until something is received 
			int seq = parser.getSequnceNumber(packet);
			//ADDED for Address selectivity.
			//Only packets with our MAC addresses will be queued for delivery.
			if(forUs(packet)){
				if(parser.isACK(packet) == true) {
					System.out.println("The seqnum is: " + seq);
					ACKqueue.add(seq);
					continue;
				}
				short dest = parser.getSrc(packet);
				
				boolean wasRecvd = false;
				if(sequences[dest][1] == seq) {
					wasRecvd = true;
				}
				else {
					if(seq > (sequences[dest][1])+1) {
						System.out.println("seq is " + seq + " and had stored " + sequences[dest][1]);
						output.println("Gap detected in packet arrival!");
					}
					sequences[dest][1] = seq;
				}
				if(wasRecvd == false) {
					queue.add(packet); //add received item to the queue
					if(parser.getDest(packet) != broadcastAddress) {
						System.out.println("gonna send an ack!");
						byte[] ack = parser.makeACKFrame(dest, ourMAC, 0, seq);
						try {
							Thread.sleep(theRF.aSIFSTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						theRF.transmit(ack);
					}
					
				}
			}

		}
		
	}
	
	
	
}