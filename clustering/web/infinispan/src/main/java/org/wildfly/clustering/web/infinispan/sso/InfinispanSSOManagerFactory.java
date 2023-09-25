/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.sso;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.infinispan.affinity.AffinityIdentifierFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.sso.CompositeSSOManager;
import org.wildfly.clustering.web.cache.sso.SSOFactory;
import org.wildfly.clustering.web.cache.sso.SessionsFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactory<A, D, S> implements SSOManagerFactory<A, D, S, TransactionBatch> {

    private final InfinispanSSOManagerFactoryConfiguration configuration;

    public InfinispanSSOManagerFactory(InfinispanSSOManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <L> SSOManager<A, D, S, L, TransactionBatch> createSSOManager(SSOManagerConfiguration<L> configuration) {
        SessionsFactory<Map<D, S>, D, S> sessionsFactory = new CoarseSessionsFactory<>(this.configuration);
        Marshaller<A, MarshalledValue<A, ByteBufferMarshaller>> marshaller = new MarshalledValueMarshaller<>(new ByteBufferMarshalledValueFactory(configuration.getMarshaller()));
        SSOFactory<Map.Entry<A, AtomicReference<L>>, Map<D, S>, A, D, S, L> factory = new InfinispanSSOFactory<>(this.configuration, marshaller, configuration.getLocalContextFactory(), sessionsFactory);
        IdentifierFactory<String> idFactory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.configuration.getCache(), this.configuration.getKeyAffinityServiceFactory());
        return new CompositeSSOManager<>(factory, idFactory, this.configuration.getBatcher());
    }
}
