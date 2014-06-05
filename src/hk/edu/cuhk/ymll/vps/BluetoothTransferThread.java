package hk.edu.cuhk.ymll.vps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;


public class BluetoothTransferThread extends Thread {
	
	private final RfidCommand rfidCommand;
	private boolean sensorConnected;
	
	private final Handler indicate;
	private InputStream sensorInputStream;
	private OutputStream sensorOutputStream;
	private byte[] buf;
	private int numOfByte;
	
	private static int BUFFER_SIZE = 4096;
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public BluetoothTransferThread(BluetoothSocket sensorSocket) throws IOException {
		this.sensorInputStream = sensorSocket.getInputStream();
		this.sensorOutputStream = sensorSocket.getOutputStream();
		
		rfidCommand = new RfidCommand();
		sensorConnected = true;
		indicate = new Handler();
		buf = new byte[BUFFER_SIZE];
	}
	
	private String bytesToHex(byte[] bytes, int length) {
	    char[] hexChars = new char[length * 2];
	    for ( int j = 0; j < length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	private void recv() throws IOException, InterruptedException{
		if(sensorInputStream.available() > 0){
			int readLen = sensorInputStream.read(buf, numOfByte, buf.length-numOfByte);
			
			if(readLen > 0){
				numOfByte += readLen;
				final String hexString = bytesToHex(buf, numOfByte);
				System.out.println("Read ("+readLen+"/"+numOfByte+") - "+(rfidCommand.isCompleteCommand(buf, numOfByte)?"Complete":"InCompleted")+": "+hexString);
			}
			
			if(rfidCommand.isCompleteCommand(buf, numOfByte)){
				final String hexString = bytesToHex(buf, numOfByte);
				indicate.post(new Runnable(){
					@Override
					public void run() {
						System.out.println("Recv: "+hexString);
					}		
				});
				numOfByte = 0;
			}
		}else{
			Thread.sleep(100);
		}
	}
	
	private void send() throws IOException{
		byte[] sendBuf = rfidCommand.getReadDataCommand(true, false, 1, 1, "FFFFFFFFFFFF");
		sensorOutputStream.write(sendBuf);
		sensorOutputStream.flush();
	}

	@Override
	public void run(){
		try{
			while(sensorConnected){
				try{
					Thread.sleep(10);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				
				try {
					recv();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				send();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSensorConnected(boolean sensorConnected) {
		this.sensorConnected = sensorConnected;
	}
}
