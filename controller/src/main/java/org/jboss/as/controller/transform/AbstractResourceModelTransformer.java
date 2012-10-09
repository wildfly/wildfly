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

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.LegacyResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Basic transformer working on the basis of the model rather the resource. This however
 * requires the original ResourceDefinition for conversion.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractResourceModelTransformer implements ResourceTransformer {

    private final ResourceDefinitionLoader loader;
    protected AbstractResourceModelTransformer(ResourceDefinitionLoader loader) {
        this.loader = loader;
    }

    /**
     * Transform the model.
     *
     * @param context the transformation context
     * @param model the model to transform
     * @return the transformed model
     */
    protected abstract ModelNode transformModel(TransformationContext context, ModelNode model);

    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource) {
        // Transform the model recursively
        final ModelNode recursive = Resource.Tools.readModel(resource);
        final ModelNode result = transformModel(context, recursive);
        // Create the target registration based on the old resource definition
        final TransformationTarget target = context.getTarget();
        final ResourceDefinition definition = loader.load(target);
        final ManagementResourceRegistration targetDefinition = ManagementResourceRegistration.Factory.create(definition);
        final Resource transformed = TransformationUtils.modelToResource(targetDefinition, result, false);
        // Add the model recursively
        context.addTransformedRecursiveResource(PathAddress.EMPTY_ADDRESS, transformed);
    }

    public interface ResourceDefinitionLoader {

        /**
         * Load the resource definition.
         *
         * @param target the target
         * @return the resource definition
         */
         ResourceDefinition load(TransformationTarget target);

    }

    public abstract static class AbstractDefinitionLoader implements ResourceDefinitionLoader {

        /**
         * Open the stream to the resource definition model.
         *
         * @param target the transformation target
         * @return the stream
         * @throws IOException
         */
        abstract InputStream openStream(TransformationTarget target) throws IOException;

        @Override
        public ResourceDefinition load(TransformationTarget target) {
            ModelNode model = null;
            try {
                final InputStream is = openStream(target);
                try {
                    if (is == null) {
                        return null;
                    }
                    model = ModelNode.fromStream(is);
                } catch (IOException e) {
                    ControllerLogger.ROOT_LOGGER.cannotReadTargetDefinition(e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            //
                        }
                    }
                }
            } catch (IOException e) {
                ControllerLogger.ROOT_LOGGER.cannotReadTargetDefinition(e);
            }
            if(model != null) {
                return new LegacyResourceDefinition(model);
            } else {
                return null;
            }
        }
    }

}
