package hk.edu.cuhk.ymll.vps;

import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class RfidSensorActivity extends Activity implements IRfidSensor {

	private BluetoothAdapter bluetoothAdapter;
	private boolean isBluetoothPreviouslyDisabled;
	private SensorBroadcastReceiver sensorBroadcastReceiver;
	private BluetoothDevice sensorDevice;

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
		
		setupSensor();
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
		
		if(bluetoothAdapter != null){
			bluetoothAdapter.cancelDiscovery();
			if(sensorBroadcastReceiver != null)
				this.unregisterReceiver(sensorBroadcastReceiver);
			
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
			
			// Close Bluetooth if it is disabled before
			if(isBluetoothPreviouslyDisabled){
				bluetoothAdapter.disable();
			}
		}
	}
	
	
	@Override
	public void setupSensor() {
		System.out.println("setupSensor");
		if(!bluetoothAdapter.isEnabled()){
			isBluetoothPreviouslyDisabled = true;
			enableBluetooth(false);
		}else{
			onBluetoothEnabled(true);
		}
	}
	
	@Override
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
		if(isSuccess){
			discoverSensor();
		}
	}

	@Override
	public void discoverSensor() {
		System.out.println("discoverSensor");
		
		if(bluetoothAdapter.isDiscovering()){
			bluetoothAdapter.cancelDiscovery();
		}
	    
	    bluetoothAdapter.startDiscovery();
	}

	@Override
	public void onSensorDiscovered() {
		System.out.println("onSensorDiscovered");
		bluetoothAdapter.cancelDiscovery();
		pairSensor();
	}

	@Override
	public void pairSensor() {
		System.out.println("pairSensor");
		
		//System.out.println(Arrays.toString(device.getClass().getMethods()));
		if(sensorDevice.getBondState() == BluetoothDevice.BOND_NONE){
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
		}else if(sensorDevice.getBondState() == BluetoothDevice.BOND_BONDED){
			onSensorPaired();
		}
	}

	@Override
	public void onSensorPaired() {
		connectSensor();
	}

	@Override
	public void connectSensor() {
		System.out.println("connectSensor");		
	}

	@Override
	public void onSensorConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unpairSensor() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableBluetooth() {
		// TODO Auto-generated method stub
		
	}
	
	private class SensorBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			System.out.printf("SensorBroadcastReceiver.onReceive.Action: %s\n", action);
			
			if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
				int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);				
				if(btState == BluetoothAdapter.STATE_ON)
					onBluetoothEnabled(true);
			}
			
			else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
				System.out.println(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -4));
			}
			
			else if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				System.out.println(String.format("%s - %s - %s - %s", device.getName(), device.getAddress(), device.getBondState(), device.getBluetoothClass()));
				System.out.println(String.format("%s <-> %s", sensorAddress, device.getAddress()));
				if(sensorAddress.equalsIgnoreCase(device.getAddress())){
					RfidSensorActivity.this.sensorDevice = device;
					onSensorDiscovered();
				}
			}
			
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
				int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
				System.out.printf("Bound State: %d\n", bondState);
				if(bondState == BluetoothDevice.BOND_BONDED){
					onSensorPaired();
				}
			}
			
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
		}
	}
}
