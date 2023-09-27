/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

import java.util.function.Consumer;

/**
 * Provides specification behavior for session activation listeners.
 * @param <S> the specification type for the HttpSession
 * @param <C> the specification type for the ServletContext
 * @param <L> the specification type for the HttpSessionActivationListener
 * @author Paul Ferraro
 */
public interface HttpSessionActivationListenerProvider<S, C, L> extends HttpSessionFactory<S, C> {
    /**
     * Returns the HttpSessionActivationListener specification interface.
     * @return the HttpSessionActivationListener specification interface
     */
    Class<L> getHttpSessionActivationListenerClass();

    /**
     * Creates a pre-passivate notifier for the specified listener.
     * @param listener the specification listener
     * @return a consumer for a session
     */
    Consumer<S> prePassivateNotifier(L listener);

    /**
     * Creates a post-activate notifier for the specified listener.
     * @param listener the specification listener
     * @return a consumer for a session
     */
    Consumer<S> postActivateNotifier(L listener);

    /**
     * Creates a specification implementation with the specified pre-passivate and post-activate logic.
     * @param prePassivate a pre-passivate event consumer for a session
     * @param postActivate a post-activate event consumer for a session
     * @return a specification listener implementation
     */
    L createListener(Consumer<S> prePassivate, Consumer<S> postActivate);
}
