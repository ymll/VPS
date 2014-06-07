package hk.edu.cuhk.ymll.vps;

import hk.edu.cuhk.ymll.vps.TagDatabase.Location;
import hk.edu.cuhk.ymll.vps.TagDatabase.Navigation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import android.bluetooth.BluetoothSocket;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;


public class BluetoothTransferThread extends Thread implements SensorEventListener{
	
	private final RfidCommand rfidCommand;
	private boolean sensorConnected;
	
	private String[] navigationString;
	private Location destination;
	private TextToSpeech tts;
	private static TagDatabase tagDatabase;
	private Location previousTag = Location.NONE;
	private Location currentTag = Location.NONE;
	
	private TextView txtMessage;
	private TextView txtAngle;
	
	private final Handler indicate;
	private InputStream sensorInputStream;
	private OutputStream sensorOutputStream;
	private byte[] buf;
	private int numOfByte;
	
	public static float[] mAccelerometer = null;
	public static float[] mGeomagnetic = null;
	private double azimuth;
	
	private RfidSensorActivity activity;
	
	private static int BUFFER_SIZE = 4096;

	public BluetoothTransferThread(BluetoothSocket sensorSocket, String[] navigationString, Location destination, TextToSpeech tts, TextView txtMessage, TextView txtAngle, RfidSensorActivity activity) throws IOException {
		this.sensorInputStream = sensorSocket.getInputStream();
		this.sensorOutputStream = sensorSocket.getOutputStream();
		
		this.navigationString = navigationString;
		this.destination = destination;
		this.tts = tts;
		this.txtMessage = txtMessage;
		this.txtAngle = txtAngle;
		this.activity = activity;
		tagDatabase = new TagDatabase(navigationString);
		
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
					if(response.getStatus()==0){
						String tagRaw = response.getDataHexString();
						String tagId = tagRaw.substring(0, 8);
						String tagData = tagRaw.substring(8);
						Location loc = tagDatabase.getLocationByTag(tagId);
						System.out.printf("Status: %02X, ID: %s, Data: %s, Location: %s\n", response.getStatus(), tagId, tagData, loc.name());
						
						notifyTagUpdate(loc);
						
						if("44E13031".equals(tagId)){
							Random r = new Random();
							Location newLoc = null;
							do{
								newLoc = Location.values()[r.nextInt(4)];
							}while(newLoc == tagDatabase.tagToLocation.get("44E13031"));
							tagDatabase.tagToLocation.put("44E13031", newLoc);
							Location l = tagDatabase.tagToLocation.get("44E13031");
							System.out.println("New: "+l);
						}
					}
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
					Thread.sleep(50);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				
				try {
					recv();
					while(tts.isSpeaking()){
						Thread.sleep(50);
					}
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	        mAccelerometer = event.values;
	    }

	    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
	        mGeomagnetic = event.values;
	    }

	    if (mAccelerometer != null && mGeomagnetic != null) {
	        float R[] = new float[9];
	        float I[] = new float[9];
	        boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);

	        if (success) {
	            float orientation[] = new float[3];
	            SensorManager.getOrientation(R, orientation);
	            // at this point, orientation contains the azimuth(direction), pitch and roll values.
	              azimuth = 180 * orientation[0] / Math.PI;
	              double pitch = 180 * orientation[1] / Math.PI;
	              double roll = 180 * orientation[2] / Math.PI;
	              
	              final String text = String.format("azimuth=%.4f\npitch=%.4f\nroll=%.4f", azimuth, pitch, roll);
	              //if(waitingCompass){
	            	//  int direction = ((int) ((azimuth+45+360)/90))%4;
	            	  
	            	  /*switch(direction){
	            	  case 0:
	            		  txtMessage.setText("North " + azimuth);
	            		  break;
	            	  case 1:
	            		  txtMessage.setText("East " + azimuth);
	            		  break;
	            	  case 2:
	            		  txtMessage.setText("South " + azimuth);
	            		  break;
	            	  case 3:
	            		  txtMessage.setText("West " + azimuth);
	            		  break;
	            	  }*/
	            	  
	            	  //Navigation nav = tagDatabase.getNextAction(currentTag, currentTag, destination);
	            	  /*switch(currentTag){
	            	  case Left:
	            		  direction = ((int) ((azimuth+45+90+360)/90))%4;
	            		  break;
	            	  case Down:
	            		  direction = ((int) ((azimuth+45+360)/90))%4;
	            		  break;
	            	  case Right:
	            		  direction = ((int) ((azimuth+45-90+360)/90))%4;
	            		  break;
	            	  case Center:
	            		  direction = ((int) ((azimuth+45+360)/90))%4;
	            		  break;
	            	  case NONE:
	            	  }
	            	  
	            	  switch(direction){
	            	  case 0:
	            		  txtMessage.setText("GO " + azimuth);
	            		  break;
	            	  case 1:
	            		  txtMessage.setText("RIGHT " + azimuth);
	            		  break;
	            	  case 2:
	            		  txtMessage.setText("BACK " + azimuth);
	            		  break;
	            	  case 3:
	            		  txtMessage.setText("LEFT " + azimuth);
	            		  break;
	            	  }*/
	             // }
	              indicate.post(new Runnable(){
					@Override
					public void run() {
						txtAngle.setText(text);						
					}
	              });
	              
	        }
	    }
	}
	
	private void notifyTagUpdate(Location loc){
		if(loc != currentTag){
			previousTag = currentTag; 
			currentTag = loc;
			Navigation nav = tagDatabase.getNextAction(previousTag, currentTag, destination);
			
			if(nav == Navigation.ERROR){
				nav = Navigation.NEW;
			}
			
			String speakText = navigationString[nav.ordinal()];
			
			if(nav == Navigation.NEW){
				int direction = -1;
				
				switch (currentTag) {
				case Left:
					direction = ((int) ((azimuth + 45 - 90 + 720) / 90)) % 4;
					break;
				case Down:
					direction = ((int) ((azimuth + 45 + 720) / 90)) % 4;
					break;
				case Right:
					direction = ((int) ((azimuth + 45 + 90 + 720) / 90)) % 4;
					break;
				case Center:
					direction = ((int) ((azimuth + 45 + 720) / 90)) % 4;
					break;
				case NONE:
				}
				
				int mapping = -1;
				switch(direction){
				case 0:
					mapping = 3;
					break;
				case 1:
					mapping = 0;
					break;
				case 2:
					mapping = 2;
					break;
				case 3:
					mapping = 1;
					break;
				}
				
				System.out.printf("%s, %f, %d, %d\n", currentTag.toString(), azimuth, direction, mapping);
				speakText = navigationString[mapping];
			}
			
			String message = String.format("Current: %s, Previous: %s, Des: %s, Next: %s, Angle: %.2f, Speak: %s\n", currentTag.toString(), previousTag.toString(), destination.toString(), nav.toString(), azimuth, speakText);
			System.out.printf(message);
			activity.setLastLocation(loc);
			txtMessage.setText(message.replace(", ", "\n"));
			tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null);
		}
	}
}
