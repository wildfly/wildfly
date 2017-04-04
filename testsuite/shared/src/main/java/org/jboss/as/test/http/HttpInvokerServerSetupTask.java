/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.http;

import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * @author Stuart Douglas
 */
public class HttpInvokerServerSetupTask implements ServerSetupTask {
    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/host=default-host/setting=http-invoker").toModelNode());
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get("http-authentication-factory").set("application-http-authentication");
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.fail("Could not execute op: '" + op + "', result: " + result);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/host=default-host/setting=http-invoker").toModelNode());
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.fail("Could not execute op: '" + op + "', result: " + result);
        }
    }
}
