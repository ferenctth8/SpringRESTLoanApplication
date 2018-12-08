package com.feritoth.cla.springmvc.dao;

import java.util.List;

import com.feritoth.cla.springmvc.dbmodel.Client;

public interface ClientDao {
	
    List<Client> getAllRegisteredClients();
	
	List<Client> getMatchingClients(String nameSequence);
	
	Client findClientByCNP(String cnp);
	
	Client findClientByIP(String ipAddress);
	
	List<Client> findClientsByEmailAddressFragment(String emailAddressFragment);
	
	List<String> getAllClientCNPs();
	
	//Client registerNewClient(String cnp, String name, String emailAddress, String postalAddress);
	void saveNewClient(Client newClient);
	
	//boolean updateClientDetails(String cnp, String emailAddress, String postalAddress);
	void updateClient(Client selectedClient);
	
	//boolean removeClient(String cnp);
	void removeClient(String cnp);

}