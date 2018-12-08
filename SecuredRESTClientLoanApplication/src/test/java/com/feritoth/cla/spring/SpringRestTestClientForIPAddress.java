package com.feritoth.cla.spring;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.feritoth.cla.springmvc.controller.exception.ExceptionInfo;
import com.feritoth.cla.springmvc.jsonmodel.SerializedClient;
import com.feritoth.cla.springmvc.jsonmodel.SerializedIPAddress;

public class SpringRestTestClientForIPAddress {
	
	/* Declare here the link where the tests shall be carried out */
    public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	/* And the logger used for doing the quick tests */
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringRestTestClientForIPAddress.class);
	
	/* GET all the IPs from the system */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void listAllIPaddressesFromSystem(){
		/* Print the pattern used for the search as part of the validation process */
		LOGGER.info("Testing listAllIPaddresses RESTful method...");
		/* Create the link to be used for the request */
		RestTemplate restTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/ipAddress/";
		/* Fetch and process the response from the called REST method - do a quick status check before further execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(restURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			List<HashMap<String, Object>> allIPAddressMaps = serviceResponse.getBody();
			LOGGER.info("The following IPs have been identified as registered in the current system:");	
			for (HashMap<String, Object> ipAddressMap : allIPAddressMaps){
				LOGGER.info("IPAddress : id=" + ipAddressMap.get("ipID") + ", value=" + ipAddressMap.get("ipValue") + ", client=" + ipAddressMap.get("ownerClient"));
			}
		}
	}
	
	/* GET all the IPs assigned to a client based on his/her CNP */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void listAllIPaddressesForClient(String searchCNP){
		/* Print a test pattern for the given method */
		LOGGER.info("Testing listAllIPaddressesForClient method...");
		/* Create the link to be used for the request execution */
		RestTemplate restTemplate = new RestTemplate();
		String initialRestURL = REST_SERVICE_URI + "/ipAddress/matchClient/{clientCNP}/";
		String finalRestURL = initialRestURL.replace("{clientCNP}", searchCNP);
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(finalRestURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.NO_CONTENT){
			LOGGER.warn("The given CNP, " + searchCNP + ", has not been found as registered in the database - therefore no IPs have been assigned to its client...");
		} else {
			List<HashMap<String, Object>> allAssignedIPsForClient = serviceResponse.getBody();
			if (allAssignedIPsForClient.isEmpty()){
				LOGGER.info("The client with CNP, " + searchCNP + ", has been registered in the DB, but currently has no IP addresses assigned to him\\her...");
			} else {
				LOGGER.info("For client with CNP=" + searchCNP + ", the following IPs have been identified as registered in the current system:");	
				for (HashMap<String, Object> ipAddressMap : allAssignedIPsForClient){
					LOGGER.info("IPAddress : id=" + ipAddressMap.get("ipID") + ", value=" + ipAddressMap.get("ipValue") + ", client=" + ipAddressMap.get("ownerClient"));
				}
			}			
		}		
	}
	
	/* GET all the details of an IP address */
	@SuppressWarnings("unchecked")
	private static void getIPDetailsForValue(String ipAddressValue){
		/* Print a test message pattern for this method as well */
		LOGGER.info("Testing getIPDetailsForValue method...");
		/* Create the link to be used for the request execution */
		RestTemplate restTemplate = new RestTemplate();
		String initialRestURL = REST_SERVICE_URI + "/ipAddress/matchIPvalue/{ipValue}/";
		String finalRestURL = initialRestURL.replace("{ipValue}", ipAddressValue);
		/* Fetch and process the response from the called REST method - check again the status before finalizing result extraction*/
		ResponseEntity<Object> serviceResponse = restTemplate.getForEntity(finalRestURL, Object.class);
		HashMap<String, Object> responseMap = (HashMap<String, Object>) serviceResponse.getBody();
		if (responseMap.containsKey("url") && responseMap.containsKey("exceptionMessage")){
			ExceptionInfo exInfo = new ExceptionInfo((String)responseMap.get("url"), (String)responseMap.get("exceptionMessage"), (Integer)(responseMap.get("errorCode")), HttpStatus.valueOf((String)(responseMap.get("httpOperationStatus"))));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {			
			HashMap<String, Object> clientMap = (HashMap<String, Object>) responseMap.get("ownerClient");
			SerializedClient soc = new SerializedClient((String)clientMap.get("cnp"), (String)clientMap.get("name"), (String)clientMap.get("emailAddress"), (String)clientMap.get("postalAddress"));
			SerializedIPAddress sipAddress = new SerializedIPAddress(ipAddressValue, (Integer)(responseMap.get("ipID")));
			sipAddress.setOwnerClient(soc);
			LOGGER.info("The submitted IP address value, " + ipAddressValue + ", return the following info: " + sipAddress.toString());			
		}
	}
	
	/* POST a new IP address into the system */
	@SuppressWarnings("unchecked")
	private static void createNewIPAddress(String ipAddressValue){
		/* Print a start message for launching the verification */
		LOGGER.info("Regsitering a new IP address into the system...");
		/* Create the communication template object */
		RestTemplate registrationTemplate = new RestTemplate();
		/* Create the client to whom the IP address shall be assigned */
		SerializedClient newClient = new SerializedClient("284910573642","Martina Hamsova","martina.hamsova@gmail.com","Lomnickeho 8, Prague 4 Pankrac");		
        /* Create also the IP addresses to be inserted */
		SerializedIPAddress newIPAddress = new SerializedIPAddress();
		newIPAddress.setIpValue(ipAddressValue);
		newIPAddress.setOwnerClient(newClient);
		/* Create the URI to be invoked during registration */
		String restURL = REST_SERVICE_URI + "/ipAddress/";
		/* Create the HttpHeaders object for incorporating the method restrictions */
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptedMediaTypes);
		/* Create the HttpEntity object for incorporating the headers and invoke the executed operation */
		HttpEntity<SerializedIPAddress> newIPAddressHeader = new HttpEntity<SerializedIPAddress>(newIPAddress, headers);
		ResponseEntity<?> serviceResponse = registrationTemplate.postForEntity(restURL, newIPAddressHeader, Object.class, newIPAddress.getIpValue());
		/* Check for the returned response - see its status first and then act accordingly */		
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			/* In this case, an exception has been found - print out the details */			
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {
			String responseObject = (String) serviceResponse.getBody();
			/* In this case, print the newly created location for the inserted client */
			URI registrationURI = URI.create(responseObject);
			LOGGER.info("The new IP address has been registered under the following location:" + registrationURI);
		}
	}	
		
	/* DELETE an existing IP address */
	@SuppressWarnings("unchecked")
	private static void removeExistingIPAddress(String ipAddress){
		/* Print a message for the operation start part */
		LOGGER.info("Testing the removal of an IP address from the system...");
		/* Create the URL and the REST template for executing the given request */
		RestTemplate deleteTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/ipAddress/" + ipAddress + "/";
		/* Remove the given IP address */
		ResponseEntity<?> serviceResponse = deleteTemplate.exchange(URI.create(restURL), HttpMethod.DELETE, null, Object.class);
		/* Check the result status and display the corresponding message */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			/* In this case, an exception has been found - print out the details */			
			String exceptionMessage = (String)responseObject.get("exceptionMessage");
			Integer errorCode = Integer.valueOf((Integer)responseObject.get("errorCode"));
			HttpStatus httpStatus = HttpStatus.valueOf((String)responseObject.get("httpOperationStatus"));
			ExceptionInfo exInfo = new ExceptionInfo(restURL, exceptionMessage, errorCode, httpStatus);
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {
			/* The exception has been executed with successful outcome */
			LOGGER.info("The given IP address, " + ipAddress +  ", has been successfully removed from the DB!");
		}
		//deleteTemplate.delete(restURL);//simpler way, but unfortunately does not deal with the error cases or return any result for analysis		
	}
	
	public static void main(String[] args){
		/* List all the IP addresses currently registered in the system */
		//listAllIPaddressesFromSystem();//Worked
		/* List all the IPs from the system which are registered to one specific client */
		//listAllIPaddressesForClient("146890123456"); //Worked with refactoring
		//listAllIPaddressesForClient("146890123457"); //Worked with refactoring
		//listAllIPaddressesForClient("284910573642"); //Worked with refactoring
		/* List all the details for one given IP address value */
		//All are working
		//getIPDetailsForValue("150.90.72.81"); //Worked
		//getIPDetailsForValue("150.90.72.82"); //Worked
		//getIPDetailsForValue("150.90.72.a"); //Worked
		/* Insert a certain IP address in the system */
		createNewIPAddress("150.90.72.a");
		createNewIPAddress("150.90.72.81");
		createNewIPAddress("150.90.72.84");
		createNewIPAddress("150.90.75.84");
		/* Remove a certain IP address */
		//removeExistingIPAddress("150.90.72.81"); //Worked with refactoring
		//removeExistingIPAddress("150.90.72.72"); //Worked with refactoring
	}

}