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

package org.wildfly.extension.picketlink.federation.model.sp;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.extension.picketlink.federation.config.SPConfiguration;
import org.wildfly.extension.picketlink.federation.model.AbstractEntityProviderAddHandler;
import org.wildfly.extension.picketlink.federation.service.FederationService;
import org.wildfly.extension.picketlink.federation.service.ServiceProviderService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class ServiceProviderAddHandler extends AbstractEntityProviderAddHandler {

    public static final ServiceProviderAddHandler INSTANCE = new ServiceProviderAddHandler();

    private ServiceProviderAddHandler() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : ServiceProviderResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        super.execute(context, operation);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        ModelNode serviceProviderNode = Resource.Tools.readModel(context.readResource(EMPTY_ADDRESS));
        launchService(context, pathAddress, serviceProviderNode);
    }

    static void launchService(OperationContext context, PathAddress pathAddress, ModelNode model) throws OperationFailedException {
        String alias = pathAddress.getLastElement().getValue();
        ServiceProviderService service = new ServiceProviderService(toSPConfig(context, model, alias));
        ServiceBuilder<ServiceProviderService> serviceBuilder = context.getServiceTarget().addService(ServiceProviderService.createServiceName(alias), service);
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();

        serviceBuilder.addDependency(FederationService.createServiceName(federationAlias),
            FederationService.class,service.getFederationService());

        configureHandler(context, model, service);

        SPConfiguration configuration = service.getConfiguration();

        serviceBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(configuration.getSecurityDomain()));

        serviceBuilder.install();
    }

    static SPConfiguration toSPConfig(OperationContext context, ModelNode fromModel, String alias) throws OperationFailedException {
        SPConfiguration spType = new SPConfiguration(alias);

        String url = ServiceProviderResourceDefinition.URL.resolveModelAttribute(context, fromModel).asString();

        spType.setServiceURL(url);

        String securityDomain = ServiceProviderResourceDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, fromModel).asString();

        spType.setSecurityDomain(securityDomain);

        boolean postBinding = ServiceProviderResourceDefinition.POST_BINDING.resolveModelAttribute(context, fromModel).asBoolean();

        spType.setPostBinding(postBinding);

        boolean supportsSignatures = ServiceProviderResourceDefinition.SUPPORT_SIGNATURES.resolveModelAttribute(context, fromModel).asBoolean();

        spType.setSupportsSignature(supportsSignatures);

        boolean supportsMetadata = ServiceProviderResourceDefinition.SUPPORT_METADATA.resolveModelAttribute(context, fromModel).asBoolean();

        spType.setSupportMetadata(supportsMetadata);

        boolean strictPostBinding = ServiceProviderResourceDefinition.STRICT_POST_BINDING.resolveModelAttribute(context, fromModel).asBoolean();

        spType.setIdpUsesPostBinding(strictPostBinding);

        String errorPage = ServiceProviderResourceDefinition.ERROR_PAGE.resolveModelAttribute(context, fromModel).asString();

        spType.setErrorPage(errorPage);

        String logoutPage = ServiceProviderResourceDefinition.LOGOUT_PAGE.resolveModelAttribute(context, fromModel).asString();

        spType.setLogOutPage(logoutPage);

        return spType;
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            ServiceProviderRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }
}
