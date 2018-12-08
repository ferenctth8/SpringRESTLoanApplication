package com.feritoth.cla.springmvc.controller.utility;

import java.net.URI;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;
import com.feritoth.cla.springmvc.dbmodel.LoanCurrency;
import com.feritoth.cla.springmvc.jsonmodel.SerializedClient;
import com.feritoth.cla.springmvc.jsonmodel.SerializedIPAddress;
import com.feritoth.cla.springmvc.jsonmodel.SerializedLoan;

/**
 * This class serves as utility provider for input pre-processing during the execution of the 
 * diverse business entity operations covered by the RESTful methods.
 *  
 * @author Frantisek Slovak
 *
 */
public class IOFormatter {
	
	/* The CNP representation pattern - used for the checking the CNPs submitted for search if they are well-formed */
	private static final String CNP_PATTERN = "[0-9]+";	
	
	/* The CNP length checker reference */
	private static final int MAX_CNP_LENGTH = 12;
	
	/**
	 *  Validator method for CNP representation pattern check.
	 *  
	 *  @param clientCNP the CNP of the client to be registered
	 *  
	 *  @return a boolean value indicating whether the given CNP is well-formed 
	 *  in terms of character content (contains only digits)  
	 */
	public static boolean validateClientCNPcontent(String clientCNP){
		Pattern cnpPattern = Pattern.compile(CNP_PATTERN);
		Matcher matcher = cnpPattern.matcher(clientCNP);
		return matcher.matches();
	}
	
	/**
	 * Validator method for the CNP length check.
	 * 
	 * @param clientCNP the CNP of the client to be registered
	 * 
	 * @return a boolean value indicating whether the given CNP is well-formed 
	 * in terms of length (should not have more than 12 characters) 
	 */
	public static boolean validateClientCNPlength(String clientCNP){		
		return clientCNP.length() == MAX_CNP_LENGTH;		
	}
	
	/**
	 * A method for converting the whole entity Client list into one of equivalent business objects.
	 * 
	 * @param allRegisteredClients the list of entity clients fetched from the DB
	 * 
	 * @return the list of equivalent serializable clients ready for packaging into JSON
	 */
	public static List<SerializedClient> convertEntityToDTOforClientList(List<Client> allRegisteredClients){
		List<SerializedClient> allAvailableClients = new ArrayList<>();
		for (Client registeredClient : allRegisteredClients){
			allAvailableClients.add(convertSingleClientEntityToDTO(registeredClient));
		}
		return allAvailableClients;
	}

	/**
	 * A method for converting a single entity client to the corresponding DTO.
	 * 
	 * @param matchingClient the entity client required to be converted
	 * 
	 * @return the equivalent serializable DTO client resulting from the conversion
	 */
	public static SerializedClient convertSingleClientEntityToDTO(Client matchingClient) {
		return new SerializedClient(matchingClient);
	}
	
	/**
	 * A method for performing the reverse conversion from DTO to entity for a single client.
	 * 
	 * @param serializedClient the serialized client to be converted
	 * 
	 * @return the entity client resulting from the conversion
	 */
	public static Client convertSerializedClientDTOtoEntity(SerializedClient serializedClient){
		return new Client(serializedClient.getCnp(), serializedClient.getName(), serializedClient.getEmailAddress(), serializedClient.getPostalAddress());
	}

	/**
	 * A method for converting a whole entity IPAddress list into a serializable list of equivalent business objects.
	 * 
	 * @param allRegisteredIPAddresses the list of entity IP addresses fetched from the DB
	 * 
	 * @return the list of equivalent serializable IP addresses
	 */
	public static List<SerializedIPAddress> convertEntityToDTOforIPAddressList(List<IPAddress> allRegisteredIPAddresses) {
		List<SerializedIPAddress> allAvailableIPAddresses = new ArrayList<>();
		for (IPAddress registeredAddress : allRegisteredIPAddresses){
			allAvailableIPAddresses.add(convertSingleIPEntityToDTO(registeredAddress));
		}
		return allAvailableIPAddresses;
	}

