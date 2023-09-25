/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpSession;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.core.ApplicationListeners;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.listeners.HttpSessionListener;
import org.jboss.modcluster.container.listeners.ServletRequestListener;
import org.junit.Test;

public class UndertowContextTestCase {
    private final Deployment deployment = mock(Deployment.class);
    private final UndertowHost host = mock(UndertowHost.class);
    private final Context context = new UndertowContext(this.deployment, this.host);

    @Test
    public void getHost() {
        assertSame(this.host, this.context.getHost());
    }

    @Test
    public void getPath() {
        DeploymentInfo info = new DeploymentInfo();
        String expected = "";
        info.setContextPath(expected);

        when(this.deployment.getDeploymentInfo()).thenReturn(info);

        String result = this.context.getPath();

        assertSame(expected, result);
    }

    @Test
    public void isStarted() throws ServletException {
        ServletContext context = mock(ServletContext.class);
        ApplicationListeners listeners = new ApplicationListeners(Collections.emptyList(), context);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        assertFalse(this.context.isStarted());

        listeners.start();

        assertTrue(this.context.isStarted());

        listeners.stop();

        assertFalse(this.context.isStarted());
    }

    @Test
    public void addRequestListener() throws ServletException {
        ServletRequestListener listener = mock(ServletRequestListener.class);
        ServletContext context = mock(ServletContext.class);
        ServletRequest request = mock(ServletRequest.class);
        ApplicationListeners listeners = new ApplicationListeners(Collections.emptyList(), context);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        this.context.addRequestListener(listener);
        listeners.start();

        listeners.requestInitialized(request);

        verify(listener).requestInitialized();

        listeners.requestDestroyed(request);

        verify(listener).requestDestroyed();
    }

    @Test
    public void addSessionListener() throws ServletException {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ServletContext context = mock(ServletContext.class);
        HttpSession session = mock(HttpSession.class);
        ApplicationListeners listeners = new ApplicationListeners(Collections.emptyList(), context);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        this.context.addSessionListener(listener);
        listeners.start();

        listeners.sessionCreated(session);

        verify(listener).sessionCreated();

        listeners.sessionDestroyed(session);

        verify(listener).sessionDestroyed();
    }
}
