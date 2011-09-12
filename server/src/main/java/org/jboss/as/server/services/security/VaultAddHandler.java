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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.VaultDescriptions;
import org.jboss.as.controller.operations.validation.MapValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.vault.SecurityVault;

/**
 * Handler for the Vault
 *
 * @author Anil Saldhana
 * @author Brian Stansberry
 */
public class VaultAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    // code is an optional string
    static final ParameterValidator codeValidator = new ModelTypeValidator(ModelType.STRING, true);
    // vault-options are optional or could be an empty map, but any value must be a non-null string
    static final ParameterValidator optionsValidator = new MapValidator(new StringLengthValidator(1), true, 0, Integer.MAX_VALUE);

    public static final VaultAddHandler VAULT_INSTANCE = new VaultAddHandler();

    private ParametersValidator validator = new ParametersValidator();

    /**
     * Create the PathAddHandler
     */
    protected VaultAddHandler() {
     // code is an optional string
        validator.registerValidator(CODE, new ModelTypeValidator(ModelType.STRING, true));
        // vault-options are optional or could be an empty map, but any value must be a non-null string
        validator.registerValidator(VAULT_OPTIONS, new MapValidator(new StringLengthValidator(1), true, 0, Integer.MAX_VALUE));
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);

        model.get(CODE).set(operation.get(CODE));
        final ModelNode options = model.get(VAULT_OPTIONS);
        if (operation.hasDefined(VAULT_OPTIONS)) {
            for (Property prop : operation.get(VAULT_OPTIONS).asPropertyList()) {
                options.get(prop.getName()).set(prop.getValue());
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        final Map<String, Object> vaultOptions = new HashMap<String, Object>();
        String vaultClass = null;

        // Read from the model, as populateModel has stored the op values there
        if(model.hasDefined(CODE)){
            vaultClass = model.get(CODE).asString();
        }

        //Options
        if (model.hasDefined(VAULT_OPTIONS))  {
            for (Property vaultProp : model.get(VAULT_OPTIONS).asPropertyList()) {
                vaultOptions.put(vaultProp.getName(), vaultProp.getValue().asString());
            }
        }

        final String vaultFQN = vaultClass;

        SecurityVaultService vaultService = new SecurityVaultService(vaultFQN, vaultOptions);

        final ServiceTarget target = context.getServiceTarget();

        ServiceBuilder<SecurityVault> builder = target.addService(SecurityVaultService.SERVICE_NAME, vaultService);

        newControllers.add(builder.addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return VaultDescriptions.getVaultAddDescription(locale);
    }


    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.HOST || context.getType() == OperationContext.Type.SERVER;
    }
}