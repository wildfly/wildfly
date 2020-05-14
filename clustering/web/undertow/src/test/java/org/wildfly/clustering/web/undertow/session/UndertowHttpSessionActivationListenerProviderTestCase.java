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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;

/**
 * Unit test for {@link HttpSessionActivationListenerProvider}.
 * @author Paul Ferraro
 */
public class UndertowHttpSessionActivationListenerProviderTestCase {

    private HttpSessionActivationListenerProvider<HttpSession, ServletContext, HttpSessionActivationListener> provider = UndertowSpecificationProvider.INSTANCE;

    @Test
    public void prePassivateNotifier() {
        HttpSessionActivationListener listener = mock(HttpSessionActivationListener.class);
        ArgumentCaptor<HttpSessionEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionEvent.class);

        doNothing().when(listener).sessionWillPassivate(capturedEvent.capture());

        Consumer<HttpSession> notifier = this.provider.prePassivateNotifier(listener);

        HttpSession session = mock(HttpSession.class);

        notifier.accept(session);

        assertSame(session, capturedEvent.getValue().getSession());
    }

    @Test
    public void postActivateNotifier() {
        HttpSessionActivationListener listener = mock(HttpSessionActivationListener.class);
        ArgumentCaptor<HttpSessionEvent> capturedEvent = ArgumentCaptor.forClass(HttpSessionEvent.class);

        doNothing().when(listener).sessionDidActivate(capturedEvent.capture());

        Consumer<HttpSession> notifier = this.provider.postActivateNotifier(listener);

        HttpSession session = mock(HttpSession.class);

        notifier.accept(session);

        assertSame(session, capturedEvent.getValue().getSession());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createListener() {
        Consumer<HttpSession> prePassivate = mock(Consumer.class);
        Consumer<HttpSession> postActivate = mock(Consumer.class);

        HttpSessionActivationListener listener = this.provider.createListener(prePassivate, postActivate);

        HttpSession session = mock(HttpSession.class);
        HttpSessionEvent event = new HttpSessionEvent(session);

        listener.sessionWillPassivate(event);

        verify(prePassivate).accept(session);
        verifyZeroInteractions(postActivate);

        reset(prePassivate, postActivate);

        listener.sessionDidActivate(event);

        verifyZeroInteractions(prePassivate);
        verify(postActivate).accept(session);
    }
}
