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


import static org.jboss.dmr.ModelType.EXPRESSION;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;

/**
 * Definition for resource at address /subsystem=modcluster/proxy=X/load-provider=dynamic
 * and its legacy alias /subsystem=modcluster/proxy=X/dynamic-load-provider=configuration
 *
 * @author Radoslav Husar
 */
public class DynamicLoadProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement LEGACY_PATH = PathElement.pathElement("dynamic-load-provider", "configuration");
    public static final PathElement PATH = PathElement.pathElement("load-provider", "dynamic");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DECAY("decay", ModelType.DOUBLE, new ModelNode((double) DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR)),
        HISTORY("history", ModelType.INT, new ModelNode(DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY)),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    DynamicLoadProviderResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                ;

        new LoadMetricResourceDefinition().register(registration);
        new CustomLoadMetricResourceDefinition().register(registration);

        new ReloadRequiredResourceRegistration(descriptor).register(registration);

        return registration;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = ModClusterModel.VERSION_6_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        if (ModClusterModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                            if (attributeValue.isDefined() && attributeValue.getType() != EXPRESSION) {
                                attributeValue.set((int) Math.ceil(attributeValue.asDouble()));
                            }
                        }
                    }, Attribute.DECAY.getDefinition())
                    .end();
        }

        LoadMetricResourceDefinition.buildTransformation(version, builder);
        CustomLoadMetricResourceDefinition.buildTransformation(version, builder);
    }

}
