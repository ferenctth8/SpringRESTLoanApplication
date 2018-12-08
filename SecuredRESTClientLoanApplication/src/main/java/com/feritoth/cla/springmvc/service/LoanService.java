package com.feritoth.cla.springmvc.service;

import java.util.List;
import java.util.Map;

import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;

public interface LoanService {
	
	/* A value denoting the initial loan duration period - 1 week */	
	long INITIAL_LOAN_PERIOD_DURATION = 1L;
	
	/* Two values for denoting the maximum amount of money to be given as loan, based on the selected currency */
	long MAXIMUM_CZK_AMOUNT = 30000L; /* For CZK */
	long MAXIMUM_EUR_AMOUNT = 15000L; /* For EUR */
	
	/* A value indicating the number of maximum allowed loan attempts (i.e. successfully registered ones) from an IP address for one day */
	int MAXIMUM_LOAN_NB_PER_DAY = 3;
	
	/* A value indicating the percentage of the interest rate given for a certain loan - valid only during its first submission */
	int INTEREST_RATE_PERCENTAGE = 10;
	
	/* The value used for increasing the interest rate of a loan in case of any term prolongation */
	double INTEREST_RATE_INCREASE_FACTOR = 1.5;
	
	List<Loan> fetchAllLoans();
	
	List<Loan> getAllLoansForClient(Client client);
	
	List<Loan> findAllLoansForIPAddress(IPAddress ipAddress);
	
	Loan fetchHistoryForLoanID(Integer loanID);
	
	void registerNewLoan(Loan newLoan);
	
	void extendExistingLoan(Loan existingLoan);
	
	void removeLoan(Integer loanID);
	
	Map<String, Exception> getRegUpFlagMap();

}