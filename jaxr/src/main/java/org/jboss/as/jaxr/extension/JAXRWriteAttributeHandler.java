/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler responsible for adding JAXR attributes to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Nov-2011
 */
class JAXRWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    static AttributeDefinition DATASOURCE_ATTRIBUTE = new BindingAttributeDefinition (ModelConstants.DATASOURCE);
    static AttributeDefinition CONNECTIONFACTORY_ATTRIBUTE = new BindingAttributeDefinition (ModelConstants.CONNECTIONFACTORY);
    static AttributeDefinition DROPONSTART_ATTRIBUTE = new BooleanAttributeDefinition(ModelConstants.DROPONSTART, false);
    static AttributeDefinition CREATEONSTART_ATTRIBUTE = new BooleanAttributeDefinition(ModelConstants.CREATEONSTART, false);
    static AttributeDefinition DROPONSTOP_ATTRIBUTE = new BooleanAttributeDefinition(ModelConstants.DROPONSTOP, false);

    void registerAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(CONNECTIONFACTORY_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(DATASOURCE_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(DROPONSTART_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(CREATEONSTART_ATTRIBUTE, null, this);
        registry.registerReadWriteAttribute(DROPONSTOP_ATTRIBUTE, null, this);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        JAXRConfiguration config = JAXRConfiguration.INSTANCE;
        if (attributeName.equals(ModelConstants.CONNECTIONFACTORY)) {
            config.setConnectionFactoryBinding(resolvedValue.asString());
        }
        if (attributeName.equals(ModelConstants.DATASOURCE)) {
            config.setDataSourceBinding(resolvedValue.asString());
        }
        if (attributeName.equals(ModelConstants.DROPONSTART)) {
            config.setDropOnStart(resolvedValue.asBoolean());
        }
        if (attributeName.equals(ModelConstants.CREATEONSTART)) {
            config.setCreateOnStart(resolvedValue.asBoolean());
        }
        if (attributeName.equals(ModelConstants.DROPONSTOP)) {
            config.setDropOnStop(resolvedValue.asBoolean());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        JAXRConfiguration config = JAXRConfiguration.INSTANCE;
        if (attributeName.equals(ModelConstants.CONNECTIONFACTORY)) {
            config.setConnectionFactoryBinding(valueToRestore.asString());
        }
        if (attributeName.equals(ModelConstants.DATASOURCE)) {
            config.setDataSourceBinding(valueToRestore.asString());
        }
        if (attributeName.equals(ModelConstants.DROPONSTART)) {
            config.setDropOnStart(valueToRestore.asBoolean());
        }
        if (attributeName.equals(ModelConstants.CREATEONSTART)) {
            config.setCreateOnStart(valueToRestore.asBoolean());
        }
        if (attributeName.equals(ModelConstants.DROPONSTOP)) {
            config.setDropOnStop(valueToRestore.asBoolean());
        }
    }

    private static class BindingAttributeDefinition extends SimpleAttributeDefinition {
        BindingAttributeDefinition(String name) {
            super(name, ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);
        }
    }

    private static class BooleanAttributeDefinition extends SimpleAttributeDefinition {
        BooleanAttributeDefinition(String name, boolean defaultValue) {
            super(name, new ModelNode().set(defaultValue), ModelType.BOOLEAN, false, AttributeAccess.Flag.RESTART_ALL_SERVICES);
        }
    }
}
