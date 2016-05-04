package edu.cmu.hcii.whyline.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.activation.*;
import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

public class Feedback {

	private static final String username = "whyline@gmail.com", password = "feedback!";
	
	public static void feedback(String content, Frame windowToSend) throws AddressException, MessagingException, AWTException, IOException {
		
	    String d_host = "smtp.gmail.com";
	    String d_port  = "465";
	    String m_subject = "Testing";
	    
		 Properties props = new Properties();
		 props.put("mail.smtp.user", username);
		 props.put("mail.smtp.host", d_host);
		 props.put("mail.smtp.port", d_port);
		 props.put("mail.smtp.starttls.enable","true");
		 props.put("mail.smtp.auth", "true");
		 props.put("mail.smtp.socketFactory.port", d_port);
		 props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		 props.put("mail.smtp.socketFactory.fallback", "false");
 
		 SecurityManager security = System.getSecurityManager();
	 
		 Authenticator auth = new javax.mail.Authenticator() {
		        public PasswordAuthentication getPasswordAuthentication() { 
		        	return new PasswordAuthentication(username, password); 
		        }
		 };
			 
		 Session session = Session.getInstance(props, auth);
 
		 MimeMessage msg = new MimeMessage(session);
		 msg.setSubject(m_subject);
		 msg.setFrom(new InternetAddress(username));
		 msg.addRecipient(Message.RecipientType.TO, new InternetAddress(username));
		 
		 if(windowToSend == null) {
			 
			 msg.setText(content);
			 
		 }
		 else {

			 byte[] screenshot = null;

			 Toolkit toolkit = Toolkit.getDefaultToolkit();
			 Dimension screenSize = toolkit.getScreenSize();
			 Robot robot = new Robot();
			 BufferedImage image = robot.createScreenCapture(windowToSend.getBounds());
			 ByteArrayOutputStream png = new ByteArrayOutputStream();
			 ImageIO.write(image, "png", png);
			 
			 screenshot = png.toByteArray();
			 
			 // create the text part of the message 
			 MimeBodyPart messageBodyPart = new MimeBodyPart();
			 messageBodyPart.setText(content);
	
			 Multipart multipart = new MimeMultipart();
			 multipart.addBodyPart(messageBodyPart);
	
			 ByteArrayDataSource attachment = new ByteArrayDataSource(screenshot, "image/jpg");
		    
			 // Part two is attachment
			 messageBodyPart = new MimeBodyPart();
			 messageBodyPart.setDataHandler(new DataHandler(attachment));
			 messageBodyPart.setFileName("screenshot.jpg");
	
			 multipart.addBodyPart(messageBodyPart);
	
			 // Put parts in message
			 msg.setContent(multipart);
		    
		 }
		 
		 Transport.send(msg);

	}
	
}