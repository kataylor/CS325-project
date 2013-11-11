package wifi;

public class FrameMaker {
	
	private byte[] packetData;
	
	public FrameMaker(byte[] data) {
		packetData = data;
	}
	
	public FrameMaker() {
		
	}
	
	public byte[] makeDataFrame(short dest, short src, int crc) {
		byte[] cont = setControl("000", "0", 0);
		byte[] frame = new byte[10 + packetData.length];
		frame[0] = cont[0];
		frame[1] = cont[1];
		frame[2] = (byte)(dest & 0xff);
		frame[3] = (byte)((dest >> 8) & 0xff);
		frame[4] = (byte)(src & 0xff);
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
	
	public static void main(String[] args) {
		String s = "hello 24601";
		byte[] jvj = s.getBytes();
		FrameMaker make = new FrameMaker(jvj);
		short dest = 1001;
		short src = 383;
		int crc = 0;
		byte[] packet = make.makeDataFrame(dest, src, crc);
		short value1 = packet[3];
		value1 = (short) ((value1 << 8) | packet[2]);
		short value2 = packet[5];
		value2 = (short) ((value2 << 8) | packet[4]);
		System.out.println("this is the dest: " + value1);
		System.out.println("this is the src: " + value2);
		byte[] data = getData(packet);
		String s2 = new String(data);
		System.out.println(s2);
	}
	
	public short getDest(byte[] pack) {
		short value1 = pack[2];
		value1 = (short) ((pack[3] << 8) | pack[2]);
		return value1;
	}
	
	public short getSrc(byte[] pack) {
		short value2 = pack[4];
		value2 = (short) ((pack[5] << 8) | pack[4]);
		return value2;
	}
	
	public static byte[] getData(byte[] pack) {
		int len = pack.length-10;
		byte[] data = new byte[2038];
		for(int i = 0; i < len; i++) {
			data[i] = pack[i+6];
		}
		return data;
	}
}