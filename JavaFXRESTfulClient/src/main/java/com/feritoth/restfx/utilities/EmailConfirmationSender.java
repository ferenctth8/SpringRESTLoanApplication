package com.feritoth.restfx.utilities;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * The current class is responsible for handling the e-mail sending procedure during the successful client and loan
 * registration respectively also during the loan extension.
 *
 * @author Frantisek Slovak
 */
public class EmailConfirmationSender {

	/* The logger associated to this particular utility - to be used during the detection of any exceptions raised while trying to create the desired e-mail */
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailConfirmationSender.class);

	/* Some constants required for authentication of the given user through the OAuth protocol for Gmail */
	private static final String APPLICATION_NAME = "Gmail API Java Quickstart for E-mail Sending";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    /* The file containing the authentication token for Gmail access */
    private static final String CLIENT_SECRET_DIR = "src/main/resources/client_secret.json";

	/**
	 * The invoker method used for interacting with the e-mail creator API for achieving the transmission facility
	 *
	 * @param subject the subject of the e-mail to be created
	 * @param content the content of the e-mail to be created
	 */
	public static void sendConfirmationEmail(String subject, String content) {
		try {
			/* Create the e-mail to be sent out in its raw form */
			MimeMessage rawEmail = createNewEmail("kolcsey_ferencz2001@yahoo.com", "ferenctth8@gmail.com", subject, content);
			/* Build a new authorized API client service */
	        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	                                 .setApplicationName(APPLICATION_NAME)
	                                 .build();
	        /* Send out the e-mail to the specified destination - mention also explicitly the source */
	        sendMessage(service, "me", rawEmail);
		} catch (MessagingException | GeneralSecurityException | IOException e) {
			/* Print out an error message as well as throw a runtime exception in case of any problems encountered */
			LOGGER.error("Serious error encountered during the execution of the given operation!" + e.getMessage());
			generateAlert(AlertType.ERROR, "Email Sending Problem", "Serious error encountered during the execution of the given operation!" + e.getMessage(), true);
			throw new RuntimeException(e);
		}
	}

	/**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If there is no client_secret.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
    	InputStream in = new FileInputStream(CLIENT_SECRET_DIR);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        		                               .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
        		                               .setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }


	/**
     * Create a MimeMessage using the parameters provided.
     *
     * @param destinationAddress email address of the receiver
     * @param sourceAddress email address of the sender, the mailbox account
     * @param subject subject of the email
     * @param bodyContent body text of the email
     *
     * @return the MimeMessage to be used to send email
     *
     * @throws MessagingException in case of any error encountered during the e-mail creation procedure
     */
	private static MimeMessage createNewEmail(String destinationAddress, String sourceAddress,
			                                  String subject, String bodyContent) throws MessagingException {
		/* Create a new Properties map and a session as prerequisites */
		Properties mailProperties = new Properties();
		Session newMailSession = Session.getDefaultInstance(mailProperties, null);
		/* Create next the e-mail to be sent out */
		MimeMessage finalEmail = new MimeMessage(newMailSession);
		/* Adjust its properties before return */
		finalEmail.setFrom(new InternetAddress(sourceAddress));//the source address
		finalEmail.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(destinationAddress));//the destination address
		finalEmail.setSubject(subject);//the subject
		finalEmail.setText(bodyContent);//the body content
		/* Return it */
		return finalEmail;
	}

	/**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     *
     * @return a message containing a base64url encoded email
     *
     * @throws IOException for any problems detected during the channel transmission
     * @throws MessagingException for any problems detected during the messaging operation itself
     */
	private static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException{
		/* Create a buffer and transfer there the mail content */
		ByteArrayOutputStream mailBuffer = new ByteArrayOutputStream();
		emailContent.writeTo(mailBuffer);
		/* Convert next the content into a byte array to be securely encoded */
		byte[] mailBytes = mailBuffer.toByteArray();
		String encodedEmail = Base64.encodeBase64URLSafeString(mailBytes);
	    /* Create a new message wrapper for inserting the previously encoded content */
		Message wrapperMessage = new Message();
		wrapperMessage.setRaw(encodedEmail);
		/* Return the given wrapper */
		return wrapperMessage;
	}

	/**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me" can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     *
     * @return The sent message
     *
     * @throws IOException for any problems detected during the channel transmission
     * @throws MessagingException for any problems detected during the messaging operation itself
     */
	private static Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws MessagingException, IOException {
		/* Create the e-mail content to be sent out */
		Message message = createMessageWithEmail(emailContent);
		/* Send the created message and store the result of the operation */
		message = service.users().messages().send(userId, message).execute();
		/* Print out the operation result as a log */
		LOGGER.info("The transmission operation concluded with the following ID:" + message.getId());
		LOGGER.info("The following is the detailed result returned by the invocation of the previous operation:" + message.toPrettyString());
		/* Return the given message */
		return message;
	}

	/**
	 * An alert generator method for signaling any transmission problems.
	 *
	 * @param alertType the alert type
	 * @param title the title of the alert
	 * @param contentText the alert content
	 * @param isResizable a resizability flag
	 */
	private static void generateAlert(AlertType alertType, String title, String contentText, boolean isResizable){
		Alert newAlert = new Alert(alertType);
		newAlert.setTitle(title);
		newAlert.setContentText(contentText);
		newAlert.setResizable(isResizable);
		newAlert.showAndWait();
	}

}