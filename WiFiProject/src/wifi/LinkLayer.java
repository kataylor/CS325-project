package wifi;
import java.io.PrintWriter;

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

   /**
    * Constructor takes a MAC address and the PrintWriter to which our output will
    * be written.
    * @param ourMAC  MAC address
    * @param output  Output stream associated with GUI
    */
   public LinkLayer(short ourMAC, PrintWriter output) {
      this.ourMAC = ourMAC;
      this.output = output;      
      theRF = new RF(null, null);
      output.println("LinkLayer: Constructor ran.");
      receiver = new Receiver(theRF);
      (new Thread(receiver)).start();
   }

   /**
    * Send method takes a destination, a buffer (array) of data, and the number
    * of bytes to send.  See docs for full description.
    */
   public int send(short dest, byte[] data, int len) {
      output.println("LinkLayer: Sending "+len+" bytes to "+dest);
     
      FrameMaker theFrame = new FrameMaker(data);
      byte[] theDataFrame = theFrame.makeDataFrame(dest, ourMAC, 0); //set control info to zero because we turn the crc to all ones in makeDataFrame
      Sender sender = new Sender(theRF, theDataFrame);
      theRF.transmit(theDataFrame);
      
      //start the thread
      (new Thread(sender)).start();
      
      return len;
   }

   /**
    * Recv method blocks until data arrives, then writes it an address info into
    * the Transmission object.  See docs for full description.
    */
   public int recv(Transmission t) {
      while(receiver.queue.isEmpty() == true);
      byte[] packet = receiver.queue.remove(0);
      FrameMaker parser = new FrameMaker();
      short dest = parser.getDest(packet);
      short src = parser.getSrc(packet);
      byte[] data = parser.getData(packet);
      t.setSourceAddr(src);
      t.setDestAddr(dest);
      t.setBuf(data);
      return data.length;
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
