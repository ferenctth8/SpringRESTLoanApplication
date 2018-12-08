package com.feritoth.cla.springmvc.dao.hibernate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.feritoth.cla.springmvc.dao.LoanDao;
import com.feritoth.cla.springmvc.dbmodel.Client;
import com.feritoth.cla.springmvc.dbmodel.IPAddress;
import com.feritoth.cla.springmvc.dbmodel.Loan;

@Repository("loanDao")
public class LoanHibernateDao extends AbstractHibernateDao implements LoanDao {

	@SuppressWarnings("unchecked")
	@Override
	public List<Loan> getAllRegisteredLoans() {
		Criteria criteria = getSession().createCriteria(Loan.class);
		List<Loan> allLoans = (List<Loan>)criteria.list();
		allLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress()));
		allLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress().getClient()));
		return allLoans;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Loan> getAllLoansForClient(Client client) {
		Criteria criteria = getSession().createCriteria(Loan.class);		
		List<Loan> allRegisteredLoans = (List<Loan>)criteria.list();
		allRegisteredLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress()));
		allRegisteredLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress().getClient()));
		//return allRegisteredLoans;
		return (List<Loan>) allRegisteredLoans.parallelStream().filter(loan -> loan.getIpAddress().getClient().equals(client)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Loan> getAllLoansForIPAddress(IPAddress ipAddress) {
		Criteria criteria = getSession().createCriteria(Loan.class);
		criteria.add(Restrictions.eq("ipAddress", ipAddress));
		List<Loan> allMatchingLoans = criteria.list();
		allMatchingLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress()));
		allMatchingLoans.forEach(loan -> Hibernate.initialize(loan.getIpAddress().getClient()));
		return allMatchingLoans;
	}

	@Override
	public Loan getLoanHistoryByID(Integer loanID) {
		Criteria criteria = getSession().createCriteria(Loan.class);
		criteria.add(Restrictions.eq("loanID", loanID));
		Loan selectedLoan = (Loan) criteria.uniqueResult();
		Hibernate.initialize(selectedLoan.getIpAddress());
		Hibernate.initialize(selectedLoan.getIpAddress().getClient());
		return selectedLoan;
	}
	
	@Override
	public int countAllLoansFromDB() {		
		String LOAN_COUNT_QUERY = "SELECT COUNT(*) FROM Loan";
		return ((Long) getSession().createQuery(LOAN_COUNT_QUERY).iterate().next()).intValue();
	}
	
	@Override
	public int countLoansForIPAddressOnDay(IPAddress ipAddress,	LocalDate applicationDate) {
		//SELECT COUNT(*) FROM loan_risk_application.loan WHERE ipAddressID = 1 AND  DATE(applicationTime) = '2016-12-12';
		String LOAN_COUNT_QUERY = "SELECT COUNT(*) FROM Loan l WHERE l.ipAddress.value = :ipAddress AND DATE(l.applicationTime) = :applicationDate";		
		return ( (Long) getSession().createQuery(LOAN_COUNT_QUERY).setParameter("ipAddress", ipAddress.getValue()).setDate("applicationDate", Date.valueOf(applicationDate)).iterate().next() ).intValue();
	}

	@Override
	public void saveNewLoan(Loan loan) {
		persist(loan);
	}

	@Override
	public void updateLoan(Loan loan) {
		update(loan);
	}

	@Override
	public void removeLoan(Integer loanID) {
		Query loanRemovalQuery = getSession().createSQLQuery("delete from Loan where loanID=:loanID");
		loanRemovalQuery.setInteger("loanID", loanID);
		loanRemovalQuery.executeUpdate();
	}	

}