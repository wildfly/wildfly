/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionActivationNotifierTestCase {
    interface Session {
    }
    interface Context {
    }
    interface Listener {
    }

    private final HttpSessionActivationListenerProvider<Session, Context, Listener> provider = mock(HttpSessionActivationListenerProvider.class);
    private final ImmutableSession session = mock(ImmutableSession.class);
    private final Context context = mock(Context.class);
    private final SessionAttributesFilter filter = mock(SessionAttributesFilter.class);
    private final Listener listener1 = mock(Listener.class);
    private final Listener listener2 = mock(Listener.class);

    private final SessionActivationNotifier notifier = new ImmutableSessionActivationNotifier<>(this.provider, this.session, this.context, this.filter);

    @After
    public void destroy() {
        Mockito.reset(this.session, this.provider);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        Map<String, Listener> listeners = new TreeMap<>();
        listeners.put("listener1", this.listener1);
        listeners.put("listener2", this.listener2);

        when(this.provider.getHttpSessionActivationListenerClass()).thenReturn(Listener.class);
        when(this.filter.getAttributes(Listener.class)).thenReturn(listeners);

        Session session = mock(Session.class);
        Consumer<Session> prePassivateNotifier1 = mock(Consumer.class);
        Consumer<Session> prePassivateNotifier2 = mock(Consumer.class);
        Consumer<Session> postActivateNotifier1 = mock(Consumer.class);
        Consumer<Session> postActivateNotifier2 = mock(Consumer.class);

        when(this.provider.createHttpSession(same(this.session), same(this.context))).thenReturn(session);
        when(this.provider.prePassivateNotifier(same(this.listener1))).thenReturn(prePassivateNotifier1);
        when(this.provider.prePassivateNotifier(same(this.listener2))).thenReturn(prePassivateNotifier2);
        when(this.provider.postActivateNotifier(same(this.listener1))).thenReturn(postActivateNotifier1);
        when(this.provider.postActivateNotifier(same(this.listener2))).thenReturn(postActivateNotifier2);

        // verify pre-passivate before post-activate is a no-op
        this.notifier.prePassivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        // verify initial post-activate
        this.notifier.postActivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1).accept(session);
        verify(postActivateNotifier2).accept(session);

        reset(postActivateNotifier1, postActivateNotifier2);

        // verify subsequent post-activate is a no-op
        this.notifier.postActivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        // verify pre-passivate following post-activate
        this.notifier.prePassivate();

        verify(prePassivateNotifier1).accept(session);
        verify(prePassivateNotifier2).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        reset(prePassivateNotifier1, prePassivateNotifier2);

        // verify subsequent pre-passivate is a no-op
        this.notifier.prePassivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        // verify post-activate following pre-passivate
        this.notifier.postActivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1).accept(session);
        verify(postActivateNotifier2).accept(session);
    }

    @Test
    public void postActivate() {
        Map<String, Listener> listeners = new TreeMap<>();
        listeners.put("listener1", this.listener1);
        listeners.put("listener2", this.listener2);

        when(this.provider.getHttpSessionActivationListenerClass()).thenReturn(Listener.class);
        when(this.filter.getAttributes(Listener.class)).thenReturn(listeners);

        Session session = mock(Session.class);
        Consumer<Session> notifier1 = mock(Consumer.class);
        Consumer<Session> notifier2 = mock(Consumer.class);

        when(this.provider.createHttpSession(same(this.session), same(this.context))).thenReturn(session);
        when(this.provider.postActivateNotifier(same(this.listener1))).thenReturn(notifier1);
        when(this.provider.postActivateNotifier(same(this.listener2))).thenReturn(notifier2);

        this.notifier.postActivate();

        verify(this.provider, never()).prePassivateNotifier(this.listener1);
        verify(this.provider, never()).prePassivateNotifier(this.listener2);

        verify(notifier1).accept(session);
        verify(notifier2).accept(session);
    }
}
