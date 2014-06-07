package hk.edu.cuhk.ymll.vps;

import hk.edu.cuhk.ymll.vps.TagDatabase.Location;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class InputFormHandler {
		
	public InputFormHandler(final RfidSensorActivity activity){
		
		final EditText inputDes = (EditText)activity.findViewById(R.id.inputDes);
		Button btnGo = (Button)activity.findViewById(R.id.btnGo);
		Button btnHelp = (Button)activity.findViewById(R.id.btnHelp);
		final String[] locations = activity.getResources().getStringArray(R.array.locations);
				
		btnGo.setOnClickListener(new OnClickListener(){
			
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

		btnHelp.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				final SmsManager smsManager = SmsManager.getDefault();
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
				final EditText input = new EditText(activity);
				
				alertDialog.setView(input);
				alertDialog.setTitle(R.string.dialog_sms);
				alertDialog.setPositiveButton("Send", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						String tel = input.getText().toString();
						if(PhoneNumberUtils.isGlobalPhoneNumber(tel)){
							Location lastLocation = activity.getLastLocation();
							if(lastLocation != Location.NONE){
								String text = locations[lastLocation.ordinal()];
								smsManager.sendTextMessage(tel, null, String.format("Help!\nMy current location is %s", text), null, null);
							}
						}
					}
				});
				alertDialog.setNegativeButton("Cancel", null);
				alertDialog.show();
			}
		});
	}
}
