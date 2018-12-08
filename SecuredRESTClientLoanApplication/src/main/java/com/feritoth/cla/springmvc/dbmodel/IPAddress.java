package com.feritoth.cla.springmvc.dbmodel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ipaddress")
public class IPAddress implements Serializable {
	
	private static final long serialVersionUID = 959858043549303139L;
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	@Column (name = "IPAddressID")
	private Integer ipAddressID;
	@Column (name = "Value")
	private String value;
	@ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn (name = "ClientCNP", nullable = false)
	private Client client;	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "ipAddress")	
	private List<Loan> allLoans;
	
	public IPAddress() {
		super();		
	}

	public IPAddress(String value, Client client) {
		super();
		this.value = value;
		this.client = client;
	}	

	public IPAddress(Integer ipAddressID, String value, Client client, List<Loan> allLoans) {
		super();
		this.ipAddressID = ipAddressID;
		this.value = value;
		this.client = client;
		this.allLoans = allLoans;
	}

	public Integer getIpAddressID() {
		return ipAddressID;
	}

	public void setIpAddressID(Integer ipAddressID) {
		this.ipAddressID = ipAddressID;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}	

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public List<Loan> getAllLoans() {
		return new LinkedList<>(allLoans);
	}

	public void setAllLoans(List<Loan> allLoans) {
		this.allLoans = allLoans;
	}	

	@Override
	public int hashCode() {		
		String[] components = value.split("\\.");
		return (Integer.parseInt(components[0]) + Integer.parseInt(components[1]) + Integer.parseInt(components[2]) + Integer.parseInt(components[3])) % 31;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPAddress other = (IPAddress) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IPAddress [ipAddressID=" + ipAddressID + ", value=" + value	+ ", client=" + client + "]";
	}	

}