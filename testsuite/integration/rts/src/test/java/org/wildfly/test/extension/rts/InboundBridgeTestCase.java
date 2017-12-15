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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.jbossts.star.util.TxLinkNames;
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

    private static final String DEPENDENCIES = "Dependencies: org.jboss.narayana.rts, org.jboss.jts, org.codehaus.jettison\n";

    private String inboundBridgeResourceUrl;

    private String loggingRestATParticipantUrl;

    private String loggingRestATParticipantInvocationsUrl;

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
        inboundBridgeResourceUrl = getDeploymentUrl() + InboundBridgeResource.URL_SEGMENT;
        loggingRestATParticipantUrl = getDeploymentUrl() + LoggingRestATResource.BASE_URL_SEGMENT;
        loggingRestATParticipantInvocationsUrl = loggingRestATParticipantUrl + "/" + LoggingRestATResource.INVOCATIONS_URL_SEGMENT;
        resetInvocations();
    }

    @Test
    public void testCommit() throws Exception {
        txSupport.startTx();
        enlistInboundBridgeResource();
        txSupport.commitTx();

        final JSONArray jsonArray = getInboundBridgeResourceInvocations();

        Assert.assertEquals(4, jsonArray.length());
        Assert.assertEquals("LoggingXAResource.start", jsonArray.get(0));
        Assert.assertEquals("LoggingXAResource.end", jsonArray.get(1));
        Assert.assertEquals("LoggingXAResource.prepare", jsonArray.get(2));
        Assert.assertEquals("LoggingXAResource.commit", jsonArray.get(3));
    }

    @Test
    public void testRollback() throws Exception {
        txSupport.startTx();
        enlistInboundBridgeResource();
        txSupport.rollbackTx();

        JSONArray jsonArray = getInboundBridgeResourceInvocations();

        Assert.assertEquals(3, jsonArray.length());
        Assert.assertEquals("LoggingXAResource.start", jsonArray.get(0));
        Assert.assertEquals("LoggingXAResource.end", jsonArray.get(1));
        Assert.assertEquals("LoggingXAResource.rollback", jsonArray.get(2));
    }

    @Test
    public void testCommitWithTwoParticipants() throws Exception {
        txSupport.startTx();
        enlistLoggingRestATParticipant();
        enlistInboundBridgeResource();
        txSupport.commitTx();

        JSONArray participantResourceInvocations = getLoggingRestATParticipantInvocations();
        JSONArray xaResourceInvocations = getInboundBridgeResourceInvocations();

        Assert.assertEquals(2, participantResourceInvocations.length());
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_PREPARED + ")",
                participantResourceInvocations.get(0));
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_COMMITTED + ")",
                participantResourceInvocations.get(1));

        Assert.assertEquals(4, xaResourceInvocations.length());
        Assert.assertEquals("LoggingXAResource.start", xaResourceInvocations.get(0));
        Assert.assertEquals("LoggingXAResource.end", xaResourceInvocations.get(1));
        Assert.assertEquals("LoggingXAResource.prepare", xaResourceInvocations.get(2));
        Assert.assertEquals("LoggingXAResource.commit", xaResourceInvocations.get(3));

    }

    @Test
    public void testRollbackWithTwoParticipants() throws Exception {
        txSupport.startTx();
        enlistLoggingRestATParticipant();
        enlistInboundBridgeResource();
        txSupport.rollbackTx();

        JSONArray participantResourceInvocations = getLoggingRestATParticipantInvocations();
        JSONArray xaResourceInvocations = getInboundBridgeResourceInvocations();

        Assert.assertEquals(1, participantResourceInvocations.length());
        Assert.assertEquals("LoggingRestATResource.terminateParticipant(" + TxStatusMediaType.TX_ROLLEDBACK + ")",
                participantResourceInvocations.get(0));

        Assert.assertEquals(3, xaResourceInvocations.length());
        Assert.assertEquals("LoggingXAResource.start", xaResourceInvocations.get(0));
        Assert.assertEquals("LoggingXAResource.end", xaResourceInvocations.get(1));
        Assert.assertEquals("LoggingXAResource.rollback", xaResourceInvocations.get(2));
    }

    protected String getBaseUrl() {
        return managementClient.getWebUri().toString();
    }

    private void enlistInboundBridgeResource() {
        final Link participantLink = Link.fromUri(txSupport.getTxnUri()).rel(TxLinkNames.PARTICIPANT).title(TxLinkNames.PARTICIPANT).build();
        final Response response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request()
                .header("link", participantLink).post(null);

        Assert.assertEquals(200, response.getStatus());
    }

    private void enlistLoggingRestATParticipant() {
        String linkHeader = txSupport.makeTwoPhaseAwareParticipantLinkHeader(loggingRestATParticipantUrl, false, null, null);
        String recoveryUrl = txSupport.enlistParticipant(txSupport.getTxnUri(), linkHeader);

        Assert.assertFalse(recoveryUrl == null);
    }

    private void resetInvocations() {
        Response response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request().put(null);
        Assert.assertEquals(200, response.getStatus());

        response = ClientBuilder.newClient().target(loggingRestATParticipantInvocationsUrl).request().put(null);
        Assert.assertEquals(200, response.getStatus());
    }

    private JSONArray getInboundBridgeResourceInvocations() throws Exception {
        final String response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request().get(String.class);
        final JSONArray jsonArray = new JSONArray(response);

        return jsonArray;
    }

    private JSONArray getLoggingRestATParticipantInvocations() throws Exception {
        String response = ClientBuilder.newClient().target(loggingRestATParticipantInvocationsUrl).request().get(String.class);
        JSONArray jsonArray = new JSONArray(response);

        return jsonArray;
    }

}