/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;


/**
 * Checks that the child resources that should be ordered are in fact so on a secondary reconnect.
 * At the moment this is only jgroups protocols. Although we have good tests for the indexed adds
 * working on reconnect in core, this is here as a sanity that no special describe handler is used
 * overriding the default mechanism.
 *
 * @author Kabir Khan
 */
public class OrderedChildResourcesTestCase extends BuildConfigurationTestBase {


    public static final String secondaryAddress = System.getProperty("jboss.test.host.secondary.address", "127.0.0.1");

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    @Test
    public void testOrderedChildResources() throws Exception {
        final WildFlyManagedConfiguration masterConfig = createConfiguration("domain.xml", "host-master.xml", getClass().getSimpleName());
        final DomainLifecycleUtil masterUtils = new DomainLifecycleUtil(masterConfig);
        final WildFlyManagedConfiguration secondaryConfig = createConfiguration("domain.xml", "host-secondary.xml", getClass().getSimpleName(),
                "secondary", secondaryAddress, 19990);
        final DomainLifecycleUtil secondaryUtils = new DomainLifecycleUtil(secondaryConfig);
        try {
            masterUtils.start();
            secondaryUtils.start();

            PathAddress jgroupsTcpAddr = PathAddress.pathAddress(PROFILE, "full-ha")
                    .append(SUBSYSTEM, "jgroups")
                    .append("stack", "tcp");

            final ModelNode originalPrimaryStack = readResource(masterUtils.getDomainClient(), jgroupsTcpAddr);
            originalPrimaryStack.protect();
            final ModelNode originalSecondaryStack = readResource(secondaryUtils.getDomainClient(), jgroupsTcpAddr);
            originalSecondaryStack.protect();
            Assert.assertEquals(originalPrimaryStack, originalSecondaryStack);

            //FD_ALL is normally in the middle somewhere
            final String protocolName = "FD_ALL";
            int index = -1;
            ModelNode value = null;
            Iterator<Property> it = originalPrimaryStack.get(PROTOCOL).asPropertyList().iterator();
            for (int i = 0; it.hasNext(); i++) {
                Property property = it.next();
                if (property.getName().equals(protocolName)) {
                    value = property.getValue();
                    index = i;
                    break;
                }
            }

            //Make sure that we found the protocol and that it is not at the end
            Assert.assertTrue(0 <= index);
            Assert.assertTrue(index < originalPrimaryStack.get(PROTOCOL).keys().size() - 2);

            //Remove the protocol
            DomainTestUtils.executeForResult(Util.createRemoveOperation(jgroupsTcpAddr.append(PROTOCOL, protocolName)),
                    masterUtils.getDomainClient());

            //Reload the master into admin-only and re-add the protocol
            reloadPrimary(masterUtils, true);
            ModelNode add = value.clone();
            add.get(OP).set(ADD);
            add.get(OP_ADDR).set(jgroupsTcpAddr.append(PROTOCOL, protocolName).toModelNode());
            add.get(ADD_INDEX).set(index);
            DomainTestUtils.executeForResult(add, masterUtils.getDomainClient());

            //Reload the master into normal mode and check the protocol is in the right place on the secondary
            reloadPrimary(masterUtils, false);
            ModelNode secondaryStack = readResource(secondaryUtils.getDomainClient(), jgroupsTcpAddr);
            Assert.assertEquals(originalPrimaryStack, secondaryStack);

            //Check that :read-operation-description has add-index defined; WFLY-6782
            ModelNode rodOp = Util.createOperation(READ_OPERATION_DESCRIPTION_OPERATION, jgroupsTcpAddr.append(PROTOCOL, protocolName));
            rodOp.get(NAME).set(ADD);
            ModelNode result = DomainTestUtils.executeForResult(rodOp, masterUtils.getDomainClient());
            Assert.assertTrue(result.get(REQUEST_PROPERTIES).hasDefined(ADD_INDEX));
        } finally {
            try {
                secondaryUtils.stop();
            } finally {
                masterUtils.stop();
            }
        }
    }

    private ModelNode readResource(DomainClient client, PathAddress pathAddress) throws IOException, MgmtOperationException {
        ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, pathAddress);
        return DomainTestUtils.executeForResult(rr, client);
    }

    private void reloadPrimary(DomainLifecycleUtil domainPrimaryLifecycleUtil, boolean adminOnly) throws Exception{
        ModelNode restartAdminOnly = Util.createEmptyOperation("reload", PathAddress.pathAddress(HOST, "master"));
        restartAdminOnly.get("admin-only").set(adminOnly);
        domainPrimaryLifecycleUtil.executeAwaitConnectionClosed(restartAdminOnly);
        domainPrimaryLifecycleUtil.connect();
        domainPrimaryLifecycleUtil.awaitHostController(System.currentTimeMillis());

        if (!adminOnly) {
            //Wait for the secondary to reconnect, look for the secondary in the list of hosts
            long end = System.currentTimeMillis() + 20 * ADJUSTED_SECOND;
            boolean secondaryReconnected = false;
            do {
                Thread.sleep(1 * ADJUSTED_SECOND);
                secondaryReconnected = checkSecondaryReconnected(domainPrimaryLifecycleUtil.getDomainClient());
            } while (!secondaryReconnected && System.currentTimeMillis() < end);

        }
    }

    private boolean checkSecondaryReconnected(DomainClient masterClient) throws Exception {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(CHILD_TYPE).set(HOST);
        try {
            ModelNode ret = DomainTestUtils.executeForResult(op, masterClient);
            List<ModelNode> list = ret.asList();
            if (list.size() == 2) {
                for (ModelNode entry : list) {
                    if ("secondary".equals(entry.asString())){
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}
