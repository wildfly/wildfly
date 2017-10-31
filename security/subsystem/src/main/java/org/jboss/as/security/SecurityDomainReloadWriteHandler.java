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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Jason T. Greene
 */
public class SecurityDomainReloadWriteHandler extends RestartParentWriteAttributeHandler {

    protected SecurityDomainReloadWriteHandler(final AttributeDefinition ... definition) {
        super(Constants.SECURITY_DOMAIN, definition);
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        String domainName = parentAddress.getLastElement().getValue();
        SecurityDomainAdd.INSTANCE.launchServices(context, domainName, parentModel);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return SecurityDomainResourceDefinition.getSecurityDomainServiceName(parentAddress);
    }

    // TODO: Remove this method once WFCORE-3055 and WFCORE-3056 are fixed
    @Override
    protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
        SecurityDomainResourceDefinition.CACHE_TYPE.validateOperation(model.getModel());
    }
}
