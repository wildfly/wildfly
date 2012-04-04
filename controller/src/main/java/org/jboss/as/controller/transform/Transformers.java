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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Transformers API for manipulating transformation operations between different versions of application server
 *
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 * @since 7.1.2
 */
public interface Transformers {

    /**
     * Get information about the target.
     *
     * @return the target
     */
    TransformationTarget getTarget();

    /**
     * Transform an operation.
     *
     * @param operation the operation to transform
     * @return the transformed operation
     */
    ModelNode transformOperation(TransformationContext context, ModelNode operation);

    /**
     * Transform given resource at given context
     *
     * @param context  from where resource originates
     * @param resource to transform
     * @return transformed resource, or same if no transformation was needed
     */
    Resource transformResource(TransformationContext context, Resource resource);

    public static class Factory {
        private Factory() {
        }

        public static Transformers create(final TransformationTarget target) {
            return new TransformersImpl(target);
        }

        public static TransformationContext getTransformationContext(OperationContext context) {
            return new TransformersImpl.DelegateTransformContext(context);
        }
    }
}
