package wifi;

public class FrameMaker {
	
	private byte[] packetData;
	byte[] frame;
	
	public FrameMaker(byte[] data) {
		packetData = data;
	}
	
	/*
	 */
	public FrameMaker() {
		//do nothing
	}
	
	public byte[] makeDataFrame(short dest, short src, int crc) {
		byte[] cont = setControl("000", "0", 0);
		byte[] frame = new byte[10 + packetData.length];
		frame[0] = cont[0];
		frame[1] = cont[1];
		frame[2] = (byte)(dest);
		frame[3] = (byte)((dest >> 8) & 0xff);
		frame[4] = (byte)(src);
		frame[5] = (byte)((src >> 8) & 0xff);
		for(int i = 0; i < packetData.length; i++) {
			frame[i+6] = packetData[i];
		}
		frame[packetData.length+6] = (byte)255;
		frame[packetData.length+7] = (byte)255;
		frame[packetData.length+8] = (byte)255;
		frame[packetData.length+9] = (byte)255;
		return frame;
	}
	
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
	
	public short getDest(byte[] pack) {
		short newshort = (short) ((pack[3] << 8) + (pack[2]&0xFF));
		return newshort;
	}
	
	public short getSrc(byte[] pack) {
		short newshort = (short) ((pack[5] << 8) + (pack[4]&0xFF));
		return newshort;
	}
	
	public byte[] getData(byte[] pack) {
		int len = pack.length-10;
		byte[] data = new byte[pack.length-10];
		for(int i = 0; i < len; i++) {
			data[i] = pack[i+6];
		}
		return data;
	}
	
	/*
	 * Returns a binary representation of the frame.
	 * 
	 * @returns binaryString
	 */
	public String toBinaryString(){
		String binaryString = ""; // Initialize a new empty String.
		for(int i=0; i<=frame.length-1; i++){
			binaryString += ("0000000"+Integer.toBinaryString(0xFF & frame[i])).replaceAll(".*(.{8})$", "$1") + " ";
		}
		System.out.println("The binary representation of this frame is: " + binaryString);
		return binaryString;
	}
	
	/*
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
