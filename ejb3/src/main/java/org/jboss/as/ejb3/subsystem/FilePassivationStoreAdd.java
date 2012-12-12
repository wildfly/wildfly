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
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Paul Ferraro
 */
public class FilePassivationStoreAdd extends PassivationStoreAdd {

    public FilePassivationStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        NonClusteredBackingCacheEntryStoreSourceService<?, ?, ?> service = new NonClusteredBackingCacheEntryStoreSourceService<Serializable, Cacheable<Serializable>, Serializable>(name);
        NonClusteredBackingCacheEntryStoreSource<?, ?, ?> source = service.getValue();
        ModelNode relativeToModel = FilePassivationStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, operation);
        ModelNode groupsPath = FilePassivationStoreResourceDefinition.GROUPS_PATH.resolveModelAttribute(context, operation);
        ModelNode sessionsPath = FilePassivationStoreResourceDefinition.SESSIONS_PATH.resolveModelAttribute(context, operation);
        ModelNode subdirectoryCount = FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.resolveModelAttribute(context, operation);
        if (relativeToModel.isDefined()) {
            source.setRelativeTo(relativeToModel.asString());
        }
        if (groupsPath.isDefined()) {
            source.setGroupDirectoryName(groupsPath.asString());
        }
        if (sessionsPath.isDefined()) {
            source.setSessionDirectoryName(sessionsPath.asString());
        }
        if (subdirectoryCount.isDefined()) {
            source.setSubdirectoryCount(subdirectoryCount.asInt());
        }
        return Collections.<ServiceController<?>>singleton(this.installBackingCacheEntryStoreSourceService(service, context, model, verificationHandler));
    }
}
