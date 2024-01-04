/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.SimpleIdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.sso.CompositeSSOManager;
import org.wildfly.clustering.web.cache.sso.SSOFactory;
import org.wildfly.clustering.web.cache.sso.SessionsFactory;
import org.wildfly.clustering.web.hotrod.sso.coarse.CoarseSessionsFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOManagerFactory<A, D, S> implements SSOManagerFactory<A, D, S, TransactionBatch> {

    private final HotRodConfiguration configuration;

    public HotRodSSOManagerFactory(HotRodConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <L> SSOManager<A, D, S, L, TransactionBatch> createSSOManager(SSOManagerConfiguration<L> config) {
        SessionsFactory<Map<D, S>, D, S> sessionsFactory = new CoarseSessionsFactory<>(this.configuration);
        Marshaller<A, MarshalledValue<A, ByteBufferMarshaller>> marshaller = new MarshalledValueMarshaller<>(new ByteBufferMarshalledValueFactory(config.getMarshaller()));
        SSOFactory<Map.Entry<A, AtomicReference<L>>, Map<D, S>, A, D, S, L> factory = new HotRodSSOFactory<>(this.configuration, marshaller, config.getLocalContextFactory(), sessionsFactory);
        IdentifierFactory<String> identifierFactory = new SimpleIdentifierFactory<>(config.getIdentifierFactory());
        return new CompositeSSOManager<>(factory, identifierFactory, this.configuration.getBatcher());
    }
}
