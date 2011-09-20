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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiModuleAdd extends AbstractAddStepHandler implements DescriptionProvider {
    static final OSGiModuleAdd INSTANCE = new OSGiModuleAdd();

    private OSGiModuleAdd() {
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.SERVER || context.getType() == OperationContext.Type.HOST;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(CommonAttributes.MODULE).set(operation.get(CommonAttributes.MODULE));
        if (operation.has(CommonAttributes.STARTLEVEL)) {
            model.get(CommonAttributes.STARTLEVEL).set(operation.get(CommonAttributes.STARTLEVEL));
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode slNode = null;
        if (operation.has(CommonAttributes.STARTLEVEL)) {
            slNode = operation.get(CommonAttributes.STARTLEVEL);
            model.get(CommonAttributes.STARTLEVEL).set(slNode);
        }
        final Integer startLevel = (slNode != null ? slNode.asInt() : null);

        String identifier = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(CommonAttributes.MODULE).asString();
        OSGiModule module = new OSGiModule(ModuleIdentifier.fromString(identifier), startLevel);

        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.addModule(module);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String identifier = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(CommonAttributes.MODULE).asString();
        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.removeModule(identifier);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode node = new ModelNode();
        ResourceBundle resourceBundle = OSGiSubsystemProviders.getResourceBundle(locale);
        node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
        node.get(ModelDescriptionConstants.DESCRIPTION).set(resourceBundle.getString("module.add"));
        addModelProperties(resourceBundle, node, ModelDescriptionConstants.REQUEST_PROPERTIES);
        node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void addModelProperties(ResourceBundle bundle, ModelNode node, String propType) {
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("module.startlevel"));
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.TYPE).set(ModelType.INT);
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.REQUIRED).set(false);
    }

    static ModelNode getAddOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        if (existing.hasDefined(CommonAttributes.STARTLEVEL)) {
            op.get(CommonAttributes.STARTLEVEL).set(existing.get(CommonAttributes.STARTLEVEL));
        }
        return op;
    }
}