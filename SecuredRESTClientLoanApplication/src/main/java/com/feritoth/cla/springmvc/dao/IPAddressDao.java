package com.feritoth.cla.springmvc.dao;

import java.util.List;

import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;

public interface IPAddressDao {
	
	List<IPAddress> getAllIPAddresses();
	
	List<IPAddress> getAllIPAddressesForClient(Client searchedClient);
	
	IPAddress findIPAddressDetailsForValue(String ipAddressValue);
	
	//IPAddress registerNewIPAddress(String ipAddressValue, Client matchingClient);
	void saveNewIPAddress(IPAddress newIPAddress);
	
	//boolean updateExistingIPAddress(String newIPAddress, Client client);
	
	//boolean removeIPAddressForClient(String ipAddress, Client assignedClient);
	void removeIPAddressForClient(String ipAddress);
	
	List<String> getAllIPValues();

}