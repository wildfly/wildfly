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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Lazy transaction enlistment. Migration test from EJB Testsuite (ejbthree-1028) to AS7 [JIRA JBQA-5483].
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(deposit,
                baseBalance, "Account should be created with input deposit");

        double balanceOnDeposit = atm.depositTwice(id, increase1, increase2);
        Assertions.assertEquals(deposit + increase1 + increase2,
                balanceOnDeposit, "Deposit should be increased twice by " + increase1 + " and " + increase2);
        double balanceAfterDeposit = atm.getBalance(id);
        Assertions.assertEquals(deposit + increase1 + increase2,
                balanceAfterDeposit, "Balance got from ATM should be equal to base deposit plus two increased deposit amounts");
    }

    @Test
    public void testDepositTwiceRawSQL() throws Exception {
        ATM atm = getAtm();
        double deposit = 64, increase1 = 128, increase2 = 256;
        long id = atm.createAccount(deposit);
        double baseBalance = atm.getBalance(id);
        Assertions.assertEquals(deposit,
                baseBalance, "Account should be created with input deposit");

        double balanceOnDeposit = atm.depositTwiceRawSQL(id, increase1, increase2);
        Assertions.assertEquals(deposit + increase1 + increase2,
                balanceOnDeposit, "Deposit should be increased twice by " + increase1 + " and " + increase2);
        double balanceAfterDeposit = atm.getBalance(id);
        Assertions.assertEquals(deposit + increase1 + increase2,
                balanceAfterDeposit, "Balance got from ATM should be equal to base deposit plus two increased deposit amounts");
    }

    @Test
    public void testDepositeTwiceWithRollback() throws Exception {
        ATM atm = getAtm();
        // if only
        long id = atm.createAccount(1000000);
        double balance = atm.getBalance(id);
        Assertions.assertEquals(1000000, balance);

        balance = atm.depositTwiceWithRollback(id, 125000, 250000);
        // the entity state itself won't be rolled back
        Assertions.assertEquals(1375000, balance);
        balance = atm.getBalance(id);
        Assertions.assertEquals(1125000, balance);
    }

    @Test
    public void testRawSQL() throws Exception {
        ATM atm = getAtm();
        // if only
        long id = atm.createAccount(1000000);
        double balance = atm.getBalance(id);
        Assertions.assertEquals(1000000, balance);

        balance = atm.withdrawTwiceWithRollback(id, 125000, 250000);
        balance = atm.getBalance(id);
        Assertions.assertEquals(875000, balance);
    }

    private ATM getAtm() throws NamingException {
        return (ATM) ctx.lookup(MessageFormat.format("ejb:/tx-lazy-enlist/{0}!{1}", ATMBean.class.getSimpleName(), ATM.class.getName()));
    }
}
