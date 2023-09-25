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
class AuthorizationResourceDefinition extends SimpleResourceDefinition {

    public static final AuthorizationResourceDefinition INSTANCE = new AuthorizationResourceDefinition();

    public static final ListAttributeDefinition POLICY_MODULES = new LegacySupport.LoginModulesAttributeDefinition(Constants.POLICY_MODULES, Constants.POLICY_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.POLICY_MODULE, POLICY_MODULES);

    private AuthorizationResourceDefinition() {
        super(SecurityExtension.PATH_AUTHORIZATION_CLASSIC,
                SecurityExtension.getResourceDescriptionResolver(Constants.AUTHORIZATION),
                new AuthorizationResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(POLICY_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.POLICY_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.POLICY_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new LoginModuleResourceDefinition(Constants.POLICY_MODULE));
    }

    static class AuthorizationResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(POLICY_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }

}
