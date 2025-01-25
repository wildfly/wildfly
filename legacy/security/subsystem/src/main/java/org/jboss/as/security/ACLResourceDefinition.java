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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
class ACLResourceDefinition extends SimpleResourceDefinition {

    public static final ACLResourceDefinition INSTANCE = new ACLResourceDefinition();

    public static final ListAttributeDefinition ACL_MODULES = new LegacySupport.LoginModulesAttributeDefinition(Constants.ACL_MODULES, Constants.ACL_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.ACL_MODULE, ACL_MODULES);

    private ACLResourceDefinition() {
        super(SecurityExtension.ACL_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.ACL),
                new ACLResourceDefinitionAdd(),
                ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ACL_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.ACL_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.ACL_MODULE));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        ManagementResourceRegistration moduleReg = resourceRegistration.registerSubModel(new LoginModuleResourceDefinition(Constants.ACL_MODULE));

        //https://issues.jboss.org/browse/WFLY-2474 acl-module was wrongly called login-module in 7.2.0
        resourceRegistration.registerAlias(
                PathElement.pathElement(Constants.LOGIN_MODULE),
                new AliasEntry(moduleReg) {
                    @Override
                    public PathAddress convertToTargetAddress(PathAddress address, AliasContext aliasContext) {
                        PathElement element = address.getLastElement();
                        element = PathElement.pathElement(Constants.ACL_MODULE, element.getValue());
                        return address.subAddress(0, address.size() -1).append(element);
                    }
                });

    }

    static class ACLResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(ACL_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }

}
