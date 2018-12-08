package com.feritoth.cla.springmvc.dao.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.feritoth.cla.springmvc.dao.IPAddressDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;

@Repository("ipAddressDao")
public class IPAddressHibernateDao extends AbstractHibernateDao implements IPAddressDao {

	@SuppressWarnings("unchecked")
	@Override
	public List<IPAddress> getAllIPAddresses() {
		Criteria criteria = getSession().createCriteria(IPAddress.class);
		List<IPAddress> allIPs = (List<IPAddress>)criteria.list();
		allIPs.forEach(ipAddress -> Hibernate.initialize(ipAddress.getClient()));				
		return allIPs; 
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IPAddress> getAllIPAddressesForClient(Client searchedClient) {
		Criteria criteria = getSession().createCriteria(IPAddress.class);
		criteria.add(Restrictions.eq("client", searchedClient));
		List<IPAddress> allAssignedIPs = (List<IPAddress>)criteria.list();
		allAssignedIPs.forEach(ipAddress -> ipAddress.setClient(searchedClient));
		return allAssignedIPs;
	}
	
	@Override
	public IPAddress findIPAddressDetailsForValue(String ipAddressValue) {
		Criteria criteria = getSession().createCriteria(IPAddress.class);
		criteria.add(Restrictions.eq("value", ipAddressValue));
		IPAddress matchingAddress = (IPAddress) criteria.uniqueResult();
		if (matchingAddress != null) {
			Hibernate.initialize(matchingAddress.getClient());
		}		
		return matchingAddress;
	}

	@Override
	public void saveNewIPAddress(IPAddress newIPAddress) {
		persist(newIPAddress);
	}

	@Override
	public void removeIPAddressForClient(String ipAddress) {
		Query addressRemovalQuery = getSession().createSQLQuery("delete from IPAddress where value=:value");
		addressRemovalQuery.setString("value", ipAddress);
		addressRemovalQuery.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getAllIPValues() {
		Query allIPquery = getSession().createSQLQuery("select value from IPAddress");
		List<String> allRegisteredIPs = allIPquery.list();
		return allRegisteredIPs;
	}
	
}