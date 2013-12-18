package wifi;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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
	ArrayList<Integer> ACKqueue;
	RF theRF;
	FrameMaker parser;
	private PrintWriter output;
	int[][] sequences;
	private int debugLevel;
	public long beaconArrivalTime;
	public long beaconEndTime;
	public int average;
	public long totalAverage;
	public long total;
	
	/**
	 * Constructor.
	 * @param theRF
	 * @param ourMAC
	 */
	public Receiver(RF theRF, short ourMAC, PrintWriter output, int debugLevel) { //<<Changed constructor for Address Selectivity
		queue = new ArrayList<byte[]>();
		beaconArrivalTime = 0;
		totalAverage = 0;
		beaconEndTime = 0;
		ACKqueue = new ArrayList<Integer>();
		this.theRF = theRF;
		parser = new FrameMaker();
		this.ourMAC = ourMAC;
		this.output = output;
		sequences = new int[1000][2];
		this.debugLevel = debugLevel;
		total = 0;
		average = 0;
	}
	
	public void changeDebugLevel(int newLevel) {
		debugLevel = newLevel;
	}
	
	/**
	 * Get the destination address of the received packet.
	 * 
	 * @return isValid is true if the incoming packet has our MAC address or if it has the broadcastAddress.
	 */
	public boolean forUs(byte[] packet){
		boolean isValid = false; 

		FrameMaker frame = new FrameMaker(packet);
		short destAddress = frame.getDest(packet);
		if (debugLevel == 1) {
			output.println("Received packet for " + destAddress+ " at: " + theRF.clock());
		}
		if(destAddress == ourMAC || destAddress == broadcastAddress){
			isValid = true;
		}
		return isValid;
	}
	
	public boolean ACKReceived(int seqNum){
		System.out.println("\t\tIn ACKReceived!!!!!!");
		for(int i = 0; i<ACKqueue.size(); i++){
			if(ACKqueue.get(i) == seqNum){
				System.out.println("\t\tWe found our ACK!");
				return true;
			}
		}
		return false;
	}
	
	public void handleBeacon(byte[] data) {
		byte[] time = parser.getData(data);
		String s = parser.toBinaryString(time);
		long newTime = Long.parseLong(s, 2);
		newTime = newTime + 2;
		if(newTime > theRF.clock()) {
			String name = System.getProperty("os.name").toLowerCase();
			if(name.startsWith("win")) {
				SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
				Calendar cal = Calendar.getInstance();
			    cal.setTimeInMillis(newTime);
			    try {
					Runtime.getRuntime().exec("cmd /C time " + format.format(cal.getTime()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(name.indexOf("nix") >= 0 || name.indexOf("nux") >= 0) {
				Calendar cal = Calendar.getInstance();
			    cal.setTimeInMillis(newTime);
			    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
				try {
					Runtime.getRuntime().exec(new String[]{"date","--set", format.format(cal.getTime())});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
//		beaconEndTime = theRF.clock();
//		total = total + (beaconEndTime - beaconArrivalTime);
//		System.out.println(total);
//		average++;
//		System.out.println("beacon was carrying time: " + newTime);
//		if(average == 10) {
//			totalAverage = total/average;
//			System.out.println("Average time to process a beacon: " + totalAverage);
//		}
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
					System.out.println("\t\tThe seqnum is: " + seq);
					System.out.println("\t\tAdding ACK with seqnum " + seq + " to the queue");
					ACKqueue.add(seq);
					continue;
				}
				if(parser.isBeacon(packet) == true) {
					beaconArrivalTime = theRF.clock();
					handleBeacon(packet);
					continue;
				}
				short dest = parser.getSrc(packet);
				
				boolean wasRecvd = false;
				if(sequences[dest][1] == seq) {
					wasRecvd = true;
				}
				else {
					if(seq > (sequences[dest][1])+1) {
						System.out.println("\t\tseq is " + seq + " and had stored " + sequences[dest][1]);
						output.println("\t\tGap detected in packet arrival!");
					}
					sequences[dest][1] = seq;
				}
				if(wasRecvd == false) {
					queue.add(packet); //add received item to the queue
					if(parser.getDest(packet) != broadcastAddress) {
						System.out.println("\t\tgonna send an ack!");
						byte[] ack = parser.makeACKFrame(dest, ourMAC, 0, seq);
						try {
							Thread.sleep(RF.aSIFSTime);
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