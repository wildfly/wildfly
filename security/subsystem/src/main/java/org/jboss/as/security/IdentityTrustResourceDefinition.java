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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
class IdentityTrustResourceDefinition extends SimpleResourceDefinition {

    public static final IdentityTrustResourceDefinition INSTANCE = new IdentityTrustResourceDefinition();

    public static final ListAttributeDefinition TRUST_MODULES = new LegacySupport.LoginModulesAttributeDefinition(Constants.TRUST_MODULES, Constants.TRUST_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.TRUST_MODULE, TRUST_MODULES);

    private IdentityTrustResourceDefinition() {
        super(PathElement.pathElement(Constants.IDENTITY_TRUST, Constants.CLASSIC),
                SecurityExtension.getResourceDescriptionResolver(Constants.IDENTITY_TRUST),
                new IdentityTrustResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(TRUST_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.TRUST_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.TRUST_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new LoginModuleResourceDefinition(Constants.TRUST_MODULE));
    }

    static class IdentityTrustResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(TRUST_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }
}
