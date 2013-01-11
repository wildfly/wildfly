/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelType;


/**
 * The JAXR subsystem root resource
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Nov-2011
 */
class JAXRSubsystemRootResource extends SimpleResourceDefinition {
    static SimpleAttributeDefinition CONNECTION_FACTORY_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(ModelConstants.CONNECTION_FACTORY, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    static SimpleAttributeDefinition CONNECTION_FACTORY_IMPL_ATTRIBUTE =
            new SimpleAttributeDefinition(ModelConstants.CONNECTION_FACTORY_IMPL, ModelType.STRING, true);

    static AttributeDefinition[] ATTRIBUTES = {CONNECTION_FACTORY_ATTRIBUTE,CONNECTION_FACTORY_IMPL_ATTRIBUTE};

    private final JAXRConfiguration config;

    JAXRSubsystemRootResource(JAXRConfiguration config) {
        super(JAXRExtension.SUBSYSTEM_PATH,
                JAXRExtension.getResolver(),
                new JAXRSubsystemAdd(config),
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
        this.config = config;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        JAXRWriteAttributeHandler writeHandler = new JAXRWriteAttributeHandler(config);

        registry.registerReadWriteAttribute(CONNECTION_FACTORY_ATTRIBUTE, null, writeHandler);
        registry.registerReadWriteAttribute(CONNECTION_FACTORY_IMPL_ATTRIBUTE, null, writeHandler);
    }

    static void registerTransformerers(SubsystemRegistration subsystem) {

        ModelVersion subsystem110 = ModelVersion.create(1, 1);

        RejectExpressionValuesTransformer rejectTransformer = new RejectExpressionValuesTransformer(CONNECTION_FACTORY_ATTRIBUTE);
        final TransformersSubRegistration transformers110 = subsystem.registerModelTransformers(subsystem110, rejectTransformer);
        transformers110.registerOperationTransformer(ADD, rejectTransformer);
        transformers110.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectTransformer.getWriteAttributeTransformer());
    }
}
