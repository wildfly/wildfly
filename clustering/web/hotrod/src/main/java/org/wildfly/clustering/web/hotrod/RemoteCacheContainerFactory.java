/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod;

import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.NearCacheMode;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheContainerFactory implements Supplier<RemoteCacheContainer> {

    private final RemoteCacheContainerConfiguration config;

    public RemoteCacheContainerFactory(RemoteCacheContainerConfiguration config) {
        this.config = config;
    }

    @Override
    public RemoteCacheContainer get() {
        int maxActiveSessions = this.config.getMaxActiveSessions();
        Configuration configuration = new ConfigurationBuilder()
                .withProperties(this.config.getProperties())
                .nearCache().mode((maxActiveSessions == 0) ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED).maxEntries(maxActiveSessions * 3)
                .marshaller(new HotRodMarshaller(this.getClass().getClassLoader()))
                .build();

        return new RemoteCacheManager(configuration, false);
    }
}
