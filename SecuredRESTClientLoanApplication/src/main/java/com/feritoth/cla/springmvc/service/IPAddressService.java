package com.feritoth.cla.springmvc.service;

import java.util.List;
import java.util.Map;

import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;

public interface IPAddressService {
	
	List<IPAddress> fetchAllAvailableIPAddresses();
	
	List<IPAddress> fetchAllAssignedIPAddressesForClient(Client client);
	
	void registerIPAddress(IPAddress ipAddress);
	
	void removeIPAddress(String ipAddress);
	
	boolean validateIPAddress(String ipAddress);
	
	IPAddress findDetailsForIPvalue(String ipAddress);
	
	Map<String, Exception> getRegDelFlagMap();

}