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
