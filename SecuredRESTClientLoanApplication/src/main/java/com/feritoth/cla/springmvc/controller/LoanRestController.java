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
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;
import com.feritoth.cla.springmvc.jsonmodel.SerializedLoan;
import com.feritoth.cla.springmvc.service.ClientService;
import com.feritoth.cla.springmvc.service.IPAddressService;
import com.feritoth.cla.springmvc.service.LoanService;

@RestController
public class LoanRestController {
	
	/* Declare here the injected service beans used for the invocation of the underlying service methods: for client, loan and IP address */
	@Autowired
	private LoanService loanService;
	@Autowired
	private IPAddressService ipAddressService;
	@Autowired
	private ClientService clientService;
	
	/* Additionally declared resources: */
	/* The URL address of the server where the application is deployed - used for sending more user-friendly error messages */
	private static final String DEPLOYMENT_URL_ADDRESS = "http://localhost:8084/SecuredRESTClientLoanApplication";
	
	/* The logger reference used for tracing operation outcome */
	private static final Logger LOGGER = LoggerFactory.getLogger(LoanRestController.class);
	
	/* The list of constants used for mapping all possible exceptions that may occur during loan registration and extension */
	private static final String[] ALL_POSSIBLE_ERRORS = {"faultyLoanDuration", "maximumExtensibilityReached", "maximumLoanNumberPerIPForDay", 
		                                                 "timePeriodLoanRisk", "faultyIPAddress", "negativeOrZeroLoanAmount", "maximumLoanAmountOverlapped"};
	
