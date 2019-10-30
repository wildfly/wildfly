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
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.core.ApplicationListeners;
import io.undertow.servlet.core.ManagedListener;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Host;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class UndertowContextTestCase {
    private final Deployment deployment = mock(Deployment.class);
    private final Host host = mock(Host.class);
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
        ApplicationListeners listeners = new ApplicationListeners(Collections.<ManagedListener>emptyList(), context);

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
        ApplicationListeners listeners = new ApplicationListeners(Collections.<ManagedListener>emptyList(), context);
        ArgumentCaptor<ServletRequestEvent> event = ArgumentCaptor.forClass(ServletRequestEvent.class);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        this.context.addRequestListener(listener);
        listeners.start();

        listeners.requestInitialized(request);

        verify(listener).requestInitialized(event.capture());

        assertSame(request, event.getValue().getServletRequest());
        assertSame(context, event.getValue().getServletContext());

        event = ArgumentCaptor.forClass(ServletRequestEvent.class);

        listeners.requestDestroyed(request);

        verify(listener).requestDestroyed(event.capture());

        assertSame(request, event.getValue().getServletRequest());
        assertSame(context, event.getValue().getServletContext());
    }

    @Test
    public void addSessionListener() throws ServletException {
        HttpSessionListener listener = mock(HttpSessionListener.class);
        ServletContext context = mock(ServletContext.class);
        HttpSession session = mock(HttpSession.class);
        ApplicationListeners listeners = new ApplicationListeners(Collections.<ManagedListener>emptyList(), context);
        ArgumentCaptor<HttpSessionEvent> event = ArgumentCaptor.forClass(HttpSessionEvent.class);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        this.context.addSessionListener(listener);
        listeners.start();

        listeners.sessionCreated(session);

        verify(listener).sessionCreated(event.capture());

        assertSame(session, event.getValue().getSession());

        event = ArgumentCaptor.forClass(HttpSessionEvent.class);

        listeners.sessionDestroyed(session);

        verify(listener).sessionDestroyed(event.capture());

        assertSame(session, event.getValue().getSession());
    }
}
