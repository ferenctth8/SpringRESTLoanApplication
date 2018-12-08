package com.feritoth.cla.springmvc.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feritoth.cla.springmvc.dao.ClientDao;
import com.feritoth.cla.springmvc.dao.IPAddressDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.service.ClientService;

@Service("clientService")
@Transactional
public class ClientSpringService implements ClientService {
	
	@Autowired
	private ClientDao clientDao;
	
	@Autowired
	private IPAddressDao ipAddressDao;
	
	/* Declare here the map of the exceptions used during the update and the registration of a client */
	private Map<String, Exception> regUpFlagMap = new HashMap<>();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientSpringService.class);
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final String FIRST_NAME_PATTERN = "[A-Z][a-zA-Z]*";
	private static final String LAST_NAME_PATTERN = "[a-zA-z]+([ '-][a-zA-Z]+)*";
	private static final String CNP_PATTERN = "[0-9]+";
	
	private boolean validateClientCNP(String clientCNP){
		Pattern cnpPattern = Pattern.compile(CNP_PATTERN);
		Matcher matcher = cnpPattern.matcher(clientCNP);
		return matcher.matches();
	}
	
	public Map<String, Exception> getRegUpFlagMap() {
		return Collections.unmodifiableMap(regUpFlagMap);
	}

	private boolean validateClientEmailAddress(String emailAddress) {
		Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
		if (StringUtils.isNotBlank(emailAddress)){
			Matcher matcher = emailPattern.matcher(emailAddress);
			return matcher.matches();
		} 
		return false;
	}
	
	private boolean validateClientName(String clientName){
		Pattern firstNamePattern = Pattern.compile(FIRST_NAME_PATTERN);
		Pattern lastNamePattern = Pattern.compile(LAST_NAME_PATTERN);
		String[] clientNameParts = clientName.split(" ");
		Matcher firstNameMatcher = firstNamePattern.matcher(clientNameParts[1]);
		Matcher lastNameMatcher = lastNamePattern.matcher(clientNameParts[0]);		
		return firstNameMatcher.matches() && lastNameMatcher.matches();
	}
	
	private boolean checkIfClientAlreadyPresent(Client newClient, List<Client> possibleMatches) {
		List<Client> clients = possibleMatches.stream().filter(client -> client.equals(newClient)).collect(Collectors.toCollection(ArrayList<Client>::new));
		return !clients.isEmpty() && clients.size() == 1;
	}

	@Override
	public List<Client> findAllRegisteredClients() {
		return clientDao.getAllRegisteredClients();
	}

	@Override
	public List<Client> getMatchingClients(String nameSequence) {
		return clientDao.getMatchingClients(nameSequence);
	}

	@Override
	public Client findClientByCNP(String cnp) {		
		/* Check first if the given CNP is registered in the DB */
		List<String> allRegisteredCNPs = clientDao.getAllClientCNPs();
		/* In case of presence, proceed with the retrieval of the given client, otherwise return null */
		if (allRegisteredCNPs.contains(cnp)){
			return clientDao.findClientByCNP(cnp);
		}
		return null;
	}

	@Override
	public Client findClientByIP(String ipAddress) {
		/* Check again if the IP address is already registered in the DB */
		List<String> allRegisteredIPValues = ipAddressDao.getAllIPValues();
		/* In case of presence, proceed with the retrieval of the given client, otherwise return null */
		if (allRegisteredIPValues.contains(ipAddress)){
			return clientDao.findClientByIP(ipAddress);
		}
		return null;
	}

	@Override
	public List<Client> findClientsByEmailFragment(String emailAddressFragment) {
		return clientDao.findClientsByEmailAddressFragment(emailAddressFragment);
	}

	@Override
	public void registerNewClient(Client newClient) {
		/* As a prerequisite, empty the flag map in order to avoid unwanted overwrites from previous invocations */
		regUpFlagMap.clear();
		/* Check the correctness of the CNP, e-mail & postal addresses and of the course client name before registration */
		/* CNP - must be exactly 12 characters long and contain only digits */
		List<Client> possibleMatches = clientDao.getMatchingClients(newClient.getName());		
		boolean cnpOK = newClient.getCnp().length() == 12 && validateClientCNP(newClient.getCnp());
		/* In addition, it also must be unique in order to avoid conflicts during the registration */
		boolean clientNotRegisteredYet = !checkIfClientAlreadyPresent(newClient, possibleMatches);		
		/* Email Address - must match the description pattern provided as class constant */
        boolean emailAddressOK = validateClientEmailAddress(newClient.getEmailAddress());
        /* Name - must not be empty and must contain a space for the separation of the first and last names */
        boolean nameOK = validateClientName(newClient.getName());
        /* Postal Address - must not be empty and must have at least 10 characters */
        boolean postalAddressOK = StringUtils.isNotBlank(newClient.getPostalAddress()) && newClient.getPostalAddress().length()>=10;
        /* For these preconditions fulfilled, client registration can move on */
        if (clientNotRegisteredYet && cnpOK && emailAddressOK && nameOK && postalAddressOK){
        	clientDao.saveNewClient(newClient);
        } else {
        	/* Check which of these parameters was given wrongly and throw an exception accordingly to the matching case */
        	if (!cnpOK){
        		LOGGER.error("Invalid CNP provided to the application!", newClient.getCnp());
    			String exMessage = "Invalid CNP provided to the application! " + newClient.getCnp();
    			regUpFlagMap.put("faultyCNP", new IllegalArgumentException(exMessage));    			 
        	}
        	if (!clientNotRegisteredYet){
        		LOGGER.error("The CNP in question is well-formed, but has already been assigned to another client!", newClient.getCnp());
        		String exMessage = "CNP in question is already in use by another client! " + newClient.getCnp();
        		regUpFlagMap.put("clientAlreadyRegistered", new IllegalArgumentException(exMessage));
        	}
        	if (!emailAddressOK){
        		LOGGER.error("Invalid e-mail address provided to the application!", newClient.getEmailAddress());
    			String exMessage = "Invalid e-mail address provided to the application! " + newClient.getEmailAddress();
    			regUpFlagMap.put("faultyEmailAddress", new IllegalArgumentException(exMessage));
        	}
        	if (!nameOK){
        		LOGGER.error("Invalid name format provided to the application!", newClient.getName());
    			String exMessage = "Invalid name format provided to the application! " + newClient.getName();
    			regUpFlagMap.put("faultyName", new IllegalArgumentException(exMessage));
        	}
        	if (!postalAddressOK){
        		LOGGER.error("Invalid postal address provided to the application!", newClient.getPostalAddress());
    			String exMessage = "Invalid postal address provided to the application! " + newClient.getPostalAddress();
    			regUpFlagMap.put("faultyPostalAddress", new IllegalArgumentException(exMessage));
        	}
        }
	}

	@Override
	public void updateExistingClient(Client selectedClient) {
		/* As a prerequisite, empty the flag map in order to avoid unwanted overwrites from previous invocations */
		regUpFlagMap.clear();
		/* Check the consistency of the new e-mail address and postal address as well as for the client name - in case of any updates */		
		/* Email Address - must match the description pattern provided as class constant */
        boolean emailAddressOK = validateClientEmailAddress(selectedClient.getEmailAddress());
        /* Postal Address - must not be empty */
        boolean postalAddressOK = StringUtils.isNotBlank(selectedClient.getPostalAddress()) && selectedClient.getPostalAddress().length()>=10;
        /* Name - must not be empty and must contain a space for the separation of the first and last names */
        boolean nameOK = validateClientName(selectedClient.getName());
        /* For these preconditions fulfilled, client registration can move on */
        if (postalAddressOK && emailAddressOK && nameOK){
        	clientDao.updateClient(selectedClient);
        } else {
        	/* Check which of the new parameters was given in the wrong format */
        	if (!postalAddressOK){
        		LOGGER.error("New but invalid postal address provided to the application!", selectedClient.getPostalAddress());
    			String exMessage = "New but invalid postal address provided to the application! " + selectedClient.getPostalAddress();
    			regUpFlagMap.put("faultyPostalAddress", new IllegalArgumentException(exMessage));
        	}
        	if (!emailAddressOK){
        		LOGGER.error("New but invalid e-mail address provided to the application!", selectedClient.getEmailAddress());
    			String exMessage = "New but invalid e-mail address provided to the application! " + selectedClient.getEmailAddress();
    			regUpFlagMap.put("faultyEmailAddress", new IllegalArgumentException(exMessage));
        	}
        	if (!nameOK){
        		LOGGER.error("The new name format provided to the application is invalid!", selectedClient.getName());
    			String exMessage = "The new name format provided to the application is invalid! " + selectedClient.getName();
    			regUpFlagMap.put("faultyName", new IllegalArgumentException(exMessage));
        	}
        }				
	}

	@Override
	public void removeClient(String cnp) {
		/* Validate the CNP before */
		boolean cnpOK = cnp.length() == 12 && validateClientCNP(cnp);
		if (cnpOK){
			clientDao.removeClient(cnp);
		} else {
			LOGGER.error("Invalid CNP provided to the application!", cnp);
			String exMessage = "Invalid CNP provided to the application! " + cnp;
			regUpFlagMap.put("faultyCNP", new IllegalArgumentException(exMessage));
		}			
	}

	@Override
	public boolean isEmployeeCNPalreadyAssigned(String cnp) {
		Client registeredClient = findClientByCNP(cnp);
		if (registeredClient != null){
			return registeredClient.getCnp().equals(cnp);
		}
		return false;
	}
	
}