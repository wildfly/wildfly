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

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;

/**
 * @author David Bosschaert
 */
public class OSGiModuleAdd implements OperationStepHandler, DescriptionProvider {
    static final OSGiModuleAdd INSTANCE = new OSGiModuleAdd();

    private OSGiModuleAdd() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        ModelNode model = resource.getModel();

        ModelNode slNode = null;
        if (operation.has(CommonAttributes.STARTLEVEL)) {
            slNode = operation.get(CommonAttributes.STARTLEVEL);
            model.get(CommonAttributes.STARTLEVEL).set(slNode);
        }
        final Integer startLevel = (slNode != null ? slNode.asInt() : null);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String identifier = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(CommonAttributes.MODULE).asString();
                    OSGiModule module = new OSGiModule(ModuleIdentifier.fromString(identifier), startLevel);
                    SubsystemState stateService = (SubsystemState) context.getServiceRegistry(true).getRequiredService(SubsystemState.SERVICE_NAME).getValue();
                    stateService.addModule(module);

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        stateService.removeModule(identifier);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ResourceBundle resourceBundle = OSGiSubsystemProviders.getResourceBundle(locale);

        ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
        node.get(ModelDescriptionConstants.DESCRIPTION).set(resourceBundle.getString("module.add"));
        addModelProperties(resourceBundle, node, ModelDescriptionConstants.REQUEST_PROPERTIES);
        node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void addModelProperties(ResourceBundle bundle, ModelNode node, String propType) {
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION)
            .set(bundle.getString("module.startlevel"));
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.TYPE).set(ModelType.INT);
        node.get(propType, CommonAttributes.STARTLEVEL, ModelDescriptionConstants.REQUIRED).set(false);
    }

    /**
     * Create an "add" operation using the existing model
     */
    static ModelNode getAddOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        if (existing.hasDefined(CommonAttributes.STARTLEVEL)) {
            op.get(CommonAttributes.STARTLEVEL).set(existing.get(CommonAttributes.STARTLEVEL));
        }

        return op;
    }
}