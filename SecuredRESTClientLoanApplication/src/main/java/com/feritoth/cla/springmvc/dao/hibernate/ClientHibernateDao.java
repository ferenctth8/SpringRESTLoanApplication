package com.feritoth.cla.springmvc.dao.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.feritoth.cla.springmvc.dao.ClientDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;

@Repository("clientDao")
public class ClientHibernateDao extends AbstractHibernateDao implements	ClientDao {	

	@SuppressWarnings("unchecked")
	@Override
	public List<Client> getAllRegisteredClients() {
		Criteria criteria = getSession().createCriteria(Client.class);
		List<Client> clientList = (List<Client>) criteria.list();		
        return clientList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Client> getMatchingClients(String nameSequence) {
		String finalSequence = "%" + nameSequence + "%";
		Criteria criteria = getSession().createCriteria(Client.class);
		criteria.add(Restrictions.like("name", finalSequence));
		List<Client> matchingClientList = (List<Client>)criteria.list();		
		return matchingClientList;
	}

	@Override
	public Client findClientByCNP(String cnp) {		
		Criteria criteria = getSession().createCriteria(Client.class);
        criteria.add(Restrictions.eq("cnp",cnp));
        Client matchingClient = (Client) criteria.uniqueResult();
        return matchingClient;        
	}

	@Override
	public Client findClientByIP(String ipAddress) {
		Criteria criteria = getSession().createCriteria(IPAddress.class);
		criteria.add(Restrictions.eq("value", ipAddress));		
		IPAddress matchingAddress = (IPAddress) criteria.uniqueResult();
		Client ownerClient = matchingAddress.getClient();
		Hibernate.initialize(ownerClient);		
		return ownerClient;  
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Client> findClientsByEmailAddressFragment(String emailAddressSequence){
		String finalSequence = "%" + emailAddressSequence + "%";
		Criteria criteria = getSession().createCriteria(Client.class);
		criteria.add(Restrictions.like("emailAddress", finalSequence));
		List<Client> matchingClients = (List<Client>)criteria.list();		
		return matchingClients;
	}

	@Override
	public void saveNewClient(Client newClient) {
		persist(newClient);		
	}

	@Override
	public void updateClient(Client selectedClient) {
		update(selectedClient);		
	}

	@Override
	public void removeClient(String cnp) {
		Query clientRemovalQuery = getSession().createSQLQuery("delete from Client where cnp=:cnp");
		clientRemovalQuery.setString("cnp", cnp);
		clientRemovalQuery.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getAllClientCNPs() {
		Query allCNPquery = getSession().createSQLQuery("select cnp from Client");
		List<String> allRegisteredCNPs = allCNPquery.list();
		return allRegisteredCNPs;
	}

}