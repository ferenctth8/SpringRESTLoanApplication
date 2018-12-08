package com.feritoth.cla.springmvc.jsonmodel;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.feritoth.cla.springmvc.controller.utility.LocalDateTimeDeserializer;
import com.feritoth.cla.springmvc.controller.utility.LocalDateTimeSerializer;
import com.feritoth.cla.springmvc.dbmodel.Loan;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SerializedLoan implements Serializable {
	
	private static final long serialVersionUID = -4396287982045867105L;
	
	private Integer loanID;
	private SerializedIPAddress ipAddress;
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime applicationTime;
	@JsonFormat (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate returnDate;
	private Long loanedAmount;
	private LoanCurrency currency;
	private Boolean isExtended;
	private Long interestRate;
	private Long extensionCount;
		
	public SerializedLoan() {
		super();		
	}

	public SerializedLoan(LocalDateTime applicationTime, LocalDate returnDate, Long loanedAmount, 
			              LoanCurrency currency, Boolean isExtended, Long interestRate, Integer loanID) {
		super();
		this.loanID = loanID;
		this.applicationTime = applicationTime;
		this.returnDate = returnDate;
		this.loanedAmount = loanedAmount;
		this.currency = currency;
		this.isExtended = isExtended;
		this.interestRate = interestRate;
	}

	public SerializedLoan(Loan loan) {
		this.loanID = loan.getLoanID();
		this.ipAddress = new SerializedIPAddress(loan.getIpAddress());
		this.applicationTime = loan.getApplicationTime();
		this.loanedAmount = loan.getAmount();
		this.returnDate = loan.getPaybackDate();
		this.currency = LoanCurrency.valueOf(loan.getCurrency().name());
		this.isExtended = loan.isExtended();
		this.interestRate = loan.getInterestRate();
	}

	public Integer getLoanID() {
		return loanID;
	}

	public void setLoanID(Integer loanID) {
		this.loanID = loanID;
	}

	public SerializedIPAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(SerializedIPAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public LocalDateTime getApplicationTime() {
		return applicationTime;
	}

	public void setApplicationTime(LocalDateTime applicationTime) {
		this.applicationTime = applicationTime;
	}

	public LocalDate getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(LocalDate returnDate) {
		this.returnDate = returnDate;
	}

	public Long getLoanedAmount() {
		return loanedAmount;
	}

	public void setLoanedAmount(Long loanedAmount) {
		this.loanedAmount = loanedAmount;
	}

	public LoanCurrency getCurrency() {
		return currency;
	}

	public void setCurrency(LoanCurrency currency) {
		this.currency = currency;
	}

	public Boolean isExtended() {
		return isExtended;
	}

	public void setExtended(Boolean isExtended) {
		this.isExtended = isExtended;
	}

	public Long getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(Long interestRate) {
		this.interestRate = interestRate;
	}	

	public Long getExtensionCount() {
		return extensionCount;
	}

	public void setExtensionCount(Long extensionCount) {
		this.extensionCount = extensionCount;
	}

	@Override
	public String toString() {
		return "SerializedLoan [loanID=" + loanID + ", ipAddress=" + ipAddress	+ ", applicationTime=" + applicationTime + 
				             ", returnDate=" + returnDate + ", loanedAmount=" + loanedAmount + ", currency="	+ currency + 
				             ", isExtended=" + isExtended + ", interestRate=" + interestRate + ", extensionCount=" + extensionCount + "]";
	}

		
	
}