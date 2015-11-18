/*
 * This program is meant to be ran on any computer with a webcam. 
 * 
 * It will look for a single IP address on your network and ping for it. If if finds the ip address the camera system will be in the DISARMED state. 
 * If the IP is not the systems will be in the ARMED state.  
 * System is always recording. Alerts will be sent to your email address if system is ARMED and motion is detected. 
 * 
 * if you have any questions email me at jobs@almcken.com
 * 
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.media.MediaLocator;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * Runs the main j hawk program.
 * @author Alex McKenzie
 *
 */
public class JHawkShell {
	
	
	private final ScheduledExecutorService  EXECUTOR;
	private final ScheduledExecutorService  EXECUTORPOOL;
	
	private String dropboxPath;
	private String relativePath;
	private final static String camera1 = "camera1\\";
	private final static String PROPERTIES_PATH = "config.properties";
	
	private String imgPath5;
	private String moviesPath;
	private String tempDir;
	private String TEXT_NUMBER;
	private String WORK_NUMBER;
	private String ALARM_EMAIL;
	private String m_googleUserName;
	private String m_googlePassword;
	
	
	private final static int VIDEO_WIDTH = 640;
	private final int VIDEO_HEIGHT = 480;
	private float VIDEO_FRAME_RATE;
	
	private final List<Date> m_motionEvents;
	
	private boolean systemArmed = false;
	
	private int EMAIL_INTERVALS;//minutes
	//3600000; == 1 hour
	
	private int m_armingLevel;
	

	private String m_ipAddress; 
	private final Webcam webcam;
	
	private final BlockingQueue<File> m_queueVideosToMake = new LinkedBlockingQueue<File>();
	
	private final static boolean DEBUG = false;
	
	/**
	 * Main Method to start the program.
	 * @param args
	 */
	public static void main(String[] args) {
		new JHawkShell();
		
		//TODO setup system tray icon, for access to close. 
		//https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/uiswing/examples/misc/TrayIconDemoProject/src/misc/TrayIconDemo.java
	}
	

