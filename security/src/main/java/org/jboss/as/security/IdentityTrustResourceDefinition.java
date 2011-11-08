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
public class IdentityTrustResourceDefinition extends SimpleResourceDefinition {

    public static final IdentityTrustResourceDefinition INSTANCE = new IdentityTrustResourceDefinition();

    public static final ListAttributeDefinition TRUST_MODULES = new LoginModulesAttributeDefinition(Constants.TRUST_MODULES, Constants.TRUST_MODULE);

    private IdentityTrustResourceDefinition() {
        super(PathElement.pathElement(Constants.IDENTITY_TRUST, Constants.CLASSIC),
                SecurityExtension.getResourceDescriptionResolver(Constants.IDENTITY_TRUST),
                new IdentityTrustResourceDefinitionAdd(), new SecurityDomainReloadRemoveHandler());
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(TRUST_MODULES, null, new SecurityDomainReloadWriteHandler(TRUST_MODULES));
    }

    static class IdentityTrustResourceDefinitionAdd extends SecurityDomainReloadAddHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            TRUST_MODULES.validateAndSet(operation, model);
        }

    }

}
