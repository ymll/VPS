package hk.edu.cuhk.ymll.vps;


public interface IRfidSensor {
	public void setupSensor();
	public void enableBluetooth(boolean getUserConsent);
	public void onBluetoothEnabled(boolean isSuccess);
	public void discoverSensor();
	public void onSensorDiscovered();
	public void pairSensor();
	public void onSensorPaired();
	public void connectSensor();
	public void onSensorConnected();
	public void unpairSensor();
	public void disableBluetooth();
}
