/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.hotrod;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * @author Radoslav Husar
 */
@BuiltBy(HotRodStoreConfigurationBuilder.class)
@ConfigurationFor(HotRodStore.class)
public class HotRodStoreConfiguration extends AbstractStoreConfiguration<HotRodStoreConfiguration> {

    static final AttributeDefinition<RemoteCacheContainer> REMOTE_CACHE_CONTAINER = AttributeDefinition.builder("remoteCacheContainer", null, RemoteCacheContainer.class).build();

    static final AttributeDefinition<String> CACHE_CONFIGURATION = AttributeDefinition.builder("cacheConfiguration", null, String.class).build();

    public HotRodStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async) {
        super(org.infinispan.persistence.remote.configuration.Element.REMOTE_STORE, attributes, async);
    }

    public RemoteCacheContainer remoteCacheContainer() {
        return this.attributes().attribute(HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER).get();
    }

    public String cacheConfiguration() {
        return this.attributes.attribute(CACHE_CONFIGURATION).get();
    }

    @Override
    public String toString() {
        return "HotRodStoreConfiguration{attributes=" + this.attributes + '}';
    }
}
