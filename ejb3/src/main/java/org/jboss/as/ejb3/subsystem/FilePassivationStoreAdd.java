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
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSourceService;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class FilePassivationStoreAdd extends PassivationStoreAdd {

    public FilePassivationStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected BackingCacheEntryStoreSourceService<?, ?, ?, ?> createService(ModelNode model) {
        String name = model.require(EJB3SubsystemModel.NAME).asString();
        NonClusteredBackingCacheEntryStoreSourceService<?, ?, ?> service = new NonClusteredBackingCacheEntryStoreSourceService<Serializable, Cacheable<Serializable>, Serializable>(name);
        NonClusteredBackingCacheEntryStoreSource<?, ?, ?> source = service.getValue();
        if (model.hasDefined(EJB3SubsystemModel.RELATIVE_TO)) {
            source.setRelativeTo(model.get(EJB3SubsystemModel.RELATIVE_TO).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.GROUPS_PATH)) {
            source.setGroupDirectoryName(model.get(EJB3SubsystemModel.GROUPS_PATH).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.SESSIONS_PATH)) {
            source.setSessionDirectoryName(model.get(EJB3SubsystemModel.SESSIONS_PATH).asString());
        }
        if (model.hasDefined(EJB3SubsystemModel.SUBDIRECTORY_COUNT)) {
            source.setSubdirectoryCount(model.get(EJB3SubsystemModel.SUBDIRECTORY_COUNT).asInt());
        }
        return service;
    }
}
