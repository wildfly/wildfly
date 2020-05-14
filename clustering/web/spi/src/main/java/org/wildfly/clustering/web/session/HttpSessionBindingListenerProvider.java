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

import java.util.function.BiConsumer;

/**
 * Provides specification behavior for session binding listeners.
 * @param <S> the specification type for the HttpSession
 * @param <C> the specification type for the ServletContext
 * @param <L> the specification type for the HttpSessionBindingListener
 * @author Paul Ferraro
 */
public interface HttpSessionBindingListenerProvider<S, C, L> extends HttpSessionFactory<S, C> {
    /**
     * Returns the HttpSessionBindingListener specification interface.
     * @return the HttpSessionBindingListener specification interface
     */
    Class<L> getHttpSessionBindingListenerClass();

    /**
     * Creates a value-bound notifier for the specified listener.
     * @param listener the specification listener
     * @return a consumer for a session and attribute name
     */
    BiConsumer<S, String> valueBoundNotifier(L listener);

    /**
     * Creates a value-unbound notifier for the specified listener.
     * @param listener the specification listener
     * @return a consumer for a session and attribute name
     */
    BiConsumer<S, String> valueUnboundNotifier(L listener);
}
