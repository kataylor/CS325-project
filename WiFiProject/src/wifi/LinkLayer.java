package wifi;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface {
   private RF theRF;           // You'll need one of these eventually
   private short ourMAC;       // Our MAC address
   private PrintWriter output; // The output stream we'll write to
   private Receiver receiver;
   Integer[][] sequenceTable;
   private LinkedList<byte[]> queue;
   long timeout = (long) 10000;

   /**
    * Constructor takes a MAC address and the PrintWriter to which our output will
    * be written.
    * @param ourMAC  MAC address
    * @param output  Output stream associated with GUI
    */
   public LinkLayer(short ourMAC, PrintWriter output) {
	   queue = new LinkedList<byte[]>();
	  sequenceTable = new Integer[1000][2];
      this.ourMAC = ourMAC;
      this.output = output;      
      theRF = new RF(null, null);
      output.println("LinkLayer: Constructor ran.");
      receiver = new Receiver(theRF, ourMAC, output);
      (new Thread(receiver)).start();
   }
   
	public boolean ACKReceived(int seqNum){
		//System.out.println("In ACKReceived!!!!!!");
		for(int i = 0; i<receiver.ACKqueue.size(); i++){
			if(receiver.ACKqueue.get(i) == seqNum){
				System.out.println("We found our ACK!");
				return true;
			}
		}
		return false;
	}
	
	
	public boolean gotPacket(int seq, byte[]data) {
		System.out.println("About to check for a packet!");
		long runStart = theRF.clock();
		for(;;) {
			int retry = 0;
			if(retry == RF.dot11RetryLimit){
				//end
				System.out.println("I give up!");
				return true;
			}
			if(ACKReceived(seq) == true){
				System.out.println("We got an ACK!");
				System.out.println("We sent the packet!");
				return true;
			}
			if(theRF.clock() - runStart >= timeout){
				System.out.println("TIMEOUT!");
				runStart = theRF.clock();
				theRF.transmit(data);
				retry++;
			}
		}
		
		
	}

   /**
    * Send method takes a destination, a buffer (array) of data, and the number
    * of bytes to send.  See docs for full description.
    */

   public int send(short dest, byte[] data, int len) {
	   int maxSize = theRF.aMPDUMaximumLength-10;
	   int seq = 0;
	   if(sequenceTable[dest][1] != null) {
		   seq = sequenceTable[dest][1];
	   }
	   else{
		   sequenceTable[dest][1] = 0;
	   }
	   sequenceTable[dest][1]++;
		if(data.length > maxSize) {
			System.out.println("breaking things up!");
			int packNum = data.length/maxSize;
			int packLeft = data.length%maxSize;
			byte[] dataPacket = new byte[maxSize];
			int j = 0;
			int k = 0;
			while(k != packNum) {
				for(int i = 0; i < maxSize; i++) {
					dataPacket[i] = data[j];
					j++;
				}
				FrameMaker builder = new FrameMaker(dataPacket);
				byte[] frame = builder.makeDataFrame(dest, ourMAC, 0, seq);
				queue.add(frame);
				k++;
				seq++;
				sequenceTable[dest][1]++;
			}
			byte[] lastPacket = new byte[packLeft];
			for(int i = 0; j < data.length; i++) {
				lastPacket[i] = data[j];
				j++;
			}
			FrameMaker builder = new FrameMaker(lastPacket);
			byte[] frame = builder.makeDataFrame(dest, ourMAC, 0, seq);
			queue.add(frame);
			while(queue.isEmpty() != true) {
				 Sender sender = new Sender(theRF, queue.getFirst());
				 (new Thread(sender)).start();
				 while(sender.dataSent == false) {
					 //do nothing
					 continue;
				 }
				 gotPacket(seq,frame);
			}
		}
		else {
			output.println("LinkLayer: Sending "+len+" bytes to "+dest);
		    FrameMaker theFrame = new FrameMaker(data);
		    byte[] theDataFrame = theFrame.makeDataFrame(dest, ourMAC, 0, seq); //set control info to zero because we turn the crc to all ones in makeDataFrame
		    Sender sender = new Sender(theRF, theDataFrame);
		
		   //start the thread
		   Thread send = new Thread(sender);
		   send.start();
		   try {
			send.join();
		} catch (InterruptedException e) {
			System.out.println("didn't end up waiting oops");
		}
		   FrameMaker parser = new FrameMaker();
		   if(parser.isACK(data)) {
			   
		   }
		   gotPacket(seq, theDataFrame);
		}
	   
    
   
   return len;
}

   /**
    * Recv method blocks until data arrives, then writes it an address info into
    * the Transmission object.  See docs for full description.
    */
   public int recv(Transmission t) {
	  for(;;) { //run forever
		  try {
              Thread.sleep(10);//sleep
           } 
		  catch (InterruptedException e) {
              // Do nothing
           }   
		  if(receiver.queue.isEmpty() != true) { //when the queue isn't empty
			  System.out.println("got something");
	    	  byte[] packet = receiver.queue.remove(0); //get the packet
		      FrameMaker parser = new FrameMaker(); 
		      short dest = parser.getDest(packet); //get out the destination address
		      short src = parser.getSrc(packet); //get out the source address
		      byte[] data = parser.getData(packet); //get out the data
		      t.setSourceAddr(src); //put everything in the transmission object
		      t.setDestAddr(dest);
		      t.setBuf(data);
		      return data.length; //return amount of data received
		  }
	  }
     
   }

   /**
    * Returns a current status code.  See docs for full description.
    */
   public int status() {
      output.println("LinkLayer: Faking a status() return value of 0");
      return 0;
   }

   /**
    * Passes command info to your link layer.  See docs for full description.
    */
   public int command(int cmd, int val) {
      output.println("LinkLayer: Sending command "+cmd+" with value "+val);
      return 0;
   }
}
