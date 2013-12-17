package wifi;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.CRC32;

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
   long timeout = (long) 1000;
   private int debugLevel;
   private int slotSelection;
   private int beaconInterval;
   private int status;
   
   /**
    * Constructor takes a MAC address and the PrintWriter to which our output will
    * be written.
    * @param ourMAC  MAC address
    * @param output  Output stream associated with GUI
    */
   public LinkLayer(short ourMAC, PrintWriter output) {
	  debugLevel = 0;
	  slotSelection = 0;
	  beaconInterval = 0;
	  queue = new LinkedList<byte[]>();
	  sequenceTable = new Integer[1000][2];
      this.ourMAC = ourMAC;
      this.output = output;
      try {
    	  theRF = new RF(null, null);
      }
      catch(Exception e) {
    	  status = 3;
      }
      output.println("LinkLayer: Constructor ran.");
      receiver = new Receiver(theRF, ourMAC, output);
      (new Thread(receiver)).start();
      status = 1;
   }
   
	public boolean ACKReceived(int seqNum){
		System.out.println("In ACKReceived!!!!!!");
		for(int i = 0; i<receiver.ACKqueue.size(); i++){
			if(receiver.ACKqueue.get(i).equals(seqNum)){
				System.out.println("We found our ACK!");
				receiver.ACKqueue.remove(i);
				return true;
			}
		}
		return false;
	}
	
	
	public boolean gotPacket(int seq, byte[]data) {
		long time = timeout;
		System.out.println("About to check for a packet!");
		int retry = 0;
		long runStart = theRF.clock();
		for(;;) {
			
			if(retry == RF.dot11RetryLimit){
				//end
				System.out.println("I give up!");
				queue.remove(data);
				status = 5;
				return true;
			}
			if(ACKReceived(seq) == true){
				System.out.println("We got an ACK!");
				System.out.println("We sent the packet!");
				queue.remove(data);
				status = 4;
				return true;
			}
			if(theRF.clock() - runStart >= time){
				System.out.println("TIMEOUT!");
				runStart = theRF.clock();
				Sender sender = new Sender(theRF, data, slotSelection);
				Thread send = new Thread(sender);
				send.start();
				if(debugLevel == 1) {
					 output.println("Sending new packet at: " + theRF.clock());
					 output.println("Status: " + status());
				 }
				retry++;
				runStart = theRF.clock();
			}
			try {
					Thread.sleep(time);
					time = time*2;
					System.out.println("slept");
			} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					status = 2;
			}
		}
		
		
	}

   /**
    * Send method takes a destination, a buffer (array) of data, and the number
    * of bytes to send.  See docs for full description.
    */

   public int send(short dest, byte[] data, int len) {
	   if(queue.size() > 4) {
		   status = 10;
		   return 0;
	   }
	   if(dest < -1 || dest > 1100) {
		   status = 8;
		   return 0;
	   }
	   if(data.length < 0 || len < 0) {
		   status = 9;
		   return 0;
	   }
	   int maxSize = theRF.aMPDUMaximumLength-10;
	   int seq = 0;
	   if(dest != -1) {
		   if(sequenceTable[dest][1] != null) {
			   seq = sequenceTable[dest][1];
		   }
		   else{
		 	  sequenceTable[dest][1] = 0;
		   }
		   sequenceTable[dest][1]++;
	   }
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
				byte[] frame = builder.makeDataFrame(dest, ourMAC, seq);
				queue.add(frame);
				k++;
				if(dest != -1) {
					seq++;
					sequenceTable[dest][1]++;
				}
				
			}
			byte[] lastPacket = new byte[packLeft];
			for(int i = 0; j < data.length; i++) {
				lastPacket[i] = data[j];
				j++;
			}
			FrameMaker builder = new FrameMaker(lastPacket);
			byte[] frame = builder.makeDataFrame(dest, ourMAC, seq);
			queue.add(frame);
			while(queue.isEmpty() != true) {
				frame = queue.getFirst();
				 Sender sender = new Sender(theRF, frame, slotSelection);
				 Thread send = new Thread(sender);
				   send.start();
				   try {
					   send.join();
				   } catch (InterruptedException e) {
					   System.out.println("didn't end up waiting oops");
					   status = 2;
				   }
				 while(sender.dataSent == false) {
					 //do nothing
					 continue;
				 }
//				 try {
//					   send.join();
//				   } catch (InterruptedException e) {
//					   System.out.println("didn't end up waiting oops");
//				   }
				 if(debugLevel == 1) {
					 output.println("Sent original packet at: " + theRF.clock());
					 output.println("Status:  " + status());
				 }
				   FrameMaker parser = new FrameMaker();
				   if(parser.isACK(data) || dest == -1) {
					   
				   }
				   else{
					   gotPacket(seq, frame);
				   }
				   queue.removeFirst();
			}
		}
		else {
			output.println("LinkLayer: Sending "+len+" bytes to "+dest);
		    FrameMaker theFrame = new FrameMaker(data);
		    byte[] theDataFrame = theFrame.makeDataFrame(dest, ourMAC, seq); //set control info to zero because we turn the crc to all ones in makeDataFrame
		    queue.add(theDataFrame);
		    Sender sender = new Sender(theRF, theDataFrame, slotSelection);
		
		   //start the thread
		   Thread send = new Thread(sender);
		   send.start();
//		   try {
//			   send.join();
//		   } catch (InterruptedException e) {
//			   System.out.println("didn't end up waiting oops");
//		   }
		   if(debugLevel == 1) {
				 output.println("Sent original packet at: " + theRF.clock());
				 output.println("Status:  " + status());
			 }
		   FrameMaker parser = new FrameMaker();
		   if(parser.isACK(data) || dest == -1) {
			   
		   }
		   else{
			   gotPacket(seq, theDataFrame);
		   }
		}
   return len;
   }

   /**
    * Recv method blocks until data arrives, then writes it an address info into
    * the Transmission object.  See docs for full description.
    */
   public int recv(Transmission t) {
	   if(t == null) {
		   status = 9;
		   return 0;
	   }
	  for(;;) { //run forever
		  try {
              Thread.sleep(10);//sleep
           } 
		  catch (InterruptedException e) {
              status = 2;
           }   
		  if(receiver.queue.isEmpty() != true) { //when the queue isn't empty
			  System.out.println("got something");
	    	  byte[] packet = receiver.queue.remove(0); //get the packet
		      FrameMaker parser = new FrameMaker(); 
		      short dest = parser.getDest(packet); //get out the destination address
		      short src = parser.getSrc(packet); //get out the source address
		      byte[] data = parser.getData(packet); //get out the data
		      byte[] check = t.getBuf();
		      if(check.length > 0) {
		    	  t.setSourceAddr(src); //put everything in the transmission object
		    	  t.setDestAddr(dest);
		    	  t.setBuf(data);
		      }
		      else {
		    	  status = 6;
		      }
		      if(debugLevel == 1) {
					 output.println("Received packet at: " + theRF.clock());
					 output.println(status());
				 }
		      return data.length; //return amount of data received
		  }
	  }
     
   }

   /**
    * Returns a current status code.  See docs for full description.
    */
   public int status() {
		if(status == 1) {
			output.println("LinkLayer: SUCCESS");
		}
		if(status == 2) {
			output.println("LinkLayer Status: UNSPECIFIED_ERROR");
		}
		if(status == 3) {
			output.println("LinkLayer Status: RF_INIT_FAILED");
		}
		if(status == 4) {
			output.println("LinkLayer Status: TX_DELIVERED");
		}
		if(status == 5) {
			output.println("LinkLayer Status: TX_FAILED");
		}
		if(status == 6) {
			output.println("LinkLayer Status: BAD_BUF_SIZE");
		}
		if(status == 7) {
			output.println("LinkLayer Status: BAD_ADDRESS");
		}
		if(status == 8) {
			output.println("LinkLayer Status: BAD_MAC_ADDRESS");
		}
		if(status == 9) {
			output.println("LinkLayer Status: ILLEGAL_ARGUMENT");
		}
		if(status == 10) {
			output.println("LinkLayer Status: INSUFFICIENT_BUFFER_SPACE");
		}
		return status;
   }

   /**
    * Passes command info to your link layer.  See docs for full description.
    */
   public int command(int cmd, int val) {
      output.println("LinkLayer: Sending command "+cmd+" with value "+val);
      if(cmd == 0) {
    	  output.println("LinkLayer: Current Command Settings: ");
    	  output.println("Debug level: " + debugLevel);
    	  output.println("Slot Window Type: " + slotSelection);
    	  output.println("Beacon wait interval: " + beaconInterval);
      }
      if(cmd == 1) {
    	  if(val == 0 || val == 1) {
    		  debugLevel = val;
    		  if(val == 0) {
    			  output.println("Debugging off");
    		  }
    		  else {
    			  output.println("Debugging on");
    		  }
    	  }
    	  else {
    		  output.println("Please only enter 0 for no debugging output or 1 for all debugging output");
    	  }
      }
      if(cmd == 2) {
    	  if(val > 0 && val < 2) {
    		  slotSelection = val;
    		  if(val == 0) {
    			  output.println("Slot selection now random");
    		  }
    		  else {
    			  output.println("Slot selection now fixed");
    		  }
    	  }
    	  else {
    		  output.println("Please select a valid slot selection type");
    	  }
      }
      if(cmd == 3) {
    	  if(val > -2 && val < 100) {
    		  beaconInterval = val;
    		  output.println("Beacon interval set to: " + beaconInterval);
    	  }
    	  else {
    		  output.println("Please select a valid beacon interval time");
    	  }
      }
      return 0;
   }
}
