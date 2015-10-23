package com.onewaveinc.mrc;


import junit.framework.Assert;
import org.unit.Before;
import org.junit.Test;

public class TestBankAccount{
	@Test
	public void testDebigWithSufficientFunds(){
		BankAccount account = new BankAccount(10);
		double amount = account.debit(5);
		Assert.assertEquals(5.0, amount);
	}
}
