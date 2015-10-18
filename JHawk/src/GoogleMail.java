import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This code was borrowed from:
 * http://www.javasrilankansupport.com/2012/05/send-email-in-java-mail-api-using-gmail.html
 * 
 * I have changed it to better fit my needs. 
 * @author Java Srilankan Support - JSupport
 * @author Alex McKenzie
 * 
 * 
 *
 */
public class GoogleMail {
    private Session m_Session;
    private Message m_simpleMessage;
    private InternetAddress m_fromAddress;
    private InternetAddress m_toAddress;
    private Properties m_properties;
	private String m_userName;
	private String m_password;
	
	/**
	 * Creates instance of GoogleMail. Used to send emails via Google Mail. 
	 * @param user Google username
	 * @param pass Google password
	 * @throws IllegalArgumentException if any params are bad.
	 */
	public GoogleMail(String user, String pass) throws IllegalArgumentException {
		if (user == null) {
			throw new IllegalArgumentException("user name cannot be null");
		}
		if (user.isEmpty()) {
			throw new IllegalArgumentException("user name cannot be Empty");
		}
		if (pass == null) {
			throw new IllegalArgumentException("password cannot be null");
		}
		if (pass.isEmpty()) {
			throw new IllegalArgumentException("passwordcannot be Empty");
		}
		
		m_userName = user;
		m_password = pass;
	}
     
	/**
	 * Send an email via google.
	 * @param m_from
	 * @param m_to
	 * @param m_subject
	 * @param m_body
	 */
    public void sendMail(String m_from,String m_to,String m_subject,String m_body) {
    	//TODO add some sort of email address validation.
    	//TODO better checking and repsonse create a response object. 
    	try {
 
            m_properties = new Properties();
            m_properties.put("mail.smtp.host", "smtp.gmail.com"); 
            m_properties.put("mail.smtp.socketFactory.port", "465");
            m_properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
            m_properties.put("mail.smtp.auth", "true");
            m_properties.put("mail.smtp.port", "465");
             
 
            m_Session        =   Session.getDefaultInstance(m_properties,new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("m_userName","m_password"); // username and the password
                }
 
            });
             
            m_simpleMessage  =   new MimeMessage(m_Session);
 
            m_fromAddress    =   new InternetAddress(m_from);
            m_toAddress      =   new InternetAddress(m_to);
 
 
            m_simpleMessage.setFrom(m_fromAddress);
            m_simpleMessage.setRecipient(RecipientType.TO, m_toAddress);
            m_simpleMessage.setSubject(m_subject);
            m_simpleMessage.setContent(m_body,"text/html");
 
            Transport.send(m_simpleMessage);
 
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
 
//    public static void main(String[] args) {
// 
//    	GoogleMail send_mail    =   new GoogleMail();
//      send_mail.sendMail("test@gmail.com", "test@gmail.com", "Test Mail", "Hi this is Test mail from Java Srilankan Support");
//    }
 

}