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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the EJB3 subsystem's root management resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3SubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final EJB3SubsystemRootResourceDefinition INSTANCE = new EJB3SubsystemRootResourceDefinition();

    public static final SimpleAttributeDefinition DEFAULT_SLSB_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL, ModelType.STRING, true)
                    .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition DEFAULT_MDB_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL, ModelType.STRING, true)
                    .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition DEFAULT_RESOURCE_ADAPTER_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME, ModelType.STRING, true)
                    .setDefaultValue(new ModelNode().set("hornetq-ra"))
                    .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(5000)) // TODO: this should come from component description
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, false, false))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(5000)) // TODO: this should come from component description
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, false, false))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition DEFAULT_SFSB_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SFSB_CACHE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();
    public static final SimpleAttributeDefinition DEFAULT_CLUSTERED_SFSB_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE, ModelType.STRING, true)
                    .setAllowExpression(true)
//                    .setAllowNull(true)
                    .build();

    private EJB3SubsystemRootResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                EJB3Extension.getResourceDescriptionResolver(EJB3Extension.SUBSYSTEM_NAME),
                EJB3SubsystemAdd.INSTANCE, EJB3SubsystemRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SFSB_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.SFSB_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_CLUSTERED_SFSB_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.CLUSTERED_SFSB_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SLSB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.SLSB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_MDB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.MDB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_RESOURCE_ADAPTER_NAME, null, DefaultResourceAdapterWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT, null, DefaultSingletonBeanAccessTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, null, DefaultStatefulBeanAccessTimeoutWriteHandler.INSTANCE);
    }
}
