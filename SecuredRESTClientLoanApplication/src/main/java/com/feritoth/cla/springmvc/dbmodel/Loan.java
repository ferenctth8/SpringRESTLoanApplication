package com.feritoth.cla.springmvc.dbmodel;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table (name = "loan")
public class Loan {
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	@Column (name = "LoanID")
	private Integer loanID;
	@ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn (name = "IPAddressID", nullable = false)	
	private IPAddress ipAddress;
	@Column (name = "ApplicationTime")
	private Timestamp applicationTime;
	@Column (name = "LoanReturnDate")
	private Date paybackDate;
	@Column (name = "Amount")
	private Long amount;
	@Enumerated(EnumType.STRING)
	@Column (name = "Currency")
	private LoanCurrency currency;	
	@Type (type = "yes_no")
	@Column (name = "IsExtended")
	private Boolean isExtended;
	@Column (name = "InterestRate")
	private Long interestRate;
	
	public Loan() {
		super();		
	}

	public Loan(Integer loanID, IPAddress ipAddress,
			    Timestamp applicationTime, Date paybackDate,
			    Long amount, LoanCurrency currency,
			    Boolean extended, Long interestRate) {
		super();
		this.loanID = loanID;
		this.ipAddress = ipAddress;
		this.applicationTime = applicationTime;
		this.paybackDate = paybackDate;
		this.amount = amount;
		this.currency = currency;		
		this.isExtended = extended;
		this.interestRate = interestRate;
	}	

	public Integer getLoanID() {
		return loanID;
	}

	public void setLoanID(Integer loanID) {
		this.loanID = loanID;
	}

	public IPAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(IPAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public LocalDateTime getApplicationTime() {
		return applicationTime.toLocalDateTime();
	}

	public void setApplicationTime(LocalDateTime applicationTime) {
		this.applicationTime = Timestamp.valueOf(applicationTime);
	}

	public LocalDate getPaybackDate() {
		return paybackDate.toLocalDate();
	}

	public void setPaybackDate(LocalDate paybackDate) {
		this.paybackDate = Date.valueOf(paybackDate);
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
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

	public void setExtended(Boolean extended) {
		this.isExtended = extended;
	}

	public Long getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(Long interestRate) {
		this.interestRate = interestRate;
	}	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applicationTime == null) ? 0 : applicationTime.hashCode());
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Loan other = (Loan) obj;
		if (applicationTime == null) {
			if (other.applicationTime != null)
				return false;
		} else if (!applicationTime.equals(other.applicationTime))
			return false;
		if (ipAddress == null) {
			if (other.ipAddress != null)
				return false;
		} else if (!ipAddress.equals(other.ipAddress))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Loan [loanID=" + loanID + ", ipAddress=" + ipAddress.toString()
				+ ", applicationTime=" + applicationTime + ", paybackDate="
				+ paybackDate + ", amount=" + amount + ", currency=" + currency
				+ ", extended=" + isExtended + ", interestRate=" + interestRate + "]";
	}	

}