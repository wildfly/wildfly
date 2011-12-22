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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Attribute write handler for a child resource of a management security realm.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SecurityRealmChildWriteAttributeHandler extends RestartParentWriteAttributeHandler {

    private final AttributeDefinition[] attributeDefinitions;

    public SecurityRealmChildWriteAttributeHandler(AttributeDefinition... attributes) {
        super(ModelDescriptionConstants.SECURITY_REALM, attributes);
        this.attributeDefinitions = attributes;
    }

    void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : attributeDefinitions) {
            resourceRegistration.registerReadWriteAttribute(attr, null, this);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected boolean isResourceServiceRestartAllowed(OperationContext context, ServiceController<?> service) {
        return !ManagementUtil.isSecurityRealmReloadRequired(context, service);
    }

    @Override
    protected void removeServices(OperationContext context, ServiceName parentService, ModelNode parentModel) throws OperationFailedException {
        SecurityRealmRemoveHandler.INSTANCE.removeServices(context, parentService.getSimpleName(), parentModel);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                                         ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        SecurityRealmAddHandler.INSTANCE.installServices(context, parentAddress.getLastElement().getValue(), parentModel, verificationHandler, null);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        final String realmName = parentAddress.getLastElement().getValue();
        return SecurityRealmService.BASE_SERVICE_NAME.append(realmName);
    }
}
