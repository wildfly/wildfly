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


import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
public class MappingResourceDefinition extends SimpleResourceDefinition {

    public static final MappingResourceDefinition INSTANCE = new MappingResourceDefinition();

    public static final ListAttributeDefinition MAPPING_MODULES = new LegacySupport.MappingModulesAttributeDefinition();
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.MAPPING_MODULE, MAPPING_MODULES);

    private MappingResourceDefinition() {
        super(SecurityExtension.PATH_MAPPING_CLASSIC,
                SecurityExtension.getResourceDescriptionResolver(Constants.MAPPING),
                LoginModuleStackResourceDefinitionAdd.INSTANCE, new SecurityDomainReloadRemoveHandler());
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(MAPPING_MODULES, new LegacySupport.LegacyModulesAttributeReader(Constants.MAPPING_MODULE), new LegacySupport.LegacyModulesAttributeWriter(Constants.MAPPING_MODULE));
    }

    static class LoginModuleStackResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        static final LoginModuleStackResourceDefinitionAdd INSTANCE = new LoginModuleStackResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        }

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.updateModel(context, operation);
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
