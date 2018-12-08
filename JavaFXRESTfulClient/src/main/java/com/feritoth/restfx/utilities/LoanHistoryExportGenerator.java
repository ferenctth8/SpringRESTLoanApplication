package com.feritoth.restfx.utilities;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feritoth.restfx.core.SerializedClient;
import com.feritoth.restfx.core.SerializedIPAddress;
import com.feritoth.restfx.core.SerializedLoan;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * This class is responsible for the generation of the loan history reports into Excel or PDF format.
 * The history generation will be for now limited to one client and all loans this client made on
 * one particular IP address.
 *
 * @author Frantisek Slovak
 */
public class LoanHistoryExportGenerator {

	/* THe 2 filename templates for the reports desired to be generated */
	private static final String PDF_REPORT_FILENAME_TEMPLATE = "Loan history for IP address {} for client [].pdf";
	private static final String OLD_EXCEL_REPORT_FILENAME_TEMPLATE = "Loan history for IP address {} for client [].xls";
	private static final String NEW_EXCEL_REPORT_FILENAME_TEMPLATE = "Loan history for IP address {} for client [].xlsx";
	/* The logger associated to this particular utility - to be used during the detection of any exceptions raised while trying to create the desired document */
	private static final Logger LOGGER = LoggerFactory.getLogger(LoanHistoryExportGenerator.class);
	/* The fonts used for during the creation of the PDF report version */
	private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
	private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
	private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
	private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);

	/**
	 * The PDF report generator method.
	 *
	 * @param selectedClient the client whose loan history will be exported
	 * @param selectedIPaddress the IP address to which the examined loans belong
	 * @param allLoansForIP the loans recorded on the given IP address
	 */
	public static void createDetailedPDFreportForHistory(SerializedClient selectedClient, SerializedIPAddress selectedIPaddress, List<SerializedLoan> allLoansForIP) {
		/* First replace the wild-card characters from the filename template */
		String finalFileName = PDF_REPORT_FILENAME_TEMPLATE.replace("{}", selectedIPaddress.getIpValue()).replace("[]", selectedClient.getName());
		try {
			/* Name the document to be created and open it for procession */
			Document pdfReportDocument = new Document();
			PdfWriter.getInstance(pdfReportDocument, new FileOutputStream(finalFileName));
			pdfReportDocument.open();
			/* Add to it in the following order: the necessary file metadata, the title page and the effective report content */
			addMetaData(pdfReportDocument, finalFileName);
			addTitlePage(pdfReportDocument, selectedClient, selectedIPaddress);
			addContent(pdfReportDocument, allLoansForIP);
			/* Close the document */
			pdfReportDocument.close();
			/* Issue a short message for denoting the operation success */
			LocalDateTime newGenerationDateTime = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String formattedGenerationDateTime = newGenerationDateTime.format(formatter);
			String successfullOutcome = "The report for client " + selectedClient.getName() + " showing loan history on IP address " + selectedIPaddress.getIpValue() + " has been created successfully on " + formattedGenerationDateTime + "!";
			LOGGER.info(successfullOutcome);
		} catch (FileNotFoundException | DocumentException e) {
			generateAlert(AlertType.ERROR, "Error During Report Generation", "Exception detected while trying to create the desired report file!" + e.getClass().getName(), false);
			LOGGER.error("Exception detected while trying to create the desired report file!", e.getMessage());
		}
	}

	/**
	 * The method for inserting the effective content into the document.
	 *
	 * @param pdfReportDocument the document where the insert the content
	 * @param allLoansForIP the list of loans for which the history is accessed
	 *
	 * @throws DocumentException in case of any processing problems encountered during the creation of the given document
	 */
	private static void addContent(Document pdfReportDocument, List<SerializedLoan> allLoansForIP) throws DocumentException {
		/* Create the new page container paragraph */
		Paragraph mainParagraph = new Paragraph();
		/* Add one empty line */
		addEmptyLine(mainParagraph, 1);
		/* Put the introductory paragraph in first */
		Paragraph introParagraph = new Paragraph("The curent page contains the details of all the issued loans up to now:", subFont);
		mainParagraph.add(introParagraph);
		/* Add another empty line */
		addEmptyLine(mainParagraph, 1);
		/* Add next the table into the document */
		createLoanHistoryTable(mainParagraph, allLoansForIP);
		addEmptyLine(mainParagraph, 1);
		createExtendedHistory(mainParagraph, allLoansForIP);
		addEmptyLine(mainParagraph, 1);
		/* Attach this main paragraph to the final document */
		pdfReportDocument.add(mainParagraph);
	}

	/**
	 * A private method for creating the detailed loan extension history tables.
	 *
	 * @param paragraph the container paragraph for the given table
	 * @param allLoansForIP the list of loans for a given IP address
	 *
	 * @throws DocumentException in case of encountering a processing failure during document manipulation
	 */
	private static void createExtendedHistory(Paragraph paragraph, List<SerializedLoan> allLoansForIP) throws DocumentException {
		/* Go through the list of loans and and check if there any which have been extended */
		for (SerializedLoan loan : allLoansForIP){
			if (loan.getExtensionCount() > 0){
				/* Create the header required for introducing the detailed extension history for the given loan */
				Paragraph introParagraph = new Paragraph("For the loan with ID " + loan.getLoanID() + " the following detailed history has been generated:", subFont);
				paragraph.add(introParagraph);
				/* Add a separator line between this paragraph and the table */
				addEmptyLine(paragraph, 1);
				/* Create the table for the given loan */
				createHistoryTableForLoan(paragraph, loan);
				/* Add a separator line between the current table and the data processed for the next loan */
				addEmptyLine(paragraph, 1);
			}
		}
	}

	/**
	 * The method required for creating a table with the detailed loan history.
	 *
	 * @param paragraph the container paragraph for the given table
	 * @param loan the examined loan for whom the extension history is expected to be created
	 *
	 * @throws DocumentException in case of encountering a processing failure during document manipulation
	 */
	private static void createHistoryTableForLoan(Paragraph paragraph, SerializedLoan loan) throws DocumentException {
		/* Create the table and set its dimensions */
		PdfPTable historyTable = new PdfPTable(3);
		int[] widths = {15, 15, 15};
		historyTable.setWidths(widths);
		/* Next add the associated headers */
		// 1. The loan return date
		PdfPCell loanCell = new PdfPCell(new Phrase("Loan Return Date"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		historyTable.addCell(loanCell);
		// 2. The loan interest rate
		loanCell = new PdfPCell(new Phrase("Interest Rate"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		historyTable.addCell(loanCell);
		// 3. The extension count
		loanCell = new PdfPCell(new Phrase("Extension Count"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		historyTable.addCell(loanCell);
		/* Set the header row to the current table */
		historyTable.setHeaderRows(1);
		/* Populate the table with the relevant data in the end */
		for (long i = 0; i < loan.getExtensionCount() + 1; i++){
			/* Declare here all internally used auxiliary variables */
			//the date-time formatter pattern
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			//the initial interest rate = 10% of the loaned sum
			Long interestRate = loan.getLoanedAmount() / 10;
			//the initial return date = application date + 7 days - format it to get the desired representation pattern
			LocalDate returnDate = loan.getApplicationTime().plusDays(7).toLocalDate();
			if (i == 0){
				/* Just add in the previously determined initial values */
				String formattedReturnDate = returnDate.format(formatter);
				historyTable.addCell(formattedReturnDate);
				historyTable.addCell(interestRate.toString());
			} else {
				/* For the extended case: increment the return date by 7 days */
				returnDate = returnDate.plusDays(7);
				String formattedReturnDate = returnDate.format(formatter);
				historyTable.addCell(formattedReturnDate);
				/* While the interest rate by a factor of 1.5 */
				interestRate = interestRate * 15 / 10;
				historyTable.addCell(interestRate.toString());
			}
			//the extension count - this has the value equal to the position of the current counter
		    historyTable.addCell(Long.toString(i));
		}
		/* Add the table to the container paragraph */
		paragraph.add(historyTable);
	}

	/**
	 * The main table creator method.
	 *
	 * @param paragraph the container paragraph
	 * @param allLoansForIP the list of loans to be displayed
	 *
	 * @throws DocumentException in case of encountering a processing failure during document manipulation
	 */
	private static void createLoanHistoryTable(Paragraph paragraph, List<SerializedLoan> allLoansForIP) throws DocumentException {
		/* Create the table to be inserted */
		PdfPTable loanTable = new PdfPTable(8);
		int[] widths = {9, 15, 15, 12, 12, 12, 9, 12};
		loanTable.setWidths(widths);
		/* Next format the table headers to be displayed */
		// 1. The loan ID
		PdfPCell loanCell = new PdfPCell(new Phrase("Loan ID"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 2. The loan application time
		loanCell = new PdfPCell(new Phrase("Application Time"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 3. The loan return date
		loanCell = new PdfPCell(new Phrase("Loan Return Date"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 4. The loaned amount and its associated currency
		loanCell = new PdfPCell(new Phrase("Amount"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		loanCell = new PdfPCell(new Phrase("Currency"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 5. The extension flag
		loanCell = new PdfPCell(new Phrase("Extended?"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 6. The interest rate
		loanCell = new PdfPCell(new Phrase("Interest Rate"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		// 7. The loan extension count
		loanCell = new PdfPCell(new Phrase("Total EC"));
		loanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		loanTable.addCell(loanCell);
		/* Set the header row to the current table */
		loanTable.setHeaderRows(1);
		/* Add the effective data into the current table */
		for (SerializedLoan loan : allLoansForIP){
			//the loan ID
			loanTable.addCell(loan.getLoanID().toString());
			//the loan application date time
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String formattedApplicationTime = loan.getApplicationTime().format(formatter);
			loanTable.addCell(formattedApplicationTime);
			//the loan return date
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String formattedReturnDate = loan.getReturnDate().format(formatter);
			loanTable.addCell(formattedReturnDate);
			//the loaned amount and currency
			loanTable.addCell(loan.getLoanedAmount().toString());
			loanTable.addCell(loan.getCurrency().name());
			//the loan extension flag
			loanTable.addCell(loan.isExtended() ? "Yes" : "No");
			//the interest rate
			loanTable.addCell(loan.getInterestRate().toString());
			//the extension count
			loanTable.addCell(loan.getExtensionCount().toString());
		}
		/* Attach the table to the original container */
		paragraph.add(loanTable);
	}

	/**
	 * The method for creating the document title page.
	 *
	 * @param pdfDocument the document whose title page will be generated
	 * @param selectedClient the client for whom the report will be generated
	 * @param selectedIPaddress the IP address where the loans with the exported history are located
	 *
	 * @throws DocumentException for any malformations detected on the level of the given document
	 */
	private static void addTitlePage(Document pdfDocument, SerializedClient selectedClient, SerializedIPAddress selectedIPaddress) throws DocumentException {
		/* Create a document paragraph */
		Paragraph preface = new Paragraph();
		/* Add one empty line */
		addEmptyLine(preface, 1);
		// Write a big header and add an empty line
		preface.add(new Paragraph("Title of the document", catFont));
		addEmptyLine(preface, 1);
		// Will create: Report generated by: _name, _date + adds 3 empty lines
		LocalDateTime newGenerationDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String formattedGenerationTime = newGenerationDateTime.format(formatter);
		preface.add(new Paragraph("Report generated by: " + System.getProperty("user.name") + " on " + formattedGenerationTime, smallBold));
		addEmptyLine(preface, 3);
		// Add more paragraphs and empty lines (one by one) in order to display the details of the client + the value of the examined IP address
		// Introduction
		preface.add(new Paragraph("This document describes the detailed loan history for the following input parameters: ", smallBold));
		addEmptyLine(preface, 1);
		// The client name
		preface.add(new Paragraph("Name: " + selectedClient.getName(), smallBold));
		addEmptyLine(preface, 1);
		// The personal PIN code
		preface.add(new Paragraph("PIN Code: " + selectedClient.getCnp(), smallBold));
		addEmptyLine(preface, 1);
		// The e-mail address
		preface.add(new Paragraph("E-mail Address: " + selectedClient.getEmailAddress(), smallBold));
		addEmptyLine(preface, 1);
		// The postal address
		preface.add(new Paragraph("Contact Address: " + selectedClient.getPostalAddress(), smallBold));
		addEmptyLine(preface, 1);
		// The IP address chosen for examination
		preface.add(new Paragraph("Selected IP Address: " + selectedIPaddress.getIpValue(), smallBold));
		addEmptyLine(preface, 1);
		// Add the last paragraph before exiting
		preface.add(new Paragraph("This document is a preliminary version and not subject to your license agreement or any other agreement with Red Hat, Pivotal Software or Oracle!", redFont));
		pdfDocument.add(preface);
		// Start a new page where to put the actual content later on
		pdfDocument.newPage();
	}

	/**
	 * The method for adding spaces between data - used during document content formatting procedure.
	 *
	 * @param preface the big document content where to attach the separator spaces
	 * @param number the number of spaces to be attached
	 */
	private static void addEmptyLine(Paragraph preface, int number) {
		for (int i = 0; i < number; i++) {
			preface.add(new Paragraph(" "));
		}
	}

	/**
	 * The method for setting the metadata belonging to one document.
	 *
	 * @param pdfDocument the document to whom the metadata will be attached
	 * @param finalFileName the name of the file associated to the current document
	 */
	private static void addMetaData(Document pdfDocument, String finalFileName) {
		/* Add the following metadata to the given document */
		// The title
		pdfDocument.addTitle(finalFileName);
		// The subject
		pdfDocument.addSubject("Document created by the help of the following libraries: Hibernate, Spring core, Spring MVC (via REST), JavaFX and iTextPDF");
		// Some keywords
		pdfDocument.addKeywords("Java, PDF, iText, Hibernate, Spring core, Spring MVC REST, JavaFX");
		// The author and creator
		pdfDocument.addAuthor("Frantisek Slovak");
		pdfDocument.addCreator("Frantisek Slovak");
	}

	/**
	 * An alert generator method.
	 *
	 * @param alertType the alter type
	 * @param title the title of the alert
	 * @param contentText the content message of the alert
	 * @param isResizable the flag for allowing resizability
	 */
	private static void generateAlert(AlertType alertType, String title, String contentText, boolean isResizable){
		Alert newAlert = new Alert(alertType);
		newAlert.setTitle(title);
		newAlert.setContentText(contentText);
		newAlert.setResizable(isResizable);
		newAlert.showAndWait();
	}

	/**
	 * The Excel report generator method.
	 *
	 * @param displayedClient the client for whom the report will be generated
	 * @param selectedAddress the IP address from where the loans will be taken
	 * @param allLoansForIP the list of loans whose history shall be exported
	 */
	public static void createDetailedExcelReportForHistory(SerializedClient displayedClient, SerializedIPAddress selectedAddress, List<SerializedLoan> allLoansForIP) {
		/* Set up the workbooks to be created - one for the new and one for the old format */
		Workbook oldWorkbook = new HSSFWorkbook();
		Workbook newWorkbook = new XSSFWorkbook();
		/* Create the file names for holding the results */
		String finalOldFilename = OLD_EXCEL_REPORT_FILENAME_TEMPLATE.replace("{}", selectedAddress.getIpValue()).replace("[]", displayedClient.getName());
		String finalNewFilename = NEW_EXCEL_REPORT_FILENAME_TEMPLATE.replace("{}", selectedAddress.getIpValue()).replace("[]", displayedClient.getName());
		try {
			/* Create also the 2 file output streams required for result writing */
			FileOutputStream nfos = new FileOutputStream(finalNewFilename);
			FileOutputStream ofos = new FileOutputStream(finalOldFilename);
			/* Create 2 different invocations - one for processing each workbook type */
			createExcelExportInFormat(oldWorkbook, ofos, displayedClient, selectedAddress, allLoansForIP);
			createExcelExportInFormat(newWorkbook, nfos, displayedClient, selectedAddress, allLoansForIP);
		} catch (FileNotFoundException e) {
			generateAlert(AlertType.ERROR, "File Not Found Unfortunately", "Desired File Location has not found unfortunately!" + e.getClass().getName(), false);
			LOGGER.error("Exception detected while trying to create the desired report file!", e.getMessage());
		} catch (IOException e) {
			generateAlert(AlertType.ERROR, "IO Error During Report Generation", "Exception detected while trying to create the desired report file!" + e.getClass().getName(), false);
			LOGGER.error("Exception detected while trying to create the desired report file!", e.getMessage());
		}
	}

	/**
	 * The effective exported method responsible for file creation
	 *
	 * @param workbook the workbook to be populated and generated
	 * @param fos the file output stream used for putting in the result
	 * @param selectedClient the client for whom the export is being done
	 * @param selectedAddress the IP address from where the associated loans are being taken
	 * @param allLoansForIP the list of all loans being analyzed
	 *
	 * @throws IOException in case of any operation problem encountered
	 */
	private static void createExcelExportInFormat(Workbook workbook, FileOutputStream fos, SerializedClient selectedClient, SerializedIPAddress selectedAddress, List<SerializedLoan> allLoansForIP) throws IOException {
		/* Create first of all a sheet for storing all the data required for identifying the client */
		Sheet sheet = workbook.createSheet("General Loan History Information");
		/* Now use the iterator for generating the given user profile */
		createUserProfileInExcel(sheet, selectedClient, selectedAddress);
		/* Next create the general loan table and it to the same sheet */
		createGeneralLoanTable(sheet, allLoansForIP);
		/* Now add the table containing detailed loan history */
		createdDetailedLoanHistoryTable(sheet, allLoansForIP);
		/* Write out all the results to the given file and then close the given resources */
		workbook.write(fos);
		fos.close();
		workbook.close();
		/* Issue a short message for denoting the operation success */
		LocalDateTime newGenerationDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String formattedGenerationDateTime = newGenerationDateTime.format(formatter);
		String successfullOutcome = "The report for client " + selectedClient.getName() + " showing loan history on IP address " + selectedAddress.getIpValue() + " has been created successfully on " + formattedGenerationDateTime + "!";
		LOGGER.info(successfullOutcome);
	}

	/**
	 * The method responsible for creating the detailed loan history table
	 *
	 * @param sheet the sheet where to insert the given table
	 * @param allLoansForIP the loans to be displayed
	 */
	private static void createdDetailedLoanHistoryTable(Sheet sheet, List<SerializedLoan> allLoansForIP) {
		/* Set up a counter for the positioning the given table */
		int newRowIndex = 10 + allLoansForIP.size();
		/* Fetch a row in the sheet and put there the headers */
		Row currentRow = sheet.createRow(newRowIndex);
		Cell currentCell = currentRow.createCell(0);
		currentCell.setCellValue("Loan ID");
		currentCell = currentRow.createCell(1);
		currentCell.setCellValue("Return Date");
		currentCell = currentRow.createCell(2);
		currentCell.setCellValue("Interest Rate");
		currentCell = currentRow.createCell(3);
		currentCell.setCellValue("Extension Count");
        for (SerializedLoan sl : allLoansForIP){
        	/* Declare here a formatter used for processing the given dates */
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        	/* For unextended loan, just add there one line with the default information */
        	if (sl.getExtensionCount() == 0){
        		//Add also a new row to the document
            	currentRow = sheet.createRow(++newRowIndex);
        		//First the loan ID
    			Cell firstCell = currentRow.createCell(0);
    			firstCell.setCellValue(sl.getLoanID());
    			//Second, the return date - again format the value to fit in the given field
    			Cell secondCell = currentRow.createCell(1);
    			secondCell.setCellValue(sl.getReturnDate().format(formatter));
    			//Third, the interest rate
    			Cell thirdCell = currentRow.createCell(2);
    			thirdCell.setCellValue(sl.getInterestRate());
    			//Last, the extension count - its value should be 0 by default
    			Cell lastCell = currentRow.createCell(3);
    			lastCell.setCellValue(sl.getExtensionCount());
        	} else {
        		/* For extended loan, the history will need to be adjusted as follows */
        		/* Retain the original return date and interest rate before proceeding */
        		LocalDate originalReturnDate = sl.getApplicationTime().plusDays(7).toLocalDate();
        		Long originalInterestRate = sl.getLoanedAmount() / 10;
        		for (int i = 0; i < sl.getExtensionCount() + 1; i++){
        			//Add a new row
        			currentRow = sheet.createRow(++newRowIndex);
        			//First display the ID as normal
        			Cell firstCell = currentRow.createCell(0);
        			firstCell.setCellValue(sl.getLoanID());
        			//Then check value of the counter to see how the given return date and interest rate have to be adjusted
        			if (i == 0){
        				/* For 0, the original values need to be displayed for both parameters */
        				//Second, the return date - again format the value to fit in the given field
            			Cell secondCell = currentRow.createCell(1);
            			secondCell.setCellValue(originalReturnDate.format(formatter));
            			//Third, the interest rate
            			Cell thirdCell = currentRow.createCell(2);
            			thirdCell.setCellValue(originalInterestRate);
        			} else {
        				/* For non-zero case, return date increased by 7 days, while interest rate by a factor of 1.5 */
        				//Second, the return date - again format the value to fit in the given field
            			Cell secondCell = currentRow.createCell(1);
            			originalReturnDate = originalReturnDate.plusDays(7);
            			secondCell.setCellValue(originalReturnDate.format(formatter));
            			//Third, the interest rate
            			originalInterestRate = originalInterestRate * 15 / 10;
            			Cell thirdCell = currentRow.createCell(2);
            			thirdCell.setCellValue(originalInterestRate);
        			}
        			//Finally add the current extension count index
        			Cell lastCell = currentRow.createCell(3);
        			lastCell.setCellValue(i);
        		}
        	}
        }
	}

	/**
	 * The method used for creating the general loan table.
	 *
	 * @param sheet the sheet where to insert table
	 * @param allLoansForIP the loans whose details will be displayed
	 */
	private static void createGeneralLoanTable(Sheet sheet, List<SerializedLoan> allLoansForIP) {
		/* Define the row index used for marking the start of the populated zone */
		int newRowIndex = 7;
		/* Add the header row first */
    	Row currentRow = sheet.createRow(newRowIndex);
		Cell currentCell = currentRow.createCell(0);
		currentCell.setCellValue("Loan ID");
		currentCell = currentRow.createCell(1);
		currentCell.setCellValue("Application Time");
		currentCell = currentRow.createCell(2);
		currentCell.setCellValue("Return Date");
		currentCell = currentRow.createCell(3);
		currentCell.setCellValue("Amount");
		currentCell = currentRow.createCell(4);
		currentCell.setCellValue("Currency");
		currentCell = currentRow.createCell(5);
		currentCell.setCellValue("Total Interest Rate");
		currentCell = currentRow.createCell(6);
		currentCell.setCellValue("Is Extended?");
		currentCell = currentRow.createCell(7);
		currentCell.setCellValue("Total Extension Count");
		/* Afterwards, start populating the table */
		for (SerializedLoan sl : allLoansForIP){
			/* Reserve the row for the selected loan */
			Row row = sheet.createRow(++newRowIndex);
			/* Create for the selected row 8 cells for displaying all loan attributes */
			//First the ID
			Cell firstCell = row.createCell(0);
			firstCell.setCellValue(sl.getLoanID());
			//Second the Application Time - formatting will be required for adjusting the value into the given field
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String formattedApplicationDateTime = sl.getApplicationTime().format(formatter);
			Cell secondCell = row.createCell(1);
			secondCell.setCellValue(formattedApplicationDateTime);
			//Third, the final return date - again format the value to fit in the given field
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			Cell thirdCell = row.createCell(2);
			thirdCell.setCellValue(sl.getReturnDate().format(formatter));
			//Fourth, the loaned amount and currency
			Cell fourthCell = row.createCell(3);
			fourthCell.setCellValue(sl.getLoanedAmount());
			Cell fifthCell = row.createCell(4);
			fifthCell.setCellValue(sl.getCurrency().getCurrencyPrefix());
			//Fifth, the interest rate
			Cell sixthCell = row.createCell(5);
			sixthCell.setCellValue(sl.getInterestRate());
			//Last, but not least, the extension flag value and the total extension count
			Cell penultimateCell = row.createCell(6);
			penultimateCell.setCellValue(sl.isExtended() ? "Yes" : "No");
			Cell lastCell = row.createCell(7);
			lastCell.setCellValue(sl.getExtensionCount());
		}
	}

	/**
	 * A method for creating the user profile introducing the desired Excel export history.
	 *
	 * @param sheet the sheet where to put the user data
	 * @param selectedClient the client whose data has to be inserted
	 * @param selectedAddress the IP address completing the exported profile
	 */
    private static void createUserProfileInExcel(Sheet sheet, SerializedClient selectedClient, SerializedIPAddress selectedAddress) {
    	/* Create the first row for the name in the format Label:Value */
    	Row currentRow = sheet.createRow(0);
		Cell firstCell = currentRow.createCell(0);
		firstCell.setCellValue("Name:");
		Cell secondCell = currentRow.createCell(1);
		secondCell.setCellValue(selectedClient.getName());

		/* Proceed with the creation based on the same format rule announced above */
		/* The client PIN */
		currentRow = sheet.createRow(1);
		firstCell = currentRow.createCell(0);
		firstCell.setCellValue("PIN Code:");
		secondCell = currentRow.createCell(1);
		secondCell.setCellValue(selectedClient.getCnp());

		/* The e-mail address */
		currentRow = sheet.createRow(2);
		firstCell = currentRow.createCell(0);
		firstCell.setCellValue("E-mail Address:");
		secondCell = currentRow.createCell(1);
		secondCell.setCellValue(selectedClient.getEmailAddress());

		/* The postal address */
		currentRow = sheet.createRow(3);
		firstCell = currentRow.createCell(0);
		firstCell.setCellValue("Postal Address:");
		secondCell = currentRow.createCell(1);
		secondCell.setCellValue(selectedClient.getPostalAddress());

		/* The IP address */
		currentRow = sheet.createRow(4);
		firstCell = currentRow.createCell(0);
		firstCell.setCellValue("Selected IP Address:");
		secondCell = currentRow.createCell(1);
		secondCell.setCellValue(selectedAddress.getIpValue());
    }

}