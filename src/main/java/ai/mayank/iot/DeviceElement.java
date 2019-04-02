package ai.mayank.iot;

public class DeviceElement {

	private String id;
	
	public String getId() {
		return id;
	}
	private boolean state;
	private String port;
	private String deviceId;
	
	public DeviceElement() {
		
	}

	public boolean getState() { return state; }
	
	public String getDeviceId() {
		return this.deviceId;
	}
	public String getPort() {
		return this.port;
	}
}
