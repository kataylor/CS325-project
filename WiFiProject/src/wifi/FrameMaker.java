package wifi;

public class FrameMaker {
	
	/**
	 * A class that makes frames to send and parses ones received.
	 * @author Christopher Livingston and Kirah Taylor
	 */
	
	private byte[] packetData; //the specific packet of data that needs to be packed in a frame
	byte[] frame; //the frame being created
	
	/**
	 * A constructor that makes a new frame around given data for sending
	 * @param data The data to be packed in the frame
	 */
	public FrameMaker(byte[] data) {
		packetData = data;
	}
	
	/**
	 * A constructor to make an instance of FrameMaker for the purpose of parsing
	 */
	public FrameMaker() {
		//do nothing
	}
	
	/**
	 * A method to make a data frame
	 * @param dest The destination address
	 * @param src The source address
	 * @param crc The checksum
	 * @return A byte array of the data frame
	 */
	public byte[] makeDataFrame(short dest, short src, int crc) {
		byte[] cont = setControl("000", "0", 0);
		byte[] frame = new byte[10 + packetData.length];
		//Set the control
		frame[0] = cont[0];
		frame[1] = cont[1];
		
		//Set the destination address 
		frame[3] = (byte)(dest);
		frame[2] = (byte)((dest >> 8) & 0xff);
		
		//Set the source address
		frame[5] = (byte)(src);
		frame[4] = (byte)((src >> 8) & 0xff);
		
		//put the data into the frame.
		for(int i = 0; i < packetData.length; i++) {
			frame[i+6] = packetData[i];
		}
		
		//Set the control
		frame[packetData.length+6] = (byte)255;
		frame[packetData.length+7] = (byte)255;
		frame[packetData.length+8] = (byte)255;
		frame[packetData.length+9] = (byte)255;
		return frame;
	}
	
	
	public byte[] makeACKFrame(short dest, short src, int crc, int seq) {
		byte[] cont = setControl("001", "0", seq);
		byte[] frame = new byte[10];
		frame[0] = cont[0];
		frame[1] = cont[1];
		frame[3] = (byte)(dest);
		frame[2] = (byte)((dest >> 8) & 0xff);
		frame[5] = (byte)(src);
		frame[4] = (byte)((src >> 8) & 0xff);
		frame[6] = (byte)255;
		frame[7] = (byte)255;
		frame[8] = (byte)255;
		frame[9] = (byte)255;
		return frame;
	}
	
	
	/**
	 * A method to set the control bits of a frame
	 * @param type A string representation of three bits setting the type of frame to be made (eg ACK, data, etc)
	 * @param retry A string representation of the one bit that signals if the packet is a retry 
	 * @param sequence An integer containing the checksum
	 * @return Returns a binary array of the control information
	 */
	public byte[] setControl(String type, String retry, int sequence) {
		String seq = Integer.toBinaryString(sequence);
		String total = type + retry + seq;
		while(total.length() < 16) {
			total = total + "0";
		}
		byte[] b = new byte[2];
				//new BigInteger(total, 2).toByteArray();
		b[0] = Byte.parseByte(total.substring(0, 7), 2);
		b[1] = Byte.parseByte(total.substring(8), 2);
		return b;
	}
	
	
	public int getSequnceNumber(byte[] packet) {
		String s = toBinaryString(packet);
		String n = s.substring(4, 16);
		int seq = Integer.parseInt(n, 2);
		return seq;
	}
	
	
	/**
	 * A method for extracting the destination address from a frame
	 * @param pack The frame to extract the address from
	 * @return A short of the destination address
	 */
	public short getDest(byte[] pack) {
		short newshort = (short) ((pack[2] << 8) + (pack[3]&0xFF));
		return newshort;
	}
	
	/**
	 * A method for extracting the source address from a frame
	 * @param pack The frame to extract the address from
	 * @return A short of the source address
	 */
	public short getSrc(byte[] pack) {
		short newshort = (short) ((pack[4] << 8) + (pack[5]&0xFF));
		return newshort;
	}
	
	/**
	 * A method to get the data from a frame
	 * @param pack The packet to extract data from
	 * @return A byte array containing the data packet
	 */
	public byte[] getData(byte[] pack) {
		int len = pack.length-10;
		byte[] data = new byte[pack.length-10];
		for(int i = 0; i < len; i++) {
			data[i] = pack[i+6];
		}
		return data;
	}
	
	public boolean isACK(byte[] packet) {
		String pack = toBinaryString(packet);
		if(pack.startsWith("001") == true) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Returns a binary representation of the frame.
	 * 
	 * @returns binaryString
	 */
	public String toBinaryString(byte[] frame){
		String binaryString = ""; // Initialize a new empty String.
		for(int i=0; i<=frame.length-1; i++){
			binaryString += ("0000000"+Integer.toBinaryString(0xFF & frame[i])).replaceAll(".*(.{8})$", "$1");
		}
		//System.out.println("The binary representation of this frame is: " + binaryString);
		return binaryString;
	}
	
	/**
	 * Returns a String representation of the packet.
	 * 
	 * @Override
	 */
	public String toString(){
		byte[] theData = getData(frame);
		String theString = new String(theData);
		System.out.println("The data in this packet is: " + theString);
		return theString;
	}
}
