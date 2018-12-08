package com.feritoth.cla.springmvc.service;

import java.util.List;
import java.util.Map;

import com.feritoth.cla.springmvc.dbmodel.Client;

public interface ClientService {
	
	List<Client> findAllRegisteredClients();
	
	List<Client> getMatchingClients(String nameSequence);
	
	Client findClientByCNP(String cnp);
	
	Client findClientByIP(String ipAddress);
	
	List<Client> findClientsByEmailFragment(String emailAddress);
	
	void registerNewClient(Client newClient);
	
	void updateExistingClient(Client selectedClient);
	
	void removeClient(String cnp);
	
	boolean isEmployeeCNPalreadyAssigned(String cnp);
	
	Map<String, Exception> getRegUpFlagMap();

}