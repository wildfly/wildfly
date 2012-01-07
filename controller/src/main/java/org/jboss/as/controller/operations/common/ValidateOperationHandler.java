/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Validates an operation
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ValidateOperationHandler implements OperationStepHandler, DescriptionProvider {

    public static ValidateOperationHandler INSTANCE = new ValidateOperationHandler(false);
    public static ValidateOperationHandler SLAVE_HC_INSTANCE = new ValidateOperationHandler(true);

    public static String OPERATION_NAME = VALIDATE_OPERATION;

    private final boolean slave;

    private ValidateOperationHandler(boolean slave) {
        this.slave = slave;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode op = operation.require(VALUE);
        PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        if (slave) {
            op = op.clone();
            //Get rid of the initial host element
            if (addr.size() > 0 && addr.getElement(0).getKey().equals(HOST)) {
                addr = addr.subAddress(1);
                op.get(OP_ADDR).set(addr.toModelNode());
            }
        }


        ProxyOperationAddressTranslator translator = null;
        ImmutableManagementResourceRegistration proxyReg = null;
        PathAddress proxyAddr = PathAddress.EMPTY_ADDRESS;
        for (PathElement element : addr) {
            proxyAddr = proxyAddr.append(element);
            ImmutableManagementResourceRegistration reg = context.getResourceRegistration().getSubModel(proxyAddr);
            if (reg.isRemote()) {
                translator = element.getKey().equals(SERVER) ? ProxyOperationAddressTranslator.SERVER : ProxyOperationAddressTranslator.HOST;
                proxyReg = reg;
                break;
            }
        }

        if (proxyReg != null) {
            ModelNode proxyOp = operation.clone();
            proxyOp.get(OP_ADDR).set(proxyAddr.toModelNode());
            proxyOp.get(VALUE, OP_ADDR).set(translator.translateAddress(addr).toModelNode());
            ModelNode result = new ModelNode();

            context.addStep(result, proxyOp, proxyReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, VALIDATE_OPERATION), Stage.IMMEDIATE);
            if( context.completeStep() == ResultAction.ROLLBACK) {
                context.getFailureDescription().set(result.get(FAILURE_DESCRIPTION));
            }
        } else {
            try {
                new OperationValidator(context.getResourceRegistration(), false, false).validateOperation(op);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                context.getFailureDescription().set(e.getMessage());
            }
            context.completeStep();
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getValidateOperationDescription(locale);
    }
}
