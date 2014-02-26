/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Adds the ability to register transformers in a resource definition.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class TransformerResourceDefinition extends SimpleResourceDefinition {

    protected TransformerResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver) {
        super(pathElement, descriptionResolver);
    }

    protected TransformerResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    protected TransformerResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler, final OperationStepHandler removeHandler, final Flag addRestartLevel, final Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
    }

    /**
     * Register the transformers for the resource.
     *
     * @param modelVersion          the model version we're registering
     * @param rootResourceBuilder   the builder for the root resource
     * @param loggingProfileBuilder the builder for the logging profile, {@code null} if the profile was rejected
     */
    public abstract void registerTransformers(KnownModelVersion modelVersion,
                                              ResourceTransformationDescriptionBuilder rootResourceBuilder,
                                              ResourceTransformationDescriptionBuilder loggingProfileBuilder);
}
