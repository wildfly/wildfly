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

import java.util.logging.Logger;

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
 * 
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
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testSingletonBeanAccess() throws Exception {
        AccountManager accountManager = (AccountManager) ctx.lookup("java:module/" + AccountManagerBean.class.getSimpleName());

        int initialBalance = accountManager.balance();
        Assert.assertEquals("Unexpected initial balance", 0, initialBalance);

        // credit
        accountManager.credit(100);

        AccountManager anotherAccountManagerInstance = (AccountManager) ctx.lookup("java:module/"
                + AccountManagerBean.class.getSimpleName());
        int balanceAfterCredit = anotherAccountManagerInstance.balance();
        Assert.assertEquals("Unexpected balance after credit", 100, balanceAfterCredit);

        // debit
        anotherAccountManagerInstance.debit(50);

        int balanceAfterDebit = accountManager.balance();
        Assert.assertEquals("Unexpected balance after debit", 50, balanceAfterDebit);

    }
}
