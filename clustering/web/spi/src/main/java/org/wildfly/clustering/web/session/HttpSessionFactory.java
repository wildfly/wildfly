/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

/**
 * Factory for creating HttpSession specification implementations
 * @param <S> the specification type for the HttpSession
 * @param <C> the specification type for the ServletContext
 * @author Paul Ferraro
 */
public interface HttpSessionFactory<S, C> {
    /**
     * Create an HttpSession specification implementation for the specified session and servlet context.
     * @param session a session
     * @param context a servlet context
     * @return a HttpSession specification implementation
     */
    S createHttpSession(ImmutableSession session, C context);
}
