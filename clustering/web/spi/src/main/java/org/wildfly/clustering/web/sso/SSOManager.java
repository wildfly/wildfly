/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.sso;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Restartable;

/**
 * The SSO equivalent of a session manager.
 * @author Paul Ferraro
 * @param <A> the authentication identity type
 * @param <D> the deployment identifier type
 * @param <S> the session identifier type
 * @param <L> the local context type
 * @param <B> the batch type
 */
public interface SSOManager<A, D, S, L, B extends Batch> extends Restartable {
    /**
     * Creates a new single sign on entry.
     * @param ssoId a unique SSO identifier
     * @return a new SSO.
     */
    SSO<A, D, S, L> createSSO(String ssoId, A authentication);

    /**
     * Returns the single sign on entry identified by the specified identifier.
     * @param ssoId a unique SSO identifier
     * @return an existing SSO, or null, if no SSO was found
     */
    SSO<A, D, S, L> findSSO(String ssoId);

    /**
     * Searches for the sessions of the single sign on entry containing the specified session.
     * @param sessionId a unique session identifier
     * @return an existing sessions of an SSO, or null, if no SSO was found
     */
    Sessions<D, S> findSessionsContaining(S session);

    /**
     * A mechanism for starting/stopping a batch.
     * @return a batching mechanism.
     */
    Batcher<B> getBatcher();

    /**
     * Returns the identifier factory of this SSO manager.
     * @return an identifier factory
     */
    Supplier<String> getIdentifierFactory();
}
