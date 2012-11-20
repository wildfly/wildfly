/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class InfinispanSubsystemTransformer_1_3 extends AbstractSubsystemTransformer {

    public InfinispanSubsystemTransformer_1_3() {
        super("infinispan");
    }

    @Override
    public ModelNode transformModel(TransformationContext context, ModelNode model) {
        for (Property p : model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
            transformCache(model, p.getName(), ModelKeys.LOCAL_CACHE);
            transformCache(model, p.getName(), ModelKeys.DISTRIBUTED_CACHE);
            transformCache(model, p.getName(), ModelKeys.REPLICATED_CACHE);
            transformCache(model, p.getName(), ModelKeys.INVALIDATION_CACHE);
        }
        return model;
    }

    private void transformCache(final ModelNode model, final String containerName, final String cacheType) {
        if (!(model.get(ModelKeys.CACHE_CONTAINER).has(containerName) && model.get(ModelKeys.CACHE_CONTAINER, containerName).has(cacheType))) {
            return;
        }
        ModelNode cacheHolder = model.get(ModelKeys.CACHE_CONTAINER, containerName, cacheType);
        if (!cacheHolder.isDefined()) { return; }
        for (Property c : cacheHolder.asPropertyList()) {
            ModelNode cache = c.getValue();

            if (cache.has(ModelKeys.INDEXING_PROPERTIES)) {
                cache.remove(ModelKeys.INDEXING_PROPERTIES);
            }
            if (cache.has(ModelKeys.SEGMENTS)) {
                cache.remove(ModelKeys.SEGMENTS);
            }
            if (cache.has(ModelKeys.VIRTUAL_NODES)) {
                cache.remove(ModelKeys.VIRTUAL_NODES);
            }

            model.get(ModelKeys.CACHE_CONTAINER, containerName, cacheType, c.getName()).set(cache);
        }
    }
}
