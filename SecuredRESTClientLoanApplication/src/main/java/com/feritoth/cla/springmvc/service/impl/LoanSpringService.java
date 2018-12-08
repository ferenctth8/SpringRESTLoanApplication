package com.feritoth.cla.springmvc.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.feritoth.cla.springmvc.dao.IPAddressDao;
import com.feritoth.cla.springmvc.dao.LoanDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;
import com.feritoth.cla.springmvc.dbmodel.LoanCurrency;
import com.feritoth.cla.springmvc.service.LoanService;

@Repository("loanService")
@Transactional
public class LoanSpringService implements LoanService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LoanSpringService.class);
	
	@Autowired
	private LoanDao loanDao;
	
	@Autowired
	private IPAddressDao ipAddressDao;
	
	/* Declare here the map of exceptions used during the loan registration and extension operations */
	private Map<String, Exception> regUpFlagMap = new HashMap<>();
	
	@Override
	public Map<String, Exception> getRegUpFlagMap() {		
		return Collections.unmodifiableMap(regUpFlagMap);
	}
	
	private boolean checkIfMaximumLoanExtensionDurationReached(LocalDate initialReturnDate,	LocalDate newReturnDate) {
		long weeksBetweenLoanReturns = ChronoUnit.WEEKS.between(initialReturnDate, newReturnDate);
		return weeksBetweenLoanReturns < 52;
	}
	
	private boolean validateLoanPeriodForRegistration(LocalDateTime loanRegistrationDateTime, LocalDate loanReturnDate){		
		/* Check first if the return date is after the initially given one */
		boolean datesCorrect = loanRegistrationDateTime.toLocalDate().isBefore(loanReturnDate);
		/* Then see how many weeks are between the given dates - in case of registration, this number must be at exactly 1 */
		long weeksBetweenLoanReturns = ChronoUnit.WEEKS.between(loanRegistrationDateTime.toLocalDate(), loanReturnDate);
		long loanDurationLength = weeksBetweenLoanReturns / INITIAL_LOAN_PERIOD_DURATION;
		/* Also check if the number of days between the 2 dates can be divided by 7 - i.e. the remainder of the division is 0 */
		long loanDurationRemainder = ChronoUnit.DAYS.between(loanRegistrationDateTime.toLocalDate(), loanReturnDate) % 7;
		/* Return the results of the validation in a composite condition */
		return loanDurationLength == 1 && loanDurationRemainder == 0 && datesCorrect;
	}
	
	private boolean validateLoanPeriodForExtension(LocalDate initialReturnDate, LocalDate newReturnDate){
		/* Check first if the new return date is after the initially given one */
		boolean datesCorrect = initialReturnDate.isBefore(newReturnDate);
		/* Then see how many weeks are between the given dates - in case of extension, this number must be at least 2 */
		long weeksBetweenLoanReturns = ChronoUnit.WEEKS.between(initialReturnDate, newReturnDate);
		long loanDurationLength = weeksBetweenLoanReturns / INITIAL_LOAN_PERIOD_DURATION;
		/* Also check if the number of days between the 2 dates can be divided by 7 - i.e. the remainder of the division is 0 */
		long loanDurationRemainder = ChronoUnit.DAYS.between(initialReturnDate, newReturnDate) % 7;
		/* Return the results of the validation in a composite condition */
		return loanDurationLength >= 2 && loanDurationRemainder == 0 && datesCorrect;
	}
	
	private boolean validateLoanIPAddress(IPAddress loanAddress){
		IPAddress registeredAddress = ipAddressDao.findIPAddressDetailsForValue(loanAddress.getValue());
		return loanAddress.equals(registeredAddress);
	}
	
	private boolean checkDailyLoanNbForIPAddressOnDate(IPAddress ipAddress, LocalDate applicationDate){
		int currentLoanCount = loanDao.countLoansForIPAddressOnDay(ipAddress, applicationDate);
		return currentLoanCount >= MAXIMUM_LOAN_NB_PER_DAY;
	}
	
	private boolean validateRegularLoanAmount(LoanCurrency currency, Long loanAmount){
		boolean loanAmountValid = false;
		switch(currency){
		case CZK:
			loanAmountValid = (0 < loanAmount) && (loanAmount <= MAXIMUM_CZK_AMOUNT) ? true : false;
			break;
		case EUR:
			loanAmountValid = (0 < loanAmount) && (loanAmount <= MAXIMUM_EUR_AMOUNT) ? true : false;
			break;
		}
		return loanAmountValid;
	}
	
	private boolean validateRiskLoanAmount(LoanCurrency currency, Long loanAmount){
		boolean loanAmountValid = false;
		switch(currency){
		case CZK:
			loanAmountValid = (loanAmount == MAXIMUM_CZK_AMOUNT) ? true : false;
			break;
		case EUR:
			loanAmountValid = (loanAmount == MAXIMUM_EUR_AMOUNT) ? true : false;
			break;
		}
		return loanAmountValid;
	}
	
	private boolean validateRiskLoanApplicationTime(LocalTime applicationTime){
		LocalTime midnightTime = LocalTime.MIDNIGHT;
		LocalTime morningTime = LocalTime.of(6, 0, 0);
		return (applicationTime.isAfter(midnightTime) || applicationTime.equals(midnightTime)) && (applicationTime.isBefore(morningTime) || applicationTime.equals(morningTime)); 
	}
	
	private long computeInitialInterestRateForLoan(Long loanAmount){
		return loanAmount * 10 / 100;
	}
	
	private long computeNewInterestRateForLoan(Long interestRate) {		
		return interestRate * 15 / 10;
	}
	
	@Override
	public List<Loan> fetchAllLoans() {
		return loanDao.getAllRegisteredLoans();
	}

	@Override
	public List<Loan> getAllLoansForClient(Client client) {
		return loanDao.getAllLoansForClient(client);
	}

	@Override
	public List<Loan> findAllLoansForIPAddress(IPAddress ipAddress) {
		return loanDao.getAllLoansForIPAddress(ipAddress);
	}

	@Override
	public Loan fetchHistoryForLoanID(Integer loanID) {
		int allLoansCount = loanDao.countAllLoansFromDB();
		if (loanID > allLoansCount){
			return null;
		}
		return loanDao.getLoanHistoryByID(loanID);
	}

	@Override
	public void registerNewLoan(Loan newLoan) {
		/* As a prerequisite, empty the flag map in order to avoid the occurrence of previously detected problems */
		regUpFlagMap.clear();
		/* Then start with the examination of the submitted loan */
		/* First, check if the IP address of the loan is registered in the DB */
		boolean validIPAddress = validateLoanIPAddress(newLoan.getIpAddress());
		/* Second, validate the duration period of the given loan */
		boolean validDuration = validateLoanPeriodForRegistration(newLoan.getApplicationTime(), newLoan.getPaybackDate());
		/* Third, see if the loan amount is allowed to be done */
		boolean regularLoanAmountValid = validateRegularLoanAmount(newLoan.getCurrency(), newLoan.getAmount());
		/* For all registration pre-conditions fulfilled, do quick risk analysis of the surrounding loan before registration */
		if (validIPAddress && validDuration && regularLoanAmountValid){
			/* First, see if the number of loans for the given IP address and application date exceeds the maximum limit */
			boolean maximumLoansPerDayReached = checkDailyLoanNbForIPAddressOnDate(newLoan.getIpAddress(), newLoan.getApplicationTime().toLocalDate());
			/* Second, add in 2 parts the risk conditions related to the maximum possible amount to be requested and the application time */
			boolean riskLoanAmountValid = validateRiskLoanAmount(newLoan.getCurrency(), newLoan.getAmount());
			boolean riskLoanTimeValid = validateRiskLoanApplicationTime(newLoan.getApplicationTime().toLocalTime());			
			/* Check also if the risk conditions are fulfilled before the effective save of the loan */
			if (maximumLoansPerDayReached){
				LOGGER.error("Risk level surrounding loan is high due to overlap of maximum daily loan limit given for an IP Address!");
				regUpFlagMap.put("maximumLoanNumberPerIPForDay", new IllegalArgumentException("Loan cannot be saved due to the exceed on the number of allowed loans (currently set to 3) from the selected IP for a day!"));				
			} else if (riskLoanAmountValid && riskLoanTimeValid){
				LOGGER.error("Risk level surrounding loan is high due to unsuitable risk application period and maximum allowable loan value!");
				regUpFlagMap.put("timePeriodLoanRisk", new IllegalArgumentException("Loan cannot be saved due to a high risk level on the application time and granted amount! (CZK = 30000, EUR = 15000, between midnight and 6 AM in the morning)"));
			} else {
			    /* Compute the value of interest rate before saving the given rate */
				newLoan.setInterestRate(computeInitialInterestRateForLoan(newLoan.getAmount()));
				/* Also, mark the extension flag for the newly registered loan */
				newLoan.setExtended(Boolean.FALSE);
				/* Finally, save the loan in the database */
			    loanDao.saveNewLoan(newLoan);
			}
		} else {
			if (!validIPAddress){
				LOGGER.error("Impossible to register loan due to invalid or unregistered IP address! ", newLoan.getIpAddress().getValue());
				regUpFlagMap.put("faultyIPAddress", new IllegalArgumentException("Loan cannot be saved due to the invalidity of the assigned IP address!"));				
			}
			if (!validDuration){
				LOGGER.error("Impossible to register loan due to invalid duration period! ", newLoan.getApplicationTime(), newLoan.getPaybackDate());
				regUpFlagMap.put("faultyLoanDuration", new IllegalArgumentException("Loan cannot be saved due to the invalidity of the duration period!"));				
			}
			if (!regularLoanAmountValid){
				if (newLoan.getAmount() <= 0){
					LOGGER.error("Impossible to register loan due to negative or zero amount introduced by user! ", newLoan.getAmount());
					regUpFlagMap.put("negativeOrZeroLoanAmount", new IllegalArgumentException("Loan cannot be saved due to negative or zero amount introduced by user!"));					
				} else {
					LOGGER.error("Impossible to register loan due to overlap of maximum allowed amount (CZK = 30000, EUR = 15000)! ", newLoan.getAmount());
					regUpFlagMap.put("maximumLoanAmountOverlapped", new IllegalArgumentException("Loan cannot be saved due to overlap of the maximum allowed amount (CZK = 30000, EUR = 15000) for the chosen currency!"));					
				}				
			}
		}
	}

	@Override
	public void extendExistingLoan(Loan existingLoan) {
		/* As a prerequisite, empty the flag map in order to avoid unwanted overwrites from previous invocations */
		regUpFlagMap.clear();
		/* First, check if the new duration of the loan is valid */
		boolean newValidDuration = validateLoanPeriodForExtension(existingLoan.getApplicationTime().toLocalDate(), existingLoan.getPaybackDate());
		/* Second, check if the difference between the 2 dates does not exceed 52 weeks (i.e. 1 calendaristic year)*/
		boolean durationLimitNotExceeded = checkIfMaximumLoanExtensionDurationReached(existingLoan.getApplicationTime().toLocalDate(), existingLoan.getPaybackDate());
		/* If these 2 conditions are valid, mark all the fields which are allowed to be updated and then call the update facility service */
		if (newValidDuration && durationLimitNotExceeded){
			/* If this is the first extension, set the isExtended flag to true (has no effect on the loans which already have this flag set to true) */
			existingLoan.setExtended(Boolean.TRUE);
			/* Next, also compute the new interest rate and set it inside the loan */
			long newInterestRate = computeNewInterestRateForLoan(existingLoan.getInterestRate());
			existingLoan.setInterestRate(newInterestRate);
			/* Finally, call the update method */
			loanDao.updateLoan(existingLoan);
		} else {
			/* Check which of the new parameters was supplied in the wrong format */
			if (!newValidDuration){
				LOGGER.error("Impossible to update loan due to the invalidity of the specified new period!", existingLoan.getApplicationTime().toLocalDate(), existingLoan.getPaybackDate());
				String exMessage = "Impossible to update loan due to the invalidity of the specified new period!" + existingLoan.getApplicationTime().toLocalDate() + " " + existingLoan.getPaybackDate();
				regUpFlagMap.put("faultyLoanDuration", new IllegalArgumentException(exMessage));
			} 
			if (!durationLimitNotExceeded){
				LOGGER.error("Impossible to update loan due to the exceed of the maximum extensibility period!", existingLoan.getApplicationTime().toLocalDate(), existingLoan.getPaybackDate());
				String exMessage = "Impossible to update loan due to the exceed of the maximum extensibility period!" + existingLoan.getApplicationTime().toLocalDate() + " " + existingLoan.getPaybackDate();
				regUpFlagMap.put("maximumExtensibilityReached", new IllegalArgumentException(exMessage));
			}			
		}
	}	

	@Override
	public void removeLoan(Integer loanID) {
		loanDao.removeLoan(loanID);		
	}	

}