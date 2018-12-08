package com.feritoth.restfx.dispatcher;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import com.feritoth.restfx.core.LoanCurrency;
import com.feritoth.restfx.core.SerializedLoan;
import com.feritoth.restfx.utilities.EmailConfirmationSender;
import com.feritoth.restfx.utilities.ExceptionInfo;

public class LoanRESTDispatcher {

	/* The link where the underlying web application is accessed */
	public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	/* The logger associated to the current client - used during communication with the web application */
	private static final Logger LOGGER = LoggerFactory.getLogger(LoanRESTDispatcher.class);
	/* The static singleton reference for the REST template element + the name of the class used for instantiation */
	private static RestTemplate restTemplate = getInstance();
	private static final String REST_TEMPLATE_LOCATION = "org.springframework.web.client.RestTemplate";

	/* The dispatcher method for fetching all loans issued from a particular IP address - GET all the loans registered under one particular IP */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<SerializedLoan> getAllLoansForParticularIPAddress(String ipAddress){
		/* Print a start message before the effective method invocation */
		LOGGER.info("Testing the getAllLoansForParticularIPAddress RESTful GET method...");
		/* Create the address required for the invocation of the given operation */
		String loanCollectionURL = REST_SERVICE_URI + "/loan/matchIPaddress/" + ipAddress + "/";
		/* Fetch and process the response coming from the execution of the given operation - check of course its status code first */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(loanCollectionURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* For potential loans found, return them wrapped inside a list */
			List<HashMap<String, Object>> allRegisteredLoans = serviceResponse.getBody();
			return convertHashMapListToLoans(allRegisteredLoans);
		} else {
			/* Return an empty collection */
			return Collections.emptyList();
		}
	}

	/* The dispatcher method for registering a new loan inside the application DB - POST a new loan */
	@SuppressWarnings("unchecked")
	public static Object registerNewLoan(SerializedLoan newLoan) {
		/* Print a message before invoking the given operation */
		LOGGER.info("Testing the registerNewLoan RESTful POST method...");
		/* Create the URL required for invoking the given operation */
		String loanRegistrationURL = REST_SERVICE_URI + "/loan/";
		/* Build the HttpHeaders object required for storing the representation of the consumed and produced media types by the given web service */
		HttpHeaders registrationHeaders = new HttpHeaders();
		registrationHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		registrationHeaders.setAccept(acceptedMediaTypes);
		/* Generate next the HttpEntity object for incorporating the previously built headers */
		HttpEntity<SerializedLoan> newLoanHeader = new HttpEntity<>(newLoan, registrationHeaders);
		/* Invoke the given operation and examine the generated result */
		ResponseEntity<?> serviceResponse = restTemplate.postForEntity(loanRegistrationURL, newLoanHeader, Object.class, newLoan.getLoanID());
		/* Check the status of the response and take action accordingly */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, since we have an exception returned, we will pass it unmodified to the client */
			HashMap<String, Object> operationOutcome = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo(loanRegistrationURL, (String)operationOutcome.get("exceptionMessage"), (Integer)operationOutcome.get("errorCode"), HttpStatus.valueOf((String) operationOutcome.get("httpOperationStatus")));
			LOGGER.warn("Exception has been detected during the method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* In this case, return a message with the newly created location for the inserted loan */
			String responseObject = (String) serviceResponse.getBody();
			URI registrationURI = URI.create(responseObject);
			LOGGER.info("The new IP address has been registered under the following location:" + REST_SERVICE_URI + registrationURI);
			/* Send the e-mail confirming the registration of the given IP address */
			String firstMessageSection = newLoan.toString() + "\n";
			String secondMessageSection = "Dear " + newLoan.getIpAddress().getOwnerClient().getName() + ",\n You have successfully registered a new loan under the following location:" + REST_SERVICE_URI + registrationURI;
			EmailConfirmationSender.sendConfirmationEmail("New Loan Registration", secondMessageSection + firstMessageSection);
			/* Return the given URI at the end of the operation */
			return registrationURI;
		}
	}

	/* The dispatcher method for extending a particular loan - PUT the new details of the loan in the application DB */
	@SuppressWarnings("unchecked")
	public static Object extendSelectedLoan(Integer loanID, SerializedLoan selectedLoan) {
		/* Print a message before invoking the given operation */
		LOGGER.info("Testing the extendSelectedLoan RESTful PUT method...");
		/* Create the URL required for invoking the given method */
		String loanExtensionURL = REST_SERVICE_URI + "/loan/extendLoan/" + loanID + "/";
		/* Next build the HttpHeaders object for incorporating the required method restrictions on request & response processing */
		HttpHeaders extensionHeaders = new HttpHeaders();
		extensionHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		extensionHeaders.setAccept(acceptedMediaTypes);
		/* Finally build the HttpEntity object for incorporating the previously generated headers */
		HttpEntity<SerializedLoan> extendedLoanHeader = new HttpEntity<>(selectedLoan, extensionHeaders);
		/* Now invoke the given operation */
		ResponseEntity<?> serviceResponse = restTemplate.exchange(URI.create(loanExtensionURL), HttpMethod.PUT, extendedLoanHeader, Object.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, since we have an exception returned, we will pass it unmodified to the client */
			HashMap<String, Object> operationOutcome = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo(loanExtensionURL, (String)operationOutcome.get("exceptionMessage"), (Integer)operationOutcome.get("errorCode"), HttpStatus.valueOf((String) operationOutcome.get("httpOperationStatus")));
			LOGGER.warn("Exception has been detected during the method invocation!" + exInfo.toString());
			return exInfo;
		} else {
            /* Send an e-mail with the new loan information */
			SerializedLoan initialLoan = createBackOriginalLoan(serviceResponse);
			String firstMessageHalf = "Dear " + selectedLoan.getIpAddress().getOwnerClient().getName() + ",\n";
            String secondMessageHalf = "This is a confirmation e-mail for marking the successful outcome of the loan extension operation. The details of the loan selected for extension are the following:\n" + initialLoan.toString();
            EmailConfirmationSender.sendConfirmationEmail("Successful Loan Extension", firstMessageHalf + secondMessageHalf);
			/* Return back the selected loan in its initial status, before the execution of update itself */
			return initialLoan;
		}
	}

