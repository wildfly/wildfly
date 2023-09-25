/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton;

import org.jboss.logging.Logger;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SingletonUnitTestCase
 * <p>
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class SingletonUnitTestCase {
    private static final Logger log = Logger.getLogger(SingletonUnitTestCase.class.getName());

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb31singleton.jar");
        jar.addPackage(SingletonUnitTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testSingletonBeanAccess() throws Exception {
        AccountManagerBean accountManagerLocal = (AccountManagerBean) ctx.lookup("java:module/"
                + AccountManagerBean.class.getSimpleName() + "!" + AccountManagerBean.class.getName());
        AccountManager accountManagerRemote = (AccountManager) ctx.lookup("java:module/"
                + AccountManagerBean.class.getSimpleName() + "!" + AccountManager.class.getName());

        int initialBalance = accountManagerRemote.balance();
        Assert.assertEquals("Unexpected initial balance", 0, initialBalance);

        // credit
        accountManagerRemote.credit(100);

        AccountManager anotherAccountManagerRemoteInstance = (AccountManager) ctx.lookup("java:module/"
                + AccountManagerBean.class.getSimpleName() + "!" + AccountManager.class.getName());
        int balanceAfterCredit = anotherAccountManagerRemoteInstance.balance();
        Assert.assertEquals("Unexpected balance after credit", 100, balanceAfterCredit);

        // debit
        anotherAccountManagerRemoteInstance.debit(50);

        // checking whether singleton works after throwing exception in a business method
        // EJB3.1 4.8.4
        try {
            accountManagerLocal.throwException();
        } catch (Exception e) {
            // it's supposed - OK
        }

        int balanceAfterDebit = accountManagerRemote.balance();
        Assert.assertEquals("Unexpected balance after debit", 50, balanceAfterDebit);
        // testing singleton identity - EJB3.1 3.4.7.3
        Assert.assertFalse(accountManagerRemote.equals(accountManagerLocal));
        Assert.assertTrue(accountManagerRemote.equals(anotherAccountManagerRemoteInstance));
    }
}
