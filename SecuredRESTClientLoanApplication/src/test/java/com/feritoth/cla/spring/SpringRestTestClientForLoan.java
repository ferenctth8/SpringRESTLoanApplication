package com.feritoth.cla.spring;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.feritoth.cla.springmvc.jsonmodel.LoanCurrency;
import com.feritoth.cla.springmvc.jsonmodel.SerializedClient;
import com.feritoth.cla.springmvc.jsonmodel.SerializedIPAddress;
import com.feritoth.cla.springmvc.jsonmodel.SerializedLoan;

public class SpringRestTestClientForLoan {
	
	/* Declare here the link where the tests shall be carried out */
    public static final String REST_SERVICE_URI = "http://localhost:8084/SecuredRESTClientLoanApplication";
	/* And the logger used for doing the quick tests */
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringRestTestClientForLoan.class);
	
	/* GET all the loans from the system */	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void listAllLoansFromSystem(){
		/* Print the introduction used for the search as part of the validation process */
		LOGGER.info("Testing listAllLoans RESTful method...");
		/* Create the link to be used for the request */
		RestTemplate restTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/loan/";
		/* Fetch and process the response from the called REST method - do a quick status check before further execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(restURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			List<HashMap<String, Object>> allLoanMaps = serviceResponse.getBody();
			LOGGER.info("The following loans have been identified as registered in the current system:");	
			for (HashMap<String, Object> loanMap : allLoanMaps){
				LOGGER.info("Loan : id = " + loanMap.get("loanID") + ", ipAddress = " + loanMap.get("ipAddress") + ", applicationTime = " + loanMap.get("applicationTime") + 
						    ", returnDate = " + loanMap.get("returnDate") + ", loanedAmount = " + loanMap.get("loanedAmount") + ", currency = " + loanMap.get("currency") + 
						    ", isExtended = " + loanMap.get("extended") + ", interestRate = " + loanMap.get("interestRate") + ", extensionCount = " + loanMap.get("extensionCount"));
			}
		}
	}
	
	/* GET all the loans assigned to one client */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void listAllLoansForClient(String searchCNP){
		/* Print the introduction used for the given method */
		LOGGER.info("Testing listAllLoansForClient method...");
		/* Create the link to be used for the request execution */
		RestTemplate restTemplate = new RestTemplate();
		String initialRestURL = REST_SERVICE_URI + "/loan/matchClient/{clientCNP}/";
		String finalRestURL = initialRestURL.replace("{clientCNP}", searchCNP);
		/* Fetch and process the response from the called REST method - check its status before proceeding with execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(finalRestURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.NO_CONTENT){
			LOGGER.warn("The given CNP, " + searchCNP + ", has not been found as registered in the database - therefore no loans have been assigned to its client...");
		} else {
			List<HashMap<String, Object>> allLoansForClient = serviceResponse.getBody();
			if (allLoansForClient.isEmpty()){
				LOGGER.info("The client with the given CNP, " + searchCNP + ", has been located in the DB, however, no loans are currently registered under his/her name!");
			} else {
				LOGGER.info("The following loans have been registered for the client with CNP " + searchCNP + ":");
				for (HashMap<String, Object> loanMap : allLoansForClient){
					LOGGER.info("Loan : id = " + loanMap.get("loanID") + ", ipAddress = " + loanMap.get("ipAddress") + ", applicationTime = " + loanMap.get("applicationTime") + 
							    ", returnDate = " + loanMap.get("returnDate") + ", loanedAmount = " + loanMap.get("loanedAmount") + ", currency = " + loanMap.get("currency") + 
							    ", isExtended = " + loanMap.get("extended") + ", interestRate = " + loanMap.get("interestRate") + ", extensionCount = " + loanMap.get("extensionCount"));
				}
			}
		}
	}
	
	/* GET all the loans issued from one IP address */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void listAllLoansForIPaddress(String searchIPaddress){
		/* Print an introduction for this method */
		LOGGER.info("Testing listAllLoansFromIPAddress method...");
		/* Create the link to be used for the request execution */
		RestTemplate restTemplate = new RestTemplate();
		String initialRestURL = REST_SERVICE_URI + "/loan/matchIPaddress/{IPvalue}/";
		String finalRestURL = initialRestURL.replace("{IPvalue}", searchIPaddress);
		/* Fetch and process the response from the invoked REST method - check its status before proceeding with the execution */
		ResponseEntity<List> serviceResponse = restTemplate.getForEntity(finalRestURL, List.class);
		if (serviceResponse.getStatusCode() == HttpStatus.NO_CONTENT){
			LOGGER.warn("The given IP address, " + searchIPaddress + ", has not been found as registered in the database - therefore no loans have been assigned to its owner...");
		} else {
			List<HashMap<String, Object>> allLoansForIPaddress = serviceResponse.getBody();
			if (allLoansForIPaddress.isEmpty()){
				LOGGER.info("The IP address " + searchIPaddress + " has been found as registered in the DB, however, up to now, no loans have been issued from it up to now!");
			} else {
				LOGGER.info("The following loans have been registered for the IP address " + searchIPaddress + ":");
				for (HashMap<String, Object> loanMap : allLoansForIPaddress){
					LOGGER.info("Loan : id = " + loanMap.get("loanID") + ", ipAddress = " + loanMap.get("ipAddress") + ", applicationTime = " + loanMap.get("applicationTime") + 
							    ", returnDate = " + loanMap.get("returnDate") + ", loanedAmount = " + loanMap.get("loanedAmount") + ", currency = " + loanMap.get("currency") + 
							    ", isExtended = " + loanMap.get("extended") + ", interestRate = " + loanMap.get("interestRate") + ", extensionCount = " + loanMap.get("extensionCount"));
				}
			}
		}
	}
	
	/* GET all the details for one particular loan */
	@SuppressWarnings("unchecked")
	private static SerializedLoan listDetailsForLoan(int loanID){
		/* Print an introduction for this method */
		LOGGER.info("Testing listDetailsForLoanID method...");
		/* Create the link to be used for the request execution */
		RestTemplate restTemplate = new RestTemplate();
		String initialRestURL = REST_SERVICE_URI + "/loan/displayLoanHistory/{loanID}/";
		String finalRestURL = initialRestURL.replace("{loanID}", Integer.toString(loanID));
		/* Fetch and process the response from the invoked REST method - check the content of the key set before effective procession */
		ResponseEntity<Object> serviceResponse = restTemplate.getForEntity(finalRestURL, Object.class);
		HashMap<String, Object> responseMap = (HashMap<String, Object>) serviceResponse.getBody();
		if (responseMap.containsKey("url") && responseMap.containsKey("exceptionMessage")){
			ExceptionInfo exInfo = new ExceptionInfo((String)responseMap.get("url"), (String)responseMap.get("exceptionMessage"), (Integer)responseMap.get("errorCode"), HttpStatus.valueOf((String) responseMap.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
			throw new RuntimeException("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {
			/* Extract first the IP address map and process it accordingly */
			HashMap<String, Object> ipAddressMap = (HashMap<String, Object>) responseMap.get("ipAddress");
			/* Extract from it the client side next and convert it to a relevant object */
			HashMap<String, Object> clientMap = (HashMap<String, Object>) ipAddressMap.get("ownerClient");
			SerializedClient soc = new SerializedClient((String)clientMap.get("cnp"), (String)clientMap.get("name"), (String)clientMap.get("emailAddress"), (String)clientMap.get("postalAddress"));
			/* Then create the IP address which shall be made as loan owner */
			SerializedIPAddress sip = new SerializedIPAddress((String)ipAddressMap.get("ipValue"), (Integer)ipAddressMap.get("ipID"));
			sip.setOwnerClient(soc);
			/* Finally create the loan in question using the remaining parameters - also add the previously processed IP address to it */
			/* Parse the loan application moment */
			String loanApplicationTime = (String)responseMap.get("applicationTime");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime applicationTime = LocalDateTime.parse(loanApplicationTime, dtf);
			/* Then parse the return date */
			LocalDate returnDate = LocalDate.parse((String) responseMap.get("returnDate"));
			/* Convert also the rest of the parameters as follows */
			Long loanedAmount = Integer.toUnsignedLong((Integer) responseMap.get("loanedAmount"));
			LoanCurrency loanCurrency = LoanCurrency.valueOf((String) responseMap.get("currency"));
			Boolean extensionFlag = Boolean.valueOf((boolean) responseMap.get("extended"));
			Long interestRate = Integer.toUnsignedLong((Integer) responseMap.get("interestRate"));
			Long extensionCount = Integer.toUnsignedLong((Integer) responseMap.get("extensionCount"));
			SerializedLoan sl = new SerializedLoan(applicationTime, returnDate, loanedAmount, loanCurrency, extensionFlag, interestRate, loanID);
			sl.setExtensionCount(extensionCount);
			sl.setIpAddress(sip);
			/* Print out the given loan as well as return it */
			LOGGER.info("The submitted id, " + loanID + ", returned the following information:" + sl.toString());
			return sl;			
		}
	}
	
	/* POST a new loan into the system */
	@SuppressWarnings("unchecked")
	private static void createNewLoan(SerializedLoan newLoan){
		/* Print a start message at the beginning of the method */
		LOGGER.info("Testing createNewLoan method with the associated risk analysis side...");
		/* Create the communication template object */
		RestTemplate registrationTemplate = new RestTemplate();
		/* Create the URI to be invoked during registration */
		String restURL = REST_SERVICE_URI + "/loan/";
		/* Create the HttpHeaders object for incorporating the method restrictions */
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		headers.setAccept(acceptedMediaTypes);
		/* Create the HttpEntity object for incorporating the headers and invoke the operation desired to be executed */
		HttpEntity<SerializedLoan> newLoanHeader = new HttpEntity<SerializedLoan>(newLoan, headers);
		ResponseEntity<?> serviceResponse = registrationTemplate.postForEntity(restURL, newLoanHeader, Object.class, newLoan.getLoanID());
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
			LOGGER.info("The new loan has been registered under the following location:" + registrationURI);
		}
	}
	
	/* PUT the new details required for loan extension registration in the system */
	@SuppressWarnings("unchecked")
	private static void extendRegisteredLoan(Integer loanID, SerializedLoan selectedLoan){
		/* Print another start message for launching the verification */
		LOGGER.info("Testing extendRegisteredLoan for a previously submitted loan...");
		/* Create the template for the operation */
		RestTemplate extensionTemplate = new RestTemplate();
		/* Next create the URL where to send the request */
		String restURL = REST_SERVICE_URI + "/loan/extendLoan/" + loanID + "/";
		/* Finally create the HttpHeaders object for incorporating the required method restrictions */
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>();
		acceptedMediaTypes.add(MediaType.APPLICATION_JSON);
		httpHeaders.setAccept(acceptedMediaTypes);
		/* Before invocation of the tested service: build the final entity for incorporating the previously created headers */
		HttpEntity<SerializedLoan> extendedLoanHeader = new HttpEntity<SerializedLoan>(selectedLoan, httpHeaders);
		/* Invoke the tested operation and check its outcome */
		ResponseEntity<?> serviceResponse = extensionTemplate.exchange(URI.create(restURL), HttpMethod.PUT, extendedLoanHeader, Object.class);
		/* Now based upon the status of the returned response, analyze the result accordingly */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* In this case, an exception has been detected - its details shall follow */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			ExceptionInfo exInfo = new ExceptionInfo(restURL, (String)responseObject.get("exceptionMessage"), (Integer)responseObject.get("errorCode"), HttpStatus.valueOf((String)responseObject.get("httpOperationStatus")));
			LOGGER.warn("Warning:Exception has been detected during the method invocation!" + exInfo.toString());
		} else {
			/* In this case the initially passed loan details should be returned */
			HashMap<String, Object> originalLoanMap = (HashMap<String, Object>) serviceResponse.getBody();
			/* Extract first the IP address map and process it accordingly */
			HashMap<String, Object> ipAddressMap = (HashMap<String, Object>) originalLoanMap.get("ipAddress");
			/* Extract from it the client side next and convert it to a relevant object */
			HashMap<String, Object> clientMap = (HashMap<String, Object>) ipAddressMap.get("ownerClient");
			SerializedClient soc = new SerializedClient((String)clientMap.get("cnp"), (String)clientMap.get("name"), (String)clientMap.get("emailAddress"), (String)clientMap.get("postalAddress"));
			/* Then create the IP address which shall be made as loan owner */
			SerializedIPAddress sip = new SerializedIPAddress((String)ipAddressMap.get("ipValue"), (Integer)ipAddressMap.get("ipID"));
			sip.setOwnerClient(soc);
			/* Finally create the loan in question using the remaining parameters - also add the previously processed IP address to it */
			/* Parse the loan application moment */
			String loanApplicationTime = (String)originalLoanMap.get("applicationTime");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime applicationTime = LocalDateTime.parse(loanApplicationTime, dtf);
			/* Then parse the return date */
			LocalDate returnDate = LocalDate.parse((String) originalLoanMap.get("returnDate"));
			/* Convert also the rest of the parameters as follows */
			Long loanedAmount = Integer.toUnsignedLong((Integer) originalLoanMap.get("loanedAmount"));
			LoanCurrency loanCurrency = LoanCurrency.valueOf((String) originalLoanMap.get("currency"));
			Boolean extensionFlag = Boolean.valueOf((boolean) originalLoanMap.get("extended"));
			Long interestRate = Integer.toUnsignedLong((Integer) originalLoanMap.get("interestRate"));
			Long extensionCount = Integer.toUnsignedLong((Integer) originalLoanMap.get("extensionCount"));
			SerializedLoan sl = new SerializedLoan(applicationTime, returnDate, loanedAmount, loanCurrency, extensionFlag, interestRate, loanID);
			sl.setExtensionCount(extensionCount);
			sl.setIpAddress(sip);
			/* Print out the outcome of the given result */
			LOGGER.info("THe loan used for update is:" + sl);
		}
		//extensionTemplate.put(restURL, selectedLoan); simpler way, but unfortunately no response is returned for analysis
	}
	
	/* DELETE a particular loan */
	@SuppressWarnings("unchecked")
	private static void removeExistingLoan(Integer loanID){
		/* Print a message for checking the start of the operation */
		LOGGER.info("Testing removal of a previously registered loan...");
		/* Create the URL and the REST template to be used during execution of operation */
		RestTemplate deleteTemplate = new RestTemplate();
		String restURL = REST_SERVICE_URI + "/loan/" + loanID + "/";
		/* Remove the given loan */
		ResponseEntity<?> serviceResponse = deleteTemplate.exchange(URI.create(restURL), HttpMethod.DELETE, null, Object.class);
		/* Check the result status and display a corresponding message */
		if (serviceResponse.getStatusCode() == HttpStatus.OK){
			/* This means that an exception has been raised - details shall next be output */
			HashMap<String, Object> responseObject = (HashMap<String, Object>) serviceResponse.getBody();
			String exceptionMessage = (String) responseObject.get("exceptionMessage");
			Integer errorCode = Integer.valueOf((Integer)responseObject.get("errorCode"));
			HttpStatus httpStatus = HttpStatus.valueOf((String)responseObject.get("httpOperationStatus"));
			ExceptionInfo exInfo = new ExceptionInfo(restURL, exceptionMessage, errorCode, httpStatus);
			LOGGER.warn("Warning:Exception has been detected during method invocation!" + exInfo.toString());
		} else {
			LOGGER.info("The loan registered under ID = " + loanID + " has successfully been removed from the application database!");
		}
		//deleteTemplate.delete(restURL); simpler way, but unfortunately no result is returned for analysis
	}
	
	public static void main(String[] args){
		/* List all the loans from the system */
		//listAllLoansFromSystem(); //Worked (added extra field for loan extension count)
		/* Get all the loans from the system listed for one particular client */
		//listAllLoansForClient("190071130397"); //Worked (also added new field from previous point)
		//listAllLoansForClient("190071130396"); //Worked (also added new field from previous point)
		//listAllLoansForClient("146890123456"); //Worked (also added new field from previous point)
		/* Get all the loans from the system listed for one particular IP address */
		//listAllLoansForIPaddress("112.34.12.12"); //Worked (also added new field from previous point)
		//listAllLoansForIPaddress("150.90.72.81"); //Worked (also added new field from previous point)
		//listAllLoansForIPaddress("150.90.72.82"); //Worked (also added new field from previous point)
		/* Get the history associated to one particular loan */
		//listDetailsForLoan(14); //Worked (with extra added field for extension count)
		//listDetailsForLoan(15); //Worked (with extra added field for extension count)
		/* Submit a new loan into the system */
		/* Specify the test data to be used */
		//'15', '150.90.72.81', '190071130396'
        //'190071130396', 'Ferenc Toth', 'ferenctth8@gmail.com', 'Senohrabska 2, Praha 4 Zabehlice'
		//'14', '14', '2017-04-15 08:00:00', '2017-04-29', '1500', 'EUR', 'Y', '450'
        /* Case A: sending a loan with an incorrect duration into the system - for both CZK and EUR */
		SerializedClient sc = new SerializedClient("190071130396", "Ferenc Toth", "ferenctth8@gmail.com", "Senohrabska 2, Praha 4 Zabehlice");
        SerializedIPAddress sipa = new SerializedIPAddress("150.90.72.81", 15);
        sipa.setOwnerClient(sc);
        SerializedLoan newLoan = new SerializedLoan();
        newLoan.setIpAddress(sipa);
        //newLoan.setApplicationTime(LocalDateTime.of(2018, 1, 18, 0, 0, 1));
        //newLoan.setReturnDate(LocalDate.of(2018, 1, 18).plusDays(7));
        newLoan.setApplicationTime(LocalDateTime.now());
        newLoan.setReturnDate(LocalDate.now().plusDays(7));
        //newLoan.setLoanedAmount(15000L);
        //newLoan.setCurrency(LoanCurrency.EUR);
        newLoan.setLoanedAmount(30000L);
        newLoan.setCurrency(LoanCurrency.CZK);        
        createNewLoan(newLoan);
		/* Extend a loan based on its registration ID */
		/* Case A: faulty return date supplied in for loan which has not yet been extended - Worked correctly */
		//SerializedLoan selectedLoan = listDetailsForLoan(14);
		//selectedLoan.setReturnDate(LocalDate.of(2017, 4, 23));
		//extendRegisteredLoan(14, selectedLoan);
		/* Case B: correct return date supplied under similar circumstances as in case A - Worked correctly */
		//SerializedLoan selectedLoan = listDetailsForLoan(14);
		//selectedLoan.setReturnDate(LocalDate.of(2017, 4, 29));		
		//extendRegisteredLoan(14, selectedLoan);
		/* Case C: faulty return date supplied for a loan which has already been extended in the past - Worked correctly */
		//SerializedLoan selectedLoan = listDetailsForLoan(13);
		//selectedLoan.setReturnDate(LocalDate.of(2017, 4, 25));
		//extendRegisteredLoan(13, selectedLoan);
		/* Case D: correctly return date supplied under similar circumstances as in case C */
		//SerializedLoan selectedLoan = listDetailsForLoan(13);
		//selectedLoan.setReturnDate(LocalDate.of(2017, 5, 1));
		//extendRegisteredLoan(13, selectedLoan);
		/* Remove a loan from the database based on its ID */
		//removeExistingLoan(15); //Worked
		//removeExistingLoan(16); //Worked
        //removeExistingLoan(17); //Worked
	}

}