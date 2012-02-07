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
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Jason T. Greene
 */
public class ACLResourceDefinition extends SimpleResourceDefinition {

    public static final ACLResourceDefinition INSTANCE = new ACLResourceDefinition();

    public static final ListAttributeDefinition ACL_MODULES = new LoginModulesAttributeDefinition(Constants.ACL_MODULES, Constants.ACL_MODULE);

    private ACLResourceDefinition() {
        super(PathElement.pathElement(Constants.ACL, Constants.CLASSIC),
                SecurityExtension.getResourceDescriptionResolver(Constants.ACL),
                ACLResourceDefinitionAdd.INSTANCE, new SecurityDomainReloadRemoveHandler());
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ACL_MODULES, null, new SecurityDomainReloadWriteHandler(ACL_MODULES));
    }

    static class ACLResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        static final ACLResourceDefinitionAdd INSTANCE = new ACLResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            ACL_MODULES.validateAndSet(operation, model);
        }

    }

}
