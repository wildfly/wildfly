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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSource;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class FilePassivationStoreWriteHandler extends PassivationStoreWriteHandler<NonClusteredBackingCacheEntryStoreSource<?, ?, ?>> {

    FilePassivationStoreWriteHandler(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected AttributeDefinition getMaxSizeDefinition() {
        return FilePassivationStoreResourceDefinition.MAX_SIZE;
    }

    @Override
    protected void apply(NonClusteredBackingCacheEntryStoreSource<?, ?, ?> config, OperationContext context, String attributeName, ModelNode model) throws OperationFailedException {
        if (FilePassivationStoreResourceDefinition.RELATIVE_TO.getName().equals(attributeName)) {
            String relativeTo = FilePassivationStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model).asString();
            config.setRelativeTo(relativeTo);
        } else if (FilePassivationStoreResourceDefinition.GROUPS_PATH.getName().equals(attributeName)) {
            String path = FilePassivationStoreResourceDefinition.GROUPS_PATH.resolveModelAttribute(context, model).toString();
            config.setGroupDirectoryName(path);
        } else if (FilePassivationStoreResourceDefinition.SESSIONS_PATH.getName().equals(attributeName)) {
            String path = FilePassivationStoreResourceDefinition.SESSIONS_PATH.resolveModelAttribute(context, model).toString();
            config.setSessionDirectoryName(path);
        } else if (FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.getName().equals(attributeName)) {
            int count = FilePassivationStoreResourceDefinition.SUBDIRECTORY_COUNT.resolveModelAttribute(context, model).asInt();
            config.setSubdirectoryCount(count);
        }
    }
}
