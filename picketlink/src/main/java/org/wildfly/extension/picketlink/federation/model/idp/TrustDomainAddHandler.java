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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;
import org.wildfly.extension.picketlink.federation.service.TrustDomainService;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.wildfly.extension.picketlink.federation.model.idp.TrustDomainResourceDefinition.validateModelInOperation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class TrustDomainAddHandler extends AbstractAddStepHandler {

    static final TrustDomainAddHandler INSTANCE = new TrustDomainAddHandler();

    static void launchServices(OperationContext context, PathAddress pathAddress, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        String identityProviderAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        String domainName = pathAddress.getLastElement().getValue();
        TrustDomainService service = new TrustDomainService(domainName);
        ServiceBuilder<TrustDomainService> serviceBuilder = context.getServiceTarget().addService(TrustDomainService.createServiceName(identityProviderAlias, domainName), service);

        serviceBuilder.addDependency(IdentityProviderService.createServiceName(identityProviderAlias), IdentityProviderService.class, service.getIdentityProviderService());

        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }

        ServiceController<TrustDomainService> controller = serviceBuilder.setInitialMode(ServiceController.Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();

        for (SimpleAttributeDefinition attribute : TrustDomainResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }

        validateModelInOperation(context, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                     ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        launchServices(context, PathAddress.pathAddress(operation.get(ADDRESS)), model, verificationHandler, newControllers);
    }

    @Override protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            TrustDomainRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }
}
