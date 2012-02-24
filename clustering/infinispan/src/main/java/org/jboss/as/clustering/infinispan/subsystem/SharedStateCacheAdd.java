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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public abstract class SharedStateCacheAdd extends ClusteredCacheAdd {

    SharedStateCacheAdd(CacheMode mode) {
        super(mode);
    }

    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies)
            throws OperationFailedException{

        // process the basic clustered configuration
        super.processModelNode(context, containerName, cache, builder, dependencies);

        // state transfer is a child resource
        if (cache.hasDefined(ModelKeys.STATE_TRANSFER) && cache.get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME).isDefined()) {
            ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);

            final boolean enabled = CommonAttributes.ENABLED.resolveModelAttribute(context, stateTransfer).asBoolean();
            final long timeout = CommonAttributes.TIMEOUT.resolveModelAttribute(context, stateTransfer).asLong();
            final int chunkSize = CommonAttributes.CHUNK_SIZE.resolveModelAttribute(context, stateTransfer).asInt();

            builder.clustering().stateTransfer().fetchInMemoryState(enabled);
            builder.clustering().stateTransfer().timeout(timeout);
            builder.clustering().stateTransfer().chunkSize(chunkSize);
        }
    }
}
