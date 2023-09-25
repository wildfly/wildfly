/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;

/**
 * Configuration for an {@link InfinispanSessionManager}.
 * @param <SC> the ServletContext specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public interface InfinispanSessionManagerConfiguration<SC, LC> extends SessionManagerConfiguration<SC>, InfinispanConfiguration {
    @Override
    IdentifierFactory<String> getIdentifierFactory();
    Scheduler<String, ExpirationMetaData> getExpirationScheduler();
    Runnable getStartTask();
    Registrar<SessionManager<LC, TransactionBatch>> getRegistrar();
}
