package com.feritoth.cla.springmvc.jsonmodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.feritoth.cla.springmvc.dbmodel.Client;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SerializedClient implements Serializable{
	
	private static final long serialVersionUID = -1915534729104872881L;
	
	private String cnp;
	private String name;
	private String emailAddress;
	private String postalAddress;	
	private List<SerializedIPAddress> allClientIPs = new ArrayList<>();
	
	public SerializedClient() {
		super();		
	}

	public SerializedClient(String cnp, String name, String emailAddress, String postalAddress) {
		super();
		this.cnp = cnp;
		this.name = name;
		this.emailAddress = emailAddress;
		this.postalAddress = postalAddress;
	}

	public SerializedClient(Client client){
		super();
		this.cnp = client.getCnp();
		this.name = client.getName();
		this.emailAddress = client.getEmailAddress();
		this.postalAddress = client.getPostalAddress();	
	}

	public String getCnp() {
		return cnp;
	}

	public String getName() {
		return name;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPostalAddress() {
		return postalAddress;
	}	

	public void setCnp(String cnp) {
		this.cnp = cnp;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public List<SerializedIPAddress> getAllClientIPs() {
		return Collections.unmodifiableList(allClientIPs);
	}	
	
	@Override
	public String toString() {
		return "Client [cnp=" + cnp + ", name=" + name + ", emailAddress=" + emailAddress + ", postalAddress=" + postalAddress + "]";
	}
	
}