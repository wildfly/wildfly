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

    private static final String SECONDARY_ADDRESS = System.getProperty("jboss.test.host.secondary.address", "127.0.0.1");
    private static final String SECONDARY_HOST_NAME = "secondary";
    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);
    private static final String TARGET_PROTOCOL = "pbcast.STABLE";

    @Test
    public void testOrderedChildResources() throws Exception {
        String testConfiguration = this.getClass().getSimpleName();
        final WildFlyManagedConfiguration primaryConfig = createConfiguration("domain.xml", "host-primary.xml", testConfiguration);
        final WildFlyManagedConfiguration secondaryConfig = createConfiguration("domain.xml", "host-secondary.xml", testConfiguration, SECONDARY_HOST_NAME, SECONDARY_ADDRESS, 19990);
        try (DomainLifecycleUtil primaryUtil = new DomainLifecycleUtil(primaryConfig);
                DomainLifecycleUtil secondaryUtil = new DomainLifecycleUtil(secondaryConfig)) {
            primaryUtil.start();
            secondaryUtil.start();

            PathAddress stackAddress = PathAddress.pathAddress(PROFILE, "full-ha")
                    .append(SUBSYSTEM, "jgroups")
                    .append("stack", "tcp");

            final ModelNode originalPrimaryStack = readResource(primaryUtil.getDomainClient(), stackAddress);
            final ModelNode originalSecondaryStack = readResource(secondaryUtil.getDomainClient(), stackAddress);
            Assert.assertEquals(originalPrimaryStack, originalSecondaryStack);

            int index = -1;
            Iterator<Property> it = originalPrimaryStack.get(PROTOCOL).asPropertyList().iterator();
            for (int i = 0; it.hasNext(); i++) {
                Property property = it.next();
                if (property.getName().equals(TARGET_PROTOCOL)) {
                    index = i;
                    break;
                }
            }

            //Make sure that we found the protocol and that it is not at the end
            Assert.assertTrue(0 <= index);
            Assert.assertTrue(index < originalPrimaryStack.get(PROTOCOL).keys().size() - 2);

            PathAddress targetProtocolAddress = stackAddress.append(PROTOCOL, TARGET_PROTOCOL);
            //Remove the protocol
            DomainTestUtils.executeForResult(Util.createRemoveOperation(targetProtocolAddress),
                    primaryUtil.getDomainClient());

            //Reload the primary into admin-only and re-add the protocol
            reloadPrimary(primaryUtil, true);
            ModelNode add = Util.createAddOperation(targetProtocolAddress, index);
            DomainTestUtils.executeForResult(add, primaryUtil.getDomainClient());

            //Reload the primary into normal mode and check the protocol is in the right place on the secondary
            reloadPrimary(primaryUtil, false);
            ModelNode secondaryStack = readResource(secondaryUtil.getDomainClient(), stackAddress);
            Assert.assertEquals(originalPrimaryStack, secondaryStack);

            //Check that :read-operation-description has add-index defined; WFLY-6782
            ModelNode rodOp = Util.createOperation(READ_OPERATION_DESCRIPTION_OPERATION, targetProtocolAddress);
            rodOp.get(NAME).set(ADD);
            ModelNode result = DomainTestUtils.executeForResult(rodOp, primaryUtil.getDomainClient());
            Assert.assertTrue(result.get(REQUEST_PROPERTIES).hasDefined(ADD_INDEX));
        }
    }

    private static ModelNode readResource(DomainClient client, PathAddress pathAddress) throws IOException, MgmtOperationException {
        ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, pathAddress);
        ModelNode result = DomainTestUtils.executeForResult(rr, client);
        result.protect();
        return result;
    }

    private static void reloadPrimary(DomainLifecycleUtil primaryUtil, boolean adminOnly) throws Exception{
        ModelNode restartAdminOnly = Util.createEmptyOperation(RELOAD, PathAddress.pathAddress(HOST, PRIMARY_HOST_NAME));
        restartAdminOnly.get(ADMIN_ONLY).set(adminOnly);
        primaryUtil.executeAwaitConnectionClosed(restartAdminOnly);
        primaryUtil.connect();
        primaryUtil.awaitHostController(System.currentTimeMillis());

        if (!adminOnly) {
            //Wait for the secondary to reconnect, look for the secondary in the list of hosts
            long end = System.currentTimeMillis() + 20 * ADJUSTED_SECOND;
            boolean reconnected = false;
            do {
                Thread.sleep(ADJUSTED_SECOND);
                reconnected = checkSecondaryReconnected(primaryUtil.getDomainClient());
            } while (!reconnected && System.currentTimeMillis() < end);
        }
    }

    private static boolean checkSecondaryReconnected(DomainClient primaryClient) throws Exception {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(CHILD_TYPE).set(HOST);
        try {
            ModelNode ret = DomainTestUtils.executeForResult(op, primaryClient);
            List<ModelNode> list = ret.asList();
            if (list.size() == 2) {
                for (ModelNode entry : list) {
                    if (SECONDARY_HOST_NAME.equals(entry.asString())){
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}
