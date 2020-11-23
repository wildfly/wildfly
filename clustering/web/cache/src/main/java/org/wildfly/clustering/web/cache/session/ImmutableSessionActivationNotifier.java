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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Triggers activation/passivation events for all attributes of a session.
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionActivationListener specification type
 * @author Paul Ferraro
 */
public class ImmutableSessionActivationNotifier<S, C, L> implements SessionActivationNotifier {

    private final HttpSessionActivationListenerProvider<S, C, L> provider;
    private final ImmutableSession session;
    private final C context;
    private final SessionAttributesFilter filter;
    private final AtomicBoolean active = new AtomicBoolean(false);

    public ImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context) {
        this(provider, session, context, new ImmutableSessionAttributesFilter(session));
    }

    ImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context, SessionAttributesFilter filter) {
        this.provider = provider;
        this.session = session;
        this.context = context;
        this.filter = filter;
    }

    @Override
    public void prePassivate() {
        if (this.active.compareAndSet(true, false)) {
            this.notify(this.provider::prePassivateNotifier);
        }
    }

    @Override
    public void postActivate() {
        if (this.active.compareAndSet(false, true)) {
            this.notify(this.provider::postActivateNotifier);
        }
    }

    private void notify(Function<L, Consumer<S>> notifierFactory) {
        Map<String, L> listeners = this.filter.getAttributes(this.provider.getHttpSessionActivationListenerClass());
        if (!listeners.isEmpty()) {
            S session = this.provider.createHttpSession(this.session, this.context);
            for (L listener : listeners.values()) {
                notifierFactory.apply(listener).accept(session);
            }
        }
    }
}