	/**
	 * A method for converting a single IP address entity into the corresponding DTO.
	 * 
	 * @param registeredAddress the entity address to be converted
	 * 
	 * @return the equivalent serializable entity
	 */
	public static SerializedIPAddress convertSingleIPEntityToDTO(IPAddress registeredAddress) {
		return new SerializedIPAddress(registeredAddress);
	}
	
	/**
	 * A method for converting a single IP address DTO to the corresponding entity.
	 * 
	 * @param candidateIpAddress the DTO to be converted
	 * 
	 * @return the equivalent DB entity object
	 */
	public static IPAddress convertDTOtoEntityForIPAddress(SerializedIPAddress candidateIpAddress) {		
		Client ownerClient = convertSerializedClientDTOtoEntity(candidateIpAddress.getOwnerClient());
		IPAddress issueAddress = new IPAddress(candidateIpAddress.getIpValue(), ownerClient);
		if (candidateIpAddress.getIpID() != null){
			issueAddress.setIpAddressID(candidateIpAddress.getIpID());
		}
		return issueAddress;
	}
	
	/**
	 * A method for converting a whole list of Loan entities into one of equivalent business objects.
	 *  
	 * @param allRegisteredLoans the list of loans fetched from the DB
	 * 
	 * @return the list of equivalent serializable loan objects
	 */
	public static List<SerializedLoan> convertEntityToDTOforLoanList(List<Loan> allRegisteredLoans) {		
		List<SerializedLoan> allAvailableLoans = new ArrayList<>();
		allRegisteredLoans.forEach(loan -> allAvailableLoans.add(convertSingleLoanEntityToDTO(loan)));
		return allAvailableLoans;
	}
	
	/**
	 * Converter method from one DB loan entity into the corresponding business object.
	 *  
	 * @param loan the loan entity to be transformed
	 *  
	 * @return the serialized loan DTO resulting from the conversion
	 */
	public static SerializedLoan convertSingleLoanEntityToDTO(Loan loan) {		
		/* Create the converted entity */
		SerializedLoan serializedLoan =  new SerializedLoan(loan);
		/* In case the loan is extended, compute its number of extensions and add it to the converted entity */
		if (loan.isExtended()){
			Long extensionWeeks = ChronoUnit.WEEKS.between(loan.getApplicationTime().toLocalDate(), loan.getPaybackDate()) - 1;
			serializedLoan.setExtensionCount(extensionWeeks);
		} else {
			serializedLoan.setExtensionCount(0L);
		}
		/* Return in the end the given loan */
		return serializedLoan;
	}
	
	/**
	 * A method for performing the reverse conversion for loan objects (business object to DTO).
	 * 
	 * @param serializedLoan the serialized loan to be converted
	 *  
	 * @return the equivalent entity object
	 */
	public static Loan convertSingleLoanDTOToEntity(SerializedLoan serializedLoan) {		
		/* Create the support objects first */
		IPAddress issueAddress = convertDTOtoEntityForIPAddress(serializedLoan.getIpAddress()); //the IP address used for registration
		Timestamp applicationTime = Timestamp.valueOf(serializedLoan.getApplicationTime()); //the application time
		Date returnDate = Date.valueOf(serializedLoan.getReturnDate()); //the return date
		LoanCurrency currency = LoanCurrency.valueOf(serializedLoan.getCurrency().name()); //the currency for loan measurements
		/* Finally assemble the loan object */
		return new Loan(serializedLoan.getLoanID(), issueAddress, applicationTime, returnDate, 
				        serializedLoan.getLoanedAmount(), currency, serializedLoan.isExtended(), serializedLoan.getInterestRate());
	}

	/**
	 * A method for converting a String to a JSON value.
	 * 
	 * @param candidateHeader the header to be converted
	 * 
	 * @return the converted header under JSON format
	 * 
	 * @throws JsonProcessingException in case of any conversion problem
	 */
	public static String convertHeadersToJSON(URI candidateHeader) throws JsonProcessingException {
		ObjectMapper objmap = new ObjectMapper();		
		return objmap.writeValueAsString(candidateHeader);
	}			

}