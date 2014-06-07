package hk.edu.cuhk.ymll.vps;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;


public class TagDatabase {
	
	String[] messages;
	
	Map<String, Location> tagToLocation;
	int repeat = 0;
	
	public TagDatabase(String[] messages){
		this.messages = messages;
		
		tagToLocation = new HashMap<String, Location>();
		tagToLocation.put("F3C701AB", Location.Down);
		tagToLocation.put("33510DAB", Location.Down);
		tagToLocation.put("23110EAB", Location.Down);
		tagToLocation.put("53210FAB", Location.Down);
		tagToLocation.put("33300DAB", Location.Down);
		tagToLocation.put("63BB0EAB", Location.Down);
		
		tagToLocation.put("A3020DAB", Location.Center);
		tagToLocation.put("837F0EAB", Location.Center);
		tagToLocation.put("F3E301AB", Location.Center);
		tagToLocation.put("43160DAB", Location.Center);
		tagToLocation.put("530D0FAB", Location.Center);
		tagToLocation.put("33DA0DAB", Location.Center);
		
		tagToLocation.put("53C00EAB", Location.Left);
		tagToLocation.put("83F60CAB", Location.Left);
		tagToLocation.put("53800DAB", Location.Left);
		tagToLocation.put("13D50DAB", Location.Left);
		tagToLocation.put("73A90DAB", Location.Left);
		tagToLocation.put("23CB0DAB", Location.Left);
		
		tagToLocation.put("337E0EAB", Location.Right);
		tagToLocation.put("F3240FAB", Location.Right);
		tagToLocation.put("F30202AB", Location.Right);
		tagToLocation.put("33440DAB", Location.Right);
		tagToLocation.put("33290DAB", Location.Right);
		tagToLocation.put("234C0EAB", Location.Right);
		
		tagToLocation.put("44E13031", Location.Down);
		
		assert(tagToLocation.size() == 24);
	}
	
	public Location getLocationByTag(String tagId){
		Location location = tagToLocation.get(tagId);
		if(location == null)
			location = Location.NONE;
		
		if("44E13031".equals(tagId)){
			Random r = new Random();
			Location newLoc = null;
			while(newLoc == tagToLocation.get("44E13031")){
				newLoc = Location.values()[r.nextInt(4)];
			}
			tagToLocation.put("44E13031", Location.values()[r.nextInt(4)]);
		}
		return location;
	}
	
	public Navigation getNextAction(Location previousTag, Location currentTag, Location destinationTag){
		
		if(currentTag == destinationTag)
			return Navigation.GOAL;
		
		if(previousTag == Location.NONE || currentTag == Location.NONE)
			return Navigation.NEW;
		
		if(previousTag == currentTag)
			return Navigation.UNKNOWN;
		
		if(previousTag == Location.Center){
			if(currentTag != Location.Center){
				return Navigation.BACKWARD;
			}
		}
		
		if(currentTag == Location.Center && previousTag != Location.NONE && destinationTag != Location.NONE){
			if(previousTag==Location.Down && destinationTag==Location.Left)
				return Navigation.TURN_LEFT;
			if(previousTag==Location.Right && destinationTag==Location.Down)
				return Navigation.TURN_LEFT;
			if(previousTag==Location.Down && destinationTag==Location.Right)
				return Navigation.TURN_RIGHT;
			if(previousTag==Location.Left && destinationTag==Location.Down)
				return Navigation.TURN_RIGHT;
			if(previousTag==Location.Left && destinationTag==Location.Right)
				return Navigation.GO_STRAIGHT;
			if(previousTag==Location.Right && destinationTag==Location.Left)
				return Navigation.GO_STRAIGHT;
		}
		
		if(previousTag == null){
			return Navigation.UNKNOWN;
		}
		
		return Navigation.ERROR;
	}

	public static enum Location{
		Left, Down, Right, Center, NONE;
	}
	
	public static enum Navigation{
		TURN_LEFT, TURN_RIGHT, BACKWARD, GO_STRAIGHT, GOAL, NEW, UNKNOWN, ERROR, NONE;
	}
	
	public static void main(String[] args){
		int numOfTag = Location.values().length;
		TagDatabase db = new TagDatabase(new String[]{
		        "請轉左",
		        "請轉右",
		        "請向後轉",
		        "請向前行",
		        "你已到達目的地",
	            "歡迎使用視障定位系統，請將手杖移至附近的點狀地磚。",
		        "系統正檢測你的方向，請稍候。",
		        "無效",
		        "無指示"
		});
		
		try {
			Scanner s = new Scanner(new FileInputStream("haha.txt"));
			int[] order = new int[]{Location.Down.ordinal(), Location.Center.ordinal(), Location.Left.ordinal(), Location.Right.ordinal(), Location.NONE.ordinal()};
			for(int i=0; i<numOfTag; i++){
				for(int j=0; j<numOfTag; j++){
					for(int k=0; k<numOfTag; k++){					
						Navigation nav = db.getNextAction(Location.values()[order[i]], Location.values()[order[j]], Location.values()[order[k]]);
						String actual = String.format("%s->[%s]->%s\t%s\n", Location.values()[order[i]], Location.values()[order[j]], Location.values()[order[k]], db.messages[nav.ordinal()]);
						s.hasNextLine();
						String expected = s.nextLine();
						
						if(!actual.equalsIgnoreCase(expected)){
							System.err.printf("== Expected=%s== Actual=%s\n\n", actual, expected);
						}
					}
				}
			}
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
