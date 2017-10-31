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

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.TimeUnitValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
@Deprecated
public abstract class LegacyPassivationStoreResourceDefinition extends SimpleResourceDefinition {

    static final ModelVersion DEPRECATED_VERSION = ModelVersion.create(2, 0, 0);

    @Deprecated
    static final SimpleAttributeDefinition IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IDLE_TIMEOUT, ModelType.LONG, true)
            .setXmlName(EJB3SubsystemXMLAttribute.IDLE_TIMEOUT.getLocalName())
            .setDefaultValue(new ModelNode().set(300))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition IDLE_TIMEOUT_UNIT = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IDLE_TIMEOUT_UNIT, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.IDLE_TIMEOUT_UNIT.getLocalName())
            .setValidator(new TimeUnitValidator(true,true))
            .setDefaultValue(new ModelNode().set(TimeUnit.SECONDS.name()))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setAllowExpression(true)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinitionBuilder MAX_SIZE_BUILDER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_SIZE, ModelType.INT, true)
            .setXmlName(EJB3SubsystemXMLAttribute.MAX_SIZE.getLocalName())
            .setDefaultValue(new ModelNode().set(100000))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
    ;

    private final PassivationStoreWriteHandler writeHandler;
    private final AttributeDefinition[] attributes;

    LegacyPassivationStoreResourceDefinition(String element, OperationStepHandler addHandler, OperationStepHandler removeHandler, OperationEntry.Flag addRestartLevel, OperationEntry.Flag removeRestartLevel, PassivationStoreWriteHandler writeHandler, AttributeDefinition... attributes) {
        super(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element), addHandler, removeHandler, addRestartLevel, removeRestartLevel, new DeprecationData(DEPRECATED_VERSION));
        this.writeHandler = new PassivationStoreWriteHandler(attributes);
        this.attributes = attributes;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition definition: this.attributes) {
            resourceRegistration.registerReadWriteAttribute(definition, null, this.writeHandler);
        }
    }

}
