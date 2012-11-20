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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
public class JASPIAuthenticationResourceDefinition extends SimpleResourceDefinition {

    public static final JASPIAuthenticationResourceDefinition INSTANCE = new JASPIAuthenticationResourceDefinition();

    //public static final ListAttributeDefinition AUTH_MODULES = new JASPIAuthenticationModulesAttributeDefinition();

    private JASPIAuthenticationResourceDefinition() {
        super(PathElement.pathElement(Constants.AUTHENTICATION, Constants.JASPI),
                SecurityExtension.getResourceDescriptionResolver(Constants.AUTHENTICATION + "." + Constants.JASPI),
                JASPIAuthenticationResourceDefinitionAdd.INSTANCE, new SecurityDomainReloadRemoveHandler());
    }

    /*public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(AUTH_MODULES, null, new SecurityDomainReloadWriteHandler(AUTH_MODULES));
    }*/

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new JASPIMappingModuleDefinition(Constants.AUTH_MODULE));
    }

    static class JASPIAuthenticationResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        static final JASPIAuthenticationResourceDefinitionAdd INSTANCE = new JASPIAuthenticationResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            //AUTH_MODULES.validateAndSet(operation, model);
        }

    }
}
