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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Triggers activation/passivation events for a single session attribute.
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionActivationListener specification type
 * @author Paul Ferraro
 */
public class ImmutableSessionAttributeActivationNotifier<S, C, L> implements SessionAttributeActivationNotifier {

    private final Function<Supplier<L>, L> prePassivateListenerFactory;
    private final Function<Supplier<L>, L> postActivateListenerFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;
    private final S session;
    private final Map<Supplier<L>, L> listeners = new ConcurrentHashMap<>();

    public ImmutableSessionAttributeActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context) {
        this.provider = provider;
        this.prePassivateListenerFactory = new HttpSessionActivationListenerFactory<>(provider, true);
        this.postActivateListenerFactory = new HttpSessionActivationListenerFactory<>(provider, false);
        this.session = provider.createHttpSession(session, context);
    }

    @Override
    public void prePassivate(Object object) {
        this.notify(object, this.prePassivateListenerFactory, this.provider::prePassivateNotifier);
    }

    @Override
    public void postActivate(Object object) {
        this.notify(object, this.postActivateListenerFactory, this.provider::postActivateNotifier);
    }

    private void notify(Object object, Function<Supplier<L>, L> listenerFactory, Function<L, Consumer<S>> notifierFactory) {
        Class<L> listenerClass = this.provider.getHttpSessionActivationListenerClass();
        if (listenerClass.isInstance(object)) {
            Supplier<L> reference = new HttpSessionActivationListenerKey<>(listenerClass.cast(object));
            L listener = this.listeners.computeIfAbsent(reference, listenerFactory);
            notifierFactory.apply(listener).accept(this.session);
        }
    }

    @Override
    public void close() {
        for (L listener : this.listeners.values()) {
            this.provider.prePassivateNotifier(listener).accept(this.session);
        }
        this.listeners.clear();
    }

    /**
     * Map key of a {@link HttpSessionActivationListener} that uses identity equality.
     */
    private static class HttpSessionActivationListenerKey<L> implements Supplier<L> {
        private final L listener;

        HttpSessionActivationListenerKey(L listener) {
            this.listener = listener;
        }

        @Override
        public L get() {
            return this.listener;
        }

        @Override
        public int hashCode() {
            return this.listener.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof HttpSessionActivationListenerKey)) return false;
            @SuppressWarnings("unchecked")
            HttpSessionActivationListenerKey<L> reference = (HttpSessionActivationListenerKey<L>) object;
            return this.listener == reference.listener;
        }
    }

    /**
     * Factory for creating HttpSessionActivationListener values.
     */
    private static class HttpSessionActivationListenerFactory<S, C, L> implements Function<Supplier<L>, L> {
        private final HttpSessionActivationListenerProvider<S, C, L> provider;
        private final boolean active;

        HttpSessionActivationListenerFactory(HttpSessionActivationListenerProvider<S, C, L> provider, boolean active) {
            this.provider = provider;
            this.active = active;
        }

        @Override
        public L apply(Supplier<L> reference) {
            HttpSessionActivationListenerProvider<S, C, L> provider = this.provider;
            L listener = reference.get();
            // Prevents redundant session activation events for a given listener.
            AtomicBoolean active = new AtomicBoolean(this.active);
            Consumer<S> prePassivate = new Consumer<S>() {
                @Override
                public void accept(S session) {
                    if (active.compareAndSet(true, false)) {
                        provider.prePassivateNotifier(listener).accept(session);
                    }
                }
            };
            Consumer<S> postActivate = new Consumer<S>() {
                @Override
                public void accept(S session) {
                    if (active.compareAndSet(false, true)) {
                        provider.postActivateNotifier(listener).accept(session);
                    }
                }
            };
            return provider.createListener(prePassivate, postActivate);
        }
    }
}
