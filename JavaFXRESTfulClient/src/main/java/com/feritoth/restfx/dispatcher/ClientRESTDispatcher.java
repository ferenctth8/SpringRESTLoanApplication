package com.feritoth.restfx.dispatcher;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.utilities.EmailConfirmationSender;
import com.feritoth.restfx.utilities.ExceptionInfo;

public class ClientRESTDispatcher {

	/* The link where the underlying web application is accessed */
	public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	/* The logger associated to the current client - used during communication with the web application */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRESTDispatcher.class);
	/* The static singleton reference for the REST template element + the name of the class used for instantiation */
	private static RestTemplate restTemplate = getInstance();
	private static final String REST_TEMPLATE_LOCATION = "org.springframework.web.client.RestTemplate";

	/* The dispatcher method for returning all registered customers - GET all users */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<SerializedClient> getAllRegisteredClients() {
		/* Print a start message before launching the given operation */
		LOGGER.info("Testing listAllClients RESTful GET method...");
		/* Create the link to be used */
		String restURL = REST_SERVICE_URI + "/client/";
		/* Fetch and process the response coming from the invoked REST method - check its status code */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(restURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* For potential clients found, return them inside a list */
			List<HashMap<String, Object>> allClientMaps = serviceResponse.getBody();
			return convertHashMapListToClients(allClientMaps);
		} else {
			/* Return an empty collection */
			return Collections.emptyList();
		}
	}

	/* The dispatcher method required for registering new clients - POST a new user into the database */
	@SuppressWarnings("unchecked")
	public static Object registerNewClient(SerializedClient newClient){
		/* Print a start message before launching the given operation */
		LOGGER.info("Testing registerNewClient RESTful POST method...");
		/* Create the URI required during the registration */
		String restURL = REST_SERVICE_URI + "/client/";
		/* Create the HttpHeaders object used for incorporating the method restrictions */
		HttpHeaders registrationHeaders = new HttpHeaders();
		registrationHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		registrationHeaders.setAccept(acceptedMediaTypes);
		/* Create the HttpEntity object for incorporating the created headers and invoke the requested operation */
		HttpEntity<SerializedClient> newClientHeader = new HttpEntity<>(newClient, registrationHeaders);
		ResponseEntity<?> serviceResponse = restTemplate.postForEntity(restURL, newClientHeader, Object.class, newClient.getCnp());
		/* Check the returned response - examine its status and act accordingly based on it */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, process accordingly the retrieved exception */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* In this case, return a message with the newly created location for the inserted client */
			String responseObject = (String) serviceResponse.getBody();
			URI registrationURI = URI.create(responseObject);
			LOGGER.info("The new client has been registered under the following location:" + REST_SERVICE_URI + registrationURI);
			/* Before the effective return, however, send an e-mail using the given information */
			String firstMessagePart = newClient.toString() + "\n";
			String secondMessagePart = "Dear user,\n " + "Your profile can now be found at the following location:" + REST_SERVICE_URI + registrationURI;
			EmailConfirmationSender.sendConfirmationEmail("New Successful Client Registration", secondMessagePart + "\n" + firstMessagePart);
			/* Return the given URI at the end of the operation */
			return registrationURI;
		}
	}

	/* The dispatcher method required for updating a selected client - PUT the new details of a client */
	@SuppressWarnings("unchecked")
	public static Object updateSelectedClient(String cnp, SerializedClient selectedClient) {
		/* Print a start message before launching the given operation */
		LOGGER.info("Testing updateSelectedClient RESTful PUT method...");
		/* Create the URI required to access the given operation */
		String restURL = REST_SERVICE_URI + "/client/updateClient/" + selectedClient.getCnp() + "/";
		/* Create next the HttpHeaders object required for incorporating the parameters */
		HttpHeaders updateHeaders = new HttpHeaders();
		updateHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		updateHeaders.setAccept(acceptedMediaTypes);
		/* Create next the HttpEntity object for incorporating the headers and invoke the requested operation */
		HttpEntity<SerializedClient> updatedClientHeader = new HttpEntity<>(selectedClient, updateHeaders);
		ResponseEntity<?> serviceResponse = restTemplate.exchange(URI.create(restURL), HttpMethod.PUT, updatedClientHeader, Object.class);
		/* Check the status code of the returned response and act accordingly */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, process accordingly the retrieved exception */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* For this case the new client details should be displayed */
			HashMap<String, Object> responseClientData = (HashMap<String, Object>) serviceResponse.getBody();
            SerializedClient responseClient = new SerializedClient((String)responseClientData.get("cnp"), (String)responseClientData.get("name"), (String)responseClientData.get("emailAddress"), (String)responseClientData.get("postalAddress"));
            /* Send a confirmation e-mail about the new client details */
            String firstMessageHalf = "Dear " + selectedClient.getName() + ",\n";
            String secondMessageHalf = "This is a confirmation e-mail for your new contact details. They are listed as follows:\n" + responseClient.toString();
            EmailConfirmationSender.sendConfirmationEmail("Client Profile Update", firstMessageHalf + secondMessageHalf);
            /* Finally return the newly created client */
            return responseClient;
		}
	}

	/* The dispatcher method required for removing a client - DELETE an existing user from the database */
	@SuppressWarnings("unchecked")
	public static Object removeExistingClient(String clientPIN){
		/* Print a message before the start of the selected operation */
		LOGGER.info("Testing removeExistingClient RESTful DELETE method...");
		/* Create the URI required for invoking the selected operation */
		String restURL = REST_SERVICE_URI + "/client/" + clientPIN + "/";
		/* Invoke the given operation */
		ResponseEntity<?> serviceResponse = restTemplate.exchange(URI.create(restURL), HttpMethod.DELETE, null, Object.class);
		/* Check the status of the returned response and act accordingly based upon it */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, process accordingly the retrieved exception */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* The operation has been executed with a successful outcome */
			return "The client with PIN " + clientPIN + " and its associated history have all been successfully cleared from the application!";
		}
	}

	/* The dispatcher method for client filtering - GET the matching users from the database */
	@SuppressWarnings("rawtypes")
	public static List<SerializedClient> filterClients(String filterParameter, String comboSearchOption) {
		/* Print a message before starting the chosen operations */
		LOGGER.info("Testing the filterClients RESTful GET method...");
		if (StringUtils.isBlank(comboSearchOption) || StringUtils.isBlank(filterParameter)){
			/* By default, return an empty list for no matching options */
			return new ArrayList<SerializedClient>();
		}
		/* Check next which search option has been activated */
		switch(comboSearchOption){
		case "Filter By Name":
			/* Create the URL to be used for invoking the selected operation */
			String nameURLTemplate = REST_SERVICE_URI + "/client/matchNameSequence/" + filterParameter + "/";
			/* Next invoke the selected operation */
			ResponseEntity<List> nameServiceResponse = restTemplate.getForEntity(nameURLTemplate, List.class);
			/* Convert the response of the filter and return it in the end */
			return convertFilterResponses(nameServiceResponse, null);
		case "Filter By Email":
			/* Create the URL to be used for invoking the selected operation */
			String emailURLTemplate = REST_SERVICE_URI + "/client/matchEmailFragment/" + filterParameter + "/";
			/* Next invoke the selected operation */
			ResponseEntity<List> emailServiceResponse = restTemplate.getForEntity(emailURLTemplate, List.class);
			/* Convert the response of the filter and return it in the end */
			return convertFilterResponses(null, emailServiceResponse);
		case "Filter By Both":
			/* Create both URLs to invoke the operations */
			nameURLTemplate = REST_SERVICE_URI + "/client/matchNameSequence/" + filterParameter + "/";
			emailURLTemplate = REST_SERVICE_URI + "/client/matchEmailFragment/" + filterParameter + "/";
			/* Invoke the operations in question next */
			nameServiceResponse = restTemplate.getForEntity(nameURLTemplate, List.class);
			emailServiceResponse = restTemplate.getForEntity(emailURLTemplate, List.class);
			/* Convert the invocation results and return them */
			return convertFilterResponses(nameServiceResponse, emailServiceResponse);
		}

		/* By default, return an empty list for no matching options */
		return new ArrayList<SerializedClient>();
	}

	/* The dispatcher method for client profile retrieval - GET the details associated to a selected user from the database */
	@SuppressWarnings("unchecked")
	public static Object getDetailsForSelectedClient(String selectedPINcode) {
		/* Print a start message before launching the given operation */
		LOGGER.info("Testing the getDetailsForSelectedClient RESTful GET method...");
		/* Create the template required for the invocation of the selected operation */
		String profileURLTemplate = REST_SERVICE_URI + "/client/matchID/" + selectedPINcode + "/";
		/* Get the response from the invocation of the selected operation */
		ResponseEntity<Object> profileServiceResponse = restTemplate.getForEntity(profileURLTemplate, Object.class);
		HashMap<String, Object> responseMap = (HashMap<String, Object>) profileServiceResponse.getBody();
		if (responseMap.containsKey("url") && responseMap.containsKey("exceptionMessage")){
			ExceptionInfo exInfo = new ExceptionInfo((String)responseMap.get("url"), (String)responseMap.get("exceptionMessage"), (Integer)(responseMap.get("errorCode")), HttpStatus.valueOf((String)(responseMap.get("httpOperationStatus"))));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			SerializedClient sc = new SerializedClient((String)responseMap.get("cnp"), (String)responseMap.get("name"), (String)responseMap.get("emailAddress"), (String)responseMap.get("postalAddress"));
			return sc;
		}
	}

	/* The response converter method used for transforming the responses obtained from the filters */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<SerializedClient> convertFilterResponses(ResponseEntity<List> nameServiceResponse, ResponseEntity<List> emailServiceResponse) {
		/* Declare here the two lists to be returned */
		List<SerializedClient> namedClients = null;
		List<SerializedClient> emailedClients = null;
		/* Create the place where the results  */
		List<SerializedClient> finalResult = new ArrayList<>();
		/* Check next if each of the given answers is not null before effectively converting them */
		/* For answer one */
		if (nameServiceResponse != null){
			/* Examine its status and act accordingly */
			if (nameServiceResponse.getStatusCode() == HttpStatus.OK){
				/* For available content, perform the necessary conversion */
				List<HashMap<String, Object>> matchingClientMaps = nameServiceResponse.getBody();
				namedClients = convertHashMapListToClients(matchingClientMaps);
			}
		}

		/* For answer two */
		if (emailServiceResponse != null){
			/* Examine its status and act accordingly */
			if (emailServiceResponse.getStatusCode() == HttpStatus.OK){
				/* For available content, perform the necessary conversion */
				List<HashMap<String, Object>> matchingClientMaps = emailServiceResponse.getBody();
				emailedClients = convertHashMapListToClients(matchingClientMaps);
			}
		}

		/* Extra step before the final return: for both answers being not null, eliminate the duplicates */
		if (nameServiceResponse != null && emailServiceResponse != null){
			if (nameServiceResponse.getStatusCode() == HttpStatus.OK && emailServiceResponse.getStatusCode() == HttpStatus.OK){
				/* Remove the duplicates from the given list */
				TreeMap<String, SerializedClient> filteredClientMap = new TreeMap<>();
				for (SerializedClient client : namedClients){
					filteredClientMap.putIfAbsent(client.getCnp(), client);
				}
				for (SerializedClient client : emailedClients){
					filteredClientMap.putIfAbsent(client.getCnp(), client);
				}
				/* Return the values from the given tree map */
				finalResult.addAll(filteredClientMap.values());
				return finalResult;
			}
		}

		/* Type safety check for null values in case of both lists to be avoided before addition */
		if (namedClients != null){
			finalResult.addAll(namedClients);
		}
		if (emailedClients != null){
			finalResult.addAll(emailedClients);
		}
		return finalResult;
	}

	/* The auxiliary data converter method */
	private static List<SerializedClient> convertHashMapListToClients(List<HashMap<String, Object>> allClientMaps) {
		/* Create the list to be returned */
		List<SerializedClient> allClients = new ArrayList<>();
		/* Then populate it accordingly */
		for (HashMap<String, Object> clientMap : allClientMaps){
			SerializedClient registeredClient = new SerializedClient((String)clientMap.get("cnp"), (String)clientMap.get("name"), (String)clientMap.get("emailAddress"), (String)clientMap.get("postalAddress"));
			allClients.add(registeredClient);
		}
		/* Finally return the populated list */
		return allClients;
	}

	/* The singleton reference instantiator method */
	private static RestTemplate getInstance() {
        /* Instantiate the reference if needed */
		try {
			/* Check if the reference is not null - then instantiate it*/
			if (restTemplate == null) {
				Class<?> restTemplateFactory = Class.forName(REST_TEMPLATE_LOCATION);
				restTemplate = (RestTemplate) restTemplateFactory.newInstance();
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			/* For any exception occurring, just print a relevant log message and throw a runtime exception */
			LOGGER.error("Unable to get the singleton due to the following problem:" + e.getMessage());
			throw new RuntimeException(e);
		}
        /* Return the reference in case of no or successful instantiation */
		return restTemplate;
	}

}