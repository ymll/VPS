package hk.edu.cuhk.ymll.vps;

import hk.edu.cuhk.ymll.vps.TagDatabase.Location;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LocationActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_location);
		
		init();
	}
	
	private void init(){
		final EditText inputDes = (EditText)super.findViewById(R.id.inputDes);
		Button btnGo = (Button)super.findViewById(R.id.btnGo);
				
		btnGo.setOnClickListener(new OnClickListener(){
			private String[] locations = LocationActivity.this.getResources().getStringArray(R.array.locations);
			
			@Override
			public void onClick(View view) {
				String inputtedString = inputDes.getText().toString();
				
				assert(locations.length-1 == Location.values().length);
				for(int i=0; i<locations.length; i++){
					if(locations[i].equalsIgnoreCase(inputtedString)){
						Intent bluetoothIntent = new Intent();
						bluetoothIntent.setClass(LocationActivity.this, RfidSensorActivity.class);
						bluetoothIntent.putExtra("destination", i);
						LocationActivity.this.startActivity(bluetoothIntent);
						break;
					}
				}
			}
		});
	}
}
