package com.feritoth.restfx.dispatcher;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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

import com.feritoth.restfx.core.SerializedIPAddress;
import com.feritoth.restfx.utilities.EmailConfirmationSender;
import com.feritoth.restfx.utilities.ExceptionInfo;

public class IPAddressRESTDispatcher {

	/* The link where the underlying web application is accessed */
	public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	/* The logger associated to the current client - used during communication with the web application */
	private static final Logger LOGGER = LoggerFactory.getLogger(IPAddressRESTDispatcher.class);
	/* The static singleton reference for the REST template element + the name of the class used for instantiation */
	private static RestTemplate restTemplate = getInstance();
	private static final String REST_TEMPLATE_LOCATION = "org.springframework.web.client.RestTemplate";

	/* The dispatcher method for fetching all the IP addresses related to a particular client - GET all IPs registered for a particular client */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<SerializedIPAddress> getAllIPAddressesAssignedToClient(String clientPINcode){
		/* Print a start message before the effective invocation of the method */
		LOGGER.info("Testing the getAllIPAddressesAssignedToClient RESTful GET method...");
		/* Create the address required for the invocation of the selected operation */
		String addressCollectionURL = REST_SERVICE_URI + "/ipAddress/matchClient/" + clientPINcode + "/";
		/* Fetch and process the response coming from the invocation of the given operation - check of course its status code first */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(addressCollectionURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* For potential IPs found, return them inside a list */
			List<HashMap<String, Object>> allRegisteredIPs = serviceResponse.getBody();
			return convertHashMapListToIPAddresses(allRegisteredIPs);
		} else {
			/* Return an empty collection */
			return Collections.emptyList();
		}
	}

	/* The dispatcher method for registering a new IP address for a particular client - POST a new IP address for a particular client */
	@SuppressWarnings("unchecked")
	public static Object registerNewIPAddress(SerializedIPAddress newIPaddress) {
		/* Print a start message before invoking the given operation */
		LOGGER.info("Testing the registerNewIPAddress RESTful POST method...");
		/* Create next the URI required for invoking the given operation */
		String addressRegistrationURL = REST_SERVICE_URI + "/ipAddress/";
		/* Create then the necessary headers for incorporating the method restrictions on consumed and produced media types */
		HttpHeaders registrationHeaders = new HttpHeaders();
		registrationHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		registrationHeaders.setAccept(acceptedMediaTypes);
		/* Create afterwards the HttpEntity object for incorporating previously generated headers */
		HttpEntity<SerializedIPAddress> newIPaddressHeader = new HttpEntity<>(newIPaddress, registrationHeaders);
		/* Invoke the given operation */
		ResponseEntity<?> serviceResponse = restTemplate.postForEntity(addressRegistrationURL, newIPaddressHeader, Object.class, newIPaddress.getIpValue());
		/* Check the status code of the given operation */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, process accordingly the retrieved exception */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo((String)responseObject.get("url"), (String)responseObject.get("exceptionMessage"), (Integer)(responseObject.get("errorCode")), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* In this case, return a message with the newly created location for the inserted IP address */
			String responseObject = (String) serviceResponse.getBody();
			URI registrationURI = URI.create(responseObject);
			LOGGER.info("The new IP address has been registered under the following location:" + REST_SERVICE_URI + registrationURI);
			/* Send the e-mail confirming the registration of the given IP address */
			String firstMessageSection = newIPaddress + "\n";
			String secondMessageSection = "Dear " + newIPaddress.getOwnerClient().getName() + ",\n You have successfully registered a new IP address under the following location:" + REST_SERVICE_URI + registrationURI;
			EmailConfirmationSender.sendConfirmationEmail("New IP address registration", secondMessageSection + "\n" + firstMessageSection);
			/* Return the given URI at the end of the operation */
			return registrationURI;
		}
	}

	/* The dispatcher method for removing a selected IP address from the system - DELETE a particular IP address */
	@SuppressWarnings("unchecked")
	public static Object removeSelectedIPAddress(String ipValue) {
		/* Print a start message before effectively invoking the given operation */
		LOGGER.info("Testing the removeSelectedIPAddress RESTful DELETE method...");
		/* Create the URL for invoking the given operation */
		String addressRemovalURL = REST_SERVICE_URI + "/ipAddress/" + ipValue + "/";
		/* Now invoke the given operation and examine the returned result */
		ResponseEntity<?> serviceResponse = restTemplate.exchange(URI.create(addressRemovalURL), HttpMethod.DELETE, null, Object.class);
		/* For result invocation - check the status code of the given operation */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* This means that an error has been detected during the invocation of the given operation */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo(addressRemovalURL, (String)responseObject.get("exceptionMessage"), (Integer)responseObject.get("errorCode"), HttpStatus.valueOf((String) responseObject.get("httpStatus")));
			LOGGER.warn("Exception has been detected during the method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* For invocation ending successfully, just return a message of successful termination */
			return "The given IP address, " + ipValue +  ", has been successfully removed from the application!";
		}
	}

	/* The auxiliary data converter method */
	private static List<SerializedIPAddress> convertHashMapListToIPAddresses(List<HashMap<String, Object>> allRegisteredIPs) {
		/* Create the list to be returned */
		List<SerializedIPAddress> allAssignedIPs = new ArrayList<>();
		/* Then populate it accordingly */
		for (HashMap<String, Object> ipAddressMap : allRegisteredIPs){
			SerializedIPAddress registeredIPAddress = new SerializedIPAddress((String)ipAddressMap.get("ipValue"), (Integer)ipAddressMap.get("ipID"));
			allAssignedIPs.add(registeredIPAddress);
		}
		/* Finally return the populated list */
		return allAssignedIPs;
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