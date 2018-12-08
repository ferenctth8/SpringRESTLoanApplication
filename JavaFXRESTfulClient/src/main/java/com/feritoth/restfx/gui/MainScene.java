package com.feritoth.restfx.gui;

import org.apache.commons.lang3.StringUtils;

import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.dispatcher.ClientRESTDispatcher;
import com.feritoth.restfx.utilities.ExceptionInfo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainScene extends Application {

	/* The constant String token for granting admin access */
	private static final String ADMIN_TOKEN = "admin";

	/* The host window and scene */
	private Stage mainWindow;
	private Scene mainScene;
	/* The admin scene */
	//private Stage adminWindow;
	private Scene adminScene;
    /* The client scene */
	private Scene clientScene;
	/* The host pane */
	protected GridPane rootPane;
	/* The field and label for the administrator login */
	private Label adminLabel;
	protected TextField adminField;
	/* The field and label for the client login */
	private Label clientLabel;
	protected TextField clientField;
	/* The buttons required for the execution of the login procedure */
	protected Button adminButton;
	protected Button clientButton;

	@Override
	public void start(Stage primaryStage) {
		try {
			/* Instantiate the window and add a title to it */
			mainWindow = primaryStage;
			mainWindow.setTitle("Java FX RESTful client - main frame");

			/* Initialize all the frame components */
			initializeFrameComponents();

			/* Create the host scene, link it to the file of properties and make there the last adjustments */
			mainScene = new Scene(rootPane,430,150);
			mainScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			mainWindow.setScene(mainScene);
			mainWindow.setResizable(false);
			/* Display the window in the end */
			mainWindow.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/* Initialize and style up the components for the current frame */
	private void initializeFrameComponents() {
		/* Start by creating the host pane and its settings */
		rootPane = new GridPane();
		rootPane.setPadding(new Insets(10,15,20,25));
		rootPane.setVgap(10);
		rootPane.setHgap(15);

		/* Next continue by positioning the elements inside it */
		/* First the label and the field for the admin user */
		/* The label */
		adminLabel = new Label("Application Admin Login:");
		adminLabel.setStyle("-fx-text-fill:aliceblue");
		adminLabel.setId("bold-label");
		GridPane.setConstraints(adminLabel, 0, 0);

		/* The text field */
		adminField = new TextField();
		adminField.setPromptText("admin");
		adminField.setTooltip(new Tooltip("Please write here <<ADMIN>> in any format for proceeding..."));
		GridPane.setConstraints(adminField, 1, 0);

		/* Second the label and the text field for a regular client */
		/* The label */
		clientLabel = new Label("Regular Client Login:");
		clientLabel.setStyle("-fx-text-fill: #E8E8E8");
		clientLabel.setId("bold-label");
		GridPane.setConstraints(clientLabel, 0, 1);

		/* The text field */
		clientField = new TextField();
	    clientField.setPromptText("123456789000");
	    clientField.setTooltip(new Tooltip("Please input your twelve-digit personal numerical ID for proceeding..."));
	    GridPane.setConstraints(clientField, 1, 1);

	    /* Finally the buttons for scene switching */
	    /* The admin button - put it under the fields and labels */
	    adminButton = new Button("Admin Login");
	    adminButton.getStyleClass().add("button-red");
	    GridPane.setConstraints(adminButton, 0, 2);

	    /* The client button - to be put near the other one */
	    clientButton = new Button("Client Login");
	    clientButton.getStyleClass().add("button-blue");
	    GridPane.setConstraints(clientButton, 1, 2);

	    /* Add the action listeners to the buttons in question */
	    setupAllActionListeners();

	    /* Add the created components into the root pane */
	    rootPane.getChildren().addAll(adminLabel, adminField, clientLabel, clientField, adminButton, clientButton);
	}

	private void setupAllActionListeners() {
		/* Add the action listener to the admin button first */
		adminButton.setOnAction(e -> {
			/* Pick the related text field content */
			String adminToken = adminField.getText();
			/* Examine it and act accordingly to the results */
			if (!StringUtils.equalsIgnoreCase(adminToken, ADMIN_TOKEN)){
				/* Display an alert box with the corresponding message */
				Alert adminWarningAlert = new Alert(AlertType.WARNING);
				adminWarningAlert.setTitle("Wrong Token");
				adminWarningAlert.setContentText("Wrong admin role token supplied!");
				adminWarningAlert.showAndWait();
			} else {
				/* Display the new frame and scene */
				/* Create the admin pane required for instantiation */
				adminScene = new AdminScene(new VBox(), 960, 480);
				//adminScene = new AdminScene(new GridPane(), 900, 450);
				/* Change the window scene and its title + make it resizable */
				mainWindow.setScene(adminScene);
				mainWindow.setTitle("JavaFX RESTful client - Administrator Profile");
				mainWindow.setResizable(true);
				/* Get the return button from the previous frame and attach to it a listener */
				((AdminScene) adminScene).getReturnButton().setOnAction(ae -> mainWindow.setScene(mainScene));
			}
		});

		clientButton.setOnAction(e -> {
			/* Again pick the corresponding text content */
			String clientNPC = clientField.getText();
			/* Check its length and content - decide accordingly on how to proceed */
			if (StringUtils.length(clientNPC) != 12){
				/* Display a first warning */
				Alert lengthAlert = new Alert(AlertType.WARNING);
				lengthAlert.setTitle("Not Enough Digits");
				lengthAlert.setContentText("Too few/too many digits supplied for search!");
				lengthAlert.showAndWait();
			} else if (!StringUtils.isNumeric(clientNPC)){
				/* Display a second warning */
				Alert formatAlert = new Alert(AlertType.WARNING);
				formatAlert.setTitle("Invalid code characters");
				formatAlert.setContentText("Personal numerical code contains invalid characters!");
				formatAlert.showAndWait();
			} else {
				/* Last case - get the data associated to the user to see if he/she exists */
				/* Check if there is a client registered with the given PIN in the DB */
				Object existenceResponse = ClientRESTDispatcher.getDetailsForSelectedClient(clientNPC);
				if (existenceResponse instanceof ExceptionInfo){
					/* For negative case, just display a suitable alert box with the exception message from the received response */
					ExceptionInfo inexistentUserException = (ExceptionInfo) existenceResponse;
					Alert inexistentUserAlert = new Alert(AlertType.ERROR);
					inexistentUserAlert.setTitle(inexistentUserException.getHttpOperationStatus().name() + " - " + inexistentUserException.getErrorCode());
					inexistentUserAlert.setContentText(inexistentUserException.getExceptionMessage() + " \n " + inexistentUserException.getUrl());
					inexistentUserAlert.setResizable(true);
					inexistentUserAlert.showAndWait();
				} else {
					/* For a positive case, just proceed with the display of the given scene */
					SerializedClient selectedClient = (SerializedClient) existenceResponse;
					clientScene = new ClientScene(selectedClient, new VBox(), 1600, 800);
					/* Change the window scene and its title + make it resizable */
					mainWindow.setScene(clientScene);
					mainWindow.setTitle("JavaFX RESTful client - Personal profile of " + selectedClient.getName());
					mainWindow.setResizable(true);
					/* Get the return button from the previous frame and attach to it a listener */
					((ClientScene) clientScene).getReturnButton().setOnAction(ae -> mainWindow.setScene(mainScene));
				}
			}
		});
	}

	public static void main(String[] args) {
		launch(args);
	}

}