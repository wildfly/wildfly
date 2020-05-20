/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.sso;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.hotrod.tx.HotRodBatcher;
import org.wildfly.clustering.infinispan.client.Key;
import org.wildfly.clustering.marshalling.spi.MarshalledValueMarshaller;
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

    private final HotRodSSOManagerFactoryConfiguration configuration;

    public HotRodSSOManagerFactory(HotRodSSOManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <C, L> SSOManager<A, D, S, L, TransactionBatch> createSSOManager(SSOManagerConfiguration<C, L> config) {
        RemoteCache<Key<String>, ?> cache = this.configuration.getRemoteCache();
        SessionsFactory<Map<D, S>, D, S> sessionsFactory = new CoarseSessionsFactory<>(this.configuration.getRemoteCache());
        SSOFactory<Map.Entry<A, AtomicReference<L>>, Map<D, S>, A, D, S, L> factory = new HotRodSSOFactory<>(this.configuration.getRemoteCache(), new MarshalledValueMarshaller<>(config.getMarshalledValueFactory()), config.getLocalContextFactory(), sessionsFactory);
        Batcher<TransactionBatch> batcher = new HotRodBatcher(cache);
        return new CompositeSSOManager<>(factory, config.getIdentifierFactory(), batcher);
    }
}
