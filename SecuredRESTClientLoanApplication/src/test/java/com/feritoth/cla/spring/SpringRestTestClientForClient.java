package com.feritoth.cla.spring;

import java.net.URI;
import java.net.URISyntaxException;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.feritoth.cla.springmvc.controller.exception.ExceptionInfo;
import com.feritoth.cla.springmvc.jsonmodel.SerializedClient;

public class SpringRestTestClientForClient {
	
	public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringRestTestClientForClient.class);

	/* GET all the users */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void listAllClients(){
		/* Print the pattern used for searching as part of the verification */
		LOGGER.info("Testing listAllClients RESTful method...");
		/* Create the link the to be used for request */
		RestTemplate restTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/client/";
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(restURL, List.class);		
		if(serviceResponse.getStatusCode() == HttpStatus.OK){
			List<HashMap<String, Object>> allClientMaps = serviceResponse.getBody();
			LOGGER.info("The following clients have been currently registered in the system...");
            for(HashMap<String, Object> clientMap : allClientMaps){
            	LOGGER.info("Client : cnp=" + clientMap.get("cnp") + ", Name=" + clientMap.get("name") + ", Email Address=" + clientMap.get("emailAddress") + ", Postal Address=" + clientMap.get("postalAddress"));
            }
        } else {
            LOGGER.warn("The list of users is currently empty unfortunately...");
        }
	}
	
	/* GET all users whose names match a certain search pattern */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void getClientsForNamePattern(String nameSearchPattern){
		/* Print the pattern used for searching as part of the verification */
		LOGGER.info("Testing getClientsForNamePattern RESTful method for pattern " + nameSearchPattern + "...");
		/* Create the link the to be used for request */
		RestTemplate restTemplate = new RestTemplate();
		String restURLTemplate = REST_SERVICE_URI + "/client/matchNameSequence/{nameSequence}/";
		String finalURL = restURLTemplate.replace("{nameSequence}", nameSearchPattern);
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(finalURL, List.class);
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */		
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			List<HashMap<String, Object>> matchingClientMaps = serviceResponse.getBody();
			LOGGER.info("The submitted pattern, " + nameSearchPattern + ", generated the following search results...");
			for (HashMap<String, Object> map : matchingClientMaps){
				LOGGER.info("Client : cnp=" + map.get("cnp") + ", Name=" + map.get("name") + ", Email Address=" + map.get("emailAddress") + ", Postal Address=" + map.get("postalAddress"));
			}
		} else {
			LOGGER.warn("The pattern submitted here as parameter, " + nameSearchPattern + ", does not match any record in the DB unfortunately...");
		}		
	}
	
	/* GET a list of clients based on an e-mail address fragment */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void getClientsForEmailFragment(String emailSearchPattern){
		/* Print the pattern used for searching as part of the verification */
		LOGGER.info("Testing getClientsForEmailPattern RESTful method for search sequence " + emailSearchPattern + "...");
		/* Create the link the to be used for request */
		RestTemplate restTemplate = new RestTemplate();
		String restURLTemplate = REST_SERVICE_URI + "/client/matchEmailFragment/{emailAddressFragment}/";
		String finalURL = restURLTemplate.replace("{emailAddressFragment}", emailSearchPattern);
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(finalURL, List.class);		
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			List<HashMap<String, Object>> matchingClientsMap = serviceResponse.getBody();
			LOGGER.info("The submitted pattern, " + emailSearchPattern + ", generated the following search results...");
			for (HashMap<String, Object> map : matchingClientsMap){
				LOGGER.info("Client : cnp=" + map.get("cnp") + ", Name=" + map.get("name") + ", Email Address=" + map.get("emailAddress") + ", Postal Address=" + map.get("postalAddress"));
			}
		} else {
			LOGGER.warn("The pattern submitted here as parameter, " + emailSearchPattern + ", does not match any record in the DB unfortunately...");
		}
	}
	
	/* GET a client based on its ID */
	@SuppressWarnings("unchecked")
	private static void getClientForCNP(String cnp){
		/* Print the pattern used for searching as part of the verification */
		LOGGER.info("Testing getClientForCNP RESTful method for CNP " + cnp + "...");
		/* Create the link the to be used for request */
		RestTemplate restTemplate = new RestTemplate();
		String restURLTemplate = REST_SERVICE_URI + "/client/matchID/{cnp}/";
		String finalURL = restURLTemplate.replace("{cnp}", cnp);		
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */
		ResponseEntity<Object> serviceResponse = restTemplate.getForEntity(finalURL, Object.class);
		HashMap<String, Object> responseMap = (HashMap<String, Object>) serviceResponse.getBody();
		if (responseMap.containsKey("url") && responseMap.containsKey("exceptionMessage")){
			ExceptionInfo exInfo = new ExceptionInfo((String)responseMap.get("url"), (String)responseMap.get("exceptionMessage"), (Integer)(responseMap.get("errorCode")), HttpStatus.valueOf((String)(responseMap.get("httpOperationStatus"))));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {			
			SerializedClient sc = new SerializedClient((String)responseMap.get("cnp"), (String)responseMap.get("name"), (String)responseMap.get("emailAddress"), (String)responseMap.get("postalAddress"));
			LOGGER.info("The submitted CNP, " + cnp + ", return the following info: " + sc.toString());			
		}		 		
	}
	
	/* GET a client based on an IP address */
	@SuppressWarnings("unchecked")
	private static void getClientForIPAddress(String ipAddress){
		/* Print the pattern used for searching as part of the verification */
		LOGGER.info("Testing getClientForIPAddress RESTful method for IP address " + ipAddress + "...");
		/* Create the link the to be used for request */
		RestTemplate restTemplate = new RestTemplate();
		String restURLTemplate = REST_SERVICE_URI + "/client/matchIPAddress/{ipAddress}/";
		String finalURL = restURLTemplate.replace("{ipAddress}", ipAddress);
		/* Fetch back the response from the called REST method - check its status before proceeding with execution*/
		ResponseEntity<Object> serviceResponse = restTemplate.getForEntity(finalURL, Object.class);
		HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
		if (responseObject.containsKey("url") && responseObject.containsKey("exceptionMessage")){
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {
			SerializedClient matchingClient = new SerializedClient((String)responseObject.get("cnp"), (String)responseObject.get("name"), (String)responseObject.get("emailAddress"), (String)responseObject.get("postalAddress"));
			LOGGER.info("The submitted IP address, " + ipAddress + ", return the following info: " + matchingClient.toString());			 		
		}
	}	
	
	/* POST a new user in the system */
	@SuppressWarnings("unchecked")
	private static void createNewClient() {
		/* Print a start message for launching the verification */
		LOGGER.info("Testing registerNewClient method for a custom client...");
		/* Create the communication template object */
		RestTemplate registrationTemplate = new RestTemplate();
		/* Create the client to be registered */
		SerializedClient newClient = new SerializedClient("284910573642","Martina Hamsova","martina.hamsova@gmail.com","Lomnickeho 8, Prague 4 Pankrac");
		/* Create the URI required during the registration */
		String restURL = REST_SERVICE_URI + "/client/";		
		/* Create the HttpHeaders object for incorporating the method restrictions */
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptedMediaTypes);		
		/* Create the HttpEntity object for incorporating the headers and invoke the executed operation */
		HttpEntity<SerializedClient> newClientHeader = new HttpEntity<SerializedClient>(newClient, headers);
		ResponseEntity<?> serviceResponse = registrationTemplate.postForEntity(restURL, newClientHeader, Object.class, newClient.getCnp());
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
			LOGGER.info("The new client has been registered under the following location:" + registrationURI);
		}
	}
	
	/* PUT the new details of the user into the system */
	@SuppressWarnings("unchecked")
	private static void updateExistingClient(String clientCNP){
		/* Print another start message for launching the verification */
		LOGGER.info("Testing updateExistingClient for a previously registered client...");
		/* Create the template for update */
		RestTemplate updateTemplate = new RestTemplate();		
		/* Then create the new client used for update */
		//SerializedClient newClient = new SerializedClient(clientCNP, "Martina Hervirova", "martina.hervirova@gmail.com", "Lomnickeho 8, Prague 4 Pankrac");
		//SerializedClient newClient = new SerializedClient(clientCNP, "Martina Hervirova", "martina.hervirova", "Lomnickeho 8, Prague 4 Pankrac");
		SerializedClient newClient = new SerializedClient(clientCNP, "Martina Hervirova", "martina.hervirovagmail.com", "Lomnickeho 8, Prague 4 Pankrac");
		//SerializedClient newClient = new SerializedClient(clientCNP, "Martina Hervirova", "martina.hervirova@gmail.com", "Lomn");
		/* And finally also the URL where the request will be sent */
		String restURL = REST_SERVICE_URI + "/client/updateClient/" + newClient.getCnp() + "/";
		/* Create the HttpHeaders object for incorporating the method restrictions */
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptedMediaTypes);
		/* Create the HttpEntity object for incorporation of headers */
		HttpEntity<SerializedClient> updatedClientHeader = new HttpEntity<SerializedClient>(newClient, headers);
		/* Invoke the tested operation template and check for the result */
		ResponseEntity<?> serviceResponse = updateTemplate.exchange(URI.create(restURL), HttpMethod.PUT, updatedClientHeader, Object.class);
		/* Check for the returned response to see its status and analyze accordingly the results */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			/* In this case, an exception has been detected - its details shall follow */
			ExceptionInfo exInfo = new ExceptionInfo(restURL, (String)responseObject.get("exceptionMessage"), (Integer)responseObject.get("errorCode"), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during the method invocation!" + exInfo.toString());
		} else {
			HashMap<String, Object> originalClientData = (HashMap<String, Object>) serviceResponse.getBody();
			/* In this case, the initially passed client details should be returned */
			SerializedClient originalClient = new SerializedClient((String)originalClientData.get("cnp"), (String)originalClientData.get("name"), (String)originalClientData.get("emailAddress"), (String)originalClientData.get("postalAddress"));
			LOGGER.info("The client used for update is:" + originalClient);
		}
		//updateTemplate.put(restURL, newClient); simpler way, but unfortunately no response is returned for analysis
	}
	
	/* DELETE a selected user */
	@SuppressWarnings("unchecked")
	private static void removeExistingClient(String clientCNP){
		/* Print a message for checking the start of the operation */
		LOGGER.info("Testing removal of a previously registered client...");
		/* Create the URL and the REST template to be used during this operation */
		RestTemplate deleteTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/client/" + clientCNP + "/";
		/* Remove the user */
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
			/* The operation has been executed with successful outcome */
			LOGGER.info("The client with the CNP in question, " + clientCNP + ", has successfully been removed from the application database.");
		}
		//deleteTemplate.delete(restURL); simpler way, but unfortunately no result is returned for analysis
	}
	
	public static void main(String[] args) throws URISyntaxException, JsonProcessingException {
		/* List all clients from system */
		//listAllClients(); //Worked
		/* Search for a list of clients based on a sequence from its name */
		//getClientsForNamePattern("eiss"); //Worked
		//getClientsForNamePattern("ar"); //Worked
		//getClientsForNamePattern("ax"); //Worked
		/* Search for a client based on CNP */
		//getClientForCNP("2680915996ab"); //Worked
		//getClientForCNP("26809159964"); //Worked
        //getClientForCNP("268091599642"); //Worked        
        //getClientForCNP("2680915996421"); //Worked
        //getClientForCNP("268091599641"); //Worked
        /* Search for the client based on the IP address assigned to him/her */
        //getClientForIPAddress("150.90.72.81"); //Worked
        //getClientForIPAddress("150.90.72.82"); //Worked
        //getClientForIPAddress("150.90.72.a"); //Worked
        /* Search for a list of clients based on a sequence from its e-mail address */
        //getClientsForEmailFragment("ar"); //Worked
        //getClientsForEmailFragment("tth"); //Worked
        //getClientsForEmailFragment("ax"); //Worked
        /* Register a new client in the application */
        //createNewClient();//corrected today - Worked
		/* Update an existing client in the application */
		updateExistingClient("284910573642"); //Worked with refactoring
		//updateExistingClient("284910573643");
		/* Remove an existing client from the application */
		//removeExistingClient("284910573642"); //Worked with refactoring
		//removeExistingClient("284910573643"); //Worked with refactoring
	}
}