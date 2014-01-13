package fi.tol.android.RTDAReceiver;



/**
 * @author Olli Anttila
 *
 */
public class BTEvent {
	public static enum Event {ENTER, LEAVE};
	private Event event; 
	private String time;
	private String btBeaconAddress;
	
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public BTEvent(Event e){
		this.event = e;
	}
	public void setTime(String time){
		this.time = time;
	}
	public String getTime() {
		return time;
	}
	public String getBtBeaconAddress() {
		return btBeaconAddress;
	}
	public void setBtBeaconAddress(String btBeaconAddress) {
		this.btBeaconAddress = btBeaconAddress;
	}
	
}
