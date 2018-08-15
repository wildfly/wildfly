/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for resource at address /subsystem=modcluster/proxy=X/load-provider=simple
 *
 * @author Radoslav Husar
 */
public class SimpleLoadProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement PATH = PathElement.pathElement("load-provider", "simple");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        FACTOR("factor", ModelType.INT, new ModelNode(1), new IntRangeValidator(0, 100, true, true)),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setValidator(validator)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SimpleLoadProviderResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                ;

        new ReloadRequiredResourceRegistration(descriptor).register(registration);

        return registration;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = ModClusterModel.VERSION_6_0_0.requiresTransformation(version) ? parent.addChildResource(PATH) : parent.addChildResource(PATH);

        if (ModClusterModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.ADD, new org.jboss.as.controller.transform.OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
                    PathAddress parentAddress = address.getParent();
                    ModelNode value = operation.get(Attribute.FACTOR.getName());
                    ModelNode transformedOperation = Operations.createWriteAttributeOperation(parentAddress, ProxyConfigurationResourceDefinition.DeprecatedAttribute.SIMPLE_LOAD_PROVIDER, value);
                    return new TransformedOperation(transformedOperation, OperationResultTransformer.ORIGINAL_RESULT);
                }
            });
        }

    }
}
