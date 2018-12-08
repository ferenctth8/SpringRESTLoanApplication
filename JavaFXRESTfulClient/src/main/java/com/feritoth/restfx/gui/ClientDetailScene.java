package com.feritoth.restfx.gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.feritoth.restfx.core.LoanCurrency;
import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.core.SerializedIPAddress;
import com.feritoth.restfx.core.SerializedLoan;
import com.feritoth.restfx.dispatcher.IPAddressRESTDispatcher;
import com.feritoth.restfx.dispatcher.LoanRESTDispatcher;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ClientDetailScene extends Scene {

	/* The attribute list of the current class */
	/* The selected client whose details will be displayed */
	private SerializedClient displayedClient;
	/* The list of graphical components supported by this part of the application */
	private VBox clientDetailBox;//the main support component
	private GridPane clientInfoPane;//the pane holding the client contact details;
	private HBox ipAddressBox; //the box for displaying the table of registered IP addresses and their associated loans
	private Label[] clientInfoLabels;//the set of labels for displaying the personal information related to a client
	private TableView<SerializedIPAddress> assignedIPtableView;//the table of registered IP addresses
	private TableView<SerializedLoan> registeredLoanTableViewPerIP;//the table of loans registered for a given IP address
    private Button detailButton;//the button responsible for loan display

	/* The constructor */
	public ClientDetailScene(SerializedClient selectedClient, Parent root, int width, int height) {
		/* Call the superclass constructor */
		super(root, width, height);
		/* Instantiate the selected client, the admin scene and the main support component */
		this.displayedClient = selectedClient;
		//this.adminScene = adminScene;
		this.clientDetailBox = (VBox) root;
		/* Call the methods required for initializing the other two support components */
		createClientProfile(); //method for initializing the client profile
		createTableDataView(); //method for initializing the data associated to a client (IP addresses + loans)
	}

	/* The table ensemble creator method */
	private void createTableDataView() {
		/* Create the support component for holding the given tables */
		ipAddressBox = new HBox();
		ipAddressBox.setPadding(new Insets(15,15,15,15));
		ipAddressBox.setSpacing(25);

		/* Create next the table for holding the given IP addresses */
		createIPAddressTable();

		/* Then add the detail button */
		detailButton = new Button("Show all loans for IP");
		/* Configure and assign the corresponding action listener to the given button */
		detailButton.setOnAction(e -> displayLoansForSelectedIPAddress());

		/* Finally add the loan table */
		createLoanTable();

		/* Last, but not least, put together the given elements in their own container */
		ipAddressBox.getChildren().addAll(assignedIPtableView, detailButton, registeredLoanTableViewPerIP);
		/* And of course, add this secondary container into the main one to make it visible */
		clientDetailBox.getChildren().add(ipAddressBox);
	}

	/* The method for displaying the loans registered for a given IP address */
	private void displayLoansForSelectedIPAddress() {
		/* Pick the selected IP address */
    	SerializedIPAddress selectedIPAddress = assignedIPtableView.getSelectionModel().getSelectedItem();
		/* Check if it is null */
    	if (selectedIPAddress == null){
    		/* In affirmative case, display a suitable warning message */
    		Alert emptyDisplayAlert = new Alert(AlertType.WARNING);
			emptyDisplayAlert.setTitle("No Loans to Display");
			emptyDisplayAlert.setContentText("Currently, no IP address has been selected for display!Please chooose something to continue!");
			emptyDisplayAlert.showAndWait();
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
		idColumn.setMaxWidth(25);
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
		/* That is, the four labels with the personal information of the client */
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

		/* Add the created components into their own container */
		clientInfoPane.getChildren().addAll(clientInfoLabels[0], clientInfoLabels[1], clientInfoLabels[2], clientInfoLabels[3]);
		/* Then add this secondary container to the main one */
		clientDetailBox.getChildren().add(clientInfoPane);
	}

}