	/* The dispatcher method for removing a particular loan from the application DB - DELETE a selected loan */
	@SuppressWarnings("unchecked")
	public static Object removeSelectedLoan(Integer loanID) {
		/* Print a start message before the effective method invocation */
		LOGGER.info("Testing the removeSelectedLoan RESTful DELETE method...");
		/* Create the URL required for the invocation of the selected operation */
		String loanRemovalURL = REST_SERVICE_URI + "/loan/" + loanID + "/";
		/* Invoke the given operation and process the result accordingly */
		ResponseEntity<?> serviceResponse = restTemplate.exchange(URI.create(loanRemovalURL), HttpMethod.DELETE, null, Object.class);
		/* For result invocation - check the status code of the given operation */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* This means that an error has been detected during the invocation of the given operation */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo(loanRemovalURL, (String)responseObject.get("exceptionMessage"), (Integer)responseObject.get("errorCode"), HttpStatus.valueOf((String) responseObject.get("httpStatus")));
			LOGGER.warn("Exception has been detected during the method invocation!" + exInfo.toString());
			return exInfo;
		} else {
			/* For invocation ending successfully, just return a message of successful termination */
			return "The loan with ID = " + loanID +  " has successfully been removed from the application!";
		}
	}

	/* The original loan creator method */
	@SuppressWarnings("unchecked")
	private static SerializedLoan createBackOriginalLoan(ResponseEntity<?> serviceResponse) {
		/* Convert first the original service response */
		HashMap<String, Object> originalLoanMap = (HashMap<String, Object>) serviceResponse.getBody();
		/* Pick up and pre-process all the relevant parameters for re-creating the given loan */
		/* Start with the loan application moment - parse it accordingly in order to avoid any arising exceptions */
		String applicationTime = (String) originalLoanMap.get("applicationTime");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime loanApplicationTime = LocalDateTime.parse(applicationTime, dtf);
		/* Then the original - i.e. the value before the effective update - loan return date */
		LocalDate returnDate = LocalDate.parse((String)originalLoanMap.get("returnDate"));
		/* Finish by converting the rest of the parameters required for rebuilding the given loan */
		Long loanedAmount = Integer.toUnsignedLong((Integer) originalLoanMap.get("loanedAmount")); //the loaned amount
        LoanCurrency loanCurrency = LoanCurrency.valueOf((String) originalLoanMap.get("currency")); //the loan currency
        Boolean extensionFlag = Boolean.valueOf((boolean)originalLoanMap.get("extended")); //the extension marker flag - value before update
        Long interestRate = Integer.toUnsignedLong((Integer)originalLoanMap.get("interestRate")); //the interest rate - value before update
        Long extensionCount = Integer.toUnsignedLong((Integer)originalLoanMap.get("extensionCount")); //the extension counter - value before update
        Integer loanID = (Integer) originalLoanMap.get("loanID");
        /* Finally create back the original loan and return it */
		return new SerializedLoan(loanApplicationTime, returnDate, loanedAmount, loanCurrency, extensionFlag, interestRate, loanID, extensionCount);
	}

	/* The auxiliary data converter method */
	private static List<SerializedLoan> convertHashMapListToLoans(List<HashMap<String, Object>> allRegisteredLoans) {
		/* Create the list to be returned */
		List<SerializedLoan> allLoans = new ArrayList<>();
		/* Next populate it accordingly */
		for (HashMap<String, Object> loanMap : allRegisteredLoans){
			/* Convert the problematic fields before usage */
			//the application date & time
			String dateTimeString = (String) loanMap.get("applicationTime");
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime applicationDateTime = LocalDateTime.parse(dateTimeString, formatter);
			//the loan return date
			String dateString = (String) loanMap.get("returnDate");
			LocalDate returnDate = LocalDate.parse(dateString);
			//the loan currency
			String currencyName = (String) loanMap.get("currency");
			LoanCurrency loanCurrency = LoanCurrency.valueOf(currencyName);
			//the interest rate
			Long interestRate = Long.valueOf((Integer)loanMap.get("interestRate"));
			//the amount
			Long amount = Long.valueOf((Integer)loanMap.get("loanedAmount"));
			//the extension flag
			Boolean isExtended = Boolean.valueOf((boolean)loanMap.get("extended"));
			//the extension count field
			Long extensionCount = Long.valueOf((Integer)loanMap.get("extensionCount"));
			/* Create next the new loan object */
			SerializedLoan registeredLoan = new SerializedLoan(applicationDateTime, returnDate, amount, loanCurrency,
					                                           isExtended, interestRate, (Integer)loanMap.get("loanID"), extensionCount);
			allLoans.add(registeredLoan);
		}
		/* Finally return the populated array */
		return allLoans;
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