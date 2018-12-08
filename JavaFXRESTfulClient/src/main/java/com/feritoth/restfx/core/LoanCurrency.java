package com.feritoth.restfx.core;

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