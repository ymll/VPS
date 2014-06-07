package hk.edu.cuhk.ymll.vps;

import hk.edu.cuhk.ymll.vps.TagDatabase.Location;
import hk.edu.cuhk.ymll.vps.TagDatabase.Navigation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

public class RfidSensorActivity extends Activity implements IRfidSensor {

	private BluetoothAdapter bluetoothAdapter;
	private boolean isBluetoothPreviouslyDisabled;
	private SensorBroadcastReceiver sensorBroadcastReceiver;
	private BluetoothDevice sensorDevice;
	private BluetoothSocket sensorSocket;	
	private Location destination = Location.NONE;
	
	private BluetoothTransferThread bluetoothTransferThread;
	private TextView txtMessage;
	private TextView txtAngle;
	private TextToSpeech tts;
	
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	
	private String[] navigationString;
	private String sensorAddress;
	private String sensor_pin;
	private static final int REQUEST_BLUETOOTH_ENABLE = 43839;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		System.out.println("onCreate");
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter == null){
			this.finish();
			return;
		}
		
		super.setContentView(R.layout.activity_nav);
		txtMessage = (TextView)super.findViewById(R.id.txtMessage);
		txtAngle = (TextView)super.findViewById(R.id.txtAngle);
		
		navigationString = this.getResources().getStringArray(R.array.navigation_string);
		assert(navigationString.length == Navigation.values().length);
		
		tts = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
			@Override
			public void onInit(int status) {
				if(status == TextToSpeech.SUCCESS){
					setupSensor();
				}
			}
		});
		
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
   		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
   		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
   		destination = Location.values()[this.getIntent().getIntExtra("destination", Location.NONE.ordinal())];
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		if(bluetoothTransferThread != null){
		    mSensorManager.registerListener(bluetoothTransferThread, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		    mSensorManager.registerListener(bluetoothTransferThread, magnetometer, SensorManager.SENSOR_DELAY_GAME);			
		}		
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    
		if(bluetoothTransferThread != null){
		    mSensorManager.unregisterListener(bluetoothTransferThread, accelerometer);
		    mSensorManager.unregisterListener(bluetoothTransferThread, magnetometer);			
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_BLUETOOTH_ENABLE){
			if(resultCode == Activity.RESULT_OK){
				onBluetoothEnabled(true);
			}
		}
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		
		if(sensorSocket != null){
			try {
				sensorSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(sensorBroadcastReceiver != null){
			this.unregisterReceiver(sensorBroadcastReceiver);
		}
		
		if(bluetoothAdapter != null){
			bluetoothAdapter.cancelDiscovery();
			
			//unpairSensor();
			disableBluetooth();
		}
		
		if(tts != null)
			tts.shutdown();
	}
	
	
	@Override
	public void setupSensor() {
		System.out.println("setupSensor");
		
		sensorAddress = this.getResources().getString(R.string.sensor_address);
		sensor_pin = this.getResources().getString(R.string.sensor_pin);
	    sensorBroadcastReceiver = new SensorBroadcastReceiver();
	    
	    IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
	    intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	    intentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
	    intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
	    intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	    intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
	    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	    this.registerReceiver(sensorBroadcastReceiver, intentFilter);
		
		if(!bluetoothAdapter.isEnabled()){
			isBluetoothPreviouslyDisabled = true;
			enableBluetooth(false);
		}else{
			onBluetoothEnabled(true);
		}
	}
	
	@Override
	// Step 0: Enable Bluetooth
	public void enableBluetooth(boolean getUserConsent) {
		System.out.println("enableBluetooth");
		if(getUserConsent){
			// Turn on Bluetooth with prompt
			Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.startActivityForResult(enableBluetoothIntent, REQUEST_BLUETOOTH_ENABLE);
		}else{
			// Turn on Bluetooth without prompt
			bluetoothAdapter.enable();	
		}
	}

	@Override
	public void onBluetoothEnabled(boolean isSuccess) {
		System.out.println("onBluetoothEnabled");
		tts.speak("Bluetooth is enabled", TextToSpeech.QUEUE_FLUSH, null);
		
		if(isSuccess){
			discoverSensor();
		}
	}

	@Override
	// Step 1: Search the sensor
	public void discoverSensor() {
		System.out.println("discoverSensor");
		
		if(bluetoothAdapter.isDiscovering()){
			bluetoothAdapter.cancelDiscovery();
		}
		
		for(BluetoothDevice device : bluetoothAdapter.getBondedDevices()){
			if(device.getAddress().equalsIgnoreCase(sensorAddress)){
				sensorDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
				connectSensor();
				return;
			}
		}
	    
	    bluetoothAdapter.startDiscovery();
	}

	@Override
	public void onSensorDiscovered() {
		System.out.println("onSensorDiscovered");
		tts.speak("Sensor is found", TextToSpeech.QUEUE_ADD, null);
		
		pairSensor();
	}

	@Override
	// Step 2: Pair the sensor
	public void pairSensor() {
		System.out.println("pairSensor");
		
		if(sensorDevice.getBondState() != BluetoothDevice.BOND_BONDED){
			try {
				sensorDevice.getClass().getMethod("createBond").invoke(sensorDevice);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onSensorPaired() {
		System.out.println("onSensorPaired");
		tts.speak("Sensor is paired", TextToSpeech.QUEUE_ADD, null);
		
		if(bluetoothAdapter.isDiscovering()){
			bluetoothAdapter.cancelDiscovery();			
		}else{
			connectSensor();
		}
	}

	@Override
	// Step 3: Connect the sensor
	public void connectSensor() {
		System.out.println("connectSensor");
		
		try {
			Method m = sensorDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			sensorSocket = (BluetoothSocket) m.invoke(sensorDevice, 1);
			bluetoothAdapter.cancelDiscovery();
			sensorSocket.connect();
			System.out.println("connectSensor: Connected");
			onSensorConnected();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSensorConnected() {
		System.out.println("onSensorConnected");
		
		tts.speak(this.getResources().getString(R.string.message_sensor_connected), TextToSpeech.QUEUE_FLUSH, null);
		if(destination != Location.NONE){
			startTransfer();
		}
	}

	@Override
	public void unpairSensor() {
		if(sensorDevice != null){
			if(sensorDevice.getBondState() == BluetoothDevice.BOND_BONDED){
				try {
					sensorDevice.getClass().getMethod("removeBond").invoke(sensorDevice);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void disableBluetooth() {
		// Close Bluetooth if it is disabled before
		if(isBluetoothPreviouslyDisabled){
			bluetoothAdapter.disable();
		}
	}
	
	private class SensorBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			System.out.printf("SensorBroadcastReceiver.onReceive.Action: %s\n", action);
			
			// Step 0: Enable Bluetooth Notification
			if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
				int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);				
				if(btState == BluetoothAdapter.STATE_ON)
					onBluetoothEnabled(true);
			}
			
			else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
				int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
				int previousMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.ERROR);
				System.out.printf("%s -> %s\n", previousMode, scanMode);
			}
			
			else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				
			}
			
			// Step 1: Search the sensor (Found)
			else if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				System.out.println(String.format("%s - %s - %s - %s", device.getName(), device.getAddress(), device.getBondState(), device.getBluetoothClass()));
				System.out.println(String.format("%s <-> %s", sensorAddress, device.getAddress()));
				if(sensorAddress.equalsIgnoreCase(device.getAddress())){
					RfidSensorActivity.this.sensorDevice = device;
					onSensorDiscovered();
				}
			}
			
			// Step 2: Pair the sensor (Handle Request)
			else if("android.bluetooth.device.action.PAIRING_REQUEST".equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				try {
					device.getClass().getMethod("setPin", byte[].class).invoke(device, sensor_pin.getBytes());
					device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
					device.getClass().getMethod("cancelPairingUserInput").invoke(device);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
			
			// Step 2: Pair the sensor (Paired)
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
				int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
				System.out.printf("Bound State: %d\n", bondState);
				if(bondState == BluetoothDevice.BOND_BONDED){
					onSensorPaired();
				}
			}
			
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				if(sensorDevice != null && sensorDevice.getBondState() == BluetoothDevice.BOND_BONDED){
					connectSensor();
				}
			}
		}
	}
	
	public void setDestination(Location destination){
		if(this.destination != destination && sensorSocket != null){
			this.destination = destination;
		}else{
			this.destination = destination;	
		}
		
		if(destination != Location.NONE){
			startTransfer();
		}
	}
	
	private void startTransfer(){
		if(bluetoothTransferThread != null && bluetoothTransferThread.isAlive()){
		    mSensorManager.unregisterListener(bluetoothTransferThread, accelerometer);
		    mSensorManager.unregisterListener(bluetoothTransferThread, magnetometer);			
			
			bluetoothTransferThread.setSensorConnected(false);
			bluetoothTransferThread.interrupt();
			bluetoothTransferThread = null;
		}
		
		try {
			bluetoothTransferThread = new BluetoothTransferThread(sensorSocket, navigationString, destination, tts, txtMessage, txtAngle);
			
		    mSensorManager.registerListener(bluetoothTransferThread, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		    mSensorManager.registerListener(bluetoothTransferThread, magnetometer, SensorManager.SENSOR_DELAY_GAME);			
			
			bluetoothTransferThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
