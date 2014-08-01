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

package org.jboss.as.messaging.ha;

import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hornetq.core.config.ScaleDownConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ScaleDownAttributes {

    private static final String SCALE_DOWN_DISCOVERY_GROUP_STR = "scale-down-discovery-group";
    private static final String SCALE_DOWN_CONNECTORS_STR = "scale-down-connectors";

    // if the scale-down attribute is not defined, the whole scale down configuration is ignored
    public static SimpleAttributeDefinition SCALE_DOWN = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SCALE_DOWN, BOOLEAN)
            // scale-down attribute is represented with the "enabled" attribute of the "scale-down" XML element
            .setXmlName(CommonAttributes.ENABLED)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition SCALE_DOWN_CLUSTER_NAME = SimpleAttributeDefinitionBuilder.create("scale-down-cluster-name", STRING)
            .setXmlName(CommonAttributes.CLUSTER_NAME)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition SCALE_DOWN_GROUP_NAME = SimpleAttributeDefinitionBuilder.create("scale-down-group-name", STRING)
            .setXmlName(CommonAttributes.GROUP_NAME)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition SCALE_DOWN_DISCOVERY_GROUP_NAME =  SimpleAttributeDefinitionBuilder.create(SCALE_DOWN_DISCOVERY_GROUP_STR, STRING)
            .setXmlName(CommonAttributes.DISCOVERY_GROUP_NAME)
            .setAllowNull(true)
            .setAlternatives(SCALE_DOWN_CONNECTORS_STR)
            .setRestartAllServices()
            .build();

    public static AttributeDefinition SCALE_DOWN_CONNECTORS = new SimpleMapAttributeDefinition.Builder(SCALE_DOWN_CONNECTORS_STR, true)
            .setAttributeMarshaller(new AttributeMarshallers.ConnectorsMarshaller(SCALE_DOWN_CONNECTORS_STR))
            .setAlternatives(SCALE_DOWN_DISCOVERY_GROUP_STR)
            // we use this corrector so that connectors are represented consistently with other messaging resources
            .setCorrector(new ParameterCorrector() {
                /*
                 * https://issues.jboss.org/browse/WFLY-1796
                 *
                 * For backwards compatibility, the connector attribute must be a map where the key is a
                 * connector name and the value is not taken into account (in previous HornetQ versions, the value
                 * was the backup's server connector).
                 *
                 * This is a source of confusion when creating resources with connector: users expect to pass a
                 * list of connectors and this fails as they must pass a map with undefined values.
                 *
                 * This corrector will replace a list with the map expected to populate the model.
                 */
                @Override
                public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                    if (newValue.getType() != ModelType.LIST) {
                        return newValue;
                    } else {
                        ModelNode correctValue = new ModelNode();
                        for (ModelNode node : newValue.asList()) {
                            correctValue.get(node.asString());
                        }
                        return correctValue;
                    }
                }
            })
            .setRestartAllServices()
            .build();

    public static Collection<AttributeDefinition> SCALE_DOWN_ATTRIBUTES =  Collections.unmodifiableList(Arrays.asList(
            SCALE_DOWN,
            SCALE_DOWN_CLUSTER_NAME,
            SCALE_DOWN_GROUP_NAME,
            SCALE_DOWN_DISCOVERY_GROUP_NAME,
            SCALE_DOWN_CONNECTORS
    ));

    static ScaleDownConfiguration addScaleDownConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {

        if (!model.hasDefined(SCALE_DOWN.getName())) {
            return null;
        }

        ScaleDownConfiguration scaleDownConfiguration = new ScaleDownConfiguration();

        scaleDownConfiguration.setEnabled(SCALE_DOWN.resolveModelAttribute(context, model).asBoolean());

        ModelNode clusterName = SCALE_DOWN_CLUSTER_NAME.resolveModelAttribute(context, model);
        if (clusterName.isDefined()) {
            scaleDownConfiguration.setClusterName(clusterName.asString());
        }
        ModelNode groupName = SCALE_DOWN_GROUP_NAME.resolveModelAttribute(context, model);
        if (groupName.isDefined()) {
            scaleDownConfiguration.setGroupName(groupName.asString());
        }
        ModelNode discoveryGroupName = SCALE_DOWN_DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        if (discoveryGroupName.isDefined()) {
            scaleDownConfiguration.setDiscoveryGroup(discoveryGroupName.asString());
        }
        ModelNode connectors = SCALE_DOWN_CONNECTORS.resolveModelAttribute(context, model);
        if (connectors.isDefined()) {
            List<String> connectorNames = new ArrayList<>(connectors.keys());
            scaleDownConfiguration.setConnectors(connectorNames);
        }
        return scaleDownConfiguration;
    }
}
