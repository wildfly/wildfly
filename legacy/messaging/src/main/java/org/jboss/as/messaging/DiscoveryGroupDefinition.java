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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_PORT;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
* Discovery group resource definition
*
* @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
*/
public class DiscoveryGroupDefinition extends ModelOnlyResourceDefinition {

   public static final PathElement PATH = PathElement.pathElement(CommonAttributes.DISCOVERY_GROUP);

    public static final SimpleAttributeDefinition REFRESH_TIMEOUT = create("refresh-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(10000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_WAIT_TIMEOUT = create("initial-wait-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(10000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_STACK, JGROUPS_CHANNEL, SOCKET_BINDING, LOCAL_BIND_ADDRESS, GROUP_ADDRESS, GROUP_PORT,
            REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT
    };

    private static final OperationValidator VALIDATOR = new OperationValidator.AttributeDefinitionOperationValidator(ATTRIBUTES);

    static final DiscoveryGroupDefinition INSTANCE = new DiscoveryGroupDefinition();

    public DiscoveryGroupDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DISCOVERY_GROUP),
                new ModelOnlyAddStepHandler(ATTRIBUTES) {
                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        model.setEmptyObject();

                        VALIDATOR.validateAndSet(operation, model);
                    }
                },
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}
