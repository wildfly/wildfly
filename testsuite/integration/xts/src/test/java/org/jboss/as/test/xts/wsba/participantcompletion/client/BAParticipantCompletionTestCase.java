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

package org.jboss.as.test.xts.wsba.participantcompletion.client;

import javax.inject.Inject;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.EventLogEvent;
import org.jboss.as.test.xts.wsba.participantcompletion.service.BAParticipantCompletion;
import org.jboss.as.test.xts.wsba.participantcompletion.service.BAParticipantCompletionService1;
import org.jboss.as.test.xts.wsba.participantcompletion.service.BAParticipantCompletionService2;
import org.jboss.as.test.xts.wsba.participantcompletion.service.BAParticipantCompletionService3;
import org.jboss.jbossts.xts.bytemanSupport.BMScript;
import org.jboss.jbossts.xts.bytemanSupport.participantCompletion.ParticipantCompletionCoordinatorRules;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.xts.util.ServiceCommand.*;
import static org.jboss.as.test.xts.util.EventLogEvent.*;

/**
 * XTS business activities - participant completition test case
 */
@RunWith(Arquillian.class)
public class BAParticipantCompletionTestCase extends BaseFunctionalTest {
    UserBusinessActivity uba;
    BAParticipantCompletion client1, client2, client3;

    @Inject
    private EventLog eventLog;

    public static final String ARCHIVE_NAME = "wsba-participantcompletition-test";

    @Deployment
    public static Archive<?> createTestArchive() {
        return DeploymentHelper.getInstance().getWebArchive(ARCHIVE_NAME)
                .addPackage(BAParticipantCompletion.class.getPackage())
                .addPackage(BAParticipantCompletionClient.class.getPackage())
                .addPackage(EventLog.class.getPackage())
                .addPackage(BaseFunctionalTest.class.getPackage())
                .addClass(ParticipantCompletionCoordinatorRules.class)
                .addAsResource("context-handlers.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @BeforeClass()
    public static void submitBytemanScript() throws Exception {
        BMScript.submit(ParticipantCompletionCoordinatorRules.RESOURCE_PATH);
    }

    @AfterClass()
    public static void removeBytemanScript() {
        BMScript.remove(ParticipantCompletionCoordinatorRules.RESOURCE_PATH);
    }

    @Before
    public void setupTest() throws Exception {
        uba = UserBusinessActivityFactory.userBusinessActivity();
        client1 = BAParticipantCompletionClient.newInstance("BAParticipantCompletionService1");
        client2 = BAParticipantCompletionClient.newInstance("BAParticipantCompletionService2");
        client3 = BAParticipantCompletionClient.newInstance("BAParticipantCompletionService3");
    }

    protected EventLog getEventLog() {
        return eventLog;
    }

    @After
    public void teardownTest() throws Exception {
        getEventLog().clear();
        cancelIfActive(uba);
    }

    @Test
    public void testWSBAParticipantCompleteSingle() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(1);

        uba.begin();
        client1.saveData(DO_COMPLETE);
        uba.close();

        assertEventLogClient1(CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBAParticipantCompleteSimple() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        uba.begin();
        client1.saveData(DO_COMPLETE);
        client2.saveData(DO_COMPLETE);
        client3.saveData(DO_COMPLETE);
        uba.close();

        assertEventLogClient1(CONFIRM_COMPLETED, CLOSE);
        assertEventLogClient2(CONFIRM_COMPLETED, CLOSE);
        assertEventLogClient3(CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBAParticipantDoNotComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        try {
            uba.begin();
            client1.saveData(DO_COMPLETE);
            client2.saveData(DO_COMPLETE);
            client3.saveData();  // this participant does not inform about correct completition
            uba.close();

            Assert.fail("Exception should have been thrown by now");
        } catch (TransactionRolledBackException e) {
            // we expect this :)
        }

        assertEventLogClient1(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient2(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient3(CANCEL);
    }

    @Test
    public void testWSBAParticipantClientCancelNotComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        uba.begin();
        client1.saveData(DO_COMPLETE);
        client2.saveData();
        client3.saveData(DO_COMPLETE);
        uba.cancel();

        assertEventLogClient1(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient2(CANCEL);
        assertEventLogClient3(CONFIRM_COMPLETED, COMPENSATE);
    }

    @Test
    public void testWSBAParticipantCannotComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        try {
            uba.begin();
            client1.saveData(DO_COMPLETE);
            client2.saveData(CANNOT_COMPLETE);
            client3.saveData(DO_COMPLETE);

            Assert.fail("Exception should have been thrown by now");
        } catch (javax.xml.ws.soap.SOAPFaultException sfe) {
            // Exception is expected - enlisting participant #3 can't be done
        }

        try {
            uba.close();
        } catch (TransactionRolledBackException e) {
            // Exception is expected - rollback on close because of cannotComplete
        }

        assertEventLogClient1(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient2();
        assertEventLogClient3();
    }

    @Test
    public void testWSBAParticipantClientCancel() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        uba.begin();
        client1.saveData(DO_COMPLETE);
        client2.saveData(DO_COMPLETE);
        client3.saveData(DO_COMPLETE);
        uba.cancel();

        assertEventLogClient1(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient2(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient3(CONFIRM_COMPLETED, COMPENSATE);
    }

    @Test
    public void testWSBAParticipantApplicationException() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        try {
            uba.begin();
            client1.saveData(DO_COMPLETE);
            client2.saveData(APPLICATION_EXCEPTION);

            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            // Exception is expected
        }

        try {
            client3.saveData(DO_COMPLETE);
            uba.close();
            Assert.fail("Exception should have been thrown by now");
        } catch (com.arjuna.wst.TransactionRolledBackException tre) {
            // Exception is expected to be throws from close() method
        }

        assertEventLogClient1(CONFIRM_COMPLETED, COMPENSATE);
        assertEventLogClient2(CANCEL);
        assertEventLogClient3(CONFIRM_COMPLETED, COMPENSATE);
    }

    // --- assert methods
    // --- they take event log names from the service called by particular client
    private void assertEventLogClient1(EventLogEvent... expectedOrder) {
        assertEventLog(BAParticipantCompletionService1.SERVICE_EVENTLOG_NAME, expectedOrder);
    }

    private void assertEventLogClient2(EventLogEvent... expectedOrder) {
        assertEventLog(BAParticipantCompletionService2.SERVICE_EVENTLOG_NAME, expectedOrder);
    }

    private void assertEventLogClient3(EventLogEvent... expectedOrder) {
        assertEventLog(BAParticipantCompletionService3.SERVICE_EVENTLOG_NAME, expectedOrder);
    }
}
