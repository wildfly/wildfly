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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.TimeUnitValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the strict-max-bean-pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StrictMaxPoolResourceDefinition extends SimpleResourceDefinition {

    public static final StrictMaxPoolResourceDefinition INSTANCE = new StrictMaxPoolResourceDefinition();

    public static final SimpleAttributeDefinition MAX_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_POOL_SIZE, ModelType.INT, true)
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_MAX_POOL_SIZE))
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition INSTANCE_ACQUISITION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.INSTANCE_AQUISITION_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_TIMEOUT))
                    .setAllowExpression(true)
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition INSTANCE_ACQUISITION_TIMEOUT_UNIT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.INSTANCE_AQUISITION_TIMEOUT_UNIT.getLocalName())
                    .setValidator(TimeUnitValidator.ANY_OPTIONAL)
                    .setDefaultValue(new ModelNode().set(StrictMaxPoolConfig.DEFAULT_TIMEOUT_UNIT.name()))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES ;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(MAX_POOL_SIZE.getName(), MAX_POOL_SIZE);
        map.put(INSTANCE_ACQUISITION_TIMEOUT.getName(), INSTANCE_ACQUISITION_TIMEOUT);
        map.put(INSTANCE_ACQUISITION_TIMEOUT_UNIT.getName(), INSTANCE_ACQUISITION_TIMEOUT_UNIT);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    private StrictMaxPoolResourceDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL),
                StrictMaxPoolAdd.INSTANCE, StrictMaxPoolRemove.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, StrictMaxPoolWriteHandler.INSTANCE);
        }
    }
}
