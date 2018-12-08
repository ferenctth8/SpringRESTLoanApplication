package com.feritoth.cla.springmvc.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.feritoth.cla.springmvc.controller.exception.ExceptionInfo;
import com.feritoth.cla.springmvc.controller.utility.IOFormatter;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.jsonmodel.SerializedIPAddress;
import com.feritoth.cla.springmvc.service.ClientService;
import com.feritoth.cla.springmvc.service.IPAddressService;

@RestController
public class IPAddressRestController {
	
	/* Declare here the manipulation service references for the current class: for the client and the IP address */
	@Autowired
	private ClientService clientService;
	@Autowired
	private IPAddressService ipAddressService;
	
	/* Additionally declared resources: */
	/* The URL address of the server where the application is deployed - used for sending more user-friendly error messages */
	private static final String DEPLOYMENT_URL_ADDRESS = "http://localhost:8084/SecuredRESTClientLoanApplication";
	
	/* The logger reference used for tracing operation outcome */
	private static final Logger LOGGER = LoggerFactory.getLogger(IPAddressRestController.class);
	
	/* The list of constants depicting the errors which may occur during the introduction or removal of an IP address */
	private static final String[] ALL_POSSIBLE_ERRORS = {"faultyIPAddressValue", "unassignedIPAddress", "reservedIPAddress"};

    //-------------------------------------Fetch all the registered IP addresses-----------------------------------
	@RequestMapping(value = "/ipAddress/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedIPAddress>> listAllRegisteredIPs(){
		List<IPAddress> allRegisteredIPAddresses = ipAddressService.fetchAllAvailableIPAddresses();
		if (allRegisteredIPAddresses.isEmpty()){
			return new ResponseEntity<List<SerializedIPAddress>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<SerializedIPAddress>>(IOFormatter.convertEntityToDTOforIPAddressList(allRegisteredIPAddresses), HttpStatus.OK);
	}
	
	//-------------------------------------Fetch all the registered IP addresses assigned to a client-----------------------------------
	@RequestMapping(value = "/ipAddress/matchClient/{clientCNP}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedIPAddress>> listAllIPsForClient(@PathVariable("clientCNP") String clientCNP){
		/* First check if the CNP in question matches any registered client inside the DB */
		Client matchingClient = clientService.findClientByCNP(clientCNP);		
		/* For no match found, return a no-content response */
		if (matchingClient == null){
			return new ResponseEntity<List<SerializedIPAddress>>(HttpStatus.NO_CONTENT);
		}
		/* Otherwise - A potential match has been found, proceed with the next step: fetch all the IP addresses assigned to a client */
		List<IPAddress> allAssignedIPs = ipAddressService.fetchAllAssignedIPAddressesForClient(matchingClient);
		/* Finally return the result */
		return new ResponseEntity<List<SerializedIPAddress>>(IOFormatter.convertEntityToDTOforIPAddressList(allAssignedIPs), HttpStatus.OK);
	}
	
	//-------------------------------------Fetch all the details about one IP address -----------------------------------
	@RequestMapping(value = "/ipAddress/matchIPvalue/{ipValue}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> findDetailsForIPAddressValue(@PathVariable("ipValue") String ipAddressValue){
		/* Prepare the template for the error reception */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/ipAddress/matchIPvalue/" + ipAddressValue + "/";
		/* First check the well-formedness of the IP address before proceeding */
		if (!ipAddressService.validateIPAddress(ipAddressValue)){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NumberFormatException("The provided IP address was identified as faulty, therefore it will not be used for searching"), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NumberFormatException("The provided IP address was identified as faulty, therefore it will not be used for searching"), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* If the validation was successful, proceed with the search next */
		IPAddress matchingIPAddress = ipAddressService.findDetailsForIPvalue(ipAddressValue);
		/* Now to see if the given IP address was or not registered and proceed accordingly */
		/* Null value: return of a suitable error message */
		if (matchingIPAddress == null){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted IP address, " + ipAddressValue + ", was detected as well-formed but it has not been assigned to any registered client currently!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted IP address, " + ipAddressValue + ", was detected as well-formed but it has not been assigned to any registered client currently!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* Straight-forward case: return the found details */
		return new ResponseEntity<SerializedIPAddress>(IOFormatter.convertSingleIPEntityToDTO(matchingIPAddress), HttpStatus.OK);
    }
	
	//-------------------------------------Register a new IP address----------------------------------------
	@RequestMapping(value = "/ipAddress/", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> registerNewIPAddressForAddress(@RequestBody SerializedIPAddress candidateIpAddress) throws JsonProcessingException{
		/* Create the link for the registration of the new IP address */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/ipAddress/";
		LOGGER.info("Registering new IP address " + candidateIpAddress.getIpValue() + " for client " + candidateIpAddress.getOwnerClient().getName());
		/* Next call the registration method from the injected service - do not forget to convert the reference to the suitable type */
		ipAddressService.registerIPAddress(IOFormatter.convertDTOtoEntityForIPAddress(candidateIpAddress));
		/* Check if the map required for registering any exceptions during the operation execution has been filled with some content - main rule: first exception beats all others in terms of appearance */
		Map<String, Exception> allExceptionsMap = ipAddressService.getRegDelFlagMap();		
		/* Check if the key set of the map is not empty and then fetch out the existing elements */
		/* For each of the cases: print a suitable error message and map the generated exception into a user-friendly response for the client side for avoiding communication problems */
		if (!allExceptionsMap.keySet().isEmpty()){
			/* Check for faulty IP address */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[0])) {
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}			
			/* Check for already reserved IP address */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[2])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT), HttpStatus.OK);
			}
		}
		
		/* For everything in order, return the response confirming the created IP address status */
		HttpHeaders newIPAddressHeaders = new HttpHeaders();
		UriComponentsBuilder ccBuilder = UriComponentsBuilder.newInstance();
		newIPAddressHeaders.setLocation(ccBuilder.path("/ipAddress/matchIPvalue/{ipValue}/").buildAndExpand(candidateIpAddress.getIpValue()).toUri());		
		LOGGER.info("The headers for this operation are:" + newIPAddressHeaders.toString());
		return new ResponseEntity<String>(IOFormatter.convertHeadersToJSON(newIPAddressHeaders.getLocation()), HttpStatus.CREATED);
	}

	//-------------------------------------Remove a registered IP address-----------------------------------
	@RequestMapping(value = "/ipAddress/{ipValue}/", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> removeIPAddress(@PathVariable("ipValue") String ipValue){
		/* Create the mapping for the selected IP address */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/ipAddress/" + ipValue + "/";
		LOGGER.info("Performing removal for selected IP address with value " + ipValue + "...");
		/* Attempt the removal of the given address */
		ipAddressService.removeIPAddress(ipValue);
		/* Check the outcome of the given service operation execution */
		/* First, pick the error map from the service in question */
		Map<String, Exception> ipAddressProblemMap = ipAddressService.getRegDelFlagMap();
		/* Find the previously declared two error keys in the map and return a response based upon them */
		/* For the faulty IP address part */		
		if (ipAddressProblemMap.containsKey(ALL_POSSIBLE_ERRORS[0])){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, ipAddressProblemMap.get(ALL_POSSIBLE_ERRRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, ipAddressProblemMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* For the unregistered IP address part */
		if (ipAddressProblemMap.containsKey(ALL_POSSIBLE_ERRORS[1])){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, ipAddressProblemMap.get(ALL_POSSIBLE_ERRRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, ipAddressProblemMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* For the straight-forward case: just return the response with status NO_CONTENT */
		return new ResponseEntity<SerializedIPAddress>(HttpStatus.NO_CONTENT);
	}
}