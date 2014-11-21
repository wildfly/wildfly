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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.picketlink.identity.federation.bindings.wildfly.idp.UndertowAttributeManager;
import org.picketlink.identity.federation.bindings.wildfly.idp.UndertowRoleGenerator;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.config.IDPConfiguration;
import org.wildfly.extension.picketlink.federation.model.AbstractEntityProviderAddHandler;
import org.wildfly.extension.picketlink.federation.service.FederationService;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;

import java.util.List;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityProviderAddHandler extends AbstractEntityProviderAddHandler {

    static final IdentityProviderAddHandler INSTANCE = new IdentityProviderAddHandler();

    private IdentityProviderAddHandler() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : IdentityProviderResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new IdentityProviderValidationStepHandler(), OperationContext.Stage.MODEL);
        super.execute(context, operation);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        ModelNode identityProviderNode = Resource.Tools.readModel(context.readResource(EMPTY_ADDRESS));
        launchServices(context, identityProviderNode, verificationHandler, newControllers, pathAddress);
    }

    static void launchServices(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, PathAddress pathAddress) throws OperationFailedException {
        String alias = pathAddress.getLastElement().getValue();
        IdentityProviderService service = new IdentityProviderService(toIDPConfig(context, model, alias));
        ServiceBuilder<IdentityProviderService> serviceBuilder = context.getServiceTarget().addService(IdentityProviderService.createServiceName(alias), service);
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();

        serviceBuilder.addDependency(FederationService.createServiceName(federationAlias), FederationService.class,
                                            service.getFederationService());

        configureHandler(context, model, service);

        IDPConfiguration configuration = service.getConfiguration();

        if (!configuration.isExternal()) {
            serviceBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(configuration.getSecurityDomain()));
        }

        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }

        ServiceController<IdentityProviderService> controller = serviceBuilder.install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    static IDPConfiguration toIDPConfig(OperationContext context, ModelNode fromModel, String alias) throws OperationFailedException {
        IDPConfiguration idpType = new IDPConfiguration(alias);

        boolean external = IdentityProviderResourceDefinition.EXTERNAL.resolveModelAttribute(context, fromModel).asBoolean();

        idpType.setExternal(external);

        String url = IdentityProviderResourceDefinition.URL.resolveModelAttribute(context, fromModel).asString();

        idpType.setIdentityURL(url);

        if (!idpType.isExternal()) {
            ModelNode securityDomain = IdentityProviderResourceDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, fromModel);

            if (securityDomain.isDefined()) {
                idpType.setSecurityDomain(securityDomain.asString());
            } else {
                throw ROOT_LOGGER.requiredAttribute(ModelElement.COMMON_SECURITY_DOMAIN.getName(), alias);
            }

            boolean supportsSignatures = IdentityProviderResourceDefinition.SUPPORT_SIGNATURES.resolveModelAttribute(context, fromModel).asBoolean();

            idpType.setSupportsSignature(supportsSignatures);

            boolean supportsMetadata = IdentityProviderResourceDefinition.SUPPORT_METADATA.resolveModelAttribute(context, fromModel).asBoolean();

            idpType.setSupportMetadata(supportsMetadata);

            boolean encrypt = IdentityProviderResourceDefinition.ENCRYPT.resolveModelAttribute(context, fromModel).asBoolean();

            idpType.setEncrypt(encrypt);

            boolean sslAuthentication = IdentityProviderResourceDefinition.SSL_AUTHENTICATION.resolveModelAttribute(context, fromModel).asBoolean();

            idpType.setSSLClientAuthentication(sslAuthentication);

            boolean strictPostBinding = IdentityProviderResourceDefinition.STRICT_POST_BINDING.resolveModelAttribute(context, fromModel).asBoolean();

            idpType.setStrictPostBinding(strictPostBinding);

            ModelNode roleGenerator = fromModel.get(ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR.getName());
            String roleGeneratorType;

            if (roleGenerator.isDefined()) {
                //TODO: resolve PLINK-
                ModelNode roleGeneratorValue = roleGenerator.asProperty().getValue();
                ModelNode classNameNode = RoleGeneratorResourceDefinition.CLASS_NAME.resolveModelAttribute(context, roleGeneratorValue);
                ModelNode codeNode = RoleGeneratorResourceDefinition.CODE.resolveModelAttribute(context, roleGeneratorValue);

                if (classNameNode.isDefined()) {
                    roleGeneratorType = classNameNode.asString();
                } else if (codeNode.isDefined()) {
                    roleGeneratorType = RoleGeneratorTypeEnum.forType(codeNode.asString());
                } else {
                    throw ROOT_LOGGER.typeNotProvided(IDENTITY_PROVIDER_ROLE_GENERATOR.getName());
                }
            } else {
                roleGeneratorType = UndertowRoleGenerator.class.getName();
            }

            idpType.setRoleGenerator(roleGeneratorType);

            ModelNode attributeManager = fromModel.get(ModelElement.IDENTITY_PROVIDER_ATTRIBUTE_MANAGER.getName());
            String attributeManagerType;

            if (attributeManager.isDefined()) {
                ModelNode attributeManagerValue = attributeManager.asProperty().getValue();
                ModelNode classNameNode = AttributeManagerResourceDefinition.CLASS_NAME.resolveModelAttribute(context, attributeManagerValue);
                ModelNode codeNode = AttributeManagerResourceDefinition.CODE.resolveModelAttribute(context, attributeManagerValue);

                if (classNameNode.isDefined()) {
                    attributeManagerType = classNameNode.asString();
                } else if (codeNode.isDefined()) {
                    attributeManagerType = AttributeManagerTypeEnum.forType(codeNode.asString());
                } else {
                    throw ROOT_LOGGER.typeNotProvided(IDENTITY_PROVIDER_ATTRIBUTE_MANAGER.getName());
                }
            } else {
                attributeManagerType = UndertowAttributeManager.class.getName();
            }

            idpType.setAttributeManager(attributeManagerType);
        }

        return idpType;
    }

    @Override protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            IdentityProviderRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }
}