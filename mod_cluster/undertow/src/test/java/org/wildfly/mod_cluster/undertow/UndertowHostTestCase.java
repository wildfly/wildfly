/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.junit.Test;

public class UndertowHostTestCase {
    private final String hostName = "host";
    private final String alias = "alias";
    private final TestUndertowService service = new TestUndertowService("default-container", "default-server", "default-virtual-host", "instance-id", false);
    private final TestServer server = new TestServer("serverName", "defaultHost", this.service);
    private final TestHost undertowHost = new TestHost(this.hostName, Collections.singletonList(this.alias), this.server);
    private final Engine engine = mock(Engine.class);
    private final Host host = new UndertowHost(this.undertowHost, this.engine);

    @Test
    public void getName() {
        assertSame(this.hostName, this.host.getName());
    }

    @Test
    public void getAliases() {
        Set<String> result = this.host.getAliases();

        assertTrue(result.toString(), result.contains(this.hostName));
        assertTrue(result.toString(), result.contains(this.alias));
    }

    @Test
    public void getEngine() {
        assertSame(this.engine, this.host.getEngine());
    }

    @Test
    public void getContexts() {
        Deployment deployment = mock(Deployment.class);
        DeploymentInfo info = new DeploymentInfo();
        String expectedPath = "";
        info.setContextPath(expectedPath);
        HttpHandler handler = mock(HttpHandler.class);

        when(deployment.getDeploymentInfo()).thenReturn(info);

        this.undertowHost.registerDeployment(deployment, handler);

        Iterator<Context> result = this.host.getContexts().iterator();

        assertTrue(result.hasNext());
        Context context = result.next();
        assertSame(this.host, context.getHost());
        assertSame(expectedPath, context.getPath());
        assertFalse(result.hasNext());
    }

    @Test
    public void findContext() {
        Deployment deployment = mock(Deployment.class);
        DeploymentInfo info = new DeploymentInfo();
        String expectedPath = "";
        info.setContextPath(expectedPath);
        HttpHandler handler = mock(HttpHandler.class);

        when(deployment.getDeploymentInfo()).thenReturn(info);

        this.undertowHost.registerDeployment(deployment, handler);

        Context result = this.host.findContext(expectedPath);

        assertSame(this.host, result.getHost());
        assertSame(expectedPath, result.getPath());

        result = this.host.findContext("unknown");

        assertNull(result);
    }
}
