/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import static org.jboss.as.security.Constants.AUTH_MODULE;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
class JASPIAuthenticationResourceDefinition extends SimpleResourceDefinition {

    public static final JASPIAuthenticationResourceDefinition INSTANCE = new JASPIAuthenticationResourceDefinition();

    public static final ListAttributeDefinition AUTH_MODULES = new LegacySupport.JASPIAuthenticationModulesAttributeDefinition();
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.AUTH_MODULE, AUTH_MODULES);

    private JASPIAuthenticationResourceDefinition() {
        super(SecurityExtension.PATH_JASPI_AUTH,
                SecurityExtension.getResourceDescriptionResolver(Constants.AUTHENTICATION + "." + Constants.JASPI),
                new JASPIAuthenticationResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(AUTH_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.AUTH_MODULE), new LegacySupport.LegacyModulesAttributeWriter(AUTH_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new JASPIMappingModuleDefinition());
        resourceRegistration.registerSubModel(LoginModuleStackResourceDefinition.INSTANCE);
    }

    static class JASPIAuthenticationResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(AUTH_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }
}
