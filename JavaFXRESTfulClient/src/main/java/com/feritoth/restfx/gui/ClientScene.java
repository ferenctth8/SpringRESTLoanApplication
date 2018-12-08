package com.feritoth.restfx.gui;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.feritoth.restfx.core.LoanCurrency;
import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.core.SerializedIPAddress;
import com.feritoth.restfx.core.SerializedLoan;
import com.feritoth.restfx.dispatcher.IPAddressRESTDispatcher;
import com.feritoth.restfx.dispatcher.LoanRESTDispatcher;
import com.feritoth.restfx.utilities.ExceptionInfo;
import com.feritoth.restfx.utilities.LoanHistoryExportGenerator;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class ClientScene extends Scene {

	/* The attribute list of the current class */
	/* The selected client whose details will be displayed */
	private SerializedClient displayedClient;
	/* The list of graphical components supported by this part of the application */
	private VBox clientDetailBox;//the main support component
	/* The components responsible for holding the personal client information */
	private GridPane clientInfoPane;//the pane holding the client contact details;
	private Label[] clientInfoLabels;//the set of labels for displaying the personal information related to a client
	private Button returnButton;//the button responsible for returning to the main application gate
	/* The components responsible for displaying the IP addresses and loans registered for a client */
	private HBox ipAddressBox; //the box for displaying the table of registered IP addresses and their associated loans
	private TableView<SerializedIPAddress> assignedIPtableView;//the table of registered IP addresses
	private TableView<SerializedLoan> registeredLoanTableViewPerIP;//the table of loans registered for a given IP address
	/* The components used for operating on the previously specified 2 tables */
	private VBox detailExportBox; //the box for holding the buttons operating on the given tables
    private Button detailButton; //the button responsible for loan display
    private MenuButton exportButton; //the button responsible for creating either a PDF or an Excel file showing the history of the loans associated to the displayed client
    private MenuItem excelExportItem; //the menu item for creating an Excel-based export for the tracked loan history
    private MenuItem pdfExportItem; //the men item for creating a PDF-based export for the tracked loan history
    /* The components responsible for the registration and removal of a given IP address */
    private GridPane ipOperationsPane; //the pane for holding the components used for registering an IP address
    private TextField[] ipRegistrationFields; //the fields used for introducing the numerical values for creating a valid IP address
    private Button ipRegistrationButton; //the button used for registering a given IP address
    private Button ipRemovalButton; //the button used for removing a given IP address
    /* The components responsible for the removal and extension of a given loan */
    private HBox loanOperationsBox; //the box holding the components required for operating on loans
    private Button loanRemovalButton; //the button used for confirmation of loan removal from the DB
    private Button loanExtensionButton; //the button used for confirmation of loan extensions
    /* The components responsible for the registration of a new loan */
    private Button loanRegistrationButton; //the button used for confirming the loan registration
    private TextField loanRegistrationDateField; //the text field containing the current date and time when the loan got registered
    private DatePicker loanReturnDatePicker; //the date picker used for specifying the return date of the loan
    private TextField amountField; //the text field where the user can introduce the amount to be loaned
    private ComboBox<String> currencyBox; //the combo box used for specifying the currency associated to the loan
    private static final String FORMAT_PATTERN_STRING = "yyyy-MM-dd"; //the pattern used for formatting the dates of a given date picker

    /* The constructor */
	public ClientScene(SerializedClient selectedClient, Parent root, double width, double height) {
		/* Call the superclass constructor */
		super(root, width, height);
		/* Instantiate the selected client, the admin scene and the main support component */
		this.displayedClient = selectedClient;
		//this.adminScene = adminScene;
		this.clientDetailBox = (VBox) root;
		/* Call the methods required for initializing the other two support components */
		createClientProfile(); //method for initializing the client profile
		createTableDataView(); //method for initializing the data associated to a client (IP addresses + loans)
		createIPOperationsSection(); //method for assembling the section of operations dedicated to IP addresses
		createLoanOperationsSection(); //method for assembling the section of operations dedicated to loans
	}

	/* The method for creating the section dedicated to loan operations */
	private void createLoanOperationsSection() {
		/* Create first the support box and adjust its most important properties for display */
		loanOperationsBox = new HBox();
		loanOperationsBox.setPadding(new Insets(20,20,20,20));
		loanOperationsBox.setSpacing(20);

		/* Now add there the first component - the removal button */
		loanRemovalButton = new Button("Remove selected loan");
		/* Attach to it an action listener */
		loanRemovalButton.setOnAction(e -> removeSelectedLoan());

		/* Next add the second component - the loan extension button */
		loanExtensionButton = new Button("Extend selected loan");
		/* Attach to it an action listener */
		loanExtensionButton.setOnAction(e -> extendSelectedLoan());

		/* Afterwards, add there all the components responsible for registering a new loan */
		/* First the text field with the current date and time - must be made in-editable for consistency */
		loanRegistrationDateField = new TextField();
		loanRegistrationDateField.setEditable(false);
		LocalDateTime newApplicationDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String formattedApplicationTime = newApplicationDateTime.format(formatter);
		loanRegistrationDateField.setText(formattedApplicationTime);
		/* Next the date picker used for specifying the loan return date */
		loanReturnDatePicker = new DatePicker();
		loanReturnDatePicker.setTooltip(new Tooltip("Pick here a date for returning your loan"));
		loanReturnDatePicker.setPromptText(FORMAT_PATTERN_STRING.toLowerCase());
		/* Add also a pattern converter to the given date picker and define its associated action */
		StringConverter<LocalDate> returnDateConverter = new StringConverter<LocalDate>() {
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(FORMAT_PATTERN_STRING);

			@Override
			public String toString(LocalDate date) {
				if (date != null) {
					return dateFormatter.format(date);
				} else {
					return "";
				}
			}

			@Override
			public LocalDate fromString(String string) {
				if (string != null && !string.isEmpty()) {
					return LocalDate.parse(string, dateFormatter);
				} else {
					return null;
				}
			}
		};
		loanReturnDatePicker.setConverter(returnDateConverter);
		/* Afterwards the text field used for specifying the amount to be loaned */
		amountField = new TextField();
		amountField.setEditable(true);
		amountField.setMaxWidth(60);
		/* Then the combo box for defining the loan currency */
		currencyBox = new ComboBox<>();
		currencyBox.setPromptText("Choose a currency for your loan");
		currencyBox.getItems().addAll("EUR", "CZK");
		/* Add an action listener to the currency box for controlling the tooltip and the prompt text of the field assigned to the loaned sum */
		currencyBox.setOnAction(e -> setAmountFieldTooltip());
		/* Finally conclude by adding the operation confirmation button */
		loanRegistrationButton = new Button("Press for loan registration");
		/* Add to it the relevant action listener */
		loanRegistrationButton.setOnAction(e -> performLoanRegistration());

		/* Add the components into the support container and attach this container to the main panel for display */
		loanOperationsBox.getChildren().addAll(loanRemovalButton, loanExtensionButton, loanReturnDatePicker, loanRegistrationDateField, currencyBox, amountField, loanRegistrationButton);
		clientDetailBox.getChildren().add(loanOperationsBox);
	}

	/* The action listener method used for loan registration */
	private void performLoanRegistration() {
		/* First check to be done: if there is a valid IP address specified for the given loan */
		SerializedIPAddress selectedAddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* For no address specified, a warning needs to be issued */
		if (selectedAddress == null){
			generateAlert(AlertType.WARNING, "No IP Selected for New Loan", "Currently no IP address has been selected for the new loan! Please choose one to continue!", false);
			return;
		}
		/* Next see if the loan return date has been specified correctly */
		LocalDate newReturnDate = loanReturnDatePicker.getValue();
		if (newReturnDate == null){
			/* Again in case of missing info, a relevant warning needs to be displayed */
			generateAlert(AlertType.WARNING, "No or Invalid Return Date", "Invalid loan return date specified for the new loan! Please correct it before proceeding!", false);
			return;
		}
		/* Now check to see if the loan currency has been specified */
		String selectedCurrency = currencyBox.getSelectionModel().getSelectedItem();
		if (selectedCurrency == null){
			/* Display a relevant warning for negative case */
			generateAlert(AlertType.WARNING, "No Loan Currency", "No currency specified for the new loan! Select one for proceeding...", false);
			return;
		}
		/* Finally, examine the loaned amount validity and show a relevant warning for any problems */
		String loanedAmount = amountField.getText();
		Long newLoanedAmount = null;
		LoanCurrency newLoanCurrency = null;
		if (StringUtils.isBlank(loanedAmount)){
			/* Generate a new alert for empty loan amount field */
			generateAlert(AlertType.ERROR, "Empty Loan Amount", "No amount specified for the new loan! Please insert a value to continue!", false);
			return;
		} else {
			/* Try converting the given value to see if it is valid */
			try {
				newLoanedAmount = Long.parseLong(loanedAmount);
			} catch (NumberFormatException e){
				generateAlert(AlertType.ERROR, "Invalid Loan Amount", "Invalid amount specified for the new loan! Please insert a correct numerical value to continue!", false);
				return;
			}
			/* Then examine if the specified value corresponds to the limits of the selected currency */
			newLoanCurrency = LoanCurrency.valueOf(selectedCurrency);
			if ((StringUtils.equalsIgnoreCase(newLoanCurrency.getCurrencyPrefix(), "CZK") && newLoanedAmount < 1000) ||
				(StringUtils.equalsIgnoreCase(newLoanCurrency.getCurrencyPrefix(), "EUR") && newLoanedAmount < 500)){
				generateAlert(AlertType.WARNING, "Invalid Loan Amount", "Value below minimum allowed limit specified for the selected loan currency! Please insert a proper value for proceeding!", false);
				return;
			}
		}

		/* In case of bypassing successfully all error conditions, the next thing will be to create the loan object for dispatching */
		SerializedLoan newLoan = new SerializedLoan();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime applicationDateTime = LocalDateTime.parse(loanRegistrationDateField.getText(), formatter);
		newLoan.setApplicationTime(applicationDateTime); //the application date time
		newLoan.setCurrency(newLoanCurrency); //the currency value
		newLoan.setIpAddress(selectedAddress); //the IP address used for selection
		newLoan.setLoanedAmount(newLoanedAmount); //the loaned amount of money
		newLoan.setReturnDate(newReturnDate); //the return date of the loan
        newLoan.getIpAddress().setOwnerClient(displayedClient); //the client for whom the new loan shall be registered
		/* Send the loan to the server to be recorded */
		Object loanDispatchingResult = LoanRESTDispatcher.registerNewLoan(newLoan);
		/* Check next what kind of result has been given back by the corresponding dispatching service */
		if (loanDispatchingResult instanceof ExceptionInfo){
			/* In case of receiving an exception, convert it to the corresponding alert */
			ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) loanDispatchingResult;
			generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
					      dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
		} else if (loanDispatchingResult instanceof URI){
			/* For a successfully completed operation, perform the following steps */
			/* 1. Generate a dialog with the outcome message of the operation */
			URI resultURI = (URI) loanDispatchingResult;
			generateAlert(AlertType.INFORMATION, "New Loan Registered", "The details of the newly registered loan can be found at:\n" + LoanRESTDispatcher.REST_SERVICE_URI + resultURI.toString(), true);
			/* 2. Reload the content of the loan table for the previously selected IP address */
			registeredLoanTableViewPerIP.getItems().clear();
			List<SerializedLoan> allRegisteredLoansForGivenIP = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedAddress.getIpValue());
			registeredLoanTableViewPerIP.setItems(FXCollections.observableArrayList(allRegisteredLoansForGivenIP));
			/* 3. Clear/Reset all the fields where manual value introduction was required */
			//the date picker
			loanReturnDatePicker.getEditor().clear();
			loanReturnDatePicker.setValue(null);
			//the amount field
			amountField.clear();
			//the loan registration date field
			LocalDateTime newApplicationDateTime = LocalDateTime.now();
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String formattedApplicationTime = newApplicationDateTime.format(formatter);
			loanRegistrationDateField.setText(formattedApplicationTime);
		}
	}

	/* An action listener method for setting the tooltip and the prompt texts of the amount field based on the loan currency */
	private void setAmountFieldTooltip() {
		/* Pick the selected currency and switch the tooltip and the prompt texts accordingly */
		String selectedCurrency = currencyBox.getSelectionModel().getSelectedItem();
		switch(selectedCurrency){
		case "EUR":
			amountField.setPromptText("500");
			amountField.setTooltip(new Tooltip("Please insert here a value between 500 and 15000"));
			break;
		case "CZK":
			amountField.setPromptText("1000");
			amountField.setTooltip(new Tooltip("Please insert here a value between 1000 and 30000"));
			break;
		}
	}

	/* The method used for extending a selected loan */
	private void extendSelectedLoan() {
		/* First step - Pick the IP address from where to get the loan */
		SerializedIPAddress selectedIPaddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* Check if this item has been marked as selected */
		if (selectedIPaddress == null){
			/* Generate a suitable warning message for this case and return */
			generateAlert(AlertType.WARNING, "No IP Address Assigned", "Currently, no IP address has been selected for choosing a loan! Pick a correct one before proceeding!", false);
			return;
		}
		/* Second step - pick the loan to be extended from the loan table */
		SerializedLoan selectedLoan = registeredLoanTableViewPerIP.getSelectionModel().getSelectedItem();
		/* Check if this loan is null next */
		if (selectedLoan == null){
			/* Generate a suitable warning if there is no loan selected and return */
			generateAlert(AlertType.WARNING, "No Loan to Extend", "Currently, no loan has been selected for extension! Please choose something to continue!", false);
			return;
		}
		/* Third step - check if the new return date specified in the date picker is correct */
		LocalDate newReturnDate = loanReturnDatePicker.getValue();
		if (newReturnDate == null){
			/* For a null value, generate another suitable warning and return */
			generateAlert(AlertType.WARNING, "No Extension Date", "No new return date has been specified for the desired extension! Please pick a date before continuing!", false);
			return;
		}
		/* Fourth step - check if the new and the current return date match: in this case no update is to be made, just a warning to be displayed */
		if (newReturnDate.equals(selectedLoan.getReturnDate())){
			/* For selecting the same return date, no update is to be made - just a warning will be issued */
			generateAlert(AlertType.WARNING, "Same Return Date", "The same return date has been specified for the desired extension! No update will be done for avoiding an interest rate overflow!", false);
			return;
		}
		/* For both prerequisites successfully fulfilled, invoke the given operation */
		/* First set the new date for this local reference */
		selectedLoan.setReturnDate(newReturnDate);
		selectedLoan.setIpAddress(selectedIPaddress);
		selectedLoan.getIpAddress().setOwnerClient(displayedClient);
		/* Then invoke the corresponding operation */
		Object extensionDispatchResult = LoanRESTDispatcher.extendSelectedLoan(selectedLoan.getLoanID(), selectedLoan);
		/* Check the outcome of the invoked operation and proceed based on the analysis result */
		if (extensionDispatchResult instanceof ExceptionInfo){
			/* For an exception detected, show a relevant message dialog */
			ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) extensionDispatchResult;
        	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
			              dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
		} else if (extensionDispatchResult instanceof SerializedLoan) {
			/* For a String message, since the operation has ended successfully, the following things will need to be done */
			/* Convert back the original loan */
			SerializedLoan serializedLoan = (SerializedLoan) extensionDispatchResult;
			/* First display a confirmation message with the operation outcome */
			generateAlert(AlertType.INFORMATION, "Chosen Loan Extended", "Extension ended succesfully for the following loan:" + serializedLoan.toString(), true);
			/* Next refresh the content of the loan table for the given IP address */
			registeredLoanTableViewPerIP.getItems().clear();
			List<SerializedLoan> allLoansForIP = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedLoan.getIpAddress().getIpValue());
			registeredLoanTableViewPerIP.setItems(FXCollections.observableArrayList(allLoansForIP));
			/* Last, but not least, empty the date picker field */
			loanReturnDatePicker.getEditor().clear();
			loanReturnDatePicker.setValue(null);
		}
	}

	/* The method for removing a chosen loan */
	private void removeSelectedLoan() {
		/* Pick the selected loan */
		SerializedLoan selectedLoan = registeredLoanTableViewPerIP.getSelectionModel().getSelectedItem();
        /* Check if this loan is selected */
		if (selectedLoan == null){
			/* For no loan selected for removal, just display a usual warning */
			generateAlert(AlertType.WARNING, "No Loan to Remove", "Currently, no loan has been selected for removal! Please chooose something to continue!", false);
		} else {
			/* For a selected loan, proceed as follows */
			/* Invoke the corresponding loan removal operation from server side */
			Object dispatchingResult = LoanRESTDispatcher.removeSelectedLoan(selectedLoan.getLoanID());
			/* Check next the type of the returned response and act accordingly */
			if (dispatchingResult instanceof ExceptionInfo){
				/* For an exception detected, show a relevant message dialog */
				ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
            	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
				              dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
			} else if (dispatchingResult instanceof String){
				/* For a String message, it means that the operation has been solved on the back-end successfully - therefore the address shall be removed from the front-end as well */
				/* Display first a confirmation with the operation outcome */
				generateAlert(AlertType.INFORMATION, "Chosen Loan Removed", (String) dispatchingResult, true);
				/* Next remove the given IP record from the GUI table as well */
				registeredLoanTableViewPerIP.getItems().remove(selectedLoan);
				/* Additionally, put a replacement message in the loan table in case there are no more loans left in the DB */
				if (registeredLoanTableViewPerIP.getItems().isEmpty()){
					registeredLoanTableViewPerIP.setPlaceholder(new Label("No more loans registered yet under the chosen IP address"));
				}
			}
		}
	}

	/* The method for assembling the section dedicated to IP operations */
	private void createIPOperationsSection() {
		/* Create first the support pane and adjust its most important properties */
		ipOperationsPane = new GridPane();
		ipOperationsPane.setPadding(new Insets(25,25,25,25));
		ipOperationsPane.setHgap(15);
		/* Add there next the elements used by the defined operation */
		/* First the 4 text fields used for defining the numbers which will form the newly chosen IP address */
		ipRegistrationFields = new TextField[4];
		/* Adjust and position these elements accordingly */
		/* Start with the common properties for all the fields */
		for (int i = 0; i < ipRegistrationFields.length; i++){
			ipRegistrationFields[i] = new TextField();
			ipRegistrationFields[i].setPromptText("0");
			ipRegistrationFields[i].setMaxWidth(50);
		}
		/* Set the tooltip texts separately for each field as follows */
		ipRegistrationFields[0].setTooltip(new Tooltip("Please input here a value between 0 and 240 for proceeding (excluding 0, 240 and 127)..."));
		ipRegistrationFields[1].setTooltip(new Tooltip("Please input here a value between 0 and 255 for proceeding..."));
		ipRegistrationFields[2].setTooltip(new Tooltip("Please input here a value between 0 and 255 for proceeding..."));
		ipRegistrationFields[3].setTooltip(new Tooltip("Please input here a value between 1 and 254 for proceeding..."));
		/* Position next the components in order */
		GridPane.setConstraints(ipRegistrationFields[0], 0, 0);
		GridPane.setConstraints(ipRegistrationFields[1], 1, 0);
		GridPane.setConstraints(ipRegistrationFields[2], 2, 0);
		GridPane.setConstraints(ipRegistrationFields[3], 3, 0);
		/* Add the buttons next to the given pane */
		ipRegistrationButton = new Button("Register new IP address");
		ipRemovalButton = new Button("Remove selected IP address");
		/* Position the buttons in the pane */
		GridPane.setConstraints(ipRegistrationButton, 4, 0);
		GridPane.setConstraints(ipRemovalButton, 5, 0);
		/* Add the relevant action listeners to the given buttons */
		ipRemovalButton.setOnAction(e -> performIPAddressRemoval());
		ipRegistrationButton.setOnAction(e -> performIPAddressRegistration());
		/* Add the created elements to the pane */
		ipOperationsPane.getChildren().addAll(ipRegistrationFields[0], ipRegistrationFields[1], ipRegistrationFields[2], ipRegistrationFields[3], ipRegistrationButton, ipRemovalButton);
		/* Add the pane holding the previously defined components into the main container */
		clientDetailBox.getChildren().add(ipOperationsPane);
	}

	/* Action listener method for the registration of a new IP address */
	private void performIPAddressRegistration() {
		/* Pick the contents of the 4 text fields */
		String firstValue = ipRegistrationFields[0].getText();
		String secondValue = ipRegistrationFields[1].getText();
		String thirdValue = ipRegistrationFields[2].getText();
		String fourthValue = ipRegistrationFields[3].getText();

		/* Check if all 4 boxes have been filled out properly - or else display a relevant warning */
		if (StringUtils.isBlank(firstValue) || StringUtils.isBlank(secondValue) || StringUtils.isBlank(thirdValue) || StringUtils.isBlank(fourthValue)){
			generateAlert(AlertType.WARNING, "Incomplete IP Address Specification", "The IP specification submitted here is incomplete in its current status!", false);
		} else {
			/* For all 4 values given, build a relevant IP address to be submitted for registration */
			String newIPvalue = firstValue + "." + secondValue + "." + thirdValue + "." + fourthValue;
			SerializedIPAddress newIPaddress = new SerializedIPAddress();
			newIPaddress.setOwnerClient(displayedClient);
			newIPaddress.setIpValue(newIPvalue);
			/* Invoke next the corresponding operation on the dispatcher side */
			Object dispatchingResult = IPAddressRESTDispatcher.registerNewIPAddress(newIPaddress);
			if (dispatchingResult instanceof ExceptionInfo){
				/* For the case of an exception detected, a corresponding alert box will be shown with information taken from the returned exception */
            	ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
            	generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
					          dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
			} else if (dispatchingResult instanceof URI) {
				/* In case of success, do the following steps */
				/* 1. First display a dialog with the operation outcome */
				URI resultURI = (URI) dispatchingResult;
            	generateAlert(AlertType.INFORMATION, "New IP Address Registered", "The new IP address has been inserted and can be found on link:\n" + IPAddressRESTDispatcher.REST_SERVICE_URI + resultURI.toString(), true);
            	/* 2. Reload the content of the IP address table for the selected user */
            	assignedIPtableView.getItems().clear();
            	List<SerializedIPAddress> allRegisteredIPsForUser = IPAddressRESTDispatcher.getAllIPAddressesAssignedToClient(displayedClient.getCnp());
            	assignedIPtableView.setItems(FXCollections.observableArrayList(allRegisteredIPsForUser));
            	/* 3. Clear the registration fields for re-usability */
            	for (int i = 0; i < ipRegistrationFields.length; i++){
            		ipRegistrationFields[i].clear();
            	}
			}
		}
	}

	/* Action listener method for the removal of a selected IP address */
	private void performIPAddressRemoval() {
		/* Pick the selected IP address */
		SerializedIPAddress selectedIPaddress = assignedIPtableView.getSelectionModel().getSelectedItem();

		/* Check if it is null */
    	if (selectedIPaddress == null){
    		/* In affirmative case, display a suitable warning message */
			generateAlert(AlertType.WARNING, "No IP Address to Remove", "Currently, no IP address has been selected for removal! Please chooose something to continue!", false);
    	} else {
    		/* Check if there are loans assigned to the address in question */
    		List<SerializedLoan> assignedLoans = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedIPaddress.getIpValue());
    		if (!assignedLoans.isEmpty()){
    			/* For affirmative case, forbid the deletion */
    			generateAlert(AlertType.WARNING, "Loans Found!", "Currently, this IP adddress has loans registered under it! Please remove these first before proceeding!", false);
    		} else {
                /* Invoke the removal operation for the given IP address */
    			Object dispatchingResult = IPAddressRESTDispatcher.removeSelectedIPAddress(selectedIPaddress.getIpValue());
    			/* Check the result of the invocation */
    			if (dispatchingResult instanceof ExceptionInfo){
    				/* For an exception detected, abandon the operation and display a relevant error message */
    				ExceptionInfo dispatchingExceptionInfo = (ExceptionInfo) dispatchingResult;
    				generateAlert(AlertType.ERROR, dispatchingExceptionInfo.getHttpOperationStatus().name() + " - " + dispatchingExceptionInfo.getErrorCode(),
    						      dispatchingExceptionInfo.getExceptionMessage() + " on " + dispatchingExceptionInfo.getUrl(), true);
    			} else if (dispatchingResult instanceof String){
    				/* For a String message, it means that the operation has been solved on the back-end successfully - therefore the address shall be removed from the front-end as well */
    				/* Display first a confirmation with the operation outcome */
    				generateAlert(AlertType.INFORMATION, "Existing IP Address Removed", (String) dispatchingResult, true);
    				/* Next remove the given IP record from the GUI table as well */
    				assignedIPtableView.getItems().remove(selectedIPaddress);
    			}
    		}
    	}
	}

	/* The table ensemble creator method */
	private void createTableDataView() {
		/* Create the support component for holding the given tables */
		ipAddressBox = new HBox();
		ipAddressBox.setPadding(new Insets(15,15,15,15));
		ipAddressBox.setSpacing(25);

		/* Create next the table for holding the given IP addresses */
		createIPAddressTable();

		/* Next add the box for holding the operation buttons */
		detailExportBox = new VBox();
		detailExportBox.setPadding(new Insets(15,15,15,15));
		detailExportBox.setSpacing(25);
		/* Populate next the previously generated panel */
		/* First add there the button responsible for displaying the detail associated to one particular IP address */
		detailButton = new Button("Display loans for selected IP");
		/* Configure and assign the corresponding action listener to the given button */
		detailButton.setOnAction(e -> displayLoansForSelectedIPAddress());
        /* Then create and add there the menu button responsible for creating the export of the loan histories associated to one IP address */
		createExportMenuForLoanHistory();
		detailExportBox.getChildren().addAll(detailButton, exportButton);

		/* Finally add the loan table */
		createLoanTable();

		/* Last, but not least, put together the given elements in their own container */
		ipAddressBox.getChildren().addAll(assignedIPtableView, detailExportBox, registeredLoanTableViewPerIP);
		/* And of course, add this secondary container into the main one to make it visible */
		clientDetailBox.getChildren().add(ipAddressBox);
	}

	/* The method responsible for the assembly of the menu required for exporting the loan history related to one particular IP address */
	private void createExportMenuForLoanHistory() {
		/* Create first the menu items participating in the given operations */
		excelExportItem = new MenuItem("Create associated Excel document");
		pdfExportItem = new MenuItem("Create associated PDF document");
		/* Process next the images associated to each of the operations */
		/* For the Excel export */
		Image excelExportIcon = new Image(getClass().getResourceAsStream("menu_images/Excel_export.png"));
        ImageView excelExportView = new ImageView(excelExportIcon);
        excelExportView.setFitWidth(15);
        excelExportView.setFitHeight(15);
        /* For the PDF export */
        Image pdfExportIcon = new Image(getClass().getResourceAsStream("menu_images/PDF_export.png"));
        ImageView pdfExportView = new ImageView(pdfExportIcon);
        pdfExportView.setFitWidth(15);
        pdfExportView.setFitHeight(15);
        /* Finally attach each of the icons to the corresponding menu item */
        excelExportItem.setGraphic(excelExportView);
        pdfExportItem.setGraphic(pdfExportView);
        /* Add to each menu item the corresponding action listener */
        excelExportItem.setOnAction(e -> exportLoanHistoryIntoExcelFormat());
        pdfExportItem.setOnAction(e -> exportLoanHistoryIntoPDFdocFormat());

        /* Afterwards, create the menu button hosting the previously specified two items */
        /* First create and process its associated icon */
        Image exportMenuIcon = new Image(getClass().getResourceAsStream("menu_images/menu_icon.png"));
        ImageView exportMenuView = new ImageView(exportMenuIcon);
        exportMenuView.setFitWidth(15);
        exportMenuView.setFitHeight(15);
        /* Then assemble the effective menu button */
        exportButton = new MenuButton("Export loan history for selected IP", exportMenuView, excelExportItem, pdfExportItem);
	}

	/* A method for creating a PDF document containing the loan history associated to a particular IP address */
	private void exportLoanHistoryIntoPDFdocFormat() {
		/* Pick an IP address from the ones available for the given user */
		SerializedIPAddress selectedAddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* Check if this IP address is null */
		if (selectedAddress == null){
			/* In affirmative case, display a suitable warning message */
			generateAlert(AlertType.WARNING, "No Loan History to Export", "Currently, no IP address has been selected for loan history export! Please choose something to continue!", false);
		} else {
			/* In case of selection, proceed as follows */
			/* See if there are any loans registered under the given IP address */
			List<SerializedLoan> allLoansForIP = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedAddress.getIpValue());
			if (allLoansForIP.isEmpty()){
				generateAlert(AlertType.INFORMATION, "No Loans Found for Export", "Currently, the selected IP address " + selectedAddress.getIpValue() + " has no associated loans registered under it! Please try the operation later!", false);
			} else {
				/* Invoke next the export operation, using the following parameters: the displayed client, the selected IP address and the whole list of loans for the IP address in question */
				LoanHistoryExportGenerator.createDetailedPDFreportForHistory(displayedClient, selectedAddress, allLoansForIP);
			}
		}
	}

	/* A method for creating an Excel document containing the loan history associated to a particular IP address */
	private void exportLoanHistoryIntoExcelFormat() {
		/* Pick an IP address from the ones available for the given user */
		SerializedIPAddress selectedAddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* Check if this IP address is null */
		if (selectedAddress == null){
			/* In affirmative case, display a suitable warning message */
			generateAlert(AlertType.WARNING, "No Loan History to Export", "Currently, no IP address has been selected for loan history export! Please choose something to continue!", false);
		} else {
			/* In case of selection, proceed as follows */
			/* See if there are any loans registered under the given IP address */
			List<SerializedLoan> allLoansForIP = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedAddress.getIpValue());
			if (allLoansForIP.isEmpty()){
				generateAlert(AlertType.INFORMATION, "No Loans Found for Export", "Currently, the selected IP address " + selectedAddress.getIpValue() + " has no associated loans registered under it! Please try the operation later!", false);
			} else {
				LoanHistoryExportGenerator.createDetailedExcelReportForHistory(displayedClient, selectedAddress, allLoansForIP);
			}
		}
	}

	/* The method for displaying the loans registered for a given IP address */
	private void displayLoansForSelectedIPAddress() {
		/* Pick the selected IP address */
    	SerializedIPAddress selectedIPAddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* Check if it is null */
    	if (selectedIPAddress == null){
    		/* In affirmative case, display a suitable warning message */
    		generateAlert(AlertType.WARNING, "No Loans to Display", "Currently, no IP address has been selected for display! Please choose something to continue!", false);
    	} else {
    		/* In case of selection, proceed as follows */
    		List<SerializedLoan> allLoansForIP = LoanRESTDispatcher.getAllLoansForParticularIPAddress(selectedIPAddress.getIpValue());
    		/* Clear off the previous items which resulted from an earlier selection */
    		registeredLoanTableViewPerIP.getItems().clear();
    		/* Check if there are any registered loans for the specified IP address */
    		if (allLoansForIP.isEmpty()){
    			/* For negative case, change the placeholder message content */
    			registeredLoanTableViewPerIP.setPlaceholder(new Label("No loans registered yet under the chosen IP address"));
    		} else {
    			/* For available loans, insert the list into its position */
    			registeredLoanTableViewPerIP.setItems(FXCollections.observableArrayList(allLoansForIP));
    		}
    	}
	}

	/* The method for assembling the table of loans */
	@SuppressWarnings("unchecked")
	private void createLoanTable() {
		/* Create the table border */
		registeredLoanTableViewPerIP = new TableView<SerializedLoan>();
		registeredLoanTableViewPerIP.setPlaceholder(new Label("No IP address selected yet for examination"));

		/* Setup the column headers */
		//for the ID
		TableColumn<SerializedLoan, Integer> idColumn = new TableColumn<SerializedLoan, Integer>("ID");
		idColumn.setMaxWidth(40);
		idColumn.setResizable(false);
		idColumn.setCellValueFactory(new PropertyValueFactory<>("loanID"));
		//for the application date and time
		TableColumn<SerializedLoan, String> applicationDateColumn = new TableColumn<SerializedLoan, String>("Application Time");
		applicationDateColumn.setMinWidth(175);
		applicationDateColumn.setResizable(false);
		/* Add a special cell value factory here as the column and the displayed value have different data types */
		applicationDateColumn.setCellValueFactory(cellData -> {
			LocalDateTime applicationDateTime = cellData.getValue().getApplicationTime();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String formattedApplicationTime = applicationDateTime.format(formatter);
			return new ReadOnlyStringWrapper(formattedApplicationTime);
		});
		//for the return date
		TableColumn<SerializedLoan, LocalDate> returnDateColumn = new TableColumn<SerializedLoan, LocalDate>("Return Date");
		returnDateColumn.setMinWidth(125);
		returnDateColumn.setResizable(false);
		returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
		//for the amount
		TableColumn<SerializedLoan, Long> amountColumn = new TableColumn<SerializedLoan, Long>("Amount");
		amountColumn.setMinWidth(100);
		amountColumn.setResizable(false);
		amountColumn.setCellValueFactory(new PropertyValueFactory<>("loanedAmount"));
		//for the currency
		TableColumn<SerializedLoan, LoanCurrency> currencyColumn = new TableColumn<SerializedLoan, LoanCurrency>("Currency");
		currencyColumn.setMinWidth(75);
		currencyColumn.setResizable(false);
		currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currency"));
		//for the extension flag
		TableColumn<SerializedLoan, String> extensionColumn = new TableColumn<SerializedLoan, String>("Is Extended?");
		extensionColumn.setMinWidth(125);
		extensionColumn.setResizable(false);
		/* Once again add a special cell value factory here as the column and the displayed value have different data types */
		extensionColumn.setCellValueFactory(cellData -> {
			boolean isExtended = cellData.getValue().isExtended();
            if(isExtended) {
            	return new ReadOnlyStringWrapper("Yes");
            } else {
            	return new ReadOnlyStringWrapper("No");
            }
		});
		//for the interest rate
		TableColumn<SerializedLoan, Long> interestRateColumn = new TableColumn<SerializedLoan, Long>("Interest Rate");
		interestRateColumn.setMinWidth(100);
		interestRateColumn.setResizable(false);
		interestRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
		//for the extension count
		TableColumn<SerializedLoan, Long> extensionCountColumn = new TableColumn<SerializedLoan, Long>("Extension Count");
		extensionCountColumn.setMinWidth(150);
		extensionCountColumn.setResizable(false);
		extensionCountColumn.setCellValueFactory(new PropertyValueFactory<>("extensionCount"));

		/* Add the columns into the given table */
		registeredLoanTableViewPerIP.getColumns().addAll(idColumn, applicationDateColumn, returnDateColumn, amountColumn, currencyColumn, extensionColumn, interestRateColumn, extensionCountColumn);
	}

	/* The method for assembling the table of assigned IP addresses */
	@SuppressWarnings("unchecked")
	private void createIPAddressTable() {
		/* Create the table border */
		assignedIPtableView = new TableView<SerializedIPAddress>();
		/* Populate next the given table accordingly */
		List<SerializedIPAddress> allRegisteredIPs = IPAddressRESTDispatcher.getAllIPAddressesAssignedToClient(displayedClient.getCnp());
		assignedIPtableView.setItems(FXCollections.observableArrayList(allRegisteredIPs));
		/* Set a replacement text in case there are no IP addresses associated to the given user */
		if (allRegisteredIPs.isEmpty()){
			assignedIPtableView.setPlaceholder(new Label("No IPs registered yet to this user"));
		}

		/* Setup the column headers for the given table */
		//for the ID
		TableColumn<SerializedIPAddress, Integer> idColumn = new TableColumn<SerializedIPAddress, Integer>("ID");
		idColumn.setMaxWidth(25);
		idColumn.setResizable(false);
		idColumn.setCellValueFactory(new PropertyValueFactory<>("ipID"));
		//for the value
		TableColumn<SerializedIPAddress, String> ipValueColumn = new TableColumn<SerializedIPAddress, String>("Value");
		ipValueColumn.setMinWidth(125);
		ipValueColumn.setResizable(false);
		ipValueColumn.setCellValueFactory(new PropertyValueFactory<>("ipValue"));

		/* Add the columns into the given table */
		assignedIPtableView.getColumns().addAll(idColumn, ipValueColumn);
	}

	/* The client profile setup method */
	private void createClientProfile() {
		/* Start by creating the host pane and adjusting its settings */
		clientInfoPane = new GridPane();
		clientInfoPane.setPadding(new Insets(10,15,20,25));
		clientInfoPane.setVgap(15);

		/* Add there next the required elements together with their values */
		/* First the four labels with the personal information of the client */
		clientInfoLabels = new Label[4];
		/* Position these according to the previously defined pattern - also set their content accordingly */
		/* The name */
		clientInfoLabels[0] = new Label("Name: " + displayedClient.getName());
		GridPane.setConstraints(clientInfoLabels[0], 0, 0);
		/* The CNP */
		clientInfoLabels[1] = new Label("Code: " + displayedClient.getCnp());
		GridPane.setConstraints(clientInfoLabels[1], 0, 1);
		/* The e-mail address */
		clientInfoLabels[2] = new Label("E-mail: " + displayedClient.getEmailAddress());
		GridPane.setConstraints(clientInfoLabels[2], 0, 2);
		/* The postal address */
		clientInfoLabels[3] = new Label("Contact: " + displayedClient.getPostalAddress());
		GridPane.setConstraints(clientInfoLabels[3], 0, 3);

		/* Then also add there the button for returning to the main gate of the application - action listener will be added to it on the main gate */
		returnButton = new Button("Press for returning to the main frame");
		GridPane.setConstraints(returnButton, 0, 4);

		/* Add the created components into their own container */
		clientInfoPane.getChildren().addAll(clientInfoLabels[0], clientInfoLabels[1], clientInfoLabels[2], clientInfoLabels[3], returnButton);
		/* Then add this secondary container to the main one */
		clientDetailBox.getChildren().add(clientInfoPane);
	}

	/* Getter method for the return button - its action listener is defined in another place */
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