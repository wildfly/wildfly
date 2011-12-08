/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.transaction.bmt.lazyenlist;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Lazy transaction enlistment. Migration test from EJB Testsuite (ejbthree-1028) to AS7 [JIRA JBQA-5483].
 * 
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class LazyTransactionEnlistmentUnitTestCase {
    private static final Logger log = Logger.getLogger(LazyTransactionEnlistmentUnitTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "tx-lazy-enlist.jar").addPackage(
                LazyTransactionEnlistmentUnitTestCase.class.getPackage());
        jar.addAsManifestResource(LazyTransactionEnlistmentUnitTestCase.class.getPackage(), "persistence.xml",
                "persistence.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void test() throws Exception {
        ATM atm = (ATM) ctx.lookup("java:module/" + ATMBean.class.getSimpleName() + "!" + ATM.class.getName());
        // if only
        long id = atm.createAccount(1000000);
        System.out.println("*** id " + id);
        double balance = atm.getBalance(id);
        System.out.println("*** balance " + balance);
        Assert.assertEquals(1000000, balance, Double.NaN);

        balance = atm.depositTwiceWithRollback(id, 125000, 250000);
        System.out.println("*** balance " + balance);
        // the entity state itself won't be rolled back
        Assert.assertEquals(1375000, balance, Double.NaN);
        balance = atm.getBalance(id);
        System.out.println("*** balance " + balance);
        Assert.assertEquals(1125000, balance, Double.NaN);
    }

    @Test
    @Ignore("AS7-2874")
    public void testRawSQL() throws Exception {
        ATM atm = (ATM) ctx.lookup("java:module/" + ATMBean.class.getSimpleName() + "!" + ATM.class.getName());
        // if only
        long id = atm.createAccount(1000000);
        System.out.println("*** id " + id);
        double balance = atm.getBalance(id);
        System.out.println("*** balance " + balance);
        Assert.assertEquals(1000000, balance, Double.NaN);

        balance = atm.withdrawTwiceWithRollback(id, 125000, 250000);
        System.out.println("*** balance " + balance);
        balance = atm.getBalance(id);
        System.out.println("*** balance " + balance);
        Assert.assertEquals(875000, balance, Double.NaN);
    }
}
