package com.feritoth.cla.springmvc.dao;

import java.time.LocalDate;
import java.util.List;

import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;

public interface LoanDao {
	
	List<Loan> getAllRegisteredLoans();
	
	List<Loan> getAllLoansForClient(Client client);
	
	List<Loan> getAllLoansForIPAddress(IPAddress ipAddress);
	
	Loan getLoanHistoryByID(Integer loanID);
	
	int countLoansForIPAddressOnDay(IPAddress ipAddress, LocalDate applicationDate);
	
	int countAllLoansFromDB();
	
	/* Loan registerLoanForClient(IPAddress ipAddress, LocalDateTime applicationDateTime, LocalDate loanReturnDate, 
			                      Long amount, LoanCurrency currency, Boolean isExtended, Long interestRate);
     */
	void saveNewLoan(Loan loan);
	
	//Loan extendLoan(IPAddress ipAddress, Integer loanID, LocalDate newLoanReturnDate, Boolean isExtended, Long interestRate);
	void updateLoan(Loan loan);
	
	//boolean removeLoan(Integer loanID);
	void removeLoan(Integer loanID);

}