package hk.edu.cuhk.ymll.vps;

import java.nio.ByteBuffer;


public class RfidCommand {
    
    public byte[] getBCC(String hexString){
    	byte[] a = new byte[2];
    	char[] data = hexString.toCharArray();
    	
    	for(int i=0; i<data.length; i++)
    		a[i%2] ^= Byte.parseByte(data[i]+"", 16);
    	
    	return a;
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
}
