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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for /subsystem=jgroups/channel=* resources
 *
 * @author Paul Ferraro
 */
public class ChannelResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.CHANNEL, name);
    }

    // attributes
    public static SimpleAttributeDefinition STACK = new SimpleAttributeDefinitionBuilder(ModelKeys.STACK, ModelType.STRING, true)
            .setXmlName(Attribute.STACK.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode("org.wildfly.clustering.server"))
            .build();

    static AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { STACK, MODULE };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform yet
    }

    private final boolean allowRuntimeOnlyRegistration;

    ChannelResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(ModelKeys.CHANNEL), new ChannelAddHandler(allowRuntimeOnlyRegistration), new ChannelRemoveHandler(allowRuntimeOnlyRegistration));
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new ForkResourceDefinition(this.allowRuntimeOnlyRegistration));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            OperationStepHandler metricsHandler = new ChannelMetricsHandler();
            for (ChannelMetric metric: ChannelMetric.values()) {
                registration.registerMetric(metric.getDefinition(), metricsHandler);
            }
        }
    }
}
