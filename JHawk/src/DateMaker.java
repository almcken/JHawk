import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Class helps make dates into nice strings for folder and files naming purposes. 
 * @author Alex
 *
 */
public final class DateMaker {
	
	private final static Map<Integer, String> DAY_OF_WEEK = new HashMap<Integer, String>();
	
	private DateMaker(){
	}
	
	public static String shortName(String name){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		StringBuilder sb = new StringBuilder();
		sb.append(name + " ");
		sb.append(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)) + "_");
		sb.append(String.format("%02d", cal.get(Calendar.MINUTE)) + "_");
		sb.append(String.format("%02d", cal.get(Calendar.SECOND)));
		return sb.toString();
	}
	
	public static String HourlyFolderMaker(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		return cal.get(Calendar.HOUR_OF_DAY) + "00";
	}
	
	public static String NextHourlyFolderMaker(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.HOUR, 1);
		return cal.get(Calendar.HOUR_OF_DAY) + "00";
	}
	
	public static String NextHourlyFolderMakerNew(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.HOUR, 1);
		return getFormattedString(cal);
	}
	
	
	public static String HourlyFolderMakerNew(){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		return getFormattedString(cal);
	}
	
	private static String getFormattedString(Calendar cal) {
		return DAY_OF_WEEK.get(cal.get(Calendar.DAY_OF_WEEK)) + "\\" +cal.get(Calendar.HOUR) + "00" + AmPm(cal); 
	}
	
	private static String AmPm(Calendar cal) {
		int data = cal.get(Calendar.AM_PM);
		if (data == 0) {
			return "AM";
		}else {
			return "PM";
		}
	}
	
	public static void main(String[] args) {
		String name = DateMaker.NextHourlyFolderMakerNew();
		File file = new File("C:\\temp\\" + name);
		file.mkdirs();
	}
	
	static {
		Map<Integer, String> dayOfWeek = new HashMap<Integer, String>();
		dayOfWeek.put(1, "SUNDAY");
		dayOfWeek.put(2, "MONDAY");
		dayOfWeek.put(3, "TUESDAY");
		dayOfWeek.put(4, "WEDNESDAY");
		dayOfWeek.put(5, "THURSDAY");
		dayOfWeek.put(6, "FRIDAY");
		dayOfWeek.put(7, "SATURDAY");
		DAY_OF_WEEK.putAll(dayOfWeek);
	}
}
