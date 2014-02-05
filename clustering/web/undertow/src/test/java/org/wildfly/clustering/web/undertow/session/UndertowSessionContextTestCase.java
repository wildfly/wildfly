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
package org.wildfly.clustering.web.undertow.session;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.core.ApplicationListeners;
import io.undertow.servlet.core.ManagedListener;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.session.SessionContext;

public class UndertowSessionContextTestCase {
    private final Deployment deployment = mock(Deployment.class);
    private final SessionContext context = new UndertowSessionContext(this.deployment);

    @Test
    public void getServletContext() {
        DeploymentInfo info = mock(DeploymentInfo.class);
        ServletContainer container = mock(ServletContainer.class);

        when(this.deployment.getDeploymentInfo()).thenReturn(info);
        ServletContextImpl context = new ServletContextImpl(container, this.deployment);

        when(this.deployment.getServletContext()).thenReturn(context);

        ServletContext result = this.context.getServletContext();

        assertSame(context, result);
    }

    @Test
    public void getSessionListeners() {
        ServletContext context = mock(ServletContext.class);
        HttpSessionListener listener1 = mock(HttpSessionListener.class);
        HttpSessionListener listener2 = mock(HttpSessionListener.class);
        List<ManagedListener> list = Arrays.asList(new ManagedListener(new ListenerInfo(HttpSessionListener.class, new ImmediateInstanceFactory<>(listener1)), false), new ManagedListener(new ListenerInfo(HttpSessionListener.class, new ImmediateInstanceFactory<>(listener2)), false));
        ApplicationListeners listeners = new ApplicationListeners(list, context);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        Iterable<HttpSessionListener> result = this.context.getSessionListeners();

        HttpSession createdSession = mock(HttpSession.class);
        HttpSession destroyedSession = mock(HttpSession.class);

        HttpSessionEvent createdEvent = new HttpSessionEvent(createdSession);
        for (HttpSessionListener listener: result) {
            listener.sessionCreated(createdEvent);
        }
        HttpSessionEvent destroyedEvent = new HttpSessionEvent(destroyedSession);
        for (HttpSessionListener listener: result) {
            listener.sessionDestroyed(destroyedEvent);
        }

        ArgumentCaptor<HttpSessionEvent> capturedCreatedEvent1 = ArgumentCaptor.forClass(HttpSessionEvent.class);
        verify(listener1).sessionCreated(capturedCreatedEvent1.capture());
        assertSame(createdSession, capturedCreatedEvent1.getValue().getSession());

        ArgumentCaptor<HttpSessionEvent> capturedCreatedEvent2 = ArgumentCaptor.forClass(HttpSessionEvent.class);
        verify(listener2).sessionCreated(capturedCreatedEvent2.capture());
        assertSame(createdSession, capturedCreatedEvent2.getValue().getSession());

        ArgumentCaptor<HttpSessionEvent> capturedDestroyedEvent1 = ArgumentCaptor.forClass(HttpSessionEvent.class);
        verify(listener1).sessionDestroyed(capturedDestroyedEvent1.capture());
        assertSame(destroyedSession, capturedDestroyedEvent1.getValue().getSession());

        ArgumentCaptor<HttpSessionEvent> capturedDestroyedEvent2 = ArgumentCaptor.forClass(HttpSessionEvent.class);
        verify(listener2).sessionDestroyed(capturedDestroyedEvent2.capture());
        assertSame(destroyedSession, capturedDestroyedEvent2.getValue().getSession());
    }

    @Test
    public void getSessionAttributeListeners() {
        ServletContext context = mock(ServletContext.class);
        HttpSessionAttributeListener listener1 = mock(HttpSessionAttributeListener.class);
        HttpSessionAttributeListener listener2 = mock(HttpSessionAttributeListener.class);
        List<ManagedListener> list = Arrays.asList(new ManagedListener(new ListenerInfo(HttpSessionAttributeListener.class, new ImmediateInstanceFactory<>(listener1)), false), new ManagedListener(new ListenerInfo(HttpSessionAttributeListener.class, new ImmediateInstanceFactory<>(listener2)), false));
        ApplicationListeners listeners = new ApplicationListeners(list, context);

        when(this.deployment.getApplicationListeners()).thenReturn(listeners);

        Iterable<HttpSessionAttributeListener> result = this.context.getSessionAttributeListeners();

        HttpSession session = mock(HttpSession.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedAddedEvent1 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedAddedEvent2 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedReplacedEvent1 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedReplacedEvent2 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedRemovedEvent1 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedRemovedEvent2 = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);

        doNothing().when(listener1).attributeAdded(capturedAddedEvent1.capture());
        doNothing().when(listener2).attributeAdded(capturedAddedEvent2.capture());
        doNothing().when(listener1).attributeReplaced(capturedReplacedEvent1.capture());
        doNothing().when(listener2).attributeReplaced(capturedReplacedEvent2.capture());
        doNothing().when(listener1).attributeRemoved(capturedRemovedEvent1.capture());
        doNothing().when(listener2).attributeRemoved(capturedRemovedEvent2.capture());

        HttpSessionBindingEvent addedEvent = new HttpSessionBindingEvent(session, "added");
        for (HttpSessionAttributeListener listener: result) {
            listener.attributeAdded(addedEvent);
        }
        HttpSessionBindingEvent replacedEvent = new HttpSessionBindingEvent(session, "replaced");
        for (HttpSessionAttributeListener listener: result) {
            listener.attributeReplaced(replacedEvent);
        }
        HttpSessionBindingEvent removedEvent = new HttpSessionBindingEvent(session, "removed");
        for (HttpSessionAttributeListener listener: result) {
            listener.attributeRemoved(removedEvent);
        }

        assertSame(session, capturedAddedEvent1.getValue().getSession());
        assertEquals("added", capturedAddedEvent1.getValue().getName());
        assertSame(session, capturedAddedEvent2.getValue().getSession());
        assertEquals("added", capturedAddedEvent2.getValue().getName());
        assertSame(session, capturedReplacedEvent1.getValue().getSession());
        assertEquals("replaced", capturedReplacedEvent1.getValue().getName());
        assertSame(session, capturedReplacedEvent2.getValue().getSession());
        assertEquals("replaced", capturedReplacedEvent2.getValue().getName());
        assertSame(session, capturedRemovedEvent1.getValue().getSession());
        assertEquals("removed", capturedRemovedEvent1.getValue().getName());
        assertSame(session, capturedRemovedEvent2.getValue().getSession());
        assertEquals("removed", capturedRemovedEvent2.getValue().getName());
    }
}