	//--------------------------------------------- Fetch all the loans from the DB --------------------------------------------------
	@RequestMapping(value = "/loan/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedLoan>> listAllRegisteredLoans(){
		List<Loan> allRegisteredLoans = loanService.fetchAllLoans();
		if (allRegisteredLoans.isEmpty()){
			return new ResponseEntity<List<SerializedLoan>>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<SerializedLoan>>(IOFormatter.convertEntityToDTOforLoanList(allRegisteredLoans), HttpStatus.OK);
	}
	
	//--------------------------------------------- Fetch all loans issued by a client -------------------------------------------------
	@RequestMapping(value = "/loan/matchClient/{clientCNP}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedLoan>> listAllLoansForClient(@PathVariable("clientCNP") String clientCNP){
		/* First check if the CNP in question matches any registered client inside the DB */
		Client matchingClient = clientService.findClientByCNP(clientCNP);		
		/* For no match found, return a no-content response */
		if (matchingClient == null){
			return new ResponseEntity<List<SerializedLoan>>(HttpStatus.NO_CONTENT);
		}
		/* Otherwise - A potential match has been found, proceed with the next step: fetch all the loans assigned to given client */
		List<Loan> allAssignedLoans = loanService.getAllLoansForClient(matchingClient);
		/* Finally return the result */
		return new ResponseEntity<List<SerializedLoan>>(IOFormatter.convertEntityToDTOforLoanList(allAssignedLoans), HttpStatus.OK);
	}
	
	//--------------------------------------------- Fetch all loans issued from one IP address ------------------------------------------
	@RequestMapping(value = "/loan/matchIPaddress/{IPvalue}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SerializedLoan>> listAllLoansFromIPAddress(@PathVariable("IPvalue") String ipAddressValue){
		/* First check if the IP address in question is registered in the DB */
		IPAddress matchingIPaddress = ipAddressService.findDetailsForIPvalue(ipAddressValue);
		/* For no match found, return a no-content response */
		if (matchingIPaddress == null){
			return new ResponseEntity<List<SerializedLoan>>(HttpStatus.NO_CONTENT);
		}
		/* Otherwise - A potential match has been found, thus proceed with the next step: fetch all the loans issued from the given IP address */
		List<Loan> allAssignedLoans = loanService.findAllLoansForIPAddress(matchingIPaddress);
		/* Finally return the result */
		return new ResponseEntity<List<SerializedLoan>>(IOFormatter.convertEntityToDTOforLoanList(allAssignedLoans), HttpStatus.OK);
	}
	
	//--------------------------------------------- Fetch the history of one particular loan (including its extensions) through its ID -----
	@RequestMapping(value = "/loan/displayLoanHistory/{loanID}/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> getDetailedHistoryOfLoan(@PathVariable("loanID") Integer loanID){
		/* Create the header to be returned in the case of an error */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/loan/displayLoanHistory/" + loanID + "/";
		/* Find the loan to be returned in the DB */
		Loan matchingLoan = loanService.fetchHistoryForLoanID(loanID);
		/* In case of a null loan, return a suitable error message */
		if (matchingLoan == null){
			//This response is mapped into an exception format that is returned under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted value for loan ID, " + loanID + ", was not currently found as registered in our database!")), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("The submitted value for loan ID, " + loanID + ", was not currently found as registered in our database!"), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* The straight-forward case: the loan was located in the database and can be returned */
		return new ResponseEntity<SerializedLoan>(IOFormatter.convertSingleLoanEntityToDTO(matchingLoan), HttpStatus.OK);
	}
	
	//--------------------------------------------- Register a loan and perform its associated risk analysis ----------------------------------------------------------------------
	@RequestMapping(value = "/loan/" , method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> registerNewLoan(@RequestBody SerializedLoan candidateLoan) throws JsonProcessingException{
		/* Create the header to be returned for the registration loan */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/loan/";
		LOGGER.info("Registering new loan on IP address " + candidateLoan.getIpAddress().getIpValue() + " for client " + candidateLoan.getIpAddress().getOwnerClient().getName());
		/* Afterwards, call the registration method from the injected service - also perform the candidate conversion from business object into relevant DB entity */
		loanService.registerNewLoan(IOFormatter.convertSingleLoanDTOToEntity(candidateLoan));
		/* Fetch back the map of exceptions from the service in question */
		Map<String, Exception> allExceptionsMap = loanService.getRegUpFlagMap();
		/* Examine the content of the map to see if there are any problems surrounding the given loan */
		/* For each of the given cases, print a suitable error message and map the resulting exception into a suitable response */
		if (!allExceptionsMap.keySet().isEmpty()){
			/* Check for the duration of the given loan first */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[0])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK); 
			}
			/* Check next for the correctness of the IP address in question */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[4])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[4]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[4]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[4]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK); 
			}
			/* Check afterwards for the correctness of the loaned amount */
			/* Case A: Negative value or 0 introduced as loan amount */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[5])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[5]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[5]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[5]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK); 
			}
			/* Case B: Loaned sum is greater than maximum allowed limit */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[6])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[6]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[6]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[6]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK); 
			}
			/* Finally, examine the result of the risk loan analysis and output the corresponding messages */
			/* Case A: the number of loans from a given IP address have been exceeded for the current day */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[2])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.PAYMENT_REQUIRED.value(), HttpStatus.PAYMENT_REQUIRED), HttpStatus.PAYMENT_REQUIRED);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[2]), HttpStatus.PAYMENT_REQUIRED.value(), HttpStatus.PAYMENT_REQUIRED), HttpStatus.OK); 
			}
			/* Case B: the loan is issued during the critical time period of the selected day and its amount reaches the maximum allowed value */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[3])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[3]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[3]), HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[3]), HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN), HttpStatus.OK); 
			}
		}
		
		/* For no errors encountered during the loan registration and in case of successful pass of the risk analysis, return the response confirming the created loan status */
		HttpHeaders newLoanHeaders = new HttpHeaders();
		UriComponentsBuilder ccBuilder = UriComponentsBuilder.newInstance();
		/* Set as ID of the newly inserted loan the total number of registered loans in the system */
		List<Loan> allLoans = loanService.fetchAllLoans();
		Integer loanID = allLoans.get(allLoans.size() - 1).getLoanID();
		newLoanHeaders.setLocation(ccBuilder.path("/loan/displayLoanHistory/{loanID}/").buildAndExpand(loanID).toUri());
		LOGGER.info("The header reference to the new created loan is:" + newLoanHeaders.toString());
		return new ResponseEntity<String>(IOFormatter.convertHeadersToJSON(newLoanHeaders.getLocation()), HttpStatus.CREATED);
	}
	
	//--------------------------------------------- Extend a loan based on its ID -------------------------------------------------------------------------------------------------
	@RequestMapping(value = "/loan/extendLoan/{loanID}/", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> extendLoan(@PathVariable("loanID") Integer loanID, @RequestBody SerializedLoan selectedLoan){
		/* Create the mapping for the extension of the loan based on its ID */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS + "/loan/extendLoan/" + loanID + "/";
		LOGGER.info("Performing extension for the chosen loan with ID = ?", loanID);
		/* Find the loan for which the extension is to be done */
		Loan initialLoan = loanService.fetchHistoryForLoanID(loanID);
		/* In case the loan in question does not exist return a response with a NOT_FOUND status */
		if (initialLoan == null){
			/* Print a relevant error message as well for this case */
			LOGGER.error(selectedLoan.toString() + " was not found in the database unfortunately...");
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException(selectedLoan.toString() + " was not found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("Loan with ID=" + loanID + " was not found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		if (!StringUtils.equalsIgnoreCase(selectedLoan.getIpAddress().getIpValue(), initialLoan.getIpAddress().getValue())){
			/* Print a relevant message for this case as well */
			LOGGER.error("Mismatch of IP addresses detected, aborting operation");
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new IllegalArgumentException("Loan with ID=" + loanID + " was registered with under a different IP address than the one supplied during update..."), HttpStatus.EXPECTATION_FAILED.value(), HttpStatus.EXPECTATION_FAILED), HttpStatus.OK);
		}
		/* Otherwise, proceed with the update - call the update method and check for any occurring exceptions */
		loanService.extendExistingLoan(IOFormatter.convertSingleLoanDTOToEntity(selectedLoan));
		/* Get next the map of errors and see which of them has been registered - again the rule of thumb is that first error beats all others in order of appearance */
		Map<String, Exception> allExceptionsMap = loanService.getRegUpFlagMap();
		/* Check if the key set of the map is not empty and fetch out the existing elements */
		/* In each of the cases in question: print a suitable error message and map the generated exception into a user-friendly response for the client side in order to avoid any problems */
		if (!allExceptionsMap.keySet().isEmpty()){
			/* Check for faulty loan duration */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[0])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[0]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.OK);
			}
			/* Check for extension exceed breach */
			if (allExceptionsMap.containsKey(ALL_POSSIBLE_ERRORS[1])){
				LOGGER.error(allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]).getMessage());
				//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
				return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, allExceptionsMap.get(ALL_POSSIBLE_ERRORS[1]), HttpStatus.PAYMENT_REQUIRED.value(), HttpStatus.PAYMENT_REQUIRED), HttpStatus.OK);
			}
		}
		/* Return in the end the response of the update operation */
		initialLoan = loanService.fetchHistoryForLoanID(loanID);
		return new ResponseEntity<SerializedLoan>(IOFormatter.convertSingleLoanEntityToDTO(initialLoan), HttpStatus.ACCEPTED);
	}
	
	//--------------------------------------------- Remove a loan based on its ID ----------------------------------------
	@RequestMapping(value = "/loan/{loanID}/", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> removeLoan(@PathVariable("loanID") Integer loanID){
		/* Create the mapping for the removal of the selected loan */
		String finalRequestMapping = DEPLOYMENT_URL_ADDRESS +  "/loan/" + loanID + "/";
		LOGGER.info("Performing removal of selected loan with ID " + loanID + "...");
		/* Find the client bearing the CNP but the old features */
		Loan initialLoan = loanService.fetchHistoryForLoanID(loanID);		
		/* If loan cannot be found, return a response with NOT_FOUND status */
		if (initialLoan == null){
			/* Print a relevant error message for this case as well */
			LOGGER.error("The loan with ID " + loanID + " was not found in the database unfortunately...");
			//Return the response for this case mapped into an exception format under the status code OK for avoiding the generation of exception on the client side during processing: 
			//return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("No loan with ID " + loanID + " was found in the database unfortunately..."), 0, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
			return new ResponseEntity<ExceptionInfo>(new ExceptionInfo(finalRequestMapping, new NullPointerException("No loan with ID " + loanID + " was found in the database unfortunately..."), HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND), HttpStatus.OK);
		}
		/* Otherwise, proceed with the removal - call the deletion method and check for exceptions */
		loanService.removeLoan(loanID);
		/* Return in the end the response of the removal operation */
		return new ResponseEntity<SerializedLoan>(HttpStatus.NO_CONTENT);		
	}
}