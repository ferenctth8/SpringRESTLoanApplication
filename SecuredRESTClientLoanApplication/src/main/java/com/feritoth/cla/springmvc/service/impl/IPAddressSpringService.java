package com.feritoth.cla.springmvc.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feritoth.cla.springmvc.dao.IPAddressDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.service.IPAddressService;

@Service("ipAddressService")
@Transactional
public class IPAddressSpringService implements IPAddressService {
	
	@Autowired
	private IPAddressDao ipAddressDao;

	private Map<String, Exception> regDelFlagMap = new HashMap<>();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IPAddressSpringService.class);
	protected static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	
	public Map<String, Exception> getRegDelFlagMap() {
		return Collections.unmodifiableMap(regDelFlagMap);
	}

	public boolean validateIPAddress(String ipAddress) {
		/* First, check if the supplied IP address value bears the correct identification pattern */
		Pattern emailPattern = Pattern.compile(IPADDRESS_PATTERN);
		Matcher matcher = emailPattern.matcher(ipAddress);
		boolean validIPAddress = matcher.matches();
		/* Throw an exception in case of the defined pattern not matching the selected IP address */
		if (!validIPAddress){
			LOGGER.error("The given IP address does not match the general validation pattern defined in the current class! " + ipAddress);
			return false;
		}
		/* Then check if the IP address is included in the acceptable range - i.e. remote machine addresses and IP addresses for host, broadcast and research purposes shall be excluded */
		String[] ipAddressComponents = ipAddress.split("\\.");
		boolean validIPAddressRange = checkIPComponentWellFormedness(ipAddressComponents[0], ipAddressComponents[3]);
		LOGGER.info("The following validation results have been obtained:" + validIPAddress + " " + validIPAddressRange);
		return validIPAddress && validIPAddressRange;
	}
	
	private boolean checkIPComponentWellFormedness(String firstElement, String lastElement) {
		/* Check the conditions for the first element */
		Integer firstNb = Integer.valueOf(firstElement);
		boolean firstCheckOK = (firstNb != 127) && (firstNb != 0) && (firstNb < 240);
		/* Check the conditions for the last element */
		Integer lastNb = Integer.valueOf(lastElement);
		boolean lastCheckOK = (lastNb != 0) && (lastNb != 255);
		/* Return the result of the check */
		LOGGER.info("The following well-formedness results have been obtained:" + firstCheckOK + " " + lastCheckOK);
		return firstCheckOK && lastCheckOK;
	}
	
	private boolean checkIfIPAddressAlreadyRegistered(String candidateAddress, String possiblyMatchingAddress){
		return StringUtils.equalsIgnoreCase(candidateAddress, possiblyMatchingAddress);
	}
	
	@Override
	public List<IPAddress> fetchAllAvailableIPAddresses() {
		return ipAddressDao.getAllIPAddresses();
	}

	@Override
	public List<IPAddress> fetchAllAssignedIPAddressesForClient(Client searchedClient) {
		return ipAddressDao.getAllIPAddressesForClient(searchedClient);
	}
	
	@Override
	public IPAddress findDetailsForIPvalue(String ipAddressValue) {
		/* Check if the supplied value is registered first in the DB */
		List<String> allRegisteredIPValues = ipAddressDao.getAllIPValues();
		/* In case the given IP address is registered in the DB, fetch its details; otherwise, just return a null */
		if (allRegisteredIPValues.contains(ipAddressValue)){
			return ipAddressDao.findIPAddressDetailsForValue(ipAddressValue);
		}
		return null;
	}

	@Override
	public void registerIPAddress(IPAddress candidateIPAddress) {
		/* Check the well-formedness of the candidate IP address first */
		boolean ipAddressWellFormed = validateIPAddress(candidateIPAddress.getValue());
		/* Clear the flag map for avoiding any unwanted overwrites from previous invocations */
		regDelFlagMap.clear();
		/* Check next if the value of the candidate IP address has not already been registered */
		IPAddress possiblyRegisteredAddress = ipAddressDao.findIPAddressDetailsForValue(candidateIPAddress.getValue());
		/* Declare here the matchResult variable - do not give it any value for now */
		boolean matchResult;
		if (possiblyRegisteredAddress == null){
			matchResult = false; // in this case the candidate address is not yet registered in the DB
		} else {
			matchResult = checkIfIPAddressAlreadyRegistered(candidateIPAddress.getValue(), possiblyRegisteredAddress.getValue());
			//now it should be in the DB as reserved already for a user
		}		
		/* If both preconditions successfully satisfied, register the given IP address */
		if (ipAddressWellFormed && !matchResult){
			ipAddressDao.saveNewIPAddress(candidateIPAddress);
		} else {
			/* One of the conditions is broken, therefore we have an exception to be returned */
			if (!ipAddressWellFormed){
				LOGGER.error("Malformed or invalid IP address provided to the application!", candidateIPAddress.getValue());
    			String exMessage = "Malformed or invalid IP address provided to the application! " + candidateIPAddress.getValue();
    			regDelFlagMap.put("faultyIPAddressValue", new IllegalArgumentException(exMessage));
			}
			if (matchResult){
				LOGGER.error("The provided IP address has already been registered for another client!", candidateIPAddress.getValue());
    			String exMessage = "Duplicate IP address provided to the application! " + candidateIPAddress.getValue();
    			regDelFlagMap.put("reservedIPAddress", new IllegalArgumentException(exMessage));
			}
		}
	}

	@Override
	public void removeIPAddress(String candidateIPAddress) {
		/* Validate the IP address value before usage */
		/* Check the well-formedness */
		boolean ipAddressWellFormed = validateIPAddress(candidateIPAddress);
		/* Clear the flag map for avoiding any overwrites from previous invocations */
		regDelFlagMap.clear();
		/* Check if the given address is present in the DB */
		IPAddress possiblyRegisteredAddress = ipAddressDao.findIPAddressDetailsForValue(candidateIPAddress);
		if (possiblyRegisteredAddress == null) {
			LOGGER.error("The provided IP address has not yet been registered in the application database!", candidateIPAddress);
			String exMessage = "Unregistered IP address provided to the application! " + candidateIPAddress;
			regDelFlagMap.put("unassignedIPAddress", new IllegalArgumentException(exMessage));
		} else {
			boolean matchResult = checkIfIPAddressAlreadyRegistered(candidateIPAddress, possiblyRegisteredAddress.getValue());
			if (ipAddressWellFormed && matchResult){
				ipAddressDao.removeIPAddressForClient(candidateIPAddress);
			} else {
				if (!ipAddressWellFormed){
					LOGGER.error("Malformed or invalid IP address provided to the application!", candidateIPAddress);
	    			String exMessage = "Malformed or invalid IP address provided to the application! " + candidateIPAddress;
	    			regDelFlagMap.put("faultyIPAddressValue", new IllegalArgumentException(exMessage));
				}
				if (!matchResult){
					LOGGER.error("The provided IP address has not yet been registered for any client!", candidateIPAddress);
	    			String exMessage = "Unregistered IP address provided to the application! " + candidateIPAddress;
	    			regDelFlagMap.put("unassignedIPAddress", new IllegalArgumentException(exMessage));
				}
			}
		}				
	}	

}