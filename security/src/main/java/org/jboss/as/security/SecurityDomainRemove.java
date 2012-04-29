/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;

/**
 * Remove a security domain configuration.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Brian Stansberry
 */
class SecurityDomainRemove extends AbstractRemoveStepHandler {

    static final String OPERATION_NAME = REMOVE;

    static final SecurityDomainRemove INSTANCE = new SecurityDomainRemove();

    /**
     * Private to ensure a singleton.
     */
    private SecurityDomainRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String securityDomain = address.getLastElement().getValue();
        context.removeService(SecurityDomainService.SERVICE_NAME.append(securityDomain));
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String securityDomain = address.getLastElement().getValue();
        final String cacheType = SecurityDomainAdd.getAuthenticationCacheType(operation);
        SecurityDomainAdd.INSTANCE.launchServices(context, securityDomain, model);
    }
}
