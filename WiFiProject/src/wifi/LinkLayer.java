package wifi;

import java.io.PrintWriter;

import rf.RF;

/**
 * Use this layer as a starting point for your project code. See
 * {@link Dot11Interface} for more details on these routines.
 * 
 * @author richards
 */
public class LinkLayer implements Dot11Interface {
	private RF theRF; // You'll need one of these eventually
	private short ourMAC; // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	private Receiver receiver;
	private Sender sender;
	Integer[][] sequenceTable;
	long timeout = 2500;
	private int debugLevel;
	private int slotSelection;
	private int beaconInterval;
	private int status;
	private int senderSize;
	private beaconSender beacons;
	public long testInt;
	public int testInt2;

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output
	 * will be written.
	 * 
	 * @param ourMAC MAC address
	 * @param output Output stream associated with GUI
	 *           
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		testInt = 0;
		testInt2 = 0;
		debugLevel = 0;
		slotSelection = 0;
		beaconInterval = -1;
		senderSize = 0;
		sequenceTable = new Integer[1000][2];
		this.ourMAC = ourMAC;
		this.output = output;
		try {
			theRF = new RF(null, null);
		} catch (Exception e) {
			status = 3;
		}
		output.println("LinkLayer: Constructor ran.");
		receiver = new Receiver(theRF, ourMAC, output, debugLevel);
		(new Thread(receiver)).start();
		sender = new Sender(theRF, slotSelection, output, debugLevel);
		(new Thread(sender)).start();
		beacons = new beaconSender(beaconInterval);
		(new Thread(beacons)).start();
		status = 1;
	}

	public boolean ACKReceived(int seqNum) {
		System.out.println("In ACKReceived!!!!!!");
		for (int i = 0; i < receiver.ACKqueue.size(); i++) {
			if (receiver.ACKqueue.get(i).equals(seqNum)) {
				System.out.println("We found our ACK!");
				receiver.ACKqueue.remove(i);
				return true;
			}
		}
		return false;
	}

	
	public void sendThread(short dest, byte[] data, int len) {
		int maxSize = RF.aMPDUMaximumLength - 10;
		int seq = 0;
		if (dest != -1) {
			if (sequenceTable[dest][1] != null) {
				seq = sequenceTable[dest][1];
			} else {
				sequenceTable[dest][1] = 0;
			}
			sequenceTable[dest][1]++;
		}
		if (data.length > maxSize) {
			System.out.println("breaking things up!");
			int packNum = data.length / maxSize;
			int packLeft = data.length % maxSize;
			byte[] dataPacket = new byte[maxSize];
			int j = 0;
			int k = 0;
			while (k != packNum) {
				for (int i = 0; i < maxSize; i++) {
					dataPacket[i] = data[j];
					j++;
				}
				FrameMaker builder = new FrameMaker(dataPacket);
				byte[] frame = builder.makeDataFrame(dest, ourMAC, seq);
				sender.queue.add(frame);
				k++;
				if (dest != -1) {
					seq++;
					sequenceTable[dest][1]++;
				}
			}
			byte[] lastPacket = new byte[packLeft];
			for (int i = 0; j < data.length; i++) {
				lastPacket[i] = data[j];
				j++;
			}
			FrameMaker builder = new FrameMaker(lastPacket);
			byte[] frame = builder.makeDataFrame(dest, ourMAC, seq);
			sender.queue.add(frame);
			while (sender.queue.isEmpty() != true) {
				frame = sender.queue.peek();
//				while (sender.dataSent == false) {
//					// do nothing
//					continue;
//				}
				if (debugLevel == 1) {
					output.println("Sending original packet at: " + theRF.clock());
					output.println("Status:  " + status());
				}
				FrameMaker parser = new FrameMaker();
				if (parser.isACK(data) || dest == -1) {

				} else {
					ackChecker ackCheck = new ackChecker(frame, seq);
					(new Thread(ackCheck)).start();
				}
			}
		} else {
			output.println("LinkLayer: Sending " + len + " bytes to " + dest);
			FrameMaker theFrame = new FrameMaker(data);
			byte[] theDataFrame = theFrame.makeDataFrame(dest, ourMAC, seq); 
			sender.queue.add(theDataFrame);
			if (debugLevel == 1) {
				output.println("Sent original packet at: " + theRF.clock());
				output.println("Status:  " + status());
			}
			FrameMaker parser = new FrameMaker();
			if (parser.isACK(data) || dest == -1 || parser.isBeacon(data)) {

			} else {
				ackChecker ackCheck = new ackChecker(theDataFrame, seq);
				(new Thread(ackCheck)).start();
			}
		}
	}

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send. See docs for full description.
	 */

	public int send(short dest, byte[] data, int len) {
		if (sender.queue.size() > 3 || senderSize > 3) {
			status = 10;
			return 0;
		}
		if (dest < -1 || dest > 1100) {
			status = 8;
			return 0;
		}
		if (data.length < 0 || len < 0) {
			status = 9;
			return 0;
		}
		sendThread(dest, data, len);
		if(dest != -1) {
			senderSize++;
		}
		return len;
	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info
	 * into the Transmission object. See docs for full description.
	 */
	public int recv(Transmission t) {
		if (t == null) {
			status = 9;
			return 0;
		}
		for (;;) { // run forever
			try {
				Thread.sleep(10);// sleep
			} catch (InterruptedException e) {
				status = 2;
			}
			if (receiver.queue.isEmpty() != true) { // when the queue isn't empty
				System.out.println("got something");
				byte[] packet = receiver.queue.remove(0); // get the packet
				FrameMaker parser = new FrameMaker();
				short dest = parser.getDest(packet); // get out the destinationaddress
				short src = parser.getSrc(packet); // get out the source address
				byte[] data = parser.getData(packet); // get out the data
				byte[] check = t.getBuf();
				if (check.length <  0) {
					status = 6;
				} else {
					t.setSourceAddr(src); // put everything in the transmissionobject
					t.setDestAddr(dest);
					t.setBuf(data);
				}
				if (debugLevel == 1) {
					output.println("Received packet at: " + theRF.clock());
					output.println(status());
				}
				return data.length; // return amount of data received
			}
		}

	}

	/**
	 * Returns a current status code. See docs for full description.
	 */
	public int status() {
		if (status == 1) {
			output.println("LinkLayer Status: SUCCESS");
		}
		if (status == 2) {
			output.println("LinkLayer Status: UNSPECIFIED_ERROR");
		}
		if (status == 3) {
			output.println("LinkLayer Status: RF_INIT_FAILED");
		}
		if (status == 4) {
			output.println("LinkLayer Status: TX_DELIVERED");
		}
		if (status == 5) {
			output.println("LinkLayer Status: TX_FAILED");
		}
		if (status == 6) {
			output.println("LinkLayer Status: BAD_BUF_SIZE");
		}
		if (status == 7) {
			output.println("LinkLayer Status: BAD_ADDRESS");
		}
		if (status == 8) {
			output.println("LinkLayer Status: BAD_MAC_ADDRESS");
		}
		if (status == 9) {
			output.println("LinkLayer Status: ILLEGAL_ARGUMENT");
		}
		if (status == 10) {
			output.println("LinkLayer Status: INSUFFICIENT_BUFFER_SPACE");
		}
		return status;
	}

	/**
	 * Passes command info to your link layer. See docs for full description.
	 */
	public int command(int cmd, int val) {
		output.println("LinkLayer: Sending command " + cmd + " with value "
				+ val);
		if (cmd == 0) {
			output.println("LinkLayer: Current Command Settings: ");
			output.println("Debug level: " + debugLevel);
			output.println("Slot Window Type: " + slotSelection);
			output.println("Beacon wait interval: " + beaconInterval);
		}
		if (cmd == 1) {
			if (val == 0 || val == 1) {
				debugLevel = val;
				sender.changeDebugLevel(debugLevel);
				receiver.changeDebugLevel(debugLevel);
				if (val == 0) {
					output.println("Debugging off");
				} else {
					output.println("Debugging on");
				}
			} else {
				output.println("Please only enter 0 for no debugging output or 1 for all debugging output");
			}
		}
		if (cmd == 2) {
			if (val > 0 && val < 2) {
				slotSelection = val;
				sender.changeSlotType(val);
				if (val == 0) {
					output.println("Slot selection now random");
				} else {
					output.println("Slot selection now fixed");
				}
			} else {
				output.println("Please select a valid slot selection type");
			}
		}
		if (cmd == 3) {
			if (val > -2 && val < 100) {
				beaconInterval = val;
				beacons.changeRate(beaconInterval);
				output.println("Beacon interval set to: " + beaconInterval);
			} else {
				output.println("Please select a valid beacon interval time");
			}
		}
		return 0;
	}
	
	public class ackChecker implements Runnable {
		public byte[] data;
		int seq;
		
		public ackChecker(byte[] data, int seq) {
			this.data = data;
			this.seq = seq;
		}
		
		public void run() {
			long time = timeout;
			System.out.println("About to check for a packet!");
			int retry = 0;
			long runStart = theRF.clock();
			for (;;) {
				if (ACKReceived(seq) == true) {
					System.out.println("We got an ACK!");
					System.out.println("We sent the packet!");
					sender.queue.remove(data);
					senderSize--;
					status = 4;
					return;
				}
				if (retry == RF.dot11RetryLimit) {
					// end
					System.out.println("I give up!");
					sender.queue.remove(data);
					senderSize--;
					status = 5;
					return;
				}
				if (theRF.clock() - runStart >= time) {
					System.out.println("TIMEOUT!");
					runStart = theRF.clock();
					sender.queue.add(data);
					if (debugLevel == 1) {
						output.println("Sending new packet at: " + theRF.clock());
						output.println("Status: " + status());
					}
					retry++;
					time = time * 2;
					runStart = theRF.clock();
				}
				try {
					Thread.sleep(time);
					System.out.println("slept");
				} catch (InterruptedException e) {
					e.printStackTrace();
					status = 2;
				}
			}
		}
	}
	
	
	public class beaconSender implements Runnable {
		public int sendRate;
		
		public beaconSender(int sendRate) {
			this.sendRate = sendRate*1000;
			
		}
		
		public void changeRate(int newVal) {
			sendRate = newVal*1000;
		}
		
		public void run() {
			for(;;) {
				if(sendRate > 0) {
					testInt2++;
					try {
						Thread.sleep(sendRate);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					long ts = theRF.clock();
					testInt = testInt + ts;
					byte[] time = new byte[8];
					time[0] = (byte)(ts >>> 56);
				    time[1] = (byte)(ts >>> 48);
				    time[2] = (byte)(ts >>> 40);
				    time[3] = (byte)(ts >>> 32);
				    time[4] = (byte)(ts >>> 24);
				    time[5] = (byte)(ts >>> 16);
				    time[6] = (byte)(ts >>>  8);
				    time[7] = (byte)(ts >>>  0);
					FrameMaker beaconMaker = new FrameMaker(time);
					byte[] beacon = beaconMaker.makeBeaconFrame(ourMAC, 0, 0);
					if(senderSize <= 3) {
						sender.queue.add(beacon);
						senderSize++;
					}
				}
				else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						status = 2;
					}
					if(testInt2 == 10) {
						testInt = testInt/testInt2;
						System.out.println("the average creation time is: " + testInt);
					}
				}
			}
		}
		
		public long interpretArray(byte[] time) {
			String s = toBinaryString(time);
			long newTime = Long.parseLong(s, 2);
			return newTime;
		}
		
		public String toBinaryString(byte[] frame){
			String binaryString = ""; // Initialize a new empty String.
			for(int i=0; i<=frame.length-1; i++){
				binaryString += ("0000000"+Integer.toBinaryString(0xFF & frame[i])).replaceAll(".*(.{8})$", "$1");
			}
			//System.out.println("The binary representation of this frame is: " + binaryString);
			return binaryString;
		}
	}
}