/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CustomStoreResourceDefinition.Attribute.CLASS;

import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.PathAddress;

/**
 * @author Paul Ferraro
 */
public class CustomStoreBuilder<C extends AbstractStoreConfiguration, B extends AbstractStoreConfigurationBuilder<C, B>> extends StoreBuilder<C, B> {

    @SuppressWarnings("unchecked")
    CustomStoreBuilder(PathAddress cacheAddress) {
        super(cacheAddress, (context, model) -> {
            String className = CLASS.resolveModelAttribute(context, model).asString();
            try {
                return (B) new ConfigurationBuilder().persistence().addStore(CustomStoreBuilder.class.getClassLoader().loadClass(className).asSubclass(StoreConfigurationBuilder.class));
            } catch (ClassNotFoundException | ClassCastException e) {
                throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
            }
        });
    }

    @Override
    public void accept(B builder) {
        // Nothing to configure
    }
}
