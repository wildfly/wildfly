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

package org.jboss.as.clustering.infinispan.subsystem.remote;


import static org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreConfiguration.CACHE_CONFIGURATION;
import static org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;

/**
 * @author Radoslav Husar
 */
public class HotRodStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    public HotRodStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
        super(builder, new AttributeSet(HotRodStoreConfiguration.class, AbstractStoreConfiguration.attributeDefinitionSet(), CACHE_CONFIGURATION, REMOTE_CACHE_CONTAINER));
    }

    public HotRodStoreConfigurationBuilder cacheConfiguration(String cacheConfiguration) {
        this.attributes.attribute(CACHE_CONFIGURATION).set(cacheConfiguration);
        return this;
    }

    public HotRodStoreConfigurationBuilder remoteCacheContainer(RemoteCacheContainer remoteCacheContainer) {
        this.attributes.attribute(REMOTE_CACHE_CONTAINER).set(remoteCacheContainer);
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public HotRodStoreConfiguration create() {
        return new HotRodStoreConfiguration(this.attributes.protect(), this.async.create(), this.singletonStore.create());
    }

    @Override
    public HotRodStoreConfigurationBuilder self() {
        return this;
    }

}
