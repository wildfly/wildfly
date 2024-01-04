/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

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
    private final Function<L, Consumer<S>> prePassivateNotifier;
    private final Function<L, Consumer<S>> postActivateNotifier;

    public ImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context) {
        this(provider, session, context, new ImmutableSessionAttributesFilter(session));
    }

    ImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context, SessionAttributesFilter filter) {
        this.provider = provider;
        this.session = session;
        this.context = context;
        this.filter = filter;
        this.prePassivateNotifier = this.provider::prePassivateNotifier;
        this.postActivateNotifier = this.provider::postActivateNotifier;
    }

    @Override
    public void prePassivate() {
        if (this.active.compareAndSet(true, false)) {
            this.notify(this.prePassivateNotifier);
        }
    }

    @Override
    public void postActivate() {
        if (this.active.compareAndSet(false, true)) {
            this.notify(this.postActivateNotifier);
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
