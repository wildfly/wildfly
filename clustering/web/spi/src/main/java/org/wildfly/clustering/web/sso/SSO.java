/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.sso;

/**
 * Represents a single sign on entry for a user.
 * @author Paul Ferraro
 * @param <A> the authentication identity type
 * @param <D> the deployment identifier type
 * @param <S> the session identifier type
 * @param <L> the local context type
 */
public interface SSO<A, D, S, L> {
    /**
     * A unique identifier for this SSO.
     * @return a unique identifier
     */
    String getId();

    /**
     * Returns the authentication for this SSO.
     * @return an authentication.
     */
    A getAuthentication();

    /**
     * Returns the session for which the user is authenticated.
     * @return
     */
    Sessions<D, S> getSessions();

    /**
     * Invalidates this SSO.
     */
    void invalidate();

    /**
     * The local context of this SSO.
     * The local context is *not* replicated to other nodes in the cluster.
     * @return a local context.
     */
    L getLocalContext();
}
