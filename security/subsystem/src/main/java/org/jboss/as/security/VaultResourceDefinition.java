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
package org.jboss.as.security;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.security.service.SecurityVaultService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * @author Jason T. Greene
 */
public class VaultResourceDefinition extends SimpleResourceDefinition {

    public static final VaultResourceDefinition INSTANCE = new VaultResourceDefinition();

    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(Constants.CODE, ModelType.STRING, true)
                    .build();

    public static final PropertiesAttributeDefinition OPTIONS = new PropertiesAttributeDefinition.Builder(Constants.VAULT_OPTIONS, true)
            .setXmlName(Constants.VAULT_OPTION)
            .setAllowExpression(true)
            .build();


    private VaultResourceDefinition() {
        super(SecurityExtension.VAULT_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.VAULT),
                VaultResourceDefinitionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(OPTIONS, null, new ReloadRequiredWriteAttributeHandler(OPTIONS));
        resourceRegistration.registerReadWriteAttribute(CODE, null, new ReloadRequiredWriteAttributeHandler(CODE));
    }

    static class VaultResourceDefinitionAdd extends AbstractBoottimeAddStepHandler {
        static final VaultResourceDefinitionAdd INSTANCE = new VaultResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            CODE.validateAndSet(operation, model);
            OPTIONS.validateAndSet(operation, model);
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            Map<String, Object> vaultOptions = new HashMap<String, Object>();
            ModelNode vaultClassNode = CODE.resolveModelAttribute(context, model);
            String vaultClass = vaultClassNode.getType() == ModelType.UNDEFINED ? null : vaultClassNode.asString();

            if (operation.hasDefined(Constants.VAULT_OPTIONS)) {
                for (Map.Entry<String,String> vaultOption : OPTIONS.unwrap(context,model).entrySet()) {
                    vaultOptions.put(vaultOption.getKey(), vaultOption.getValue());
                }
            }
            // add security vault service
            if (vaultClass != null || !vaultOptions.isEmpty()) {
                final SecurityVaultService vaultService = new SecurityVaultService(vaultClass, vaultOptions);
                context.getServiceTarget()
                        .addService(SecurityVaultService.SERVICE_NAME, vaultService)
                        .setInitialMode(ServiceController.Mode.ACTIVE).install();
            }

        }
    }

}
