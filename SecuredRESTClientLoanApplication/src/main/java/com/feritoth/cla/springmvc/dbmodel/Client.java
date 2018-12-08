package com.feritoth.cla.springmvc.dbmodel;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "client")
public class Client implements Serializable {	
	
	private static final long serialVersionUID = -1915534729104872881L;
	
	@Id
	@Column(name="CNP", nullable = false)
	private String cnp;
	@Column(name="Name", nullable = false)
	private String name;
	@Column(name="EmailAddress", nullable = false)
	private String emailAddress;
	@Column(name="PostalAddress", nullable = false)
	private String postalAddress;    
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
	private List<IPAddress> ipAddresses = new LinkedList<IPAddress>();
	
	//TODO create clientDTO where to put the problematic properties //spring hateoas example to check
	
	public Client() {
		super();		
	}
	
	public Client(Client client){
		this.cnp = client.cnp;
		this.name = client.name;
		this.emailAddress = client.emailAddress;
		this.postalAddress = client.postalAddress;
	}

	public Client(String cnp, String name, String emailAddress, String postalAddress) {
		super();
		this.cnp = cnp;
		this.name = name;
		this.emailAddress = emailAddress;
		this.postalAddress = postalAddress;		
	}

	public Client(String cnp, String name, String emailAddress,	String postalAddress, List<IPAddress> ipAddresses) {
		super();
		this.cnp = cnp;
		this.name = name;
		this.emailAddress = emailAddress;
		this.postalAddress = postalAddress;
		this.ipAddresses = ipAddresses;
	}

	public String getCnp() {
		return cnp;
	}

	public void setCnp(String cnp) {
		this.cnp = cnp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getPostalAddress() {
		return postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public List<IPAddress> getIpAddresses() {
		return Collections.unmodifiableList(ipAddresses);
	}

	public void setIpAddresses(List<IPAddress> ipAddresses) {
		this.ipAddresses = ipAddresses;
	}

	@Override
	public boolean equals(Object client){
		if (this == client){
			return true;
		}
		
		if (client == null){
			return false;
		}
		
		if (!(client instanceof Client)){
			return false;
		}
		
		final Client realClient = (Client) client;
		
		if (this.cnp.equals(realClient.getCnp()) && this.emailAddress.equals(realClient.getEmailAddress()) && this.name.equals(realClient.getName())){
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		int firstHashCodeTerm = emailAddress.hashCode() * 19;
		int secondHashCodeTerm = name.hashCode() * 31;
		String simplifiedCNP = cnp.substring(3,4) + cnp.substring(7,8) + cnp.substring(11,12); 
		int thirdHashCodeTerm = Integer.parseInt(simplifiedCNP) * 2;
		return firstHashCodeTerm + secondHashCodeTerm + thirdHashCodeTerm;
	}
	
	@Override
	public String toString() {
		return "Client [cnp=" + cnp + ", name=" + name + ", emailAddress=" + emailAddress + ", postalAddress=" + postalAddress + "]\n";
	}	

}