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
public class HotRodStoreConfiguration extends AbstractStoreConfiguration {

    static final AttributeDefinition<RemoteCacheContainer> REMOTE_CACHE_CONTAINER = AttributeDefinition.builder("remoteCacheContainer", null, RemoteCacheContainer.class).build();

    static final AttributeDefinition<String> CACHE_CONFIGURATION = AttributeDefinition.builder("cacheConfiguration", null, String.class).build();

    public HotRodStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async) {
        super(attributes, async);
    }

    @Override
    public String toString() {
        return "HotRodStoreConfiguration{attributes=" + this.attributes + '}';
    }
}
