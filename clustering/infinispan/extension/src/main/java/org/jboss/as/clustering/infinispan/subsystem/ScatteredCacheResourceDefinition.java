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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class ScatteredCacheResourceDefinition extends SegmentedCacheResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("scattered-cache", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        INVALIDATION_BATCH_SIZE("invalidation-batch-size", ModelType.INT, new ModelNode(128), builder -> builder.setValidator(new IntRangeValidatorBuilder().min(0).configure(builder).build())),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            parent.rejectChildResource(WILDCARD_PATH);
        } else {
            ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

            SegmentedCacheResourceDefinition.buildTransformation(version, builder);
        }
    }

    /**
     * @param path
     * @param descriptorConfigurator
     * @param handler
     */
    ScatteredCacheResourceDefinition() {
        super(WILDCARD_PATH, descriptor -> descriptor.addAttributes(Attribute.class), new ClusteredCacheServiceHandler(ScatteredCacheBuilder::new));
    }
}
