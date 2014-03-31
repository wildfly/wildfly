/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.services.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.operations.SystemPropertyDeferredProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for the Vault
 *
 * @author Anil Saldhana
 * @author Brian Stansberry
 */
public class VaultAddHandler extends AbstractAddStepHandler {

    private final AbstractVaultReader vaultReader;

    /**
     * Create the PathAddHandler
     */
    public VaultAddHandler(AbstractVaultReader vaultReader) {
        this.vaultReader = vaultReader;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : VaultResourceDefinition.ALL_ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
        if (model.hasDefined(VaultResourceDefinition.MODULE.getName()) && !model.hasDefined(VaultResourceDefinition.CODE.getName())){
            throw ServerMessages.MESSAGES.vaultModuleWithNoCode();
        }

    }


    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode codeNode = VaultResourceDefinition.CODE.resolveModelAttribute(context, model);
        ModelNode moduleNode = VaultResourceDefinition.MODULE.resolveModelAttribute(context, model);
        String vaultClass = codeNode.isDefined() ? codeNode.asString() : null;
        String module = moduleNode.isDefined() ? moduleNode.asString() : null;

        if (vaultReader != null) {
            final Map<String, Object> vaultOptions = new HashMap<String, Object>();
            if (operation.hasDefined(VaultResourceDefinition.VAULT_OPTIONS.getName())) {
                for (Map.Entry<String, String> vaultOption : VaultResourceDefinition.VAULT_OPTIONS.unwrap(context, model).entrySet()) {
                    vaultOptions.put(vaultOption.getKey(), vaultOption.getValue());
                }
            }
            try {
                vaultReader.createVault(vaultClass, module, vaultOptions);
            } catch (VaultReaderException e) {
                throw ServerMessages.MESSAGES.cannotCreateVault(e, e);
            }

            // WFLY-1904 if any system properties were not resolved due to needing vault resolution,
            // resolve them now
            final SystemPropertyDeferredProcessor deferredResolver = context.getAttachment(SystemPropertyDeferredProcessor.ATTACHMENT_KEY);
            if (deferredResolver != null) {
                deferredResolver.processDeferredProperties(context);
            }
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        vaultReader.destroyVault();
    }
}
