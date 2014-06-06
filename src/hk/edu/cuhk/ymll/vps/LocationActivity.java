package hk.edu.cuhk.ymll.vps;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LocationActivity extends Activity {
	
	private String currentLocationString = "Loading";
	String destinationString = "Not Assigned";
	
	private TextView txtLocation;
	
	private TextToSpeech tts;
	
	private void updateLocationString(){
		String updatedLocationString = LocationActivity.this.getResources().getString(R.string.location_string, currentLocationString, destinationString);
		txtLocation.setText(updatedLocationString);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(R.layout.activity_location);
		
		tts = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
			@Override
			public void onInit(int status) {
				if(status == TextToSpeech.SUCCESS){
					init();
				}
			}
		});
	}
	
	private void init(){
		txtLocation = (TextView)super.findViewById(R.id.txtLocation);
		final EditText inputDes = (EditText)super.findViewById(R.id.inputDes);
		Button btnGo = (Button)super.findViewById(R.id.btnGo);
		
		txtLocation.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				tts.speak(s.toString(), TextToSpeech.QUEUE_FLUSH, null);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}			
		});
		
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
