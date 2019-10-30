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

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.mod_cluster.LoadMetricResourceDefinition.SharedAttribute;

/**
 * @author Radoslav Husar
 */
public class CustomLoadMetricResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String loadMetric) {
        return PathElement.pathElement("custom-load-metric", loadMetric);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CLASS("class", ModelType.STRING),
        MODULE("module", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setDefaultValue(new ModelNode("org.wildfly.extension.mod_cluster"))
                        .setRequired(false)
                        .setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build())
                        ;
            }
        },
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
                    .setRestartAllServices()
            ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    CustomLoadMetricResourceDefinition() {
        super(WILDCARD_PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(LoadMetricResourceDefinition.WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(SharedAttribute.class)
                .addAttributes(Attribute.class)
                ;

        new ReloadRequiredResourceRegistration(descriptor).register(registration);

        return registration;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (ModClusterModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(Attribute.MODULE.getDefinition().getDefaultValue()), Attribute.MODULE.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.MODULE.getDefinition().getDefaultValue()), Attribute.MODULE.getDefinition())
                    .end();
        }
    }
}
