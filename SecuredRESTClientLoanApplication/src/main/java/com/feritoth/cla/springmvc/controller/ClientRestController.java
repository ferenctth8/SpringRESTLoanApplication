package com.feritoth.cla.springmvc.controller;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.feritoth.cla.springmvc.jsonmodel.SerializedClient;
import com.feritoth.cla.springmvc.service.ClientService;
import com.feritoth.cla.springmvc.service.IPAddressService;

@RestController
public class ClientRestController {
	
	/* Declare here the manipulation service references used in this class: for the client and the IP */
	@Autowired
	private ClientService clientService; 
	@Autowired
	private IPAddressService ipAddressService;
	
	/* Additionally declared resources: */	
	/* The URL address of the server where the application is deployed - used for sending more user-friendly error messages */
	private static final String DEPLOYMENT_URL_ADDRESS = "http://localhost:8084/SecuredRESTClientLoanApplication";
	
	/* The logger reference used for tracing operation outcome */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRestController.class);
	
	/* The list of constants used for mapping any exceptions that can occur during update or registration */
	private static final String[] ALL_POSSIBLE_ERRORS = {"faultyEmailAddress", "faultyName", "faultyPostalAddress"};	
		
	//------------------------Fetch all the registered clients---------------------------------
	@RequestMapping(value="/client/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})	
	public ResponseEntity<List<SerializedClient>> listAllClients(){
		List<Client> allRegisteredClients = clientService.findAllRegisteredClients();		
		if (allRegisteredClients.isEmpty()){
			//return ResponseEntity.noContent().build();
			return new ResponseEntity<List<SerializedClient>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<SerializedClient>>(IOFormatter.convertEntityToDTOforClientList(allRegisteredClients), HttpStatus.OK);
	}	
	
	//------------------------Fetch clients matching a potential name sequence-----------------
	@RequestMapping(value="/client/matchNameSequence/{nameSequence}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedClient>> findAllMatchingClientsByName(@PathVariable("nameSequence") String nameSequence){
		List<Client> allMatchingClients = clientService.getMatchingClients(nameSequence);
		if (allMatchingClients.isEmpty()){
			return new ResponseEntity<List<SerializedClient>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<SerializedClient>>(IOFormatter.convertEntityToDTOforClientList(allMatchingClients), HttpStatus.OK);
	}
	
	//------------------------Fetch a client based on its CNP (registration ID)----------------
	@RequestMapping(value="/client/matchID/{cnp}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> findClientWithCNP(@PathVariable("cnp") String cnp) {
		/* Check the well-formedness of the CNP before search submission - for any errors, stop the search and return with an immediate error message */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/client/matchID/" + cnp + "/";
		if (cnp.length() != 12){
			LOGGER.error("CNP of invalid length provided to the application!", cnp);
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("Number of provided characters for this field is different from the required limit!")), HttpStatus.LENGTH_REQUIRED);			
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("Number of provided characters for this field is different from the required limit!"), HttpStatus.LENGTH_REQUIRED.value(), HttpStatus.LENGTH_REQUIRED), HttpStatus.OK);			
		}
		if (!StringUtils.isNumeric(cnp)){
			LOGGER.error("CNP of invalid pattern provided to the application!", cnp);
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("Invalid characters detected in the submitted CNP value - search thus still cannot be performed!")), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("Invalid characters detected in the submitted CNP value - search thus still cannot be performed!"), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* In case of everything being in order, perform the search and return the result */
		Client matchingClient = clientService.findClientByCNP(cnp);		
		/* For no client found in the DB for the submitted CNP, just return a response with a suitable error message */
		if (matchingClient == null){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted value for CNP, " + cnp + ", was detected as well-formed for the search but no matches were currently found for it in our database!")), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted value for CNP, " + cnp + ", was detected as well-formed for the search but no matches were currently found for it in our database!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* The straight-forward case: the search was successful and returned a matching client */
		return new ResponseEntity<SerializedClient>(IOFormatter.convertSingleClientEntityToDTO(matchingClient), HttpStatus.OK);
	}
	
	//------------------------Fetch a client by an IP address----------------------------------
	@RequestMapping(value = "/client/matchIPAddress/{ipAddress}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> findClientWithIP(@PathVariable("ipAddress")String ipAddress) throws NumberFormatException, NullPointerException {
		/* Check the well-formedness of the IP address before submission */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/client/matchIPAddress/" + ipAddress + "/";
		if (!ipAddressService.validateIPAddress(ipAddress)){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NumberFormatException("The provided IP address was identified as faulty, therefore it will not be used for searching"), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NumberFormatException("The provided IP address was identified as faulty, therefore it will not be used for searching"), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* In case of everything being in order, proceed with the search and return the given result */
		Client matchingClient = clientService.findClientByIP(ipAddress);		
		/* For no match, return with another suitable error message */
		if (matchingClient == null){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted IP address, " + ipAddress + ", was detected as well-formed but it has not been assigned to any registered client currently!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted IP address, " + ipAddress + ", was detected as well-formed but it has not been assigned to any registered client currently!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
	    }
		/* The straight-forward case: the search went successfully and returned a matching client */
		return new ResponseEntity<SerializedClient>(IOFormatter.convertSingleClientEntityToDTO(matchingClient), HttpStatus.OK);
	}
	
	//------------------------Fetch all clients with a matching email address fragment---------
	@RequestMapping(value = "/client/matchEmailFragment/{emailAddressFragment}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedClient>> findAllMatchingClientsByEmail(@PathVariable("emailAddressFragment")String emailAddressFragment){ 
		List<Client> allMatchingClients = clientService.findClientsByEmailFragment(emailAddressFragment);
		if (allMatchingClients.isEmpty()){
			return new ResponseEntity<List<SerializedClient>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<SerializedClient>>(IOFormatter.convertEntityToDTOforClientList(allMatchingClients), HttpStatus.OK);
	}
	
	//------------------------Register a new client--------------------------------------------
	@RequestMapping(value = "/client/", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> createClient(@RequestBody SerializedClient candidateClient) throws JsonProcessingException{
		/* Create the mapping for the registration of the new client */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/client/";
		LOGGER.info("Creating new client " + candidateClient.getName() + "...");
		/* Start by checking the content well-formedness of the given client identifier */
		if (!IOFormatter.validateClientCNPcontent(candidateClient.getCnp())){
			/* Print a relevant error message for this case */
			LOGGER.error("CNP with invalid content provided to the application! " + candidateClient.getCnp());
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question has an invalid content format! " + candidateClient.getCnp()), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question has an invalid content format! " + candidateClient.getCnp()), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
		}
		/* Afterwards, check the length of the given CNP - return again a suitable error message */
		if (!IOFormatter.validateClientCNPlength(candidateClient.getCnp())){
			/* Print a relevant error message for this case too */
			LOGGER.error("CNP with invalid length provided to the application! " + candidateClient.getCnp());
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question has an invalid length! " + candidateClient.getCnp()), HttpStatus.LENGTH_REQUIRED.value(), HttpStatus.LENGTH_REQUIRED), HttpStatus.LENGTH_REQUIRED);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question has an invalid length! " + candidateClient.getCnp()), HttpStatus.LENGTH_REQUIRED.value(), HttpStatus.LENGTH_REQUIRED), HttpStatus.OK);
		}
		/* Check next if the CNP of the given client has been assigned to anyone else already - return a corresponding error message */
		if (clientService.isEmployeeCNPalreadyAssigned(candidateClient.getCnp())){
			/* Print another relevant error message for this case */
			LOGGER.error("The CNP in question is well-formed, but has already been assigned to another client!", candidateClient.getCnp());
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question is already in use by another client! " + candidateClient.getCnp()), HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT), HttpStatus.CONFLICT);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("CNP in question is already in use by another client! " + candidateClient.getCnp()), HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT), HttpStatus.OK);
		}
		/* Call the registration method from the service */
		clientService.registerNewClient(IOFormatter.convertSerializedClientDTOtoEntity(candidateClient));
		/* Get next the map of errors and see which of them has been registered - main rule: first error beats all others in order of appearance */
		Map<String, Exception> allExceptionsMap = clientService.getRegUpFlagMap();
		/* Check if the key set of the map is not empty and then fetch out the existing elements */
		/* For each of the cases: print a suitable error message and map the generated exception into a user-friendly response for the client side for avoiding communication problems */
		if (!allExceptionsMap.keySet().isEmpty()){
			/* Check for faulty e-mail */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[0])) {
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
			/* Check for faulty name */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[1])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
			/* Check for faulty address */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[2])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
		}
		
		/* For everything in order, return the response confirming the created user status */
		HttpHeaders newClientHeaders = new HttpHeaders();
		UriComponentsBuilder ccBuilder = UriComponentsBuilder.newInstance();
		newClientHeaders.setLocation(ccBuilder.path("/client/matchID/{cnp}/").buildAndExpand(candidateClient.getCnp()).toUri());		
		LOGGER.info("The headers for this operation are:" + newClientHeaders.toString());
		return new ResponseEntity<String>(IOFormatter.convertHeadersToJSON(newClientHeaders.getLocation()), HttpStatus.CREATED);
	}
	
	//------------------------Update details of a client---------------------------------------
	@RequestMapping(value="/client/updateClient/{cnp}/", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> updateClient(@PathVariable("cnp") String clientCNP, @RequestBody SerializedClient selectedClient){
		/* Create the mapping for the update of the selected client */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/client/updateClient/" + clientCNP + "/";
		LOGGER.info("Performing update for the selected client with name = " + selectedClient.getName() + "...");
		/* Find the client bearing the CNP but the old features */
		Client initialClient = clientService.findClientByCNP(clientCNP);
		/* If client cannot be found, return a response with NOT_FOUND status */
		if (initialClient == null){
			/* Print a relevant error message for this case as well */
			LOGGER.error(selectedClient.toString() + " was not found in the database unfortunately...");
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException(selectedClient.toString() + " was not found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException(selectedClient.toString() + " was not found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* Otherwise, proceed with the update - call the update method and check for exceptions */
		clientService.updateExistingClient(IOFormatter.convertSerializedClientDTOtoEntity(selectedClient));
		/* Get next the map of errors and see which of them has been registered - main rule: first error beats all others in order of appearance */
		Map<String, Exception> allExceptionsMap = clientService.getRegUpFlagMap();
		/* Check if the key set of the map is not empty and then fetch out the existing elements */
		/* For each of the cases: print a suitable error message and map the generated exception into a user-friendly response for the client side for avoiding communication problems */
		if (!allExceptionsMap.keySet().isEmpty()){
			/* Check for faulty e-mail */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[0])) {
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
			/* Check for faulty name */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[1])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
			/* Check for faulty address */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[2])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
	    }
		/* Return in the end the response of the update operation */
		return new ResponseEntity<SerializedClient>(selectedClient, HttpStatus.ACCEPTED);
	}
	
	//------------------------Remove a client--------------------------------------------------
	@RequestMapping(value="/client/{cnp}/", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> removeClient(@PathVariable("cnp") String clientCNP){
		/* Create the mapping for the removal of the selected client */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS +  "/client/" + clientCNP + "/";
		LOGGER.info("Performing removal of selected client with CNP " + clientCNP + "...");
		/* Find the client bearing the CNP but the old features */
		Client initialClient = clientService.findClientByCNP(clientCNP);
		/* If client cannot be found, return a response with NOT_FOUND status */
		if (initialClient == null){
			/* Print a relevant error message for this case as well */
			LOGGER.error(clientCNP + " was not found in the database unfortunately...");
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("No client with CNP " + clientCNP + " was found in the database unfortunately..."), 0, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("No client with CNP " + clientCNP + " was found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* Otherwise, proceed with the removal - call the deletion method and check for exceptions */
		clientService.removeClient(clientCNP);		
		/* Return in the end the response of the removal operation */
		return new ResponseEntity<SerializedClient>(HttpStatus.NO_CONTENT);
	}
}