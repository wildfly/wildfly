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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiCapabilityAdd extends AbstractAddStepHandler {
    static final OSGiCapabilityAdd INSTANCE = new OSGiCapabilityAdd();

    private OSGiCapabilityAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        if (operation.has(ModelConstants.STARTLEVEL)) {
            model.get(ModelConstants.STARTLEVEL).set(operation.get(ModelConstants.STARTLEVEL));
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode slNode = null;
        if (operation.has(ModelConstants.STARTLEVEL)) {
            slNode = operation.get(ModelConstants.STARTLEVEL);
            model.get(ModelConstants.STARTLEVEL).set(slNode);
        }
        final Integer startLevel = (slNode != null ? slNode.asInt() : null);

        String identifier = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CAPABILITY).asString();
        OSGiCapability module = new OSGiCapability(identifier, startLevel);

        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.addCapability(module);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String identifier = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CAPABILITY).asString();
        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.removeCapability(identifier);
        }
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resourceBundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resourceBundle.getString("capability.add"));
            addModelProperties(resourceBundle, node, ModelDescriptionConstants.REQUEST_PROPERTIES);
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }

        private void addModelProperties(ResourceBundle bundle, ModelNode node, String propType) {
            node.get(propType, ModelConstants.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("capability.startlevel"));
            node.get(propType, ModelConstants.STARTLEVEL, ModelDescriptionConstants.TYPE).set(ModelType.INT);
            node.get(propType, ModelConstants.STARTLEVEL, ModelDescriptionConstants.REQUIRED).set(false);
        }
    };
}
