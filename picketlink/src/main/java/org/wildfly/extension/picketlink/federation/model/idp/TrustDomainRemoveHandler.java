/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.federation.service.TrustDomainService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class TrustDomainRemoveHandler extends AbstractRemoveStepHandler {

    static final TrustDomainRemoveHandler INSTANCE = new TrustDomainRemoveHandler();

    private TrustDomainRemoveHandler() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        String identityProviderAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        String domainName = pathAddress.getLastElement().getValue();

        context.removeService(TrustDomainService.createServiceName(identityProviderAlias, domainName));
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        TrustDomainAddHandler.launchServices(context, PathAddress.pathAddress(operation.get(ADDRESS)), model, null, null);
    }
}
