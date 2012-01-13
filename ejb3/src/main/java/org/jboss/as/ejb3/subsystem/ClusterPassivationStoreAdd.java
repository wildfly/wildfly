/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.io.Serializable;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ClusterPassivationStoreAdd extends PassivationStoreAdd {

    public ClusterPassivationStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected BackingCacheEntryStoreSourceService<?, ?, ?, ?> createService(ModelNode model) {
        String name = model.require(EJB3SubsystemModel.NAME).asString();
        ClusteredBackingCacheEntryStoreSourceService<?, ?, ?> service = new ClusteredBackingCacheEntryStoreSourceService<Serializable, Cacheable<Serializable>, Serializable>(name);
        ClusteredBackingCacheEntryStoreConfig config = service.getValue();
        if (model.hasDefined(EJB3SubsystemModel.CACHE_CONTAINER)) {
            config.setCacheContainer(model.get(EJB3SubsystemModel.CACHE_CONTAINER).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.BEAN_CACHE)) {
            config.setBeanCache(model.get(EJB3SubsystemModel.BEAN_CACHE).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE)) {
            config.setBeanCache(model.get(EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE)) {
            config.setPassivateEventsOnReplicate(model.get(EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE).asBoolean());
        }
        return service;
    }
}
