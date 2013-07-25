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
package org.jboss.as.test.integration.mod_cluster;


import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests that adding mod_cluster subsystem works.
 *
 * @author Radoslav Husar
 * @version March 2013
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ModClusterSubsystemAddTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    @InSequence(1)
    public void testModClusterAdd() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        final ModelControllerClient controllerClient = managementClient.getControllerClient();

        try {
            ctx.connectController();

            // Add the mod_cluster extension first (not in this profile by default)
            ModelNode request = ctx.buildRequest("/extension=org.jboss.as.modcluster:add");
            ModelNode response = controllerClient.execute(request);
            String outcome = response.get("outcome").asString();
            Assert.assertEquals("Adding mod_cluster extension failed! " + request.toJSONString(false), "success", outcome);

            // Now lets execute subsystem add operation but we need to specify a connector
            ctx.getBatchManager().activateNewBatch();
            Batch b = ctx.getBatchManager().getActiveBatch();
            b.add(ctx.toBatchedCommand("/socket-binding-group=standard-sockets/socket-binding=modcluster:add(multicast-port=23364, multicast-address=224.0.1.105)"));
            b.add(ctx.toBatchedCommand("/subsystem=modcluster:add"));
            b.add(ctx.toBatchedCommand("/subsystem=modcluster/mod-cluster-config=configuration:add(connector=http,advertise-socket=modcluster)"));
            request = b.toRequest();
            b.clear();
            ctx.getBatchManager().discardActiveBatch();

            response = controllerClient.execute(request);
            outcome = response.get("outcome").asString();
            Assert.assertEquals("Adding mod_cluster subsystem failed! " + request.toJSONString(false), "success", outcome);
        } finally {
            ctx.terminateSession();
        }
    }

    @Test
    @InSequence(2)
    public void testModClusterRemove() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        final ModelControllerClient controllerClient = managementClient.getControllerClient();

        try {
            ctx.connectController();

            // Test subsystem remove
            ModelNode request = ctx.buildRequest("/subsystem=modcluster:remove");
            ModelNode response = controllerClient.execute(request);
            String outcome = response.get("outcome").asString();
            Assert.assertEquals("Removing mod_cluster subsystem failed! " + request.toJSONString(false), "success", outcome);

            // Cleanup socket binding
            request = ctx.buildRequest("/socket-binding-group=standard-sockets/socket-binding=modcluster:remove");
            response = controllerClient.execute(request);
            outcome = response.get("outcome").asString();
            Assert.assertEquals("Removing socket binding failed! " + request.toJSONString(false), "success", outcome);

            // Cleanup and remove the extension
            request = ctx.buildRequest("/extension=org.jboss.as.modcluster:remove");
            response = controllerClient.execute(request);
            outcome = response.get("outcome").asString();
            Assert.assertEquals("Removing mod_cluster extension failed! " + request.toJSONString(false), "success", outcome);
        } finally {
            ctx.terminateSession();
        }
    }
}

