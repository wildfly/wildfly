/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt.lazyenlist;

import java.text.MessageFormat;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Lazy transaction enlistment. Migration test from EJB Testsuite (ejbthree-1028) to AS7 [JIRA JBQA-5483].
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LazyTransactionEnlistmentUnitTestCase {
    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "tx-lazy-enlist.jar").addPackage(
                LazyTransactionEnlistmentUnitTestCase.class.getPackage());
        jar.addAsManifestResource(LazyTransactionEnlistmentUnitTestCase.class.getPackage(), "persistence.xml",
                "persistence.xml");
        return jar;
    }

    @Test
    public void testDepositTwice() throws Exception {
        ATM atm = getAtm();
        double deposit = 64, increase1 = 128, increase2 = 256;
        long id = atm.createAccount(deposit);
        double baseBalance = atm.getBalance(id);
        Assert.assertEquals("Account should be created with input deposit",
                deposit, baseBalance, Double.NaN);

        double balanceOnDeposit = atm.depositTwice(id, increase1, increase2);
        Assert.assertEquals("Deposit should be increased twice by " + increase1 + " and " + increase2,
                deposit + increase1 + increase2, balanceOnDeposit, Double.NaN);
        double balanceAfterDeposit = atm.getBalance(id);
        Assert.assertEquals("Balance got from ATM should be equal to base deposit plus two increased deposit amounts",
                deposit + increase1 + increase2, balanceAfterDeposit, Double.NaN);
    }

    @Test
    public void testDepositTwiceRawSQL() throws Exception {
        ATM atm = getAtm();
        double deposit = 64, increase1 = 128, increase2 = 256;
        long id = atm.createAccount(deposit);
        double baseBalance = atm.getBalance(id);
        Assert.assertEquals("Account should be created with input deposit",
                deposit, baseBalance, Double.NaN);

        double balanceOnDeposit = atm.depositTwiceRawSQL(id, increase1, increase2);
        Assert.assertEquals("Deposit should be increased twice by " + increase1 + " and " + increase2,
                deposit + increase1 + increase2, balanceOnDeposit, Double.NaN);
        double balanceAfterDeposit = atm.getBalance(id);
        Assert.assertEquals("Balance got from ATM should be equal to base deposit plus two increased deposit amounts",
                deposit + increase1 + increase2, balanceAfterDeposit, Double.NaN);
    }

    @Test
    public void testDepositeTwiceWithRollback() throws Exception {
        ATM atm = getAtm();
        // if only
        long id = atm.createAccount(1000000);
        double balance = atm.getBalance(id);
        Assert.assertEquals(1000000, balance, Double.NaN);

        balance = atm.depositTwiceWithRollback(id, 125000, 250000);
        // the entity state itself won't be rolled back
        Assert.assertEquals(1375000, balance, Double.NaN);
        balance = atm.getBalance(id);
        Assert.assertEquals(1125000, balance, Double.NaN);
    }

    @Test
    public void testRawSQL() throws Exception {
        ATM atm = getAtm();
        // if only
        long id = atm.createAccount(1000000);
        double balance = atm.getBalance(id);
        Assert.assertEquals(1000000, balance, Double.NaN);

        balance = atm.withdrawTwiceWithRollback(id, 125000, 250000);
        balance = atm.getBalance(id);
        Assert.assertEquals(875000, balance, Double.NaN);
    }

    private ATM getAtm() throws NamingException {
        return (ATM) ctx.lookup(MessageFormat.format("ejb:/tx-lazy-enlist/{0}!{1}", ATMBean.class.getSimpleName(), ATM.class.getName()));
    }
}
