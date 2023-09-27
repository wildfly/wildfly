/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.hotrod;


import static org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration.CACHE_CONFIGURATION;
import static org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

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

    @Override
    public HotRodStoreConfiguration create() {
        return new HotRodStoreConfiguration(this.attributes.protect(), this.async.create());
    }

    @Override
    public HotRodStoreConfigurationBuilder self() {
        return this;
    }
}
