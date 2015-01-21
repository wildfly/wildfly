/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.sso;

import java.util.Map;

import org.infinispan.Cache;
import org.jboss.msc.service.AbstractService;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.MarshalledValueMarshaller;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.marshalling.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.SimpleMarshallingContextFactory;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.AffinityIdentifierFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseAuthenticationEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessionsKey;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactory<A, D> extends AbstractService<SSOManagerFactory<A, D, TransactionBatch>> implements SSOManagerFactory<A, D, TransactionBatch> {

    private final InfinispanSSOManagerFactoryConfiguration configuration;

    public InfinispanSSOManagerFactory(InfinispanSSOManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <L> SSOManager<A, D, L, TransactionBatch> createSSOManager(IdentifierFactory<String> identifierFactory, LocalContextFactory<L> localContextFactory) {
        MarshallingContext marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new AuthenticationMarshallingContext(this.configuration.getModuleLoader()), null);
        MarshalledValueFactory<MarshallingContext> marshalledValueFactory = new SimpleMarshalledValueFactory(marshallingContext);
        Marshaller<A, MarshalledValue<A, MarshallingContext>> marshaller = new MarshalledValueMarshaller<>(marshalledValueFactory, marshallingContext);
        Cache<String, CoarseAuthenticationEntry<A, D, L>> authenticationCache = this.configuration.getCache();
        Cache<CoarseSessionsKey, Map<D, String>> sessionsCache = this.configuration.getCache();
        SSOFactory<CoarseSSOEntry<A, D, L>, A, D, L> factory = new CoarseSSOFactory<>(authenticationCache, sessionsCache, marshaller, localContextFactory);
        IdentifierFactory<String> idFactory = new AffinityIdentifierFactory<>(identifierFactory, authenticationCache, this.configuration.getKeyAffinityServiceFactory());
        Batcher<TransactionBatch> batcher = new InfinispanBatcher(authenticationCache);
        return new InfinispanSSOManager<>(factory, idFactory, batcher);
    }
}
