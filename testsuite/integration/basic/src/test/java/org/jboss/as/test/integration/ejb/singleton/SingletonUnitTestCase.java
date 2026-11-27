/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton;

import org.jboss.logging.Logger;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * SingletonUnitTestCase
 * <p>
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(0, initialBalance, "Unexpected initial balance");

        // credit
        accountManagerRemote.credit(100);

        AccountManager anotherAccountManagerRemoteInstance = (AccountManager) ctx.lookup("java:module/"
                + AccountManagerBean.class.getSimpleName() + "!" + AccountManager.class.getName());
        int balanceAfterCredit = anotherAccountManagerRemoteInstance.balance();
        Assertions.assertEquals(100, balanceAfterCredit, "Unexpected balance after credit");

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
        Assertions.assertEquals(50, balanceAfterDebit, "Unexpected balance after debit");
        // testing singleton identity - EJB3.1 3.4.7.3
        Assertions.assertFalse(accountManagerRemote.equals(accountManagerLocal));
        Assertions.assertTrue(accountManagerRemote.equals(anotherAccountManagerRemoteInstance));
    }
}
