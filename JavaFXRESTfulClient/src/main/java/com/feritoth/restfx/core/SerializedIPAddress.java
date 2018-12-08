package com.feritoth.restfx.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SerializedIPAddress implements Serializable {

	private static final long serialVersionUID = 959858043549303139L;

	private Integer ipID;
	private String ipValue;
	private SerializedClient ownerClient;
	private List<SerializedLoan> allLoans = new ArrayList<SerializedLoan>();

	public SerializedIPAddress() {
		super();
	}

	public SerializedIPAddress(String ipValue, Integer id) {
		super();
		this.ipValue = ipValue;
		this.ipID = id;
	}

	public Integer getIpID() {
		return ipID;
	}

	public String getIpValue() {
		return ipValue;
	}

	public SerializedClient getOwnerClient() {
		return ownerClient;
	}

	public void setOwnerClient(SerializedClient ownerClient) {
		this.ownerClient = ownerClient;
	}

	public void setIpID(Integer ipID) {
		this.ipID = ipID;
	}

	public void setIpValue(String ipValue) {
		this.ipValue = ipValue;
	}

	public List<SerializedLoan> getAllLoans() {
		return Collections.unmodifiableList(allLoans);
	}

	@Override
	public String toString() {
		return "SerializedIPAddress [ipID=" + ipID + ", ipValue=" + ipValue + ", ownerClient=" + ownerClient + "]";
	}

}