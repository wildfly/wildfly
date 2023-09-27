/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.listener;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.undertow.util.FileUtils;

@RunWith(Arquillian.class)
@ServerSetup(UnescapedURITestCase.Setup.class)
@RunAsClient
public class UnescapedURITestCase {

    @ArquillianResource
    private URI uri;

    private static final int PORT = 7645;
    private static final String NEWBINDING = "newbinding";

    static class Setup extends SnapshotRestoreSetupTask {


        @Override
        public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode node = Operations.createAddOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=" + NEWBINDING).toModelNode());
            node.get("port").set(PORT);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), node);

            node = Operations.createAddOperation(PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/http-listener=newlistener").toModelNode());
            node.get("socket-binding").set(NEWBINDING);
            node.get("allow-unescaped-characters-in-url").set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), node);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "enctest.war")
                .addClass(EchoServlet.class);
    }

    @Test
    public void testForUnescapedCharacterInURLisRejected() throws Exception {
        String res = getResult(uri.getPort());
        Assert.assertTrue(res, res.startsWith("HTTP/1.1 400"));
        Assert.assertFalse(res, res.contains("ECHO")); //we should not have hit the servlet
    }


    @Test
    public void testForUnescapedCharacterInURLisAccepted() throws Exception {
        String res = getResult(PORT);
        Assert.assertTrue(res, res.startsWith("HTTP/1.1 200"));
        Assert.assertTrue(res, res.contains("ECHO:/한 글"));
    }

    String getResult(int port) throws Exception {

        try (Socket socket = new Socket(uri.getHost(), port)) {
            socket.getOutputStream().write("GET /enctest/enc/한%20글 HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            return FileUtils.readFile(socket.getInputStream());
        }
    }

}
