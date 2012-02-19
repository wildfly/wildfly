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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 25-Oct-2011
 */
public class OSGiFrameworkPropertyWrite extends AbstractWriteAttributeHandler {
    static final OSGiFrameworkPropertyWrite INSTANCE = new OSGiFrameworkPropertyWrite();

    private OSGiFrameworkPropertyWrite() {
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer() || context.getProcessType() == ProcessType.HOST_CONTROLLER;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        String propValue = resolvedValue.asString();
        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.setProperty(propName, propValue);
        }
        return true;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        String propValue = valueToRestore.asString();
        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.setProperty(propName, propValue);
        }
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property.write"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property.value"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}
