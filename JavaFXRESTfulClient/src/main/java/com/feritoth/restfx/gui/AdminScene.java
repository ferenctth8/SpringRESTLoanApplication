package com.feritoth.restfx.gui;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.core.SerializedIPAddress;
import com.feritoth.restfx.dispatcher.ClientRESTDispatcher;
import com.feritoth.restfx.dispatcher.IPAddressRESTDispatcher;
import com.feritoth.restfx.utilities.ExceptionInfo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminScene extends Scene {

	/* The attribute list of the current class - graphical components */
	private VBox adminBox;//the support component
	//private GridPane adminPane;
	private HBox registrationBox;//the secondary support component A
	private HBox searchRemovalBox;//the secondary support component B
	private HBox variousOpBox;//the secondary support component C
	private TableView<SerializedClient> mainClientTable;//the table of clients
	//the components required for the insertion of a client
	private TextField nameInputField;
	private TextField userPINfield;
	private TextField emailAddressField;
	private TextField postalAddressField;
	private Button registrationButton;
    //the component required for the removal of a client
	private Button removalButton;
	//the component required for returning to the main frame
	private Button returnButton;
	//the component required for the update of the given client
	private Button updateButton;
	//the component required for detail display
	private Button detailButton;
	private Stage clientDetailWindow;
	private Scene clientDetailScene;
	//the components required for the search of certain clients
	private TextField filterField;
	private Button searchButton;
	private ComboBox<String> searchCriteriaBox;
	private TableView<SerializedClient> filteredClientTable;

	/* A logger - used exclusively for debugging purposes */
	//private static final Logger LOGGER = LoggerFactory.getLogger(AdminScene.class);
	/* A String array list for holding the elements to be inserted in the combo box */
	private static final List<String> COMBO_CHOICES = Arrays.asList("Filter By Name", "Filter By Email", "Filter By Both");

	/* The constructor */
	public AdminScene(Parent root, int width, int height) {
		/* Call the superclass constructor */
		super(root, width, height);
		/* Instantiate the corresponding pane */
		adminBox = (VBox) root;
		//adminPane = (GridPane) root;
		/* Call a method for continuing the instantiation process - load the data related to the clients into a table */
		createClientTableModel();
		/* Continue by calling a method related to the registration of a new client */
		createClientOperationForm();
	}

	/* The table setup method */
	@SuppressWarnings("unchecked")
	private void createClientTableModel() {
		/* Get the list of clients who are currently registered in the application */
		List<SerializedClient> allRegisteredClients = ClientRESTDispatcher.getAllRegisteredClients();
		/* Check if the client list is empty and display a suitable warning for it */
		if (allRegisteredClients.isEmpty()){
			Alert adminWarningAlert = new Alert(AlertType.WARNING);
			adminWarningAlert.setTitle("No Users Available");
			adminWarningAlert.setContentText("No users have been found in the application database!");
			adminWarningAlert.showAndWait();
            generateAlert(AlertType.WARNING, "No Users Available", "No users have been found in the application database!", false);
			return;
		}
		/* Otherwise, proceed with the table view creation and population */
		mainClientTable = createClientTable(allRegisteredClients);

        /* Allow as well the main table to be editable first */
		mainClientTable.setEditable(true);

		/* Make the cells for the client name, client email and and client address editable on the main table */
		mainClientTable.getColumns().get(0).setEditable(true);
		mainClientTable.getColumns().get(2).setEditable(true);
		mainClientTable.getColumns().get(3).setEditable(true);

		/* Attach to these columns the corresponding listener method */
		mainClientTable.getColumns().get(0).setOnEditCommit(e -> changeNameCellEvent((CellEditEvent<SerializedClient, String>) e));
		mainClientTable.getColumns().get(2).setOnEditCommit(e -> changeEmailCellEvent((CellEditEvent<SerializedClient, String>) e));
		mainClientTable.getColumns().get(3).setOnEditCommit(e -> changeAddressCellEvent((CellEditEvent<SerializedClient, String>) e));

		/* Add the generated table to the main pane in order to make it visible */
		adminBox.getChildren().addAll(mainClientTable);

		//adminPane.getChildren().addAll(clientTable);
	}

	/* Create here the methods used for the update and its revert on the chosen columns */
	//For the name
	private void changeNameCellEvent(CellEditEvent<SerializedClient, String> editedCell){
		/* Get the selected client from the table and change its name */
		SerializedClient selectedClient = mainClientTable.getSelectionModel().getSelectedItem();
		selectedClient.setNameField(editedCell.getNewValue().toString());
	}

	//For the email
	private void changeEmailCellEvent(CellEditEvent<SerializedClient, String> editedCell){
		/* Get the selected client from the table and change its e-mail */
		SerializedClient selectedClient = mainClientTable.getSelectionModel().getSelectedItem();
		selectedClient.setEmailField(editedCell.getNewValue().toString());
	}

	//For the postal address
	private void changeAddressCellEvent(CellEditEvent<SerializedClient, String> editedCell){
		/* Get the selected client from the table and change its postal address */
		SerializedClient selectedClient = mainClientTable.getSelectionModel().getSelectedItem();
		selectedClient.setAddressField(editedCell.getNewValue().toString());
	}

	/* The method taking care of the instantiation and distribution across the GUI of the client operations */
	private void createClientOperationForm() {
		/* Assemble together the client registration form components */
		assembleClientRegistrationPart();
		/* Next attach the previously created ensemble to the main frame */
		adminBox.getChildren().add(registrationBox);

		/* Assemble together the form for the other operations - update, removal, detail display and return */
		assembleOtherClientOperationsPart();
		/* Add this ensemble next to the main frame */
		adminBox.getChildren().add(variousOpBox);

		/* Create as a second step the new form for client search and removal */
		assembleClientSearchPart();
		/* Add to the main panel the previously instantiated ensemble and the search result table */
		adminBox.getChildren().addAll(searchRemovalBox,filteredClientTable);
	}

	/* The method used for assembling the other operations */
	private void assembleOtherClientOperationsPart() {
		/* For Client Removal */
		/* Add here the button required for removing a given user */
		removalButton = new Button("Remove Selected Client");
		/* Assign to it the corresponding action listener as well */
		removalButton.setOnAction(e -> performClientRemoval());

		/* Add a second button: for detail display of a given client */
		detailButton = new Button("Show details of selected client");
		detailButton.setOnAction(e -> showSelectedClientProfile());

		/* Add a third button: for return to the main frame - here just create the button itself (action assigned to it in the main gate frame) */
		returnButton = new Button("Return to application main gate");

		/* Add a fourth button: for client update confirmation */
		updateButton = new Button("Update Selected Client");
		/* Attach to it the corresponding action listener */
		updateButton.setOnAction(e -> performClientUpdate());

		/* Group together these 4 buttons into the corresponding support container */
		variousOpBox = new HBox();
		variousOpBox.setPadding(new Insets(25,25,25,25));
		variousOpBox.setSpacing(25);
		variousOpBox.getChildren().addAll(detailButton, updateButton, removalButton, returnButton);
	}

	/* A method for assembling the search and removal part */
	@SuppressWarnings("unchecked")
	private void assembleClientSearchPart() {
		/* For Client Search */
		/* Create and populate the combo box first*/
		searchCriteriaBox = new ComboBox<>(FXCollections.observableArrayList(COMBO_CHOICES));
		searchCriteriaBox.setPromptText("Select your filter criteria");

		/* Next add there the text field required for searching */
		filterField = new TextField();
		filterField.setPromptText("Your search parameter");
		filterField.setMaxWidth(175);
		filterField.setTooltip(new Tooltip("Please enter here your searched character sequence"));

		/* Next add there the button required for activating the filter operation */
	    searchButton = new Button("Search For Matching Clients");
	    /* Assign to it a corresponding action listener */
	    searchButton.setOnAction(e -> performClientFiltering());

	    /* Create also the table to be populated as result of the search */
	    filteredClientTable = createClientTable(Collections.EMPTY_LIST);

	    /* Put together the created ensemble and attach it to the main panel */
	    searchRemovalBox = new HBox();
	    searchRemovalBox.setPadding(new Insets(20,20,20,20));
	    searchRemovalBox.setSpacing(20);
	    searchRemovalBox.getChildren().addAll(searchCriteriaBox, filterField, searchButton);
	}

	/* The client registration form assembler method */
	private void assembleClientRegistrationPart() {
		/* For Client Registration */
		/* Create all the 4 text fields first */
		/* In all 4 cases: set a minimum/maximum field length + add a tooltip text and a prompt text for facilitating the desired operation */
		//for the client name
		nameInputField = new TextField();
		nameInputField.setPromptText("John Doe");
		nameInputField.setMinWidth(100);
		nameInputField.setTooltip(new Tooltip("Enter your name here based on the format of prompt pattern"));

		//for the client PIN/CNP
		userPINfield = new TextField();
		userPINfield.setPromptText("123456789000");
		userPINfield.setMaxWidth(125);
		userPINfield.setTooltip(new Tooltip("Enter your 12-digit PIN here based on the format of prompt patern"));

		//for the client email
		emailAddressField = new TextField();
		emailAddressField.setPromptText("ferenc7612@gmail.com");
		emailAddressField.setMinWidth(100);
		emailAddressField.setTooltip(new Tooltip("Enter your e-mail here based on the format of prompt pattern"));

		//for the client postal address
		postalAddressField = new TextField();
		postalAddressField.setPromptText("Senohrabska 2, Praha 4 Zabehlice");
		postalAddressField.setMinWidth(200);
		postalAddressField.setTooltip(new Tooltip("Enter your postal address here based on the format of prompt pattern"));

		/* Add the button required for finalizing the given operation */
		registrationButton = new Button("Register New Client");
		/* Add an action listener to the given button */
		registrationButton.setOnAction(e -> performClientRegistration());

		/* Now glue together these components into a form to be attached to the main frame */
		/* Make also the necessary size adjustments */
		registrationBox = new HBox();
		registrationBox.setPadding(new Insets(10, 10, 10, 10));
		registrationBox.setSpacing(10);
		registrationBox.getChildren().addAll(nameInputField, userPINfield, emailAddressField, postalAddressField, registrationButton);
	}

	/* Method for performing the client filtering */
	private void performClientFiltering() {
		/* Pick the value of the filter parameter first */
		String filterParameter = filterField.getText();
		/* For a blank parameter, display a corresponding warning */
		if (StringUtils.isBlank(filterParameter)){
			generateAlert(AlertType.WARNING, "No Search Text Submitted", "Currently, no key phrase given for search! Please type something to continue!", false);
		}
		/* Otherwise, proceed next with the combo choice analysis */
		String comboSearchOption = searchCriteriaBox.getValue();
		if (StringUtils.isBlank(comboSearchOption)){
			generateAlert(AlertType.WARNING, "No Search Criterion Chosen", "Currently, no search criterion selected! Please chooose something to continue!", false);
		}
		/* For successful surpass of the safety validation, proceed as follows */
		List<SerializedClient> matchingClients = ClientRESTDispatcher.filterClients(filterParameter, comboSearchOption);
		/* Display an informative warning for no search results returned */
		if (matchingClients.isEmpty()){
			generateAlert(AlertType.INFORMATION, "No Search Results Available", "Your current search attempt returned no results! Please try again with a different pattern!", false);
		}
		/* Use next the generated list for recreating the search table */
		filteredClientTable.getItems().clear();
		filteredClientTable.setItems(FXCollections.observableArrayList(matchingClients));
	}

	/* Method for displaying the details of a client profile */
    private void showSelectedClientProfile() {
		/* Pick the selected client */
    	SerializedClient selectedClient = mainClientTable.getSelectionModel().getSelectedItem();
		/* Check if it is null */
    	if (selectedClient == null){
    		/* In affirmative case, display a suitable warning message */
			generateAlert(AlertType.WARNING, "No Client to Display", "Currently, no client has been selected for display! Please chooose something to continue!", false);
    	} else {
    		/* Create the final display frame and its associated scene */
    		clientDetailWindow = new Stage();
    		clientDetailScene = new ClientDetailScene(selectedClient, new VBox(), 1410, 400);
    		/* Set the most important properties of this new frame */
    		clientDetailWindow.setTitle("Personal profile of " + selectedClient.getName());
    		clientDetailWindow.setScene(clientDetailScene);
    		clientDetailWindow.setResizable(false);
    		/* Display the window and disable the detail, removal and update buttons until returning from the current profile */
    		detailButton.setDisable(true);
    		removalButton.setDisable(true);
    		updateButton.setDisable(true);
    		/* Take care of implementing a proper way of returning to this main part from the newly opened frame */
    		clientDetailWindow.setOnCloseRequest(e -> {
            	 /* Enable back the three buttons previously disabled */
    			 detailButton.setDisable(false);
            	 removalButton.setDisable(false);
         		 updateButton.setDisable(false);
            });
    		/* Display the given window */
    		clientDetailWindow.show();
    	}
	}

	/* Method for execution of client update */
	private void performClientUpdate() {
		/* Get the selected client from the table */
		SerializedClient selectedClient = mainClientTable.getSelectionModel().getSelectedItem();
		/* Check if the client being selected is not null and act accordingly based upon it */
		if (selectedClient == null){
			generateAlert(AlertType.WARNING, "No Client to Update", "Currently, no client has been selected for update! Please chooose something to continue!", false);
		} else {
			/* Call the dispatcher method for updating the selected client */
			Object dispatchingResult = ClientRESTDispatcher.updateSelectedClient(selectedClient.getCnp(), selectedClient);
			if (dispatchingResult instanceof ExceptionInfo){
            	/* For the case of an exception detected, a corresponding alert box will be shown with information taken from the returned exception */
            	ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
            	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
  			                  dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
            	/* Put back the original values to the places where the update was allowed - fetch back for this the initially registered users */
            	mainClientTable.getItems().clear();
            	List<SerializedClient> allRegisteredClients = ClientRESTDispatcher.getAllRegisteredClients();
            	mainClientTable.setItems(FXCollections.observableArrayList(allRegisteredClients));
            } else if (dispatchingResult instanceof SerializedClient){
            	generateAlert(AlertType.INFORMATION, "Selected User Updated", "The selected user has been updated!Its newly recorded personal data are:\n" + ((SerializedClient)dispatchingResult).toString(), true);
            }
		}
	}

	/* Method for performing the client removal */
	private void performClientRemoval() {
		/* Declare here the lists of available and selected clients */
		ObservableList<SerializedClient> selectedClients, allClients;
		/* Initialize the lists as follows */
		allClients = mainClientTable.getItems(); //available clients
		selectedClients = mainClientTable.getSelectionModel().getSelectedItems();
		/* Check if the list of selected users is empty or not before proceeding with the operation */
		if (selectedClients.isEmpty()){
			/* Display a warning for affirmative case */
			generateAlert(AlertType.WARNING, "No User Chosen For Removal", "Currently you have no selected user for removal! Please choose one in order to continue!", true);
		} else {
			/* Perform the removal of the selected client - use the new Java 8 feature */
			selectedClients.forEach(client -> {
				/* Ask if the client has any IP addresses assigned to him/her - need to remove these first before proceeding */
				List<SerializedIPAddress> allIPsForClient = IPAddressRESTDispatcher.getAllIPAddressesAssignedToClient(client.getCnp());
				if (!allIPsForClient.isEmpty()){
					generateAlert(AlertType.ERROR, "Valid IP addresses found", "This user has been detected as registered with valid IP addresses and possible loans! Please remove these first before proceeding!", true);
				} else {
					/* First, invoke the operation for the removal of the given client from the back-end */
					Object dispatchingResult = ClientRESTDispatcher.removeExistingClient(client.getCnp());
					/* Then check the outcome of the given operation */
					if (dispatchingResult instanceof ExceptionInfo){
						/* For the case of an exception detected, a corresponding alert box will be shown with information taken from the returned exception */
		            	ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
		            	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
	            			          dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
					} else if (dispatchingResult instanceof String) {
						/* For a successful removal - display an information dialog */
						generateAlert(AlertType.INFORMATION, "Existing User Removed", (String) dispatchingResult, true);
		            	/* Afterwards remove the given element from the GUI list as well */
		            	allClients.remove(client);
					}
				}
			});
		}
	}

	/* Method responsible for the client registration */
	private void performClientRegistration() {
		/* Get the content of all 4 text fields */
		String clientName = nameInputField.getText();
		String userPIN = userPINfield.getText();
		String clientEmailAddress = emailAddressField.getText();
		String clientPostalAddress = postalAddressField.getText();

		/* Check that they are all filled out - or else display a suitable warning for each case */
		boolean nameOK = checkClientField(clientName, 1);
		boolean pinOK = checkClientField(userPIN, 2);
		boolean emailOK = checkClientField(clientEmailAddress, 3);
		boolean contactOK = checkClientField(clientPostalAddress, 4);

		/* Proceed only in case of all 4 fields having a correct value */
		if (nameOK || pinOK || emailOK || contactOK){
			generateAlert(AlertType.INFORMATION, "Defect Remediation Required", "Please review again the submitted information before proceeding!", false);
		} else {
			/* Create the client to be dispatched */
            SerializedClient newClient = new SerializedClient(userPIN, clientName, clientEmailAddress, clientPostalAddress);
            /* Invoke the dispatcher method and analyze the results */
            Object dispatchingResult = ClientRESTDispatcher.registerNewClient(newClient);
            if (dispatchingResult instanceof ExceptionInfo){
            	/* For the case of an exception detected, a corresponding alert box will be shown with information taken from the returned exception */
            	ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
            	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
            			      dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
            } else if (dispatchingResult instanceof URI){
            	/* For the case of a successful registration: */
            	/* 1. First display a dialog with the operation outcome */
            	URI resultURI = (URI) dispatchingResult;
            	generateAlert(AlertType.INFORMATION, "New User Created", "The new user has been inserted and can be found on link:\n" + ClientRESTDispatcher.REST_SERVICE_URI + resultURI.toString(), true);
            	/* 2. Clear the initial table content and retrieve it again from the DB together with the new recording */
            	mainClientTable.getItems().clear();
            	List<SerializedClient> allRegisteredClients = ClientRESTDispatcher.getAllRegisteredClients();
            	mainClientTable.setItems(FXCollections.observableArrayList(allRegisteredClients));
            	/* 3. Clear the fields used in the registration process */
            	nameInputField.clear();
            	userPINfield.clear();
            	emailAddressField.clear();
            	postalAddressField.clear();
            }
		}
	}

	/* A method for content checking */
	private boolean checkClientField(String clientFieldValue, int registrationToken){
		if (StringUtils.isBlank(clientFieldValue)){
			Alert registrationWarningAlert = new Alert(AlertType.WARNING);
			registrationWarningAlert.setTitle("Empty Data Field");
			switch(registrationToken){
			case 1:
				registrationWarningAlert.setContentText("Empty Value detected for the client name!");
				break;
			case 2:
				registrationWarningAlert.setContentText("Empty Value detected for the client PIN!");
				break;
			case 3:
				registrationWarningAlert.setContentText("Empty Value detected for the client email!");
				break;
			case 4:
				registrationWarningAlert.setContentText("Empty Value detected for the client contact address!");
				break;
			}
			registrationWarningAlert.showAndWait();
			return true;
		}
		return false;
	}

	/* A generic table creator method */
	@SuppressWarnings("unchecked")
	private TableView<SerializedClient> createClientTable(List<SerializedClient> clientList){
		/* Create the main client table to be returned */
		TableView<SerializedClient> newClientTableView = new TableView<SerializedClient>();
		newClientTableView.setItems(FXCollections.observableArrayList(clientList));

		/* Next setup the columns which would be used for representation, together with their main properties */
		//for the name
		TableColumn<SerializedClient, String> nameColumn = new TableColumn<>("Name");
		nameColumn.setMaxWidth(200);
		nameColumn.setMinWidth(150);
		nameColumn.setResizable(false);
		nameColumn.setEditable(false);
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

		//for the 12-digit PIN
		TableColumn<SerializedClient, String> pinColumn = new TableColumn<>("CNP");
		pinColumn.setMaxWidth(130);
		pinColumn.setMinWidth(110);
		pinColumn.setResizable(false);
		pinColumn.setEditable(false);
		pinColumn.setCellValueFactory(new PropertyValueFactory<>("cnp"));
		pinColumn.setCellFactory(TextFieldTableCell.forTableColumn());

		//for the email address
		TableColumn<SerializedClient, String> emailColumn = new TableColumn<>("E-mail Address");
		emailColumn.setMaxWidth(440);
		emailColumn.setMinWidth(220);
		emailColumn.setEditable(false);
		emailColumn.setCellValueFactory(new PropertyValueFactory<>("emailAddress"));
		emailColumn.setCellFactory(TextFieldTableCell.forTableColumn());

		//for the postal address
		TableColumn<SerializedClient, String> addressColumn = new TableColumn<>("Postal Address");
		addressColumn.setMaxWidth(600);
		addressColumn.setMinWidth(300);
		addressColumn.setEditable(false);
		addressColumn.setCellValueFactory(new PropertyValueFactory<>("postalAddress"));
		addressColumn.setCellFactory(TextFieldTableCell.forTableColumn());

		/* Add the previously generated column structure to the table */
		newClientTableView.getColumns().addAll(nameColumn, pinColumn, emailColumn, addressColumn);

		/* Return the created ensemble */
		return newClientTableView;
	}

	/* Getter methods for the return button - its action listener is defined in a different location */
	public Button getReturnButton() {
		return returnButton;
	}

	/* An alert generator method */
	private void generateAlert(AlertType alertType, String title, String contentText, boolean isResizable){
		Alert newAlert = new Alert(alertType);
		newAlert.setTitle(title);
		newAlert.setContentText(contentText);
		newAlert.setResizable(isResizable);
		newAlert.showAndWait();
	}

}