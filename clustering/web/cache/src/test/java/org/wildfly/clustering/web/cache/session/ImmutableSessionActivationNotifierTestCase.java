/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        // We should be able to trigger an initial pre-passivate event
        this.notifier.prePassivate();

        verify(prePassivateNotifier1).accept(session);
        verify(prePassivateNotifier2).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        reset(postActivateNotifier1, postActivateNotifier2, prePassivateNotifier1, prePassivateNotifier2);

        // We should be able to trigger a second pre-passivate event
        this.notifier.prePassivate();

        verify(prePassivateNotifier1).accept(session);
        verify(prePassivateNotifier2).accept(session);
        verify(postActivateNotifier1, never()).accept(session);
        verify(postActivateNotifier2, never()).accept(session);

        reset(postActivateNotifier1, postActivateNotifier2, prePassivateNotifier1, prePassivateNotifier2);

        // We should be able to trigger an initial post-activate event
        this.notifier.postActivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1).accept(session);
        verify(postActivateNotifier2).accept(session);

        reset(postActivateNotifier1, postActivateNotifier2, prePassivateNotifier1, prePassivateNotifier2);

        // We should be able to trigger a second post-activate event
        this.notifier.postActivate();

        verify(prePassivateNotifier1, never()).accept(session);
        verify(prePassivateNotifier2, never()).accept(session);
        verify(postActivateNotifier1).accept(session);
        verify(postActivateNotifier2).accept(session);
    }
}
