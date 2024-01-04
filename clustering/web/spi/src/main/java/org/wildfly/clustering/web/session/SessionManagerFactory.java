/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import org.wildfly.clustering.ee.Batch;

/**
 * A factory for creating a session manager.
 * @param <SC> the ServletContext specification type
 * @param <LC> the local context type
 * @param <B> the batch type
 * @author Paul Ferraro
 */
public interface SessionManagerFactory<SC, LC, B extends Batch> extends AutoCloseable {
    /**
     * Create as session manager using the specified context and identifier factory.
     * @param context a session context
     * @param idFactory a session identifier factory
     * @return a new session manager
     */
    SessionManager<LC, B> createSessionManager(SessionManagerConfiguration<SC> configuration);

    @Override
    void close();
}
