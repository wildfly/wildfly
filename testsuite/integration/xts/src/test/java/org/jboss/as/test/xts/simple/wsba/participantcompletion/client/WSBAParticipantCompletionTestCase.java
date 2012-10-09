/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.simple.wsba.participantcompletion.client;

import javax.inject.Inject;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.simple.wsba.participantcompletion.jaxws.SetServiceBA;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class WSBAParticipantCompletionTestCase {
    private static final Logger log = Logger.getLogger(WSBAParticipantCompletionTestCase.class);

    public static final String DEPLOYMENT_NAME = "wsba-participant-completion";

    @Inject
    @ClientStub
    public SetServiceBA client;

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addPackages(false, "org.jboss.as.test.xts.simple.wsba")
                .addPackages(true, "org.jboss.as.test.xts.simple.wsba.participantcompletion")
                .addClass(TestSuiteEnvironment.class)
                .addAsResource("context-handlers.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"),"MANIFEST.MF");
    }

    @Before
    public void clientReset() {
        client.clear();
    }

    /**
     * Test the simple scenario where an item is added to the set within a Business Activity which is closed successfully.
     *
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testSuccess() throws Exception {
        log.info("[CLIENT] Creating a new Business Activity");
        UserBusinessActivity uba = UserBusinessActivityFactory.userBusinessActivity();
        try {
            String value = "1";

            log.info("[CLIENT] Beginning Business Activity (All calls to Web services that support WS-BA wil be included in this activity)");
            uba.begin();

            log.info("[CLIENT] invoking addValueToSet(1) on WS");
            client.addValueToSet(value);

            /*
                AS7-5700

                When a WSBA participant employs the ParticipantCompletion protocol, it is the responsibility of the participant
                to notify the coordinator when it has completed its work. This notification is asynchronous.

                In the 'WSBAParticipantCompletionTestCase' test, the client invokes the participant's web service who notifies
                completion just before returning from the invocation. The client then sends a message to the coordinator requesting
                to close (complete) the activity.

                As the "complete" message (from the participant to coordinator) is asynchronous, we now have a race. If the
                Client's close message is processed by the coordinator before the participant's "complete" message, then the coordinator
                cancels the BA as not all participants have completed.
                This results in the client receiving a TransactionRolledBackException and the completed participant
                is (eventually) compensated. The outcome is atomic, but a BA that would have otherwise succeeded, is unsuccessful.

                In reality we only expect this scenario to happen in the rather artificial scenario where all parties (client,
                coordinator and participants) are on the same server. It also only seems to happen on very slow machines. Therefore,
                it's fine to fix the test to prevent this scenario from arising, rather than to somehow change the protocol
                (without breaking the WS-BA standard) to prevent it.

                We have two options, that I can see to fix the test:

                1) Byteman Rendezvous. Here we would introduce a dependency on Byteman and write a script that delays the client's
                    close message until all participants' 'complete' messages have been acknowledged by the coordinator. This is
                    probably an over-engineered solution as we would be introducing Byteman, to these tests, for this single case.

                2) We add a Thread.sleep(10000) to the test, just before the client sends the 'close' message to the coordinator.
                    This is what we did to the XTS tests in the JBossTS project as a stop-gap until we decided how to do it "properly".

                I suggest we go with 2) as it is the simplest solution. The extra time added to the test is just 10s as there is only
                one test affected by this. In the future, when we have more tests we should reduce this sleep period or consider
                using another solution (such as Byteman), in order to keep the test duration acceptable.
             */
            Thread.sleep(10000);

            log.info("[CLIENT] Closing Business Activity (This will cause the BA to complete successfully)");
            uba.close();

            Assert.assertTrue("Expected value to be in the set, but it wasn't", client.isInSet(value));
        } finally {
            cancelIfActive(uba);
        }
    }

    /**
     * Tests the scenario where an item is added to the set with in a business activity that is later cancelled. The test checks
     * that the item is in the set after invoking addValueToSet on the Web service. After cancelling the Business Activity, the
     * work should be compensated and thus the item should no longer be in the set.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testCancel() throws Exception {
        log.info("[CLIENT] Creating a new Business Activity");
        UserBusinessActivity uba = UserBusinessActivityFactory.userBusinessActivity();
        try {
            String value = "1";

            log.info("[CLIENT] Beginning Business Activity (All calls to Web services that support WS-BA will be included in this activity)");
            uba.begin();

            log.info("[CLIENT] invoking addValueToSet(1) on WS");
            client.addValueToSet(value);

            Assert.assertTrue("Expected value to be in the set, but it wasn't", client.isInSet(value));

            log.info("[CLIENT] Cancelling Business Activity (This will cause the work to be compensated)");
            uba.cancel();

            Assert.assertTrue("Expected value to not be in the set, but it was", !client.isInSet(value));

        } finally {
            cancelIfActive(uba);
        }
    }

    /**
     * Utility method for cancelling a Business Activity if it is currently active.
     *
     * @param uba The User Business Activity to cancel.
     */
    private void cancelIfActive(UserBusinessActivity uba) {
        try {
            uba.cancel();
        } catch (Throwable th2) {
            // do nothing, already closed
        }
    }
}
