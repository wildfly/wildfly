/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.services.net;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Represents a {@link org.jboss.as.controller.ResourceDefinition} for a outbound-socket-binding
 *
 * @author Jaikiran Pai
 */
public abstract class OutboundSocketBindingResourceDefinition extends SimpleResourceDefinition {

    /*public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1)).build();*/

    public static final SimpleAttributeDefinition SOURCE_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SOURCE_PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final SimpleAttributeDefinition SOURCE_INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SOURCE_INTERFACE, ModelType.STRING, true)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static final SimpleAttributeDefinition FIXED_SOURCE_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FIXED_SOURCE_PORT, ModelType.BOOLEAN, true)
            .setAllowExpression(true).setDefaultValue(new ModelNode().set(false)).build();

    public static final SimpleAttributeDefinition[] ATTRIBUTES = {SOURCE_PORT,SOURCE_INTERFACE,FIXED_SOURCE_PORT};
    private final boolean remoteDestination;

    protected OutboundSocketBindingResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                                      final OperationStepHandler addHandler, final OperationStepHandler removeHandler, final boolean remoteDestination) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.remoteDestination = remoteDestination;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        for (SimpleAttributeDefinition ad:ATTRIBUTES){
            resourceRegistration.registerReadWriteAttribute(ad, null, new OutboundSocketBindingWriteHandler(ad, false));
        }
    }
}
