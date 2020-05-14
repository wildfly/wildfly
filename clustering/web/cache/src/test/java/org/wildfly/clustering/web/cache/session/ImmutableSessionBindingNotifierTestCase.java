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

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.wildfly.clustering.web.session.HttpSessionBindingListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionBindingNotifierTestCase {
    interface Session {
    }
    interface Context {
    }
    interface Listener {
    }

    private final HttpSessionBindingListenerProvider<Session, Context, Listener> provider = mock(HttpSessionBindingListenerProvider.class);
    private final ImmutableSession session = mock(ImmutableSession.class);
    private final Context context = mock(Context.class);
    private final SessionAttributesFilter filter = mock(SessionAttributesFilter.class);
    private final Listener listener1 = mock(Listener.class);
    private final Listener listener2 = mock(Listener.class);

    private final SessionBindingNotifier notifier = new ImmutableSessionBindingNotifier<>(this.provider, this.session, this.context, this.filter);

    @After
    public void destroy() {
        Mockito.reset(this.session, this.provider);
    }

    @Test
    public void bound() {
        Map<String, Listener> listeners = new TreeMap<>();
        listeners.put("listener1", this.listener1);
        listeners.put("listener2", this.listener2);

        when(this.provider.getHttpSessionBindingListenerClass()).thenReturn(Listener.class);
        when(this.filter.getAttributes(Listener.class)).thenReturn(listeners);

        Session session = mock(Session.class);
        BiConsumer<Session, String> notifier1 = mock(BiConsumer.class);
        BiConsumer<Session, String> notifier2 = mock(BiConsumer.class);

        when(this.provider.createHttpSession(same(this.session), same(this.context))).thenReturn(session);
        when(this.provider.valueBoundNotifier(same(this.listener1))).thenReturn(notifier1);
        when(this.provider.valueBoundNotifier(same(this.listener2))).thenReturn(notifier2);

        this.notifier.bound();

        verify(this.provider, never()).valueUnboundNotifier(this.listener1);
        verify(this.provider, never()).valueUnboundNotifier(this.listener2);

        verify(notifier1).accept(session, "listener1");
        verify(notifier2).accept(session, "listener2");
    }

    @Test
    public void unbound() {
        Map<String, Listener> listeners = new TreeMap<>();
        listeners.put("listener1", this.listener1);
        listeners.put("listener2", this.listener2);

        when(this.provider.getHttpSessionBindingListenerClass()).thenReturn(Listener.class);
        when(this.filter.getAttributes(Listener.class)).thenReturn(listeners);

        Session session = mock(Session.class);
        BiConsumer<Session, String> notifier1 = mock(BiConsumer.class);
        BiConsumer<Session, String> notifier2 = mock(BiConsumer.class);

        when(this.provider.createHttpSession(same(this.session), same(this.context))).thenReturn(session);
        when(this.provider.valueUnboundNotifier(same(this.listener1))).thenReturn(notifier1);
        when(this.provider.valueUnboundNotifier(same(this.listener2))).thenReturn(notifier2);

        this.notifier.unbound();

        verify(this.provider, never()).valueBoundNotifier(this.listener1);
        verify(this.provider, never()).valueBoundNotifier(this.listener2);

        verify(notifier1).accept(session, "listener1");
        verify(notifier2).accept(session, "listener2");
    }
}
