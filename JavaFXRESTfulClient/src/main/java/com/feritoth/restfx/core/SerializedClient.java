package com.feritoth.restfx.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import javafx.beans.property.SimpleStringProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SerializedClient implements Serializable{

	/* The serialization UID generated for the current class */
	private static final long serialVersionUID = -3051832266825173865L;

	/* List of specific attributes for this entity */
	private String cnp;
	private String name;
	private String emailAddress;
	private String postalAddress;
	private List<SerializedIPAddress> allAssignedIPs = new ArrayList<>();

	/* Put here the attributes used for the update listeners */
	@JsonIgnore
	private SimpleStringProperty nameField;
	@JsonIgnore
	private SimpleStringProperty emailField;
	@JsonIgnore
	private SimpleStringProperty addressField;

	/* Default class constructor */
	public SerializedClient() {
		super();
	}

	/* Parameterized class constructor */
	public SerializedClient(String cnp, String name, String emailAddress, String postalAddress) {
		super();
		this.cnp = cnp;
		this.name = name;
		this.emailAddress = emailAddress;
		this.postalAddress = postalAddress;
		initializeListenerFields(name, emailAddress, postalAddress);
	}

	/* Update listener initializer method */
	private void initializeListenerFields(String name, String emailAddress, String postalAddress) {
		this.nameField = new SimpleStringProperty(name);
		this.emailField = new SimpleStringProperty(emailAddress);
		this.addressField = new SimpleStringProperty(postalAddress);
	}

	/* Getters and setters for regular fields */

	public String getCnp() {
		return cnp;
	}

	public void setCnp(String cnp) {
		this.cnp = cnp;
	}

	public String getName() {
		return nameField.get();
	}

	/*public void setName(String name) {
		this.name = name;
	}*/

	public String getEmailAddress() {
		return emailField.get();
	}

	/*public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}*/

	public String getPostalAddress() {
		return addressField.get();
	}

	/*public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}*/

	public List<SerializedIPAddress> getAllAssignedIPs() {
		return Collections.unmodifiableList(allAssignedIPs);
	}

	/* Getters & setters for the listener fields */

	public SimpleStringProperty getNameField() {
		return nameField;
	}

	public void setNameField(String nameField) {
		this.nameField = new SimpleStringProperty(nameField);
	}

	public SimpleStringProperty getEmailField() {
		return emailField;
	}

	public void setEmailField(String emailField) {
		this.emailField = new SimpleStringProperty(emailField);
	}

	public SimpleStringProperty getAddressField() {
		return addressField;
	}

	public void setAddressField(String addressField) {
		this.addressField = new SimpleStringProperty(addressField);
	}

	/* Overridden version of toString() method */
	@Override
	public String toString() {
		return "SerializedClient [cnp=" + cnp + ", name=" + name + ", emailAddress=" + emailAddress + ", postalAddress=" + postalAddress + "]";
	}

}