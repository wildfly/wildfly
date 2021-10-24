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

import org.jboss.as.controller.AbstractAddStepHandler;
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
class ClassicAuthenticationResourceDefinition extends SimpleResourceDefinition {

    public static final ClassicAuthenticationResourceDefinition INSTANCE = new ClassicAuthenticationResourceDefinition();

    public static final LegacySupport.LoginModulesAttributeDefinition LOGIN_MODULES = new LegacySupport.LoginModulesAttributeDefinition(Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.LOGIN_MODULE, LOGIN_MODULES);

    private ClassicAuthenticationResourceDefinition() {
        super(SecurityExtension.PATH_CLASSIC_AUTHENTICATION,
                SecurityExtension.getResourceDescriptionResolver(Constants.AUTHENTICATION + "." + Constants.CLASSIC),
                new ClassicAuthenticationResourceDefinitionAdd(), ModelOnlyRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
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

    static class ClassicAuthenticationResourceDefinitionAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            if (operation.hasDefined(LOGIN_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }

    }
}
