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
        super(1, 3, 0);
    }

    @Override
    public ModelNode transformModel(TransformationContext context, ModelNode model) {
        for (Property p : model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
            ModelNode container = p.getValue();
            container.remove(ModelKeys.MODULE);
            for (Property cache : container.asPropertyList()) {
                transformCache(cache.getValue());
            }
            model.get(ModelKeys.CACHE_CONTAINER, p.getName()).set(container);
        }
        return model;
    }

    private void transformCache(final ModelNode cache) {
       cache.remove(ModelKeys.INDEXING_PROPERTIES);
    }
}
