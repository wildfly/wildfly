/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for cross-process wildcard reads in a mixed domain. See https://issues.jboss.org/browse/WFCORE-621.
 *
 * @author Brian Stansberry
 */
public class WildcardReadsTestCase {

    protected static final String DOMAIN_CONFIG = "master-config/domain-minimal.xml";

    private static final PathElement HOST_WILD = PathElement.pathElement(HOST);
    private static final PathElement HOST_SLAVE = PathElement.pathElement(HOST, "slave");
    private static final PathElement SERVER_WILD = PathElement.pathElement(RUNNING_SERVER);
    private static final PathElement INTERFACE_WILD = PathElement.pathElement(INTERFACE);
    private static final PathElement INTERFACE_PUBLIC = PathElement.pathElement(INTERFACE, "public");

    private static final Set<String> VALID_STATES = new HashSet<>(Arrays.asList("running", "stopped"));

    DomainTestSupport support;
    Version.AsVersion version;

    @Before
    public void init() throws Exception {
        support = MixedDomainTestSuite.getSupport(this.getClass());
        version = MixedDomainTestSuite.getVersion(this.getClass());
    }

    @AfterClass
    public synchronized static void afterClass() {
        MixedDomainTestSuite.afterClass();
    }

    @Test
    public void testAllHostsAllServersReadInterfaceResources() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD, INTERFACE_WILD));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 6, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertEquals(item.toString(), ModelType.EXPRESSION, item.get(RESULT, INET_ADDRESS).getType());
        }
        assertEquals(resp.toString(), 3, masterCount);
    }

    @Test
    public void testSlaveAllServersReadInterfaceResources() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD, INTERFACE_WILD));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 3, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertEquals(item.toString(), ModelType.EXPRESSION, item.get(RESULT, INET_ADDRESS).getType());
        }
        assertEquals(resp.toString(), 0, masterCount);
    }

    @Test
    public void testAllHostsAllServersReadRootResource() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD));
        op.get(INCLUDE_RUNTIME).set(true);
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 4 : 3, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), VALID_STATES.contains(item.get(RESULT, "server-state").asString().toLowerCase(Locale.ENGLISH)));
        }
        assertEquals(resp.toString(), 2, masterCount);
    }

    @Test
    public void testSlaveAllServersReadRootResource() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD));
        op.get(INCLUDE_RUNTIME).set(true);
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 2 : 1, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), VALID_STATES.contains(item.get(RESULT, "server-state").asString().toLowerCase(Locale.ENGLISH)));
        }
        assertEquals(resp.toString(), 0, masterCount);
    }

    @Test
    public void testAllHostsAllServersReadInterfaceAttribute() {
        ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD, INTERFACE_PUBLIC));
        op.get(NAME).set("inet-address");
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 2, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertEquals(item.toString(), ModelType.EXPRESSION, item.get(RESULT).getType());
        }
        assertEquals(resp.toString(), 1, masterCount);
    }

    @Test
    public void testSlaveAllServersReadInterfaceAttribute() {
        ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD, INTERFACE_PUBLIC));
        op.get(NAME).set("inet-address");
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 1, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertEquals(item.toString(), ModelType.EXPRESSION, item.get(RESULT).getType());
        }
        assertEquals(resp.toString(), 0, masterCount);

    }

    @Test
    public void testAllHostsAllServersReadRootAttribute() {
        ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD));
        op.get(NAME).set("server-state");
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 4 : 3, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), VALID_STATES.contains(item.get(RESULT).asString().toLowerCase(Locale.ENGLISH)));
        }
        assertEquals(resp.toString(), 2, masterCount);

    }

    @Test
    public void testSlaveAllServersReadRootAttribute() {
        ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD));
        op.get(NAME).set("server-state");
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 2 : 1, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), VALID_STATES.contains(item.get(RESULT).asString().toLowerCase(Locale.ENGLISH)));
        }
        assertEquals(resp.toString(), 0, masterCount);

    }

    @Test
    @Ignore("WFCORE-948")
    public void testAllHostsAllServersReadInterfaceDescription() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD, INTERFACE_PUBLIC));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 2, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertTrue(item.toString(), item.hasDefined(RESULT, ATTRIBUTES, INET_ADDRESS));
        }
        assertEquals(resp.toString(), 1, masterCount);

    }

    @Test
    @Ignore("WFCORE-948")
    public void testSlaveAllServersReadInterfaceDescription() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD, INTERFACE_PUBLIC));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), 1, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 3)) {
                masterCount++;
            }
            assertTrue(item.toString(), item.hasDefined(RESULT, ATTRIBUTES, INET_ADDRESS));
        }
        assertEquals(resp.toString(), 0, masterCount);

    }

    @Test
    @Ignore("WFCORE-948")
    public void testAllHostsAllServersReadRootDescription() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.pathAddress(HOST_WILD, SERVER_WILD));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 4 : 3, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), item.hasDefined(RESULT, ATTRIBUTES, "server-state"));
        }
        assertEquals(resp.toString(), 2, masterCount);

    }

    @Test
    @Ignore("WFCORE-948")
    public void testSlaveAllServersReadRootDescription() {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.pathAddress(HOST_SLAVE, SERVER_WILD));
        ModelNode resp = executeForResult(op);
        assertEquals(resp.toString(), expectUnstartedServerResource() ? 2 : 1, resp.asInt());
        int masterCount = 0;
        for (ModelNode item : resp.asList()) {
            if (isMasterItem(item, 2)) {
                masterCount++;
            }
            assertTrue(item.toString(), item.hasDefined(RESULT, ATTRIBUTES, "server-state"));
        }
        assertEquals(resp.toString(), 0, masterCount);

    }

    protected boolean expectUnstartedServerResource() {
        return true;
    }

    private ModelNode executeForResult(ModelNode op) {
        ModelNode result = support.getDomainMasterLifecycleUtil().executeForResult(op);
        assertEquals(result.toString(), ModelType.LIST, result.getType());
        return result;
    }

    private boolean isMasterItem(ModelNode item, int itemSize) {
        assertTrue(item.toString(), item.hasDefined(ADDRESS));
        PathAddress pa = PathAddress.pathAddress(item.get(ADDRESS));
        assertEquals(item.toString(), itemSize, pa.size());
        return pa.getElement(0).getValue().equals("master");
    }
}
