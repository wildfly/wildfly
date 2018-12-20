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

package org.jboss.as.test.integration.ejb.management.deployments;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * @author Stuart Douglas, Ondrej Chaloupka
 * @author wangchao
 */
public class EjbInvocationStatisticsTestCaseSetup implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(EjbInvocationStatisticsTestCaseSetup.class);

    private static ModelNode getAddress() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("passivation-store", "infinispan");
        address.protect();
        return address;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("max-size");
        operation.get("value").set(1);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.trace("modelnode operation write-attribute max-size=1: " + result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("max-size");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        ServerReload.reloadIfRequired(managementClient);
    }
}