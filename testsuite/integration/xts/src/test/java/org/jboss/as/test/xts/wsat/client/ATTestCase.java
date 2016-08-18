/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.wsat.client;

import javax.inject.Inject;
import javax.xml.ws.soap.SOAPFaultException;


import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.test.xts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.EventLogEvent;
import org.jboss.as.test.xts.wsat.service.AT;
import org.jboss.as.test.xts.wsat.service.ATService1;
import org.jboss.as.test.xts.wsat.service.ATService2;
import org.jboss.as.test.xts.wsat.service.ATService3;

import static org.jboss.as.test.xts.util.ServiceCommand.*;
import static org.jboss.as.test.xts.util.EventLogEvent.*;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XTS atomic transaction test case
 */
@RunWith(Arquillian.class)
public class ATTestCase extends BaseFunctionalTest {

    private UserTransaction ut;
    private AT client1, client2, client3;

    public static final String ARCHIVE_NAME = "wsat-test";

    @Inject
    EventLog eventLog;

    @Deployment
    public static WebArchive createTestArchive() {
        final WebArchive archive = DeploymentHelper.getInstance().getWebArchiveWithPermissions(ARCHIVE_NAME)
                .addPackage(AT.class.getPackage())
                .addPackage(ATClient.class.getPackage())
                .addPackage(EventLog.class.getPackage())
                .addPackage(BaseFunctionalTest.class.getPackage())
                // needed to setup the server-side handler chain
                .addAsResource("context-handlers.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
        return archive;
    }

    @Before
    public void setupTest() throws Exception {
        ut = UserTransactionFactory.userTransaction();
        client1 = ATClient.newInstance("ATService1");
        client2 = ATClient.newInstance("ATService2");
        client3 = ATClient.newInstance("ATService3");
    }

    protected EventLog getEventLog() {
        return eventLog;
    }

    @After
    public void teardownTest() throws Exception {
        getEventLog().clear();
        rollbackIfActive(ut);
    }

    @Test
    public void testWSATSingleSimple() throws Exception {
        ut.begin();
        client1.invoke();
        ut.commit();

        assertEventLogClient1(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATSimple() throws Exception {
        ut.begin();
        client1.invoke();
        client2.invoke();
        client3.invoke();
        ut.commit();

        assertEventLogClient1(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertEventLogClient2(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertEventLogClient3(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATClientRollback() throws Exception {
        ut.begin();
        client1.invoke();
        client2.invoke();
        client3.invoke();
        ut.rollback();

        assertEventLogClient1(ROLLBACK, VOLATILE_ROLLBACK);
        assertEventLogClient2(ROLLBACK, VOLATILE_ROLLBACK);
        assertEventLogClient3(ROLLBACK, VOLATILE_ROLLBACK);
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollback() throws Exception {
        try {
            ut.begin();
            client1.invoke();
            client2.invoke(VOTE_ROLLBACK); // rollback voted on durable participant
            client3.invoke();
            ut.commit();
        } catch (TransactionRolledBackException e) {
            assertEventLogClient1(BEFORE_PREPARE, PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            assertEventLogClient2(BEFORE_PREPARE, PREPARE, VOLATILE_ROLLBACK);
            assertEventLogClient3(BEFORE_PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            throw e;
        }
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollbackPrePrepare() throws Exception {
        try {
            ut.begin();
            client1.invoke();
            client2.invoke(VOTE_ROLLBACK_PRE_PREPARE); // rollback voted on volatile participant
            client3.invoke();
            ut.commit();
        } catch (TransactionRolledBackException e) {
            assertEventLogClient1(BEFORE_PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            assertEventLogClient2(BEFORE_PREPARE, ROLLBACK);
            assertEventLogClient3(ROLLBACK, VOLATILE_ROLLBACK);
            throw e;
        }
    }

    @Test
    public void testWSATRollbackOnly() throws Exception {
        try {
            ut.begin();
            client1.invoke();
            client2.invoke(ROLLBACK_ONLY);
            client3.invoke(); // failing on enlisting next participant
            // ut.commit();
            Assert.fail("The " + SOAPFaultException.class.getName() + " is expected for RollbackOnly test");
        } catch (SOAPFaultException sfe) {
            assertEventLogClient1(ROLLBACK, VOLATILE_ROLLBACK);
            assertEventLogClient2(ROLLBACK, VOLATILE_ROLLBACK);
            assertEventLogClient3();
        }
    }

    @Test
    public void testWSATVoteReadOnly() throws Exception {
        ut.begin();
        client1.invoke(VOTE_READONLY_VOLATILE); // volatile for VOLATILE_COMMIT
        client2.invoke(VOTE_READONLY_DURABLE); // durable for COMMIT
        client3.invoke(VOTE_READONLY_DURABLE, VOTE_READONLY_VOLATILE);
        ut.commit();

        assertEventLogClient1(BEFORE_PREPARE, PREPARE, COMMIT);
        assertEventLogClient2(BEFORE_PREPARE, PREPARE, VOLATILE_COMMIT);
        assertEventLogClient3(BEFORE_PREPARE, PREPARE);
    }

    @Test
    public void testWSATApplicationException() throws Exception {
        try {
            ut.begin();
            client1.invoke();
            client2.invoke(APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            //Exception expected
        } finally {
            client3.invoke();
            ut.rollback();
        }

        assertEventLogClient1(ROLLBACK, VOLATILE_ROLLBACK);
        assertEventLogClient2(ROLLBACK, VOLATILE_ROLLBACK);
        assertEventLogClient3(ROLLBACK, VOLATILE_ROLLBACK);
    }

    @Test
    public void testWSATApplicationExceptionCommit() throws Exception {
        try {
            ut.begin();
            client1.invoke();
            client2.invoke(APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            //Exception expected
        } finally {
            client3.invoke();
            ut.commit();
        }

        assertEventLogClient1(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertEventLogClient2(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertEventLogClient3(BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }


    // --- assert methods
    // --- they take event log names from the service called by particular client
    private void assertEventLogClient1(EventLogEvent... expectedOrder) {
        assertEventLog(ATService1.LOG_NAME, expectedOrder);
    }

    private void assertEventLogClient2(EventLogEvent... expectedOrder) {
        assertEventLog(ATService2.LOG_NAME, expectedOrder);
    }

    private void assertEventLogClient3(EventLogEvent... expectedOrder) {
        assertEventLog(ATService3.LOG_NAME, expectedOrder);
    }
}
