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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author David Bosschaert
 */
public class OSGiPropertyAdd implements OperationStepHandler, DescriptionProvider {
    static final OSGiPropertyAdd INSTANCE = new OSGiPropertyAdd();

    private OSGiPropertyAdd() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        ModelNode model = resource.getModel();

        final ModelNode propVal = operation.get(CommonAttributes.VALUE);
        model.get(CommonAttributes.VALUE).set(propVal);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(CommonAttributes.PROPERTY).asString();

                    SubsystemState stateService = (SubsystemState) context.getServiceRegistry(true).getRequiredService(SubsystemState.SERVICE_NAME).getValue();
                    Object oldVal = stateService.setProperty(propName, propVal.asString());
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        stateService.setProperty(propName, oldVal);
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
        node.get(ModelDescriptionConstants.DESCRIPTION).set(resourceBundle.getString("property.add"));
        addModelProperties(resourceBundle, node, ModelDescriptionConstants.REQUEST_PROPERTIES);
        node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    static void addModelProperties(ResourceBundle bundle, ModelNode node, String propType) {
        node.get(propType, CommonAttributes.VALUE, ModelDescriptionConstants.DESCRIPTION)
            .set(bundle.getString("property.value"));
        node.get(propType, CommonAttributes.VALUE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        node.get(propType, CommonAttributes.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
    }

    /**
     * Create an "add" operation using the existing model
     */
    static ModelNode getAddOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        op.get(CommonAttributes.VALUE).set(existing.get(CommonAttributes.VALUE));

        return op;
    }
}
