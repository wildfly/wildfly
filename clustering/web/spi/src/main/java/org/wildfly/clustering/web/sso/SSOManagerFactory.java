/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.sso;

import org.wildfly.clustering.ee.Batch;

/**
 * Factory for creating SSO manager instances.
 * @param <A> authentication type
 * @param <D> deployment type
 * @param <S> session type
 * @param <B> batch type
 */
public interface SSOManagerFactory<A, D, S, B extends Batch> {
    /**
     * Creates a new SSO manager using the specified configuration.
     * @param <L> local context type
     * @param config a SSO manager configuration
     * @return a new SSO manager
     */
    <L> SSOManager<A, D, S, L, B> createSSOManager(SSOManagerConfiguration<L> config);
}
