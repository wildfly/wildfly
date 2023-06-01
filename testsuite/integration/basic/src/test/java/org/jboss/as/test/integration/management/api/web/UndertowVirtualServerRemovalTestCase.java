/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.web;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.jboss.as.test.shared.ServerReload.getContainerRunningState;
import static org.jboss.as.test.shared.ServerReload.reloadIfRequired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.undertow.Constants.DEFAULT_HOST;
import static org.wildfly.extension.undertow.Constants.DEFAULT_SERVER;

/**
 * Tests on the virtual servers and hosts removal in undertow subsystem
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UndertowVirtualServerRemovalTestCase extends ContainerResourceMgmtTestBase {

    @Deployment(order=1)
    public static Archive<?> getDeployment() {
        final String name = UndertowVirtualServerRemovalTestCase.class.getSimpleName();
        WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.addAsWebResource(new StringAsset(name), "index.html");
        return war;
    }

    /**
     * Test to try removing default server multiple times, all operations will fail because default server cannot be removed.
     */
    @Test
    public void testRemoveDefaultVirtualServer() throws IOException, MgmtOperationException {
        // default hosts cannot be removed, even with multiple times
        final int times = 2;
        for (int i = 0; i < times; i ++) {
            ModelNode response = executeOperation(hostOperation(DEFAULT_SERVER, DEFAULT_HOST, "remove"), false);
            assertEquals(ModelDescriptionConstants.FAILED, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertNotNull(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION));
            assertTrue(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString().contains("WFLYCTL0367"));
        }
        // default servers cannot be removed, even with multiple times
        for (int i = 0; i < times; i ++) {
            ModelNode response = executeOperation(serverOperation(DEFAULT_SERVER, "remove"), false);
            assertEquals(ModelDescriptionConstants.FAILED, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertNotNull(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION));
            assertTrue(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString().contains("WFLYCTL0367"));
        }
    }


    @Test
    public void testRemoveNonDefaultHostsWithoutAllowResourceServiceRestartNoChild() throws Exception {
        testServerAndHostRemoval(false, false, false, true);
    }

    @Test
    public void testRemoveNonDefaultHostsWithAllowResourceServiceRestartNoChild() throws Exception {
        testServerAndHostRemoval(false, false, true, false);
    }

    @Test
    public void testRemoveNonDefaultHostsWithoutAllowResourceServiceRestartLocation() throws Exception {
        testServerAndHostRemoval(true, false, false, true);
    }

    @Test
    public void testRemoveNonDefaultHostsWithAllowResourceServiceRestartLocation() throws Exception {
        testServerAndHostRemoval(true, false, true, false);
    }

    @Test
    public void testRemoveNonDefaultHostsWithoutAllowResourceServiceRestartFilter() throws Exception {
        testServerAndHostRemoval(false, true, false, true);
    }

    @Test
    public void testRemoveNonDefaultHostsWithAllowResourceServiceRestartFilter() throws Exception {
        testServerAndHostRemoval(false, true, true, false);
    }

    @Test
    public void testRemoveNonDefaultHostsWithoutAllowResourceServiceRestart() throws Exception {
        testServerAndHostRemoval(true, true, false, true);
    }

    @Test
    public void testRemoveNonDefaultHostsWithAllowResourceServiceRestart() throws Exception {
        testServerAndHostRemoval(true, true, true, false);
    }

    private void testServerAndHostRemoval(boolean withLocation, boolean withFilter, boolean allowResourceServiceRestart, boolean expectReloadAfterHostRemoval) throws Exception {
        try {
            addServerAndHost(withLocation, withFilter);
            removeHost(allowResourceServiceRestart);
            final String state = expectReloadAfterHostRemoval ? "reload-required" : "running";
            assertEquals(state, getContainerRunningState(getManagementClient()));
            reloadIfRequired(getManagementClient());
        } finally {
            removeServer(allowResourceServiceRestart);
            assertEquals("reload-required", getContainerRunningState(getManagementClient()));
            reloadIfRequired(getManagementClient());
            if (withFilter) {
                executeOperation(Util.createRemoveOperation(filterPathAddress()));
                reloadIfRequired(getManagementClient());
            }
            if (withLocation) {
                executeOperation(Util.createRemoveOperation(welcomeHandlerPath()));
                reloadIfRequired(getManagementClient());
            }
        }
    }

    private void executeAllowResourceServiceRestartOperation(ModelNode operation, boolean allowResourceServiceRestart)
            throws IOException, MgmtOperationException {
        ModelNode op = operation.clone();
        if (allowResourceServiceRestart) {
            op.get(ModelDescriptionConstants.OPERATION_HEADERS)
                    .get(ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART)
                    .set(true);
        }
        executeOperation(op);
    }

    private void removeHost(boolean allowResourceServiceRestart) throws IOException, MgmtOperationException {
        ModelNode removeOp = hostOperation("abc", "abc-host", "remove");
        executeAllowResourceServiceRestartOperation(removeOp, allowResourceServiceRestart);
    }

    private void removeServer(boolean allowResourceServiceRestart) throws IOException, MgmtOperationException {
        ModelNode removeOp = serverOperation("abc", "remove");
        executeAllowResourceServiceRestartOperation(removeOp, allowResourceServiceRestart);
    }

    private void addServerAndHost(boolean withLocation, boolean withFilter) throws IOException, MgmtOperationException {
        executeOperation(serverOperation("abc", "add"));
        ModelNode addHostOp = hostOperation("abc", "abc-host", "add");
        addHostOp.get("alias").add("localhost");
        executeOperation(addHostOp);
        if (withLocation) {
            PathAddress welcomeHandlerPath = welcomeHandlerPath();
            ModelNode addHandlerOp = Util.createAddOperation(welcomeHandlerPath);
            addHandlerOp.get("path").set(".");
            executeOperation(addHandlerOp);

            PathAddress locationAddress = locationAddress("abc", "abc-host", "/abc");
            ModelNode addLocationOp = Util.createAddOperation(locationAddress);
            addLocationOp.get("handler").set("welcome");
            executeOperation(addLocationOp);
        }
        if (withFilter) {
            PathAddress filterPathAddress = filterPathAddress();
            ModelNode addFilterOp = Util.createAddOperation(filterPathAddress);
            addFilterOp.get("header-name").set("x-powered-by");
            addFilterOp.get("header-value").set("Undertow");
            executeOperation(addFilterOp);
            ModelNode addHostFilterRefOp = Util.createAddOperation(hostAddress("abc", "abc-host").append("filter-ref", "x-powered-by-header"));
            executeOperation(addHostFilterRefOp);
        }
    }

    private PathAddress welcomeHandlerPath() {
        return PathAddress.pathAddress("subsystem", "undertow")
                .append("configuration", "handler")
                .append("file", "welcome");
    }

    private PathAddress filterPathAddress() {
        return PathAddress.pathAddress("subsystem", "undertow")
                .append("configuration", "filter")
                .append("response-header", "x-powered-by-header");
    }

    private PathAddress serverAddress(String server) {
        return PathAddress.pathAddress("subsystem", "undertow").append("server", server);
    }

    private PathAddress hostAddress(String server, String host) {
        return serverAddress(server).append("host", host);
    }

    private PathAddress locationAddress(String server, String host, String location) {
        return hostAddress(server, host).append("location", location);
    }

    private ModelNode serverOperation(String server, String op) {
        return Util.createOperation(op, serverAddress(server));
    }

    private ModelNode hostOperation(String server, String host, String op) {
        return Util.createOperation(op, hostAddress(server, host));
    }

}
