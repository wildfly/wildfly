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
class AuditResourceDefinition extends SimpleResourceDefinition {

    static final AuditResourceDefinition INSTANCE = new AuditResourceDefinition();

    static final ListAttributeDefinition PROVIDER_MODULES = new LegacySupport.ProviderModulesAttributeDefinition(Constants.PROVIDER_MODULES, Constants.PROVIDER_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.PROVIDER_MODULE, PROVIDER_MODULES);

    private AuditResourceDefinition() {
        super(SecurityExtension.PATH_AUDIT_CLASSIC,
                SecurityExtension.getResourceDescriptionResolver(Constants.AUDIT),
                new AuditResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(PROVIDER_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.PROVIDER_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.PROVIDER_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new MappingProviderModuleDefinition(Constants.PROVIDER_MODULE));
    }

    static class AuditResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(PROVIDER_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }

}
