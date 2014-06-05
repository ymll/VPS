package hk.edu.cuhk.ymll.vps;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class RfidCommand {
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public int getCompleteCommandLength(byte[] command, int commandLen){
		if(commandLen < 6)
			return -1;
		
		int dataLen = command[2];
		if(commandLen < (dataLen+5))
			return -1;
		
		if(command[0]!=2 || command[dataLen+4]!=3)
			return -1;
		
		return dataLen+5; 
	}
	
	public static String bytesToHex(byte[] bytes, int start, int length) {
	    char[] hexChars = new char[length * 2];
	    for ( int j = 0; j < length; j++ ) {
	        int v = bytes[j+start] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
    
    private byte[] commandToByteArray(String command){
    	char[] commandCharArray = command.toCharArray();
		ByteBuffer byteBuffer = ByteBuffer.allocate(commandCharArray.length/2+4);
		byte bcc = 0;
		
		byteBuffer.put((byte)2);
    	for(int i=0; i<commandCharArray.length; i+=2){
    		byte b1 = (byte)Character.digit(commandCharArray[i], 16);
    		byte b2 = (byte)Character.digit(commandCharArray[i+1], 16);
    		byte b = (byte) ((b1 << 4) | b2);

    		byteBuffer.put(b);
    		bcc ^= b;
    	}    	
    	byteBuffer.put(bcc);
    	byteBuffer.put((byte)3);
		
		return byteBuffer.array();
    }
	
	public byte[] getReadDataCommand(boolean isTypeA, boolean isReadMultipleCards, int blockSize, int blockIndex, String password){
		
		assert(blockSize >= 1 && blockSize <= 4);
		assert(blockIndex >= 0 && blockIndex <= 64-blockSize);
		
		String command = String.format("000A20%01X%01X%02X%02X%12s", isTypeA ? 0 : 1,
				isReadMultipleCards ? 1 : 0, blockSize, blockIndex, password);
		
		return commandToByteArray(command);
	}
	
	public byte[] getBuzzerCommand(int period, int loopCount){
		assert(period >= 0 && period < 256);
		assert(loopCount >= 0 && loopCount < 256);
		String command = String.format("000389%2X%2X", period, loopCount);
		
		return commandToByteArray(command);
	}
	
	public static class Response{
		int status;
		byte[] data;
		String dataHexString;
		
		public Response(byte[] responseRawData, int length){
			status = responseRawData[3] & 0xFF;
			int dataLen = (responseRawData[2] & 0xFF) -1;
			
			assert(dataLen >= 0);
			data = Arrays.copyOfRange(responseRawData, 4, 4+dataLen);
		}

		public int getStatus() {
			return status;
		}

		public byte[] getData() {
			return data;
		}
		
		public String getDataHexString() {
			if(dataHexString == null)
				dataHexString = RfidCommand.bytesToHex(data, 0, data.length);
			return dataHexString;
		}
	}
}
