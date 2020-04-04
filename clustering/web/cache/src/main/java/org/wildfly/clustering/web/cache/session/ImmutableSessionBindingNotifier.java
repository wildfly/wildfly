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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.wildfly.clustering.web.session.HttpSessionBindingListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Triggers binding events for all attributes of a session.
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionBindingListener specification type
 * @author Paul Ferraro
 */
public class ImmutableSessionBindingNotifier<S, C, L> implements SessionBindingNotifier {
    private final HttpSessionBindingListenerProvider<S, C, L> provider;
    private final ImmutableSession session;
    private final C context;
    private final SessionAttributesFilter filter;

    public ImmutableSessionBindingNotifier(HttpSessionBindingListenerProvider<S, C, L> provider, ImmutableSession session, C context) {
        this(provider, session, context, new ImmutableSessionAttributesFilter(session));
    }

    ImmutableSessionBindingNotifier(HttpSessionBindingListenerProvider<S, C, L> provider, ImmutableSession session, C context, SessionAttributesFilter filter) {
        this.provider = provider;
        this.session = session;
        this.context = context;
        this.filter = filter;
    }

    @Override
    public void bound() {
        this.notify(this.provider::valueBoundNotifier);
    }

    @Override
    public void unbound() {
        this.notify(this.provider::valueUnboundNotifier);
    }

    private void notify(Function<L, BiConsumer<S, String>> notifierFactory) {
        Map<String, L> listeners = this.filter.getAttributes(this.provider.getHttpSessionBindingListenerClass());
        if (!listeners.isEmpty()) {
            for (Map.Entry<String, L> entry : listeners.entrySet()) {
                L listener = entry.getValue();
                S session = this.provider.createHttpSession(this.session, this.context);
                notifierFactory.apply(listener).accept(session, entry.getKey());
            }
        }
    }
}
