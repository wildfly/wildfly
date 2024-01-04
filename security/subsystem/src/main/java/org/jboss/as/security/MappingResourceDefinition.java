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
class MappingResourceDefinition extends SimpleResourceDefinition {

    public static final MappingResourceDefinition INSTANCE = new MappingResourceDefinition();

    public static final ListAttributeDefinition MAPPING_MODULES = new LegacySupport.MappingModulesAttributeDefinition();
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.MAPPING_MODULE, MAPPING_MODULES);

    private MappingResourceDefinition() {
        super(SecurityExtension.PATH_MAPPING_CLASSIC,
                SecurityExtension.getResourceDescriptionResolver(Constants.MAPPING),
                new LoginModuleStackResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(MAPPING_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.MAPPING_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.MAPPING_MODULE));
    }

    static class LoginModuleStackResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(MAPPING_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new MappingModuleDefinition(Constants.MAPPING_MODULE));
    }

}
