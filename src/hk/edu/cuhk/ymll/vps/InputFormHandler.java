package hk.edu.cuhk.ymll.vps;

import hk.edu.cuhk.ymll.vps.TagDatabase.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class InputFormHandler {
		
	public InputFormHandler(final RfidSensorActivity activity){
		
		final EditText inputDes = (EditText)activity.findViewById(R.id.inputDes);
		Button btnGo = (Button)activity.findViewById(R.id.btnGo);
				
		btnGo.setOnClickListener(new OnClickListener(){
			private String[] locations = activity.getResources().getStringArray(R.array.locations);
			
			@Override
			public void onClick(View view) {
				String inputtedString = inputDes.getText().toString();
				
				assert(locations.length-1 == Location.values().length);
				for(int i=0; i<locations.length; i++){
					if(locations[i].equalsIgnoreCase(inputtedString)){
						activity.setDestination(Location.values()[i]);
						break;
					}
				}
			}
		});

	}
}