	/**
	 * 
	 */
	public JHawkShell() {
		
		loadProperties();
		imgPath5 = relativePath + camera1 + "imgs5\\";
		moviesPath = dropboxPath + camera1 + "movies\\";
		tempDir = relativePath + camera1 + "maker\\";
				

		m_motionEvents = new ArrayList<Date>();
		EXECUTOR = Executors.newSingleThreadScheduledExecutor();
		EXECUTORPOOL = Executors.newScheduledThreadPool(1);

		
		final File f5 = new File(imgPath5);
		File movie = new File(moviesPath);
		File temp = new File(tempDir);
		f5.mkdir();
		f5.mkdirs();
		movie.mkdir();
		movie.mkdirs();
		temp.mkdir();
		temp.mkdirs();
		
		
		webcam = Webcam.getDefault();
		if (webcam == null) {
			System.out.println("NO WEBCAM FOUND. Please check usb connections and make sure the drivers are installed");
			System.exit(0);
		}
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		
		WebcamMotionDetector detector = new WebcamMotionDetector(Webcam.getDefault());
		detector.setInterval(500); // one check per 500 ms
		detector.addMotionListener(new WebcamMotionListener(){
			@Override
			public void motionDetected(WebcamMotionEvent wme) {
				if (systemArmed && m_armingLevel >= 10) {
					long time = System.currentTimeMillis();
					m_motionEvents.add(new Date(time));
					if (DEBUG) {
						System.err.println("ALARM !!!!");	
					}
					
					
				}
				
			}
		});
		detector.start();
		
		
		final Runnable MoveFiles = new Runnable() {
		       public void run() { 
		    	   if (DEBUG) {	
		    		   System.out.println("MoveFiles Task");
		    	   }
		    	   String temp = tempDir + System.currentTimeMillis() + "\\";
		    	   File newDir = moveFiles(imgPath5, temp);
		    	   m_queueVideosToMake.add(newDir);
		       }
		};
		
		final Runnable SendEmail = new Runnable() {
			public void run() { 
				if (DEBUG) {
					System.out.println("**Send Email Task**");
				}
				List<Date> tempList = new ArrayList<Date>();
				tempList.addAll(m_motionEvents);
				if(!tempList.isEmpty()){
					m_motionEvents.clear();
					GoogleMail mail = null;
					try {
						mail = new GoogleMail(m_googleUserName, m_googlePassword);
					} catch (IllegalArgumentException e) {
						System.out.println("unable to send email. please check your username or password");
						return;
					}
					
					StringBuilder sb = new StringBuilder();
					Date start = new Date(System.currentTimeMillis() - EMAIL_INTERVALS);
					Date end = new Date(System.currentTimeMillis());
					sb.append("Events " + start + " - " + end + " : ");
					sb.append("<br>");
					for(Date dates:tempList){
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(dates.getTime());
						String ampm = cal.get(Calendar.AM_PM) == 0 ? "AM" : "PM";
						sb.append(cal.get(Calendar.HOUR) + ":" + 
								String.format("%02d", cal.get(Calendar.MINUTE)) + "." + 
								String.format("%02d", cal.get(Calendar.SECOND)) +
								" " + ampm);
						sb.append("<br>");
					}
					
					Calendar sDate = Calendar.getInstance();
					String subjectDate = sDate.get(Calendar.MONTH) + 1 + "-" +  sDate.get(Calendar.DAY_OF_MONTH) + "-" + sDate.get(Calendar.YEAR);
					// from, to, subject, body
					mail.sendMail(m_googleUserName, ALARM_EMAIL, "WEBCAM MOTION EVENTS " + subjectDate, sb.toString());
					//TODO attach video
				}
				//delete file in next folder
				String outputURL = moviesPath + "\\" + DateMaker.NextHourlyFolderMakerNew() + "\\";
				File NextFolderToDelete = new File(outputURL);
				//cleanUp(NextFolderToDelete);
			}
		};
		
		final Runnable FolderCleanup = new Runnable() {
			public void run() { 
				System.out.println("Clean Up Task");
				//delete file in next folder
				String outputURL = moviesPath + "\\" + DateMaker.NextHourlyFolderMakerNew() + "\\";
				File NextFolderToDelete = new File(outputURL);
				cleanUp(NextFolderToDelete);
			}
		};
		
		final Runnable GetImage = new Runnable() {
			public void run() { 
				final BufferedImage img = webcam.getImage();
				new Thread(new Runnable(){
					@Override
					public void run() {
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(System.currentTimeMillis());
						File f = new File(imgPath5 + cal.getTimeInMillis() + ".jpg");
						BufferedImage bf = process(img, cal);
						try {
							ImageIO.write(bf, "jpg", f);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		};
		
		final Runnable AlarmSensor = new Runnable() {
			public void run() { 
				InetAddress address = null;
				try {
					address = InetAddress.getByName(m_ipAddress);
				} catch (UnknownHostException e) {
					e.printStackTrace();
					systemArmed = true;
				}
				try {
					if (address.isReachable(10000)) {
						//is reachable
						systemArmed = false;
						m_armingLevel = 0;
					} else {
						//is not reachable
						systemArmed = true;
						m_armingLevel++;
					}
				} catch (IOException e) {
					System.out.println("Error 3,  " + e);
					e.printStackTrace();
					systemArmed = true;
				}
				
				if (m_armingLevel >= 10) {
					m_armingLevel = 15;
				}
				
				
				//alarm time based system
				Calendar now = Calendar.getInstance();
				Calendar morning = Calendar.getInstance();//today at 730am
				Calendar night = Calendar.getInstance();//today at 500pm
				morning.set(Calendar.HOUR_OF_DAY, 8);
				morning.set(Calendar.MINUTE, 30);
				night.set(Calendar.HOUR_OF_DAY, 17);
				night.set(Calendar.MINUTE, 00);
				
				//alarmOn = testRange(now.getTime(), morning.getTime(), night.getTime()); //time based alarmm
				
				
				
				
			}
		};
		
		EXECUTOR.scheduleAtFixedRate(MoveFiles, 30, 30, TimeUnit.SECONDS);
		Calendar c = Calendar.getInstance();
		int timeToTopHour = Math.abs(c.get(Calendar.MINUTE)-60);
		
		EXECUTORPOOL.scheduleAtFixedRate(SendEmail, EMAIL_INTERVALS, EMAIL_INTERVALS, TimeUnit.MINUTES);
		EXECUTORPOOL.scheduleAtFixedRate(FolderCleanup, 60, 60, TimeUnit.MINUTES);
		long frame_rate = (long)(1000/VIDEO_FRAME_RATE);
		EXECUTORPOOL.scheduleAtFixedRate(GetImage, frame_rate, frame_rate, TimeUnit.MILLISECONDS);
		EXECUTORPOOL.scheduleAtFixedRate(AlarmSensor, 60, 60, TimeUnit.SECONDS);
		
		while (true) {
			if (!m_queueVideosToMake.isEmpty()) {
				File buildMovie = m_queueVideosToMake.remove();
				Thread t = new Thread(new VideoMaker(buildMovie));
			    t.start();
			} else {
				try {
					Thread.sleep(15000);//15 seconds
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		
	} // end of constructor
	
	/**
	 * Checks to see if a date is within a start and end date.
	 * Example:
	 * if the test date June 5. Start date is June 1 and end date is June 10 this method will return true
	 * if the test date June 15. Start date is June 1 and end date is June 10 this method will return false
	 * @param testDate the date you wish to test
	 * @param start start date
	 * @param end end date
	 * @return test result.
	 */
	private final boolean testRange(Date testDate, Date start, Date end){
		return !(testDate.before(start) || testDate.after(end));
	}
	
	   	
	
	/**
	 *  Takes images that webcam produced and turns it into a video. 
	 * @author Alex
	 *
	 */
	private class VideoMaker extends Thread {
		    File newDir;

		    public VideoMaker(File text) {
		      newDir = text;
		    }

		    @Override
		    public void run() {
		    	if (DEBUG) {
		    		System.out.println("Five Minute Task - files moved");
		    	}
	    	    Vector<BufferedImage> v = getImgsFromDir(newDir.getAbsolutePath());
	    	    String outputURL = "file:\\" + moviesPath + "\\" + DateMaker.HourlyFolderMakerNew() + "\\" + DateMaker.shortName("minute") + ".mov";
	    	    //TODO change the output name to be the name of the start of the first image. 
	    	    new File(outputURL).mkdirs();
	    	    if (DEBUG) {
	    	    	System.out.println("Five Minute Task - got files.");
	    	    }
	    		// Generate the output media locators.
	    		MediaLocator oml;

	    		if ((oml = createMediaLocator(outputURL)) == null) {
	    		    System.err.println("Cannot build media locator from: " + outputURL);
	    		    System.exit(0);
	    		}
	    		if (DEBUG) {
	    			System.out.println("Five Minute Task - before movie");
	    		}
	    		JpegImagesToMovie imageToMovie = new JpegImagesToMovie(newDir.getAbsolutePath());
	    		imageToMovie.doIt(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME_RATE, v, oml);
	    		
	    		//sleeps for 5 seconds to make sure movie maker has released all OS control over images to they can be deleted.
	    		try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    		cleanUp(newDir);
	    		v = null;
	    		imageToMovie = null;
	    		System.gc();
		    }
		  } 
	
	

	/**
	 * Loads in the data from a properties file.
	 */
	private void loadProperties() {
		File path = new File(PROPERTIES_PATH);
		System.out.println("Properties are being loaded from: " + path.getAbsolutePath());
		Properties prop = new Properties();
		
		InputStream input;
		try {
			input = new FileInputStream(path);
			prop.load(input);
			dropboxPath = prop.getProperty("dropbox");
			relativePath = prop.getProperty("relativePath");
			EMAIL_INTERVALS = Integer.parseInt(prop.getProperty("EMAIL_INTERVALS"));
			VIDEO_FRAME_RATE = Float.parseFloat(prop.getProperty("VIDEO_FRAME_RATE"));
			TEXT_NUMBER = prop.getProperty("TEXT_NUMBER");
			WORK_NUMBER = prop.getProperty("WORK_NUMBER");
			ALARM_EMAIL = prop.getProperty("ALARM_EMAIL");
			m_ipAddress = prop.getProperty("ip");
			m_googleUserName = prop.getProperty("gmailUserName");
			m_googlePassword = prop.getProperty("gmailPassword");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (m_googleUserName.isEmpty()) {
			System.out.println("WARNING - email service will not work unless you provide a valid gmail account.");
		}
		if (m_googlePassword.isEmpty()) {
			System.out.println("WARNING - email service will not work unless you provide a valid gmail account password.");
		}
	}

	/**
	 * Moves files from one DIR to another. 
	 * @param dirFrom
	 * @param dirTo
	 * @return
	 */
	private File moveFiles(String dirFrom, String dirTo){
		File from = new File(dirFrom);
		File temp = new File(dirTo);
		temp.mkdir();
		temp.mkdirs();
		try {
			for(File file:from.listFiles()){
				Files.move(file.toPath(), new File(temp.toPath()+ "\\" + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return temp;
		
	}
	
	/**
	 * 
	 * Gathers all the images that are found in a given path and returns you a Vector of BufferedImages
	 * @param path
	 * @return
	 */
	private Vector<BufferedImage> getImgsFromDir(String path){
		Vector<BufferedImage> v = new Vector<BufferedImage>();
 	    String inputDir = path;
 	    File inputFileDir = new File(inputDir);
 	    for(File files:inputFileDir.listFiles()){
 	    	BufferedImage img = null;
				try {
					img = ImageIO.read(files);
				} catch (IOException e) {
					e.printStackTrace();
				}
 	    	v.add(img);
 	    }
 	    return v;
	}
	
	/**
	 * Sets a timestamp on a given image.
	 * @param old
	 * @param cal
	 * @return
	 */
	private BufferedImage process(BufferedImage old, Calendar cal) {
        int w = old.getWidth();
        int h = old.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(old, 0, 0, null);
        g2d.setPaint(Color.white);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
        String s = "" + cal.getTime() + " " + String.format("%03d", cal.get(Calendar.MILLISECOND));
        FontMetrics fm = g2d.getFontMetrics();
        int x = img.getWidth() - fm.stringWidth(s) - 5;
        int y = fm.getHeight();
        g2d.setColor(Color.BLACK);
        g2d.drawString(s, ShiftWest(x, 1), ShiftNorth(y, 1) );
        g2d.drawString(s, ShiftWest(x, 1), ShiftSouth(y, 1) );
        g2d.drawString(s, ShiftEast(x, 1), ShiftNorth(y, 1) );
        g2d.drawString(s, ShiftEast(x, 1), ShiftSouth(y, 1) );
        g2d.setColor(Color.white);
        g2d.drawString(s, x, y);
        g2d.dispose();
        return img;
    }
	
	private int ShiftNorth(int p, int distance) {
	   return (p - distance);
	   }
	private int ShiftSouth(int p, int distance) {
	   return (p + distance);
	   }
	private int ShiftEast(int p, int distance) {
	   return (p + distance);
	   }
	private int ShiftWest(int p, int distance) {
	   return (p - distance);
	   }
	
	
	/**
	 * Recursively deletes all files and children files of the given File Directory
	 * @param file
	 */
	private void cleanUp(File file){
		//to end the recursive loop
        if (!file.exists())
            return;
        
        //if directory, go inside and call recursively
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                //call recursively
            	cleanUp(f);
            }
        }
        //call delete to delete files and empty directory
        file.delete();
	}
	
    /**
     * Create a media locator from the given string.
     */
    private MediaLocator createMediaLocator(String url) {
		MediaLocator ml;
	
		if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
		    return ml;
	
		if (url.startsWith(File.separator)) {
		    if ((ml = new MediaLocator("file:" + url)) != null)
			return ml;
		} else {
		    String file = "file:" + System.getProperty("user.dir") + File.separator + url;
		    if ((ml = new MediaLocator(file)) != null)
			return ml;
		}
	
		return null;
    }
	

	
}
