/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.simple.wsat.client;

import java.net.MalformedURLException;

import javax.inject.Inject;

import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.simple.wsat.jaxws.RestaurantServiceAT;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple set of tests for the WSAT.
 *
 * @author paul.robinson@redhat.com, 2012-01-04
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class WSATTestCase {
    private static final Logger log = Logger.getLogger(WSATTestCase.class);

    public static final String DEPLOYMENT_NAME = "wsat";

    @Inject
    @ClientStub
    private RestaurantServiceAT client;

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addPackages(true, "org.jboss.as.test.xts.simple.wsat")
                .addClass(TestSuiteEnvironment.class)
                .addAsResource("context-handlers.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Before
    public void clientReset() throws MalformedURLException {
        client.reset();
    }

    /**
     * Test the simple scenario where a booking is made and then committed.
     *
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testCommit() throws Exception {
        log.info("[CLIENT] Creating a new WS-AT User Transaction");
        UserTransaction ut = UserTransactionFactory.userTransaction();
        try {
            log.info("[CLIENT] Beginning Atomic Transaction (All calls to Web services that support WS-AT wil be included in this transaction)");
            ut.begin();
            log.info("[CLIENT] invoking makeBooking() on WS");
            client.makeBooking();
            log.info("[CLIENT] committing Atomic Transaction (This will cause the AT to complete successfully)");
            ut.commit();

            // Check the booking is visible after the transaction has committed.
            Assert.assertEquals("Commit on the Durable2PC participant has not been invoked.", 1, client.getBookingCount());
            Assert.assertTrue("Commit on the Volatile2PC participant has not been invoked.", client.wasVolatileCommit());
            Assert.assertFalse("Rollback on the Volatile2PC participant has been invoked.", client.wasVolatileRollback());
        } finally {
            rollbackIfActive(ut);
        }
    }

    /**
     * Tests the scenario where a booking is made and the transaction is later rolledback.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testRollback() throws Exception {
        log.info("[CLIENT] Creating a new WS-AT User Transaction");
        UserTransaction ut = UserTransactionFactory.userTransaction();
        try {
            log.info("[CLIENT] Beginning Atomic Transaction (All calls to Web services that support WS-AT wil be included in this transaction)");
            ut.begin();
            log.info("[CLIENT] invoking makeBooking() on WS");
            client.makeBooking();
            log.info("[CLIENT] rolling back Atomic Transaction (This will cause the AT and thus the enlisted back-end resources to rollback)");
            ut.rollback();

            // Check the booking is visible after the transaction has committed.
            Assert.assertEquals("Rollback on the Durable2PC participant has not been invoked.", 0, client.getBookingCount());
            Assert.assertFalse("Commit on the Volatile2PC participant has been invoked.", client.wasVolatileCommit());
            Assert.assertTrue("Rollback on the Volatile2PC participant has not been invoked.", client.wasVolatileRollback());
        } finally {
            rollbackIfActive(ut);
        }
    }

    /**
     * Utility method for rolling back a transaction if it is currently active.
     *
     * @param ut The User Business Activity to cancel.
     */
    private void rollbackIfActive(UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }
}
