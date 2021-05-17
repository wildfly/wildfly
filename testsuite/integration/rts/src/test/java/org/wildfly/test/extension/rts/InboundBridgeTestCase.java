/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.extension.rts;

import javax.json.JsonArray;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.jbossts.star.util.TxStatusMediaType;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.extension.rts.common.InboundBridgeResource;
import org.wildfly.test.extension.rts.common.LoggingRestATResource;
import org.wildfly.test.extension.rts.common.LoggingXAResource;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
public final class InboundBridgeTestCase extends AbstractTestCase {
    private static final String DEPENDENCIES = "Dependencies: org.jboss.narayana.rts, org.jboss.jts\n";
    private InboundBridgeUtilities utils;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive getDeployment() {
        return AbstractTestCase.getDeployment()
                .addClasses(InboundBridgeResource.class, LoggingXAResource.class, LoggingRestATResource.class)
                .addAsWebInfResource(InboundBridgeTestCase.class.getClassLoader().getResource("web.xml"),"web.xml")
                .addAsManifestResource(new StringAsset(DEPENDENCIES), "MANIFEST.MF");
    }

    @Before
    public void before() {
        super.before();
        utils = new InboundBridgeUtilities(txSupport,
                getDeploymentUrl() + InboundBridgeResource.URL_SEGMENT, // inboundBridgeResourceUrl
                getDeploymentUrl() + LoggingRestATResource.BASE_URL_SEGMENT, // loggingRestATParticipantUrl
                getDeploymentUrl() + LoggingRestATResource.BASE_URL_SEGMENT + "/" + LoggingRestATResource.INVOCATIONS_URL_SEGMENT); // loggingRestATParticipantInvocationsUrl
        utils.resetInvocations();
    }

    @Test
    public void testCommit() throws Exception {
        txSupport.startTx();
        utils.enlistInboundBridgeResource();
        txSupport.commitTx();

        final JsonArray jsonArray = utils.getInboundBridgeResourceInvocations();

        utils.assertJsonArray(jsonArray, "LoggingXAResource.start", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.end", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.prepare", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.commit", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.rollback", 0);
    }

    @Test
    public void testRollback() throws Exception {
        txSupport.startTx();
        utils.enlistInboundBridgeResource();
        txSupport.rollbackTx();

        JsonArray jsonArray = utils.getInboundBridgeResourceInvocations();

        utils.assertJsonArray(jsonArray, "LoggingXAResource.start", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.end", 1);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.prepare", 0);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.commit", 0);
        utils.assertJsonArray(jsonArray, "LoggingXAResource.rollback", 1);
    }

    @Test
    public void testCommitWithTwoParticipants() throws Exception {
        txSupport.startTx();
        utils.enlistLoggingRestATParticipant();
        utils.enlistInboundBridgeResource();
        txSupport.commitTx();

        JsonArray participantResourceInvocations = utils.getLoggingRestATParticipantInvocations();
        JsonArray xaResourceInvocations = utils.getInboundBridgeResourceInvocations();

        Assert.assertEquals(2, participantResourceInvocations.size());
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_PREPARED + ")",
                participantResourceInvocations.getString(0));
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_COMMITTED + ")",
                participantResourceInvocations.getString(1));

        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.start", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.end", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.prepare", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.commit", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.rollback", 0);
    }

    @Test
    public void testRollbackWithTwoParticipants() throws Exception {
        txSupport.startTx();
        utils.enlistLoggingRestATParticipant();
        utils.enlistInboundBridgeResource();
        txSupport.rollbackTx();

        JsonArray participantResourceInvocations = utils.getLoggingRestATParticipantInvocations();
        JsonArray xaResourceInvocations = utils.getInboundBridgeResourceInvocations();

        Assert.assertEquals(1, participantResourceInvocations.size());
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_ROLLEDBACK + ")",
                participantResourceInvocations.getString(0));

        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.start", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.end", 1);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.prepare", 0);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.commit", 0);
        utils.assertJsonArray(xaResourceInvocations, "LoggingXAResource.rollback", 1);
    }

    protected String getBaseUrl() {
        return managementClient.getWebUri().toString();
    }
}