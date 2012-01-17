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
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public abstract class SharedStateCacheAdd extends ClusteredCacheAdd {

    SharedStateCacheAdd(CacheMode mode) {
        super(mode);
    }

    @Override
    void processModelNode(ModelNode cache, ConfigurationBuilder builder, List<Dependency<?>> dependencies) {
        // process the basic clustered configuration
        super.processModelNode(cache, builder, dependencies);

        // state transfer is a child resource
        if (cache.hasDefined(ModelKeys.SINGLETON) && cache.get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER).isDefined()) {
            ModelNode stateTransfer = cache.get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER);

            if (stateTransfer.hasDefined(ModelKeys.ENABLED)) {
                builder.clustering().stateTransfer().fetchInMemoryState(stateTransfer.get(ModelKeys.ENABLED).asBoolean());
            }
            if (stateTransfer.hasDefined(ModelKeys.TIMEOUT)) {
                builder.clustering().stateTransfer().timeout(stateTransfer.get(ModelKeys.TIMEOUT).asLong());
            }
            if (stateTransfer.hasDefined(ModelKeys.CHUNK_SIZE)) {
                builder.clustering().stateTransfer().chunkSize(stateTransfer.get(ModelKeys.CHUNK_SIZE).asInt());
            }
        }
    }
}
