package com.feritoth.cla.springmvc.dbmodel;

public enum LoanCurrency {
	
	CZK("CZK"), EUR("EUR");
	
	private final String currencyPrefix;

	private LoanCurrency(String currencyPrefix) {
		this.currencyPrefix = currencyPrefix;
	}

	public String getCurrencyPrefix() {
		return currencyPrefix;
	}
	
}