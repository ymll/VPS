package hk.edu.cuhk.ymll.vps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		final Button btnLoc = (Button)super.findViewById(R.id.btnLoc);
		btnLoc.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, LocationActivity.class);
				MainActivity.this.startActivity(intent);
			}
		});
		
		Intent bluetoothIntent = new Intent();
		bluetoothIntent.setClass(this, RfidSensorActivity.class);
		this.startActivity(bluetoothIntent);
	}
}
