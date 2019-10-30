/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
