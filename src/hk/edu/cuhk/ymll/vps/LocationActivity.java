package hk.edu.cuhk.ymll.vps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LocationActivity extends Activity {
	
	private String currentLocationString = "Loading...";
	String destinationString = "Not Assigned";
	
	private TextView txtLocation;
	
	private void updateLocationString(){
		String updatedLocationString = LocationActivity.this.getResources().getString(R.string.location_string, currentLocationString, destinationString);
		txtLocation.setText(updatedLocationString);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_location);
		
		txtLocation = (TextView)super.findViewById(R.id.txtLocation);
		final EditText inputDes = (EditText)super.findViewById(R.id.inputDes);
		Button btnGo = (Button)super.findViewById(R.id.btnGo);
		
		updateLocationString();
		
		btnGo.setOnClickListener(new OnClickListener(){
			private String[] locations = LocationActivity.this.getResources().getStringArray(R.array.locations);
			
			@Override
			public void onClick(View view) {
				String inputtedString = inputDes.getText().toString();
				for(String loc: locations){
					if(loc.equalsIgnoreCase(inputtedString)){
						destinationString = loc;
						updateLocationString();
						break;
					}
				}
			}
		});
	}
}
