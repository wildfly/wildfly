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

import static org.jboss.as.security.Constants.AUTH_MODULE;

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
public class JASPIAuthenticationResourceDefinition extends SimpleResourceDefinition {

    public static final JASPIAuthenticationResourceDefinition INSTANCE = new JASPIAuthenticationResourceDefinition();

    public static final ListAttributeDefinition AUTH_MODULES = new LegacySupport.JASPIAuthenticationModulesAttributeDefinition();
    private static final OperationStepHandler LEGACY_ADD_HANDLER = new LegacySupport.LegacyModulesConverter(Constants.AUTH_MODULE, AUTH_MODULES);

    private JASPIAuthenticationResourceDefinition() {
        super(SecurityExtension.PATH_JASPI_AUTH,
                SecurityExtension.getResourceDescriptionResolver(Constants.AUTHENTICATION + "." + Constants.JASPI),
                new JASPIAuthenticationResourceDefinitionAdd(), new SecurityDomainReloadRemoveHandler());
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

    static class JASPIAuthenticationResourceDefinitionAdd extends SecurityDomainReloadAddHandler {

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.updateModel(context, operation);
            if (operation.hasDefined(AUTH_MODULES.getName())) {
                context.addStep(new ModelNode(), operation, LEGACY_ADD_HANDLER, OperationContext.Stage.MODEL, true);
            }
        }
    }
}
