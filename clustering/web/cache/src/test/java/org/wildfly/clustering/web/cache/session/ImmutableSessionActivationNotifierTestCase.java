/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionActivationNotifierTestCase {
    private final ImmutableSession session = mock(ImmutableSession.class);
    private final ServletContext context = mock(ServletContext.class);
    private final SessionActivationNotifier notifier = new ImmutableSessionActivationNotifier(this.session, this.context);

    private final HttpSessionActivationListener listener1 = mock(HttpSessionActivationListener.class);
    private final HttpSessionActivationListener listener2 = mock(HttpSessionActivationListener.class);

    @Before
    public void init() {
        Object object1 = new Object();
        Object object2 = new Object();

        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(new TreeSet<>(Arrays.asList("non-listener1", "non-listener2", "listener1", "listener2")));
        when(attributes.getAttribute("non-listener1")).thenReturn(object1);
        when(attributes.getAttribute("non-listener2")).thenReturn(object2);
        when(attributes.getAttribute("listener1")).thenReturn(this.listener1);
        when(attributes.getAttribute("listener2")).thenReturn(this.listener2);
    }

    @After
    public void destroy() {
        Mockito.reset(this.session, this.context, this.listener1, this.listener2);
    }

    @Test
    public void prePassivate() {
        String sessionId = "abc";
        when(this.session.getId()).thenReturn(sessionId);

        ArgumentCaptor<HttpSessionEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionEvent.class);

        this.notifier.prePassivate();

        verify(this.listener1, Mockito.never()).sessionDidActivate(capturedEvent.capture());
        verify(this.listener1, Mockito.never()).sessionDidActivate(capturedEvent.capture());
        verify(this.listener1, Mockito.times(1)).sessionWillPassivate(capturedEvent.capture());
        verify(this.listener2, Mockito.times(1)).sessionWillPassivate(capturedEvent.capture());

        for (HttpSessionEvent event : capturedEvent.getAllValues()) {
            Assert.assertSame(this.context, event.getSession().getServletContext());
            Assert.assertSame(sessionId, event.getSession().getId());
        }
    }

    @Test
    public void postActivate() {
        String sessionId = "abc";
        when(this.session.getId()).thenReturn(sessionId);

        ArgumentCaptor<HttpSessionEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionEvent.class);

        this.notifier.postActivate();

        verify(this.listener1, Mockito.times(1)).sessionDidActivate(capturedEvent.capture());
        verify(this.listener2, Mockito.times(1)).sessionDidActivate(capturedEvent.capture());
        verify(this.listener1, Mockito.never()).sessionWillPassivate(capturedEvent.capture());
        verify(this.listener1, Mockito.never()).sessionWillPassivate(capturedEvent.capture());

        for (HttpSessionEvent event : capturedEvent.getAllValues()) {
            Assert.assertSame(this.context, event.getSession().getServletContext());
            Assert.assertSame(sessionId, event.getSession().getId());
        }
    }
}
