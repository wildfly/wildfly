/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;

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
        verifyNoInteractions(postActivate);

        reset(prePassivate, postActivate);

        listener.sessionDidActivate(event);

        verifyNoInteractions(prePassivate);
        verify(postActivate).accept(session);
    }
}
