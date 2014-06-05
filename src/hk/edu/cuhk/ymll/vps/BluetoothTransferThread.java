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

	public BluetoothTransferThread(BluetoothSocket sensorSocket) throws IOException {
		this.sensorInputStream = sensorSocket.getInputStream();
		this.sensorOutputStream = sensorSocket.getOutputStream();
		
		rfidCommand = new RfidCommand();
		sensorConnected = true;
		indicate = new Handler();
		buf = new byte[BUFFER_SIZE];
	}

	private void recv() throws IOException, InterruptedException{
		if(sensorInputStream.available() > 0){
			int readLen = sensorInputStream.read(buf, numOfByte, buf.length-numOfByte);
			if(readLen > 0){
				numOfByte += readLen;
			}
		}
		
		int completeCommandLength = rfidCommand.getCompleteCommandLength(buf, numOfByte);
		if(completeCommandLength > 0){
			final RfidCommand.Response response = new RfidCommand.Response(buf, completeCommandLength);
			indicate.post(new Runnable(){
				@Override
				public void run() {
					if(response.getStatus()==0)
						System.out.printf("Status: %02X, ID: %s, Data: %s\n", response.getStatus(), response.getDataHexString().substring(0, 8), response.getDataHexString().substring(8));
				}
			});
			System.arraycopy(buf, completeCommandLength, buf, 0, numOfByte-completeCommandLength);
			numOfByte -= completeCommandLength;
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
