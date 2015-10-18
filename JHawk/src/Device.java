import java.net.UnknownHostException;
/**
 * Represents a Device that connects to the network. Like a cell phone. 
 * @author Alex
 *
 */
public class Device {
	private final String m_ipAddress;
	private final String m_macAddress;
	private final String m_name;
	public Device(String ip, String mac, String name) throws UnknownHostException{
		m_ipAddress = ip;
		m_macAddress = mac;
		m_name = name;
	}

	public String getIpAddress(){
		return m_ipAddress;
	}
	public String getMac(){
		return m_macAddress;
	}
	public String getName(){
		return m_name;
	}
	
	public String toString(){
		return "IP=" + getIpAddress() + "\tMAC=" + getMac() + "\tNAME=" + getName();
	}
	
}