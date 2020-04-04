/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.function.BiConsumer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.session.HttpSessionBindingListenerProvider;

/**
 * Unit test for {@link HttpSessionBindingListenerProvider}.
 * @author Paul Ferraro
 */
public class UndertowHttpSessionBindingListenerProviderTestCase {

    private HttpSessionBindingListenerProvider<HttpSession, ServletContext, HttpSessionBindingListener> provider = UndertowSpecificationProvider.INSTANCE;

    @Test
    public void valueBoundNotifier() {
        HttpSessionBindingListener listener = mock(HttpSessionBindingListener.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);

        doNothing().when(listener).valueBound(capturedEvent.capture());

        BiConsumer<HttpSession, String> notifier = this.provider.valueBoundNotifier(listener);

        HttpSession session = mock(HttpSession.class);

        notifier.accept(session, "foo");

        assertSame(session, capturedEvent.getValue().getSession());
        assertEquals("foo", capturedEvent.getValue().getName());
    }

    @Test
    public void valueUnboundNotifier() {
        HttpSessionBindingListener listener = mock(HttpSessionBindingListener.class);
        ArgumentCaptor<HttpSessionBindingEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);

        doNothing().when(listener).valueUnbound(capturedEvent.capture());

        BiConsumer<HttpSession, String> notifier = this.provider.valueUnboundNotifier(listener);

        HttpSession session = mock(HttpSession.class);

        notifier.accept(session, "foo");

        assertSame(session, capturedEvent.getValue().getSession());
        assertEquals("foo", capturedEvent.getValue().getName());
    }
}
