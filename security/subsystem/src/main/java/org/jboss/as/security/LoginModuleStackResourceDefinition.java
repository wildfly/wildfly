/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

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
class LoginModuleStackResourceDefinition extends SimpleResourceDefinition {

    static final LoginModuleStackResourceDefinition INSTANCE = new LoginModuleStackResourceDefinition();

    static final ListAttributeDefinition LOGIN_MODULES = new LegacySupport.LoginModulesAttributeDefinition(Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.LOGIN_MODULE, LOGIN_MODULES);

    private LoginModuleStackResourceDefinition() {
        super(SecurityExtension.PATH_LOGIN_MODULE_STACK,
                SecurityExtension.getResourceDescriptionResolver(Constants.LOGIN_MODULE_STACK),
                new LoginModuleStackResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(LOGIN_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.LOGIN_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.LOGIN_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new LoginModuleResourceDefinition(Constants.LOGIN_MODULE));
    }

    static class LoginModuleStackResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(LOGIN_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }
}